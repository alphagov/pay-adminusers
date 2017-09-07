package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class GetUserProfilesRequest {

    public static final String FIELD_EXTERNAL_IDS = "external_ids";

    private List<String> externalIds = newArrayList();

    public GetUserProfilesRequest() {
    }

    public GetUserProfilesRequest(List<String> externalIds) {
        this.externalIds = externalIds;
    }

    public List<String> getExternalIds() {
        return externalIds;
    }

    public void setExternalIds(List<String> externalIds) {
        this.externalIds = externalIds;
    }
}
