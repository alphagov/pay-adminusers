package uk.gov.pay.adminusers.infra;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;

public class PostgresDockerRule implements TestRule {
    
    private static PostgresContainer container;

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
                container = new PostgresContainer(docker);
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
