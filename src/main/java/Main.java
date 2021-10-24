import conf.SProxyConf;
import core.SProxy;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class Main {



    //based on https://github.com/asarkar/kotlin/tree/master/netty-learning/proxy/src/main/kotlin

    private static final Logger logger = LoggerFactory.getLogger(SProxy.class);

    public static void main(String[] args) {

        String file = "";
        if (args==null)
             file = ClassLoader.getSystemClassLoader().getResource("configuration.yaml").getFile();
        else
            file = Arrays.stream(args).findFirst().orElse(file);

        SProxy sproxy = new SProxy();
        try {
            SProxyConf config = SProxyConf.parse(file);
            var ch = sproxy.start(config.proxy);
            logger.info("Proxy listening");
            // wait for channel to close, this will lock current thread and keep server active
            ch.closeFuture().sync();
            logger.info("Proxy off");
        } catch (Exception e) {
            logger.error("Invalid config, exiting abnormally", e);
            System.err.println("Invalid config, exiting abnormally");
            System.exit(2);
        } finally {
            sproxy.stop();
        }
    }


}
