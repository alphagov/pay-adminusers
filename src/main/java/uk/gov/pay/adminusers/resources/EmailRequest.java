package uk.gov.pay.adminusers.resources;

import java.util.Map;

public class EmailRequest {

    private final String emailAddress;
    private final String gatewayAccountId;
    private final EmailTemplate template;
    private final Map<String, String> personalisation;

    public EmailRequest(String emailAddress, String gatewayAccountId, EmailTemplate template, Map<String, String> personalisation) {
        this.emailAddress = emailAddress;
        this.gatewayAccountId = gatewayAccountId;
        this.template = template;
        this.personalisation = personalisation;
    }

    public String getGatewayAccountId() {
        return gatewayAccountId;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public EmailTemplate getTemplate() {
        return template;
    }

    public Map<String, String> getPersonalisation() {
        return personalisation;
    }
}
