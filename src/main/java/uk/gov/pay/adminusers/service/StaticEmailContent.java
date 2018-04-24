package uk.gov.pay.adminusers.service;

import java.util.Map;

public class StaticEmailContent {
    private String templateId;
    private Map<String, String> personalisation;

    public StaticEmailContent(String templateId, Map<String, String> personalisation) {
        this.templateId = templateId;
        this.personalisation = personalisation;
    }

    public String getTemplateId() {
        return templateId;
    }

    public Map<String, String> getPersonalisation() {
        return personalisation;
    }
}
