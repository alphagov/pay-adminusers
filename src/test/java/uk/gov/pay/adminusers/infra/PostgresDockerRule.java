package uk.gov.pay.adminusers.infra;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import static org.junit.Assert.assertNotNull;

public class PostgresDockerRule implements TestRule {

    private static String host;
    private static PostgresContainer container;

    private static final String DOCKER_HOST = "DOCKER_HOST";
    private static final String DOCKER_CERT_PATH = "DOCKER_CERT_PATH";

    static {
        try {
            String dockerHost = Optional.ofNullable(System.getenv(DOCKER_HOST)).
                    orElseThrow(() -> new RuntimeException(DOCKER_HOST + " environment variable not set. It has to be set to the docker daemon location."));
            URI dockerHostURI = new URI(dockerHost);
            boolean isDockerDaemonLocal = "unix".equals(dockerHostURI.getScheme());
            if(!isDockerDaemonLocal) {
                assertNotNull(DOCKER_CERT_PATH + " environment variable not set.", System.getenv(DOCKER_CERT_PATH));
            }
            host = isDockerDaemonLocal ? "localhost" : dockerHostURI.getHost();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public PostgresDockerRule() {
        startPostgresIfNecessary();
    }

    public String getConnectionUrl() {
        return container.getConnectionUrl();
    }

    @Override
    public Statement apply(Statement statement, Description description) {
        return statement;
    }

    private void startPostgresIfNecessary() {
        try {
            if (container == null) {
                DockerClient docker = DefaultDockerClient.fromEnv().build();
                container = new PostgresContainer(docker, host);
            }
        } catch (DockerCertificateException | InterruptedException | DockerException | IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public String getUsername() {
        return container.getUsername();
    }

    public String getPassword() {
        return container.getPassword();
    }

    public void stop() {
        container.stop();
        container = null;
    }

    public String getDriverClass() {
        return "org.postgresql.Driver";
    }
}
