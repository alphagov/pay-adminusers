package uk.gov.pay.adminusers;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class JsonResourceLoader {
    private static final String TEMPLATE_BASE_NAME = "templates";

    public static final String DISPUTE_CREATED_EVENT = TEMPLATE_BASE_NAME + "/events/dispute_created_event.json";

    public static final String DISPUTE_LOST_EVENT = TEMPLATE_BASE_NAME + "/events/dispute_lost_event.json";

    public static final String DISPUTE_WON_EVENT = TEMPLATE_BASE_NAME + "/events/dispute_won_event.json";

    public static final String DISPUTE_EVIDENCE_SUBMITTED_EVENT = TEMPLATE_BASE_NAME + "/events/dispute_evidence_submitted_event.json";

    public static final String DISPUTE_CREATED_SNS_MESSAGE = TEMPLATE_BASE_NAME + "/sns/dispute_created_sns_message.json";

    private static JsonElement loadJsonFromFile(String filePath) throws FileNotFoundException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        return JsonParser.parseReader(reader);
    }

    public static String load(String location) throws FileNotFoundException {
        String filePath = JsonResourceLoader.class.getClassLoader().getResource(location).getPath();
        return loadJsonFromFile(filePath).toString();
    }

}
