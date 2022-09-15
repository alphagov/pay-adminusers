package uk.gov.pay.adminusers.service;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.pay.adminusers.model.Invite;
import uk.gov.pay.adminusers.model.InviteType;

import javax.ws.rs.WebApplicationException;
import java.util.Optional;

public class InviteOtpValidator {
    
    public Optional<WebApplicationException> validate(Invite invite, JsonNode requestPayload) {
        Optional<WebApplicationException> result = Optional.empty();
        
        switch (invite.getType()) {
            case SERVICE: {
                result = validateForServiceJourney(invite, requestPayload);
            }
            case USER: {
                result = validateForUserJourney(invite, requestPayload);
            }
        }
        
        return result;
    }

    private Optional<WebApplicationException> validateForServiceJourney(Invite invite, JsonNode requestPayload) {
        return null;
    }

    private Optional<WebApplicationException> validateForUserJourney(Invite invite, JsonNode requestPayload) {
        return null;
    }
}
