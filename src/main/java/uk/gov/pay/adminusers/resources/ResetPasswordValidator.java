package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import jersey.repackaged.com.google.common.collect.ImmutableList;
import uk.gov.pay.adminusers.persistence.dao.ForgottenPasswordDao;
import uk.gov.pay.adminusers.utils.Errors;

import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class ResetPasswordValidator {


    public Optional<Errors> validateResetRequest(JsonNode payload) {
        if(payload == null){
            return Optional.of(Errors.from(ImmutableList.of("JsonNode is invalid")));
        }

        if (isBlank(payload.get("forgotten_password_code").asText()) )
        {
            return Optional.of(Errors.from(ImmutableList.of("Field [forgotten_password_code] is invalid")));
        }

        if(isBlank(payload.get("new_password").asText())){
            return Optional.of(Errors.from(ImmutableList.of("Field [new_password] is invalid")));
        }

        return Optional.empty();
    }
}
