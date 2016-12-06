package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import jersey.repackaged.com.google.common.collect.ImmutableList;
import uk.gov.pay.adminusers.utils.Errors;

import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class ForgottenPasswordValidator {
    public Optional<Errors> validateCreateRequest(JsonNode payload) {
        if (payload != null &&
                payload.get("username") != null &&
                !isBlank(payload.get("username").asText())) {

            return Optional.empty();
        }
        return Optional.of(Errors.from(ImmutableList.of("Field [username] is required")));
    }
}
