package uk.gov.pay.adminusers.pact;

import au.com.dius.pact.model.Pact;
import au.com.dius.pact.provider.junit.loader.*;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;


public class ConfigurablePactLoader implements PactLoader {

    public ConfigurablePactLoader(Class<?> clazz) {

    }

    @Override
    public List<Pact> load(String providerName) throws IOException {
        if ("broker".equals(System.getProperty("pactSource"))) {
            Properties properties = new Properties();

            properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("config/pact.properties"));
            String host = properties.getProperty("host", "localhost");
            String port = properties.getProperty("port", "80");
            String protocol = properties.getProperty("protocol", "https");
            String username = properties.getProperty("username");
            String password = properties.getProperty("password");
            String tags = System.getProperty("pactTags");
            List<String> tagsList = StringUtils.isNotBlank(tags) ? Arrays.asList(tags.split(",")) : Collections.emptyList();

            PactBrokerLoader pbl = new PactBrokerLoader(new PactBrokerImpl(host, port, protocol, username, password, tagsList));

            return pbl.load(providerName);
        } else {
            PactFolderLoader pfl = new PactFolderLoader("pacts");
            return pfl.load(providerName);
        }
    }

    private class PactBrokerImpl implements PactBroker {

        private final String host;
        private final String port;
        private final String protocol;
        private final boolean failIfNoPactsFound = true;
        private final PactBrokerAuth pactBrokerAuth;
        private final String[] tags;



        PactBrokerImpl(String host, String port, String protocol, String username, String password,  List<String> tags) {
            this.host = host;
            this.port = port;
            this.protocol = protocol;
            this.pactBrokerAuth = new PactBrokerAuthImpl(username, password);
            this.tags = tags.toArray(new String[tags.size()]);
        }

        @Override
        public String host() {
            return host;
        }

        @Override
        public String port() {
            return port;
        }

        @Override
        public String protocol() {
            return protocol;
        }

        @Override
        public String[] tags() {
            return Arrays.copyOf(tags, tags.length);
        }

        @Override
        public boolean failIfNoPactsFound() {
            return failIfNoPactsFound;
        }

        @Override
        public PactBrokerAuth authentication() {
            return pactBrokerAuth;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return null;
        }

        private class PactBrokerAuthImpl implements PactBrokerAuth {

            private final String username;
            private final String password;
            private final String scheme = "Basic";

            PactBrokerAuthImpl(String username, String password) {
                this.username = username;
                this.password = password;
            }

            @Override
            public String scheme() {
                return scheme;
            }

            @Override
            public String username() {
                return username;
            }

            @Override
            public String password() {
                return password;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }
        }
    }
}
