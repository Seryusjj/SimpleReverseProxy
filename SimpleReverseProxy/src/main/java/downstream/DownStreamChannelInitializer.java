package downstream;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import upstream.UpstreamChannelFactory;


public class DownStreamChannelInitializer extends ChannelInitializer<Channel> {


	private final UpstreamChannelFactory channelFactory;

	public DownStreamChannelInitializer(UpstreamChannelFactory channelFactory) {
		this.channelFactory = channelFactory;
	}

	@Override
	protected void initChannel(Channel ch) {
		ChannelPipeline pipeline = ch.pipeline();
		pipeline.addLast(new HttpServerCodec());
		pipeline.addLast(new HttpObjectAggregator(512 * 1024, true));
		pipeline.addLast(new DownStreamHandler(channelFactory));
	}

}



