package io.reactivej;

import com.google.common.collect.Sets;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/***
 * @author heartup@gmail.com
 */
public class NettyTransporter extends AbstractTransporter {

    private class RemoteWorkerChannelCloseListener implements ChannelFutureListener {

        private String peerId;

        public RemoteWorkerChannelCloseListener(String peerId) {
            this.peerId = peerId;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            logger.info(future.channel() + " close");
            future.removeListener(this);
            channels.remove(peerId, future.channel());
        }
    }

    private static Logger logger = LoggerFactory.getLogger(NettyTransporter.class);

    private Channel acceptChannel;
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;
    private Set<Channel> inboundChannels = Sets.newConcurrentHashSet();

    private EventLoopGroup eventLoop = new NioEventLoopGroup();
    private ConcurrentHashMap<String, Channel> channels = new ConcurrentHashMap<>();

    public NettyTransporter(ReactiveSystem system) {
        super(system);
        init();
    }

    public void init() {
        logger.info("netty transporter init...");

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch)
                                throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(
                                    new ObjectEncoder(),
                                    new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                                    new NettyEnvelopeHandler(NettyTransporter.this));
                        }
                    });

            acceptChannel = b.bind(getSystem().getPort()).sync().channel();
        } catch (InterruptedException e) {
            logger.error("exception in netty transporter init", e);
            System.exit(1);
        }
    }

    public Set<Channel> getInboundChannels() {
        return inboundChannels;
    }

    private Channel connect(String host, int port) {
        String peerId = host + ":" + port;
        Channel channel = channels.get(peerId);
        if (channel != null)
            return channel;

        try {
            Bootstrap b = new Bootstrap();
            b.group(eventLoop).channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch)
                                throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(
                                    new ObjectEncoder(),
                                    new ObjectDecoder(ClassResolvers
                                            .cacheDisabled(null)));
                        }
                    });

            Channel workerChannel = b.connect(host, port).sync().channel();
            workerChannel.closeFuture().addListener(new RemoteWorkerChannelCloseListener(peerId));
            Channel existed = channels.putIfAbsent(peerId, workerChannel);
            if (existed != null) {
                workerChannel.close().sync();
                return existed;
            }
            else {
                return workerChannel;
            }
        } catch (InterruptedException e) {
            System.exit(1);
        }
        return null;
    }

    public void stop() {
        eventLoop.shutdownGracefully();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    @Override
    public void send(String host, int port, Envelope envlop) {
        try {
            String peerId = host + ":" + port;

            Channel channel = channels.get(peerId);
            if (channel == null) {
                channel = connect(host, port);
            }

            channel.writeAndFlush(envlop);
        }
        catch (Exception e) {
            throw new MessageCannotSend(envlop, e);
        }
    }

    public void suspendReadingMessage() {
        logger.info("**********pause receive message*************");
        for (Channel c : getInboundChannels()) {
            c.config().setAutoRead(false);
        }
    }

    public void resumeReadingMessage() {
        logger.info("*************resume receive message**************");
        for (Channel c : getInboundChannels()) {
            c.config().setAutoRead(true);
        }
    }
}
