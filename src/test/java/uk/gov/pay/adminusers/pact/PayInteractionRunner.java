package uk.gov.pay.adminusers.pact;

import au.com.dius.pact.model.BrokerUrlSource;
import au.com.dius.pact.model.Interaction;
import au.com.dius.pact.model.Pact;
import au.com.dius.pact.model.PactSource;
import au.com.dius.pact.provider.junit.InteractionRunner;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.persistence.jpa.jpql.Assert;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PayInteractionRunner extends InteractionRunner {

    private static final Logger LOGGER = PayLoggerFactory.getLogger(PayInteractionRunner.class);

    private String verificationUrl;
    private String username;
    private String password;
    private HttpClient httpClient;
    private String masterBranchGitSha;

    public PayInteractionRunner(TestClass testClass, Pact<? extends Interaction> pact, PactSource pactSource) throws InitializationError {
        super(testClass, pact, pactSource);
        masterBranchGitSha = System.getProperty("PROVIDER_SHA");
        throwIfErrors(pact.getSource(), masterBranchGitSha);
        init((BrokerUrlSource) pact.getSource());
    }

    private void throwIfErrors(PactSource pactSource, String gitSha) {
        if (!(pactSource instanceof BrokerUrlSource))
            throw new RuntimeException("Couldn't initialize " + this.getClass().getCanonicalName() + " as the pactSource is not an instance of BrokerUrlSource");
        if (StringUtils.isEmpty(gitSha))
            throw new RuntimeException("The environment variable PROVIDER_SHA was not set or is empty");
    }

    private void init(BrokerUrlSource brokerUrlSource) {
        Map<String, Map<String, Object>> attributes = brokerUrlSource.getAttributes();
        Assert.isNotNull(attributes.get("pb:publish-verification-results"), "The pact got from the broker has no pb:publish-verification-results property.");
        Assert.isNotNull(attributes.get("pb:publish-verification-results").get("href"), "pb:publish-verification-results has no href property");
        verificationUrl = attributes.get("pb:publish-verification-results").get("href").toString();
        List auth = (List) brokerUrlSource.getOptions().get("authentication");
        username = (String) auth.get(1);
        password = (String) auth.get(2);
        Assert.isNotNull(username, "No auth username to the pact broker provided");
        Assert.isNotNull(password, "No auth password to the pact broker provided");
        CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        httpClient = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
    }

    @Override
    public void reportVerificationResults(Boolean result) {
        try {
            LOGGER.info("Publishing verification results...");
            HttpPost request = new HttpPost(verificationUrl);
            request.addHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(constructPublishVerificationResultJson(result, masterBranchGitSha)));
            HttpResponse response = httpClient.execute(request);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
                throw new RuntimeException("Did not receive a CREATED 201 from Pact Broker when publishing verification result, response was " +
                        IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset()));
            }
            LOGGER.info("Verification results published successfully.");
        } catch (IOException e) {
            LOGGER.error("Exception caught: " + e.getMessage());
            LOGGER.error("Failure when trying to publish verification results.");
            throw new RuntimeException(e);
        }
    }

    private String constructPublishVerificationResultJson(Boolean result, String providerApplicationVersion) {
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = Json.createWriter(stringWriter);
        JsonObject value = Json.createObjectBuilder()
                .add("success", result.toString())
                .add("providerApplicationVersion", providerApplicationVersion)
                .build();
        writer.writeObject(value);
        writer.close();
        return stringWriter.toString();
    }
}
