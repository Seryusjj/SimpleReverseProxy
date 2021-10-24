package upstream;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;

import java.net.InetSocketAddress;

public abstract class UpstreamChannelFactory {

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
            return null;
        }

        // connect to the upstream host
        return bootstrap.connect(socketAddress);
    }

    public  abstract InetSocketAddress getUpstreamHost(String targetHost);
}
