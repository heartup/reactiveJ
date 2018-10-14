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

/**
 * Created by heartup@gmail.com on 3/25/16.
 */
public class NettyTransporter extends AbstractTransporter {

    private class RemoteWorkerChannelCloseListener implements ChannelFutureListener {

        private String peerId;

        public RemoteWorkerChannelCloseListener(String peerId) {
            this.peerId = peerId;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            logger.info(future.channel() + "连接关闭");
            if (future.cause() != null) {
                future.cause().printStackTrace();
            }
            future.removeListener(this);
            // remove existly this one
            channels.remove(peerId, future.channel());
        }
    }

    private static Logger logger = LoggerFactory.getLogger(NettyTransporter.class);

    // server side
    private Channel acceptChannel;
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;
    private Set<Channel> inboundChannels = Sets.newConcurrentHashSet();

    // client side
    private EventLoopGroup eventLoop = new NioEventLoopGroup();
    private ConcurrentHashMap<String, Channel> channels = new ConcurrentHashMap<>();

    public NettyTransporter(ReactiveSystem system) {
        super(system);
        init();
    }

    public void init() {
        logger.info("NettyTransporter初始化...");

        // Configure the server.
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
                                    new ObjectDecoder(ClassResolvers.cacheDisabled(getSystem().getSystemClassLoader())),
                                    new NettyEnvelopeHandler(NettyTransporter.this));
                        }
                    });

            // Start the server.
            acceptChannel = b.bind(getSystem().getPort()).sync().channel();
        } catch (InterruptedException e) {
            logger.error("初始化NettyTransporter发生异常", e);
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

        logger.info("开始创建到{}:{}的连接", host, port);
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
                                            .cacheDisabled(getSystem().getSystemClassLoader())));
                        }
                    });

            // Make the connection attempt.
            Channel workerChannel = b.connect(host, port).sync().channel();
            workerChannel.closeFuture().addListener(new RemoteWorkerChannelCloseListener(peerId));
            Channel existed = channels.putIfAbsent(peerId, workerChannel);
            if (existed != null) {
                workerChannel.close().sync();
                logger.info("重复利用到{}:{}的连接", host, port);
                return existed;
            }
            else {
                logger.info("成功创建到{}:{}的连接", host, port);
                return workerChannel;
            }
        } catch (InterruptedException e) {
            logger.error("创建到" + host + ":" + port + "的连接的时候发生异常", e);
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
        logger.info("**********暂停接受消息*************");
        for (Channel c : getInboundChannels()) {
            c.config().setAutoRead(false);
        }
    }

    public void resumeReadingMessage() {
        logger.info("*************继续接受消息**************");
        for (Channel c : getInboundChannels()) {
            c.config().setAutoRead(true);
        }
    }
}
