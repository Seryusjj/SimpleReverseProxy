import com.sun.net.httpserver.HttpServer;
import conf.ConfigException;
import conf.SProxyConf;
import core.SProxy;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class SProxyTest {

    static HttpServer httpServer;

    static String httpServerResponse = "{\"success\": true}";

    @BeforeAll
    static void initAll() throws IOException {


        httpServer = HttpServer.create(new InetSocketAddress(0), 0); // or use InetSocketAddress(0) for ephemeral port
        httpServer.createContext("/", exchange -> {
            byte[] response = httpServerResponse.getBytes();
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        httpServer.start();
    }

    @AfterAll
    static void tearDownAll() {
        httpServer.stop(0);
    }


    @Test
    void configDeserialization() throws ConfigException {
        String file = ClassLoader.getSystemClassLoader().getResource("configuration.yaml").getFile();
        SProxyConf.ProxyConfig config = SProxyConf.parse(file).proxy;
        int serviceNum = 0;
        int serviceHost1 = 0;
        int serviceHost2 = 1;
        assumeTrue(config.listen != null, "Listen section was not detected");
        assumeTrue("127.0.0.1".equals(config.listen.address), "Listen.address has an unexpected value");
        assumeTrue(80 == config.listen.port, "Listen.port has an unexpected value");
        assumeTrue(config.services != null, "Services section was not detected");
        assumeTrue(config.services.get(serviceNum) != null, "Services section was not populated properly");
        assumeTrue("service1".equals(config.services.get(serviceNum).name), "Service name has an unexpected value");
        assumeTrue("my-service.domain.com".equals(config.services.get(serviceNum).domain), "Service name has an unexpected value");
        assumeTrue(config.services.get(serviceNum).hosts != null, "Service hosts was not detected");
        assumeTrue("127.0.0.1".equals(config.services.get(serviceNum).hosts.get(serviceHost1).address), "Service hosts.address was not detected");
        assumeTrue(9091 == config.services.get(serviceNum).hosts.get(serviceHost1).port, "Service hosts.port was not detected");
        assumeTrue("127.0.0.1".equals(config.services.get(serviceNum).hosts.get(serviceHost2).address), "Service hosts.address was not detected");
        assumeTrue(9092 == config.services.get(serviceNum).hosts.get(serviceHost2).port, "Service hosts.port was not detected");
    }

    private static String execRequest(HttpGet httpRequest) throws IOException {
        org.apache.http.HttpResponse httpResponse = SimpleHttpClient.getNew().execute(httpRequest);
        InputStream inputStream = httpResponse.getEntity().getContent();
        StringBuilder textBuilder = new StringBuilder();
        Reader reader = new BufferedReader(
                new InputStreamReader(inputStream, Charset.forName(StandardCharsets.UTF_8.name())));
        int c = 0;
        while ((c = reader.read()) != -1) {
            textBuilder.append((char) c);
        }
        return textBuilder.toString();
    }

    @Test
    void directCallToHttpServer() throws IOException {
        int port = httpServer.getAddress().getPort();

        HttpGet httpRequest = new HttpGet("http://127.0.0.1" + ":" + port);
        httpRequest.setHeader("Content-Type", "application/json");
        /* Executing our request should now hit 127.0.0.1, regardless of DNS */

        var res = execRequest(httpRequest);
        assumeTrue(httpServerResponse.equals(res));
    }


    @Test
    void multiThreadedCallToProxy() throws IOException {


        int port = httpServer.getAddress().getPort();

        //config proxy dynamically to get the server port
        SProxyConf.ProxyConfig config = new SProxyConf.ProxyConfig();
        config.listen = new SProxyConf.Listen();
        config.listen.port = 8083;
        config.listen.address = "127.0.0.1";
        config.services = new ArrayList(1);
        SProxyConf.Service service = new SProxyConf.Service();
        service.domain = SimpleHttpClient.CUSTOM_TEST_DOMAIN_1;
        service.name = "htt_server";
        service.hosts = new ArrayList(1);
        SProxyConf.Host host = new SProxyConf.Host();
        host.address = "127.0.0.1";
        host.port = port;
        service.hosts.add(host);
        config.services.add(service);

        SProxy sproxy = new SProxy();
        var channel = sproxy.start(config);


        try {
            List<Function<Object, String>> actions = new ArrayList();
            // request trough proxy 1
            for (int i = 0; i < 100; i++) {
                actions.add((x) -> {
                    HttpGet httpProxyRequest = new HttpGet("http://" + service.domain + ":" + config.listen.port);
                    httpProxyRequest.setHeader("Content-Type", "application/json");
                    String res1 = null;
                    try {
                        res1 = execRequest(httpProxyRequest);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return res1;
                });
            }

            AtomicBoolean correct = new AtomicBoolean(true);
            actions.stream().forEach((Function<Object, String> request) -> {
                var r = request.apply(null);
                correct.set(correct.get() && httpServerResponse.equals(r));
            });
            assumeTrue(correct.get(), "Synchronous calls to proxy have an unexpected error");

            actions.parallelStream().forEach((Function<Object, String> request) -> {
                var r = request.apply(null);
                correct.set(correct.get() && httpServerResponse.equals(r));
            });
            assumeTrue(correct.get(), "Parallel calls to proxy have an unexpected error");

        } finally {
            // stop proxy
            sproxy.stop();
            // wait for channel to close
            channel.closeFuture().syncUninterruptibly();
        }
    }


}
