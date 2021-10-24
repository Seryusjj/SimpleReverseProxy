package upstream;

import conf.SProxyConf;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;

public class UpstreamChannelRoundRobinFactory extends UpstreamChannelFactory {

    private final SProxyConf.ProxyConfig config;
    private final HashMap<String, Iterator<SProxyConf.Host>> hostSelectorStrategy = new HashMap();

    public UpstreamChannelRoundRobinFactory(SProxyConf.ProxyConfig config) {
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
                    private int count = -1;
                    @Override
                    public boolean hasNext() {
                        return !service.hosts.isEmpty();
                    }

                    @Override
                    public SProxyConf.Host next() {
                        count = (count + 1) % service.hosts.size();
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
