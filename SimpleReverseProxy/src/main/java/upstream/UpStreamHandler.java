package upstream;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is meant to process the response from the upstream service and forward it back
 * to the original channel from {@link downstream.DownStreamHandler DownStreamHandler}
 */
public class UpStreamHandler extends SimpleChannelInboundHandler<FullHttpMessage> {

    private final Channel downstream;
    private static final Logger logger = LoggerFactory.getLogger(UpStreamHandler.class);

    public UpStreamHandler(Channel inChannel) {
        this.downstream = inChannel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.read();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpMessage msg) {
        logger.info("http response from upstream received, forwarding it back");
        // write response back to downstream channel and close it
        downstream.writeAndFlush(msg.retain()).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
         logger.debug("closing upstream handler");
         super.channelInactive(ctx);
    }
    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        logger.debug("upstream handler, done, forwarding to next step");
        super.channelWritabilityChanged(ctx);
    }

}