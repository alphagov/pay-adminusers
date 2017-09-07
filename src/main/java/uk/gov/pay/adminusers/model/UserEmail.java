package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class UserEmail {


    public static final String FIELD_EXTERNAL_ID = "external_id";
    public static final String FIELD_EMAIL = "email";

    private String externalId;
    private String email;


    public UserEmail(@JsonProperty("external_id") String externalId, @JsonProperty("email")  String email) {
        this.externalId = externalId;
        this.email = email;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "UserEmail{" +
                "externalId='" + externalId + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
