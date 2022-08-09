package uk.gov.pay.adminusers.infra;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.List;

public class SqsTestDocker {
    private static final Logger logger = LoggerFactory.getLogger(SqsTestDocker.class);

    private static GenericContainer sqsContainer;

    public static AmazonSQS initialise(List<String> queueNames) {
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

            sqsContainer = new GenericContainer("mvisonneau/alpine-sqs:1.2.0")
                    .withExposedPorts(9324)
                    .waitingFor(Wait.forHttp("/?Action=GetQueueUrl&QueueName=default"));

            sqsContainer.start();
        }
    }

    public static void stopContainer() {
        sqsContainer.stop();
        sqsContainer = null;
    }

    private static AmazonSQS createQueues(List<String> queueNames) {
        AmazonSQS amazonSQS = getSqsClient();
        queueNames.forEach(amazonSQS::createQueue);

        return amazonSQS;
    }

    public static String getQueueUrl(String queueName) {
        return getEndpoint() + "/queue/" + queueName;
    }

    public static String getEndpoint() {
        return "http://localhost:" + sqsContainer.getMappedPort(9324);
    }

    private static AmazonSQS getSqsClient() {
        // random credentials required by AWS SDK to build SQS client
        BasicAWSCredentials awsCreds = new BasicAWSCredentials("x", "x");

        return AmazonSQSClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                getEndpoint(),
                                "region-1"
                        ))
                .withRequestHandlers()
                .build();
    }
}
