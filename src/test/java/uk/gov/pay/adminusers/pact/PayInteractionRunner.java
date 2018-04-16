package uk.gov.pay.adminusers.pact;

import au.com.dius.pact.model.BrokerUrlSource;
import au.com.dius.pact.model.Interaction;
import au.com.dius.pact.model.Pact;
import au.com.dius.pact.model.PactSource;
import au.com.dius.pact.provider.junit.InteractionRunner;
import au.com.dius.pact.provider.junit.loader.PactBroker;
import au.com.dius.pact.provider.junit.loader.PactBrokerAuth;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;

import java.util.Optional;

public class PayInteractionRunner extends InteractionRunner {

    private Optional<String> verificationUrl = Optional.empty();
    private String username;
    private String password;

    public PayInteractionRunner(TestClass testClass, Pact<? extends Interaction> pact, PactSource pactSource) throws InitializationError {
        super(testClass, pact, pactSource);
        if (pact.getSource() instanceof BrokerUrlSource) {
            verificationUrl = Optional.of(((BrokerUrlSource) pact.getSource()).getAttributes().get("pb:publish-verification-results").get("href").toString());
            PactBrokerAuth authentication = testClass.getAnnotation(PactBroker.class).authentication();
            password = authentication.password();
            username = authentication.username();
        }
    }

    @Override
    public void reportVerificationResults(Boolean allPassed) {
        verificationUrl.ifPresent(url -> {
            CredentialsProvider provider = new BasicCredentialsProvider();
            provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
            HttpClient httpClient = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();

            try {
                System.out.println("Publishing verification results...");
                HttpPost request = new HttpPost(url);
                StringEntity stringEntity = new StringEntity("{\"success\":\"false\",\"providerApplicationVersion\":\"1.0.1\"}");
                request.addHeader("Content-Type", "application/json");
                request.setEntity(stringEntity);
                httpClient.execute(request);
                System.out.println("Verification results published successfully.");
            } catch (Exception e) {
                System.out.println("Exception caught: " + e.getMessage());
                System.out.println("Verification results published unsuccessfully");
            }
        });
    }
}
