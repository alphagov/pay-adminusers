package uk.gov.pay.adminusers.pact;

import au.com.dius.pact.model.Pact;
import au.com.dius.pact.provider.junit.loader.PactBrokerLoader;
import au.com.dius.pact.provider.junit.loader.PactFolderLoader;
import au.com.dius.pact.provider.junit.loader.PactLoader;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;


public class ConfigurablePactLoader implements PactLoader {

    public ConfigurablePactLoader(Class<?> clazz) {

    }

    @Override
    public List<Pact> load(String providerName) throws IOException {
        if ("local".equals(System.getProperty("pactSource"))) {
            PactFolderLoader pfl = new PactFolderLoader("pacts");
            return pfl.load(providerName);
        } else {
            Properties properties = new Properties();

            properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("config/pact.properties"));
            String host = properties.getProperty("host", "localhost");
            String port = properties.getProperty("port", "80");
            String protocol = properties.getProperty("protocol", "https");
            List<String> tags = Arrays.asList(System.getProperty("pactTags").split(","));
            PactBrokerLoader pbl = new PactBrokerLoader(host, port, protocol, tags);

            return pbl.load(providerName);
        }
    }
}
