package upstream;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;

public class UpStreamChannelInitializer extends ChannelInitializer<Channel> {


    private final Channel inChannel;

    public UpStreamChannelInitializer(Channel inChannel) {
        this.inChannel = inChannel;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new HttpClientCodec());
        pipeline.addLast(new HttpObjectAggregator(512 * 1024, true));
        pipeline.addLast(new UpStreamHandler(inChannel));
    }

}
