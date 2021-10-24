package downstream;

import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import upstream.UpstreamChannelFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class DownStreamHandler extends SimpleChannelInboundHandler<FullHttpRequest> {


    private final UpstreamChannelFactory channelFactory;
    private Channel inboundChannel;

    static AtomicInteger counter = new AtomicInteger(0);


    private ChannelFuture upstreamFuture;

    public DownStreamHandler(UpstreamChannelFactory channelFactory) {
        this.channelFactory = channelFactory;
    }


    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        inboundChannel = ctx.channel();
        ctx.read();
        // System.out.println(String.format("Simultaneous request %d", counter.incrementAndGet()));
        super.channelActive(ctx);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // System.out.println(String.format("Request done, remaining requests %d", counter.decrementAndGet()));
        if (upstreamFuture != null) {
            upstreamFuture.channel().close();
        }
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        var host = msg.headers().get(HttpHeaderNames.HOST);

        // keep message until we are done with it otherwise by the time we reach
        // line upstream.channel().writeAndFlush(msg) it will be released
        msg.retain();

        // open upstream connection
        upstreamFuture = channelFactory.CreateOutboundChannel(inboundChannel, host);
        if (upstreamFuture == null) {
            msg.release();
            inboundChannel.close();
        }

        upstreamFuture.addListener((ChannelFutureListener) upstream -> {
            if (upstream.isSuccess() && upstream.channel().isActive()) {
                // send http request to upstream channel this will release msg already
                upstream.channel().writeAndFlush(msg);
            } else {
                // if something goes wrong maybe the message is not released that's why we do it here
                msg.release();
                inboundChannel.close();
            }
        });
    }
}
