package uk.gov.pay.adminusers.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;

import java.net.URI;
import java.util.List;

public class SqsTestDocker {
    private static final Logger logger = LoggerFactory.getLogger(SqsTestDocker.class);

    private static GenericContainer sqsContainer;

    public static SqsClient initialise(List<String> queueNames) {
        try {
            createContainer();
            return createQueues(queueNames);
        } catch (Exception e) {
            logger.error("Exception initialising SQS Container - {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static void createContainer() {
        if (sqsContainer == null) {
            logger.info("Creating SQS Container");

            sqsContainer = new GenericContainer("softwaremill/elasticmq-native:1.4.2")
                    .withExposedPorts(9324)
                    .waitingFor(Wait.forLogMessage(".*ElasticMQ server.*.*started.*", 1));
            sqsContainer.start();
        }
    }

    private static SqsClient createQueues(List<String> queueNames) {
        SqsClient amazonSQS = getSqsClient();
        queueNames.forEach(queueName -> {
            CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
                    .queueName(queueName)
                    .build();

            amazonSQS.createQueue(createQueueRequest);
        });

        return amazonSQS;
    }

    public static String getQueueUrl(String queueName) {
        return getEndpoint() + "/queue/" + queueName;
    }

    public static String getEndpoint() {
        return "http://localhost:" + sqsContainer.getMappedPort(9324);
    }

    private static SqsClient getSqsClient() {
        // random credentials required by AWS SDK to build SQS client
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create("x", "x");
        return SqsClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .endpointOverride(URI.create(getEndpoint()))
                .region(Region.of("region-1"))
                .build();
    }
}
