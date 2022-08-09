package uk.gov.pay.adminusers.fixtures;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.lang3.NotImplementedException;
import uk.gov.pay.adminusers.infra.SqsTestDocker;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class EventFixtureUtil {

    public static String insert(AmazonSQS sqsClient, String eventType, String serviceId, Boolean live, String resourceExternalId,
                                String parentResourceExternalId, String eventData) {
        String messageBody = String.format("{" +
                        "\"resource_external_id\": \"%s\"," +
                        "\"service_id\": \"%s\"," +
                        "\"live\": \"%s\"," +
                        (isBlank(parentResourceExternalId) ? "%s" : "\"parent_resource_external_id\": \"%s\",") +
                        "\"event_type\":\"%s\"," +
                        "\"event_details\": %s" +
                        "}",
                resourceExternalId,
                serviceId,
                live,
                parentResourceExternalId == null ? "" : parentResourceExternalId,
                eventType,
                eventData
        );

        SendMessageResult result = sqsClient.sendMessage(SqsTestDocker.getQueueUrl("event-queue"), messageBody);
        return result.getMessageId();
    }

    public static PactDslJsonBody getAsPact(String serviceId, Boolean live, String eventType, String resourceExternalId,
                                            String parentResourceExternalId, JsonNode eventData) {
        PactDslJsonBody message = new PactDslJsonBody();

        message.stringType("event_type", eventType);
        message.stringType("resource_external_id", resourceExternalId);
        message.booleanType("live", live);
        if (!isBlank(parentResourceExternalId)) {
            message.stringType("parent_resource_external_id", parentResourceExternalId);
        }
        if (!isBlank(serviceId)) {
            message.stringType("service_id", serviceId);
        }
        if (live != null) {
            message.booleanType("live", live);
        }

        PactDslJsonBody eventDetailsPact = getNestedPact(eventData);
        message.object("event_details", eventDetailsPact);

        return message;
    }

    private static PactDslJsonBody getNestedPact(JsonNode eventData) {
        PactDslJsonBody dslJsonBody = new PactDslJsonBody();
        eventData.fields()
                .forEachRemaining((e) -> {
                    try {
                        if (e.getValue().isObject()) {
                            dslJsonBody.object(e.getKey(), getNestedPact(e.getValue()));
                        } else if (e.getValue().isArray()) {
                            // We're currently only adding a single example from an array to the pact, and then in the
                            // matchers check that the array has at least one entry matching the example. For stricter
                            // matching, this would need to be modified.
                            ArrayNode asJsonArray = (ArrayNode) e.getValue();
                            PactDslJsonBody arrayEntryExample = dslJsonBody.minArrayLike(e.getKey(), 1);
                            if (asJsonArray.get(0).isObject()) {
                                asJsonArray.get(0).fields().forEachRemaining(a -> {
                                    if (a.getValue().isNumber()) {
                                        arrayEntryExample.integerType(a.getKey(), a.getValue().intValue());
                                    } else if (a.getValue().isBoolean()) {
                                        arrayEntryExample.booleanType(a.getKey(), a.getValue().booleanValue());
                                    } else {
                                        arrayEntryExample.stringType(a.getKey(), a.getValue().textValue());
                                    }
                                });
                            } else {
                                throw new NotImplementedException();
                            }
                            arrayEntryExample.closeObject().closeArray();
                        } else if (e.getValue().isNumber()) {
                            dslJsonBody.integerType(e.getKey(), e.getValue().intValue());
                        } else if (e.getValue().isBoolean()) {
                            dslJsonBody.booleanType(e.getKey(), e.getValue().booleanValue());
                        } else {
                            dslJsonBody.stringType(e.getKey(), e.getValue().textValue());
                        }
                    } catch (Exception ex) {
                        dslJsonBody.stringType(e.getKey(), e.getValue().textValue());
                    }
                });
        return dslJsonBody;
    }
}
