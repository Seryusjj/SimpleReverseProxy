import conf.SProxyConf;
import core.SProxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * A simple proxy app based on
 * //based on https://github.com/spccold/Xproxy/blob/master/src/main/java/xproxy/downstream/XproxyDownStreamChannelInitializer.java
 *
 * This does not have a lot of features like error management and so on, just the "happy path" is tested
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static String defaultConfigFileLocation() throws IOException {
        var rs = new File(".").getCanonicalPath();
        return rs+"/configuration.yaml";
    }

    public static void main(String[] args) {
        String file = "";
        SProxy sproxy = new SProxy();
        try {
            if (args == null || args.length == 0)
                file = defaultConfigFileLocation();
            else
                file = Arrays.stream(args).findFirst().orElse(file);

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
