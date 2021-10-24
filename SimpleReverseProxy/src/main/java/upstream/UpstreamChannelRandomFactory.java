package upstream;

import conf.SProxyConf;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

public class UpstreamChannelRandomFactory extends UpstreamChannelFactory {

    private final SProxyConf.ProxyConfig config;
    private final HashMap<String, Iterator<SProxyConf.Host>> hostSelectorStrategy = new HashMap();
    private final Random rand = new Random();

    public UpstreamChannelRandomFactory(SProxyConf.ProxyConfig config) {
        this.config = config;
    }


    @Override
    public InetSocketAddress getUpstreamHost(String targetHost) {
        var service = config.proxyPass(targetHost);

        Iterator<SProxyConf.Host> selector = null;
        if (service != null) {
            selector = hostSelectorStrategy.get(targetHost);
            if (selector == null) {
                selector = new Iterator() {
                    @Override
                    public boolean hasNext() {
                        return !service.hosts.isEmpty();
                    }

                    @Override
                    public SProxyConf.Host next() {
                        int count = rand.nextInt(service.hosts.size());
                        return service.hosts.get(count);
                    }
                };
                hostSelectorStrategy.put(targetHost, selector);
            }
        }

        if (selector == null || !selector.hasNext()) {
            return null;
        }

        // get one of the host that are configured for such service
        SProxyConf.Host host = selector.next();
        return new InetSocketAddress(host.address, host.port);
    }

}
