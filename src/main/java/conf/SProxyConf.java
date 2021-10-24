package conf;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;


public class SProxyConf {
    private static final Logger logger = LoggerFactory.getLogger(SProxyConf.class);


    public static class ProxyConfig {
        @JsonProperty("listen")
        public Listen listen;

        @JsonProperty("services")
        public List<Service> services;

        public Service proxyPass(String serverName) {
            int end = serverName.indexOf(':') != -1 ? serverName.indexOf(':') : serverName.length();
            String finalServerName = serverName.substring(0, end);

            var found = services.stream()
                    .filter(x -> x.domain.equals(finalServerName))
                    .findFirst().orElse(null);
            return found;
        }
    }

    public static class Listen {
        @JsonProperty("port")
        public int port;
        @JsonProperty("address")
        public String address;
    }

    public static class Host {
        @JsonProperty("address")
        public String address;
        @JsonProperty("port")
        public int port;
    }

    public static class Service {
        @JsonProperty("name")
        public String name;
        @JsonProperty("domain")
        public String domain;
        @JsonProperty("hosts")
        public List<Host> hosts;
    }

    @JsonProperty("proxy")
    public ProxyConfig proxy;


    public static SProxyConf parse(String path) throws ConfigException {
        try {
            File configFile = new File(path);

            logger.info("Reading configuration from: " + configFile);

            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            return mapper.readValue(configFile, SProxyConf.class);
        } catch (JsonMappingException e) {
            throw new ConfigException("Error processing " + path, e);
        } catch (JsonParseException e) {
            throw new ConfigException("Error processing " + path, e);
        } catch (IOException e) {
            throw new ConfigException("Error processing " + path, e);
        }
    }


}
