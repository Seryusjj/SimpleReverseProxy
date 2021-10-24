package upstream;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public abstract class UpstreamChannelFactory {

    private static final Logger logger = LoggerFactory.getLogger(UpstreamChannelFactory.class);

    public ChannelFuture CreateOutboundChannel(Channel inboundChannel,
                                               String targetHost) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(inboundChannel.eventLoop())
                .channel(inboundChannel.getClass())
                .handler(new UpStreamChannelInitializer(inboundChannel))
                .option(ChannelOption.AUTO_READ, false);


        // get one of the host that are configured for such service
        InetSocketAddress socketAddress = getUpstreamHost(targetHost);
        if (socketAddress == null) {
            // something went wrong
            logger.error(String.format("Cannot find upstream host for request: %s", targetHost));
            return null;
        }

        // connect to the upstream host
        return bootstrap.connect(socketAddress);
    }

    public  abstract InetSocketAddress getUpstreamHost(String targetHost);
}
