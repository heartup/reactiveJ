package io.reactivej;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***
 * @author heartup@gmail.com
 */
public class NettyEnvelopeHandler extends ChannelInboundHandlerAdapter {

    private static Logger logger = LoggerFactory.getLogger(NettyEnvelopeHandler.class);

    private NettyTransporter transporter = null;


    public NettyEnvelopeHandler(NettyTransporter transporter) {
        this.transporter = transporter;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        transporter.getInboundChannels().add(ctx.channel());
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        transporter.getInboundChannels().remove(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Envelope) {
            transporter.receive((Envelope) msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
