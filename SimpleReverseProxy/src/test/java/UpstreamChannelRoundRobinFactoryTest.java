import conf.ConfigException;
import conf.SProxyConf;
import org.junit.jupiter.api.Test;
import upstream.UpstreamChannelRoundRobinFactory;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class UpstreamChannelRoundRobinFactoryTest {

    @Test
    void roundRobinAddressSelection() throws ConfigException {
        String file = ClassLoader.getSystemClassLoader().getResource("configuration.yaml").getFile();
        SProxyConf.ProxyConfig config = SProxyConf.parse(file).proxy;
        var factory = new UpstreamChannelRoundRobinFactory(config);
        //1 time give me 1st host
        var host = factory.getUpstreamHost("my-service.domain.com");
        assumeTrue(9091 == host.getPort());

        //2 time give me 1st host
        host = factory.getUpstreamHost("my-service.domain.com");
        assumeTrue(9092 == host.getPort());

        //3 time back to 1st host since we only have two
        host = factory.getUpstreamHost("my-service.domain.com");
        assumeTrue(9091 == host.getPort());
    }
}
