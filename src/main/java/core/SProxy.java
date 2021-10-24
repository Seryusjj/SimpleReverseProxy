package core;

import conf.SProxyConf;
import downstream.DownStreamChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import upstream.UpstreamChannelCircularFactory;

/**
 * A simple reverse proxy implementation using netty library
 */
public class SProxy {
    private Logger logger = LoggerFactory.getLogger(SProxy.class);

    public SProxy() {

    }

    EventLoopGroup bossGroup = Connector.newEventLoopGroup(1, new DefaultThreadFactory("SProxy-Boss-Thread"));
    EventLoopGroup workerGroup = Connector.newEventLoopGroup(3, new DefaultThreadFactory("SProxy-Downstream-Worker-Thread"));

    public Channel start(SProxyConf.ProxyConfig config) {

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(Connector.serverChannelClass())
                // UpstreamChannelCircularFactory can be replaced by a round-robin factory
                .childHandler(new DownStreamChannelInitializer(new UpstreamChannelCircularFactory(config)))
                .childOption(ChannelOption.AUTO_READ, false)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
        ;


        Channel channel = null;
        try {
            channel = b.bind(config.listen.port).sync().channel();
        } catch (InterruptedException e) {
            System.out.println("Exception caught");
        }
        return channel;
    }

    public void stop() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
