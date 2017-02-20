package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class SecondFactorToken {

    private final String username;
    private final String passcode;

    private SecondFactorToken(@JsonProperty("username") String username, @JsonProperty("passcode") String passcode) {
        this.username = username;
        this.passcode = passcode;
    }

    public static SecondFactorToken from(String username, int passcode) {
        return new SecondFactorToken(username, String.valueOf(passcode));
    }

    public String getUsername() {
        return username;
    }

    @JsonGetter
    public String getPasscode() {
        return passcode;
    }

    @JsonIgnore
    public int getPasscodeAsInt() {
        return Integer.parseInt(passcode);
    }
}
