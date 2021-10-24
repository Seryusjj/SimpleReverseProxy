package downstream;

import core.SProxy;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import upstream.UpstreamChannelFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class will handle the http request from the client and forward them to
 * {@link upstream.UpStreamHandler UpStreamHandler}
 *
 */
public class DownStreamHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger logger = LoggerFactory.getLogger(DownStreamHandler.class);
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
        logger.info(String.format("Active downstream request %d", counter.incrementAndGet()));
        super.channelActive(ctx);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        logger.debug("Downstream handler done, forwarding to next step");
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info(String.format("Closing downstream handler, remaining requests %d", counter.decrementAndGet()));
        if (upstreamFuture != null) {
            logger.info("Closing associated upstream handler");
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
