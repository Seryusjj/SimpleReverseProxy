import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Code based on:
 * https://stackoverflow.com/questions/24350150/how-to-override-dns-in-http-connections-in-java
 * This Http client is used just for testing purposes in order to set custom domains per http request.
 */
public class SimpleHttpClient {


    public static final String CUSTOM_TEST_DOMAIN_1 = "test.domain.com";


    public static HttpClient getNew() {
        /* Custom DNS resolver */
        DnsResolver dnsResolver = new SystemDefaultDnsResolver() {
            @Override
            public InetAddress[] resolve(final String host) throws UnknownHostException {
                if (host.equalsIgnoreCase(CUSTOM_TEST_DOMAIN_1)) {
            /* If we match the host we're trying to talk to,
               return the IP address we want, not what is in DNS */
                    return new InetAddress[]{InetAddress.getByName("127.0.0.1")};
                } else {
                    /* Else, resolve it as we would normally */
                    return super.resolve(host);
                }
            }
        };

        /* HttpClientConnectionManager allows us to use custom DnsResolver */
        BasicHttpClientConnectionManager connManager = new BasicHttpClientConnectionManager(
    /* We're forced to create a SocketFactory Registry.  Passing null
       doesn't force a default Registry, so we re-invent the wheel. */
                RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", PlainConnectionSocketFactory.getSocketFactory())
                        .register("https", SSLConnectionSocketFactory.getSocketFactory())
                        .build(),
                null, /* Default ConnectionFactory */
                null, /* Default SchemePortResolver */
                dnsResolver  /* Our DnsResolver */
        );

        return HttpClientBuilder.create()
                .setConnectionManager(connManager)
                .build();
    }


}
