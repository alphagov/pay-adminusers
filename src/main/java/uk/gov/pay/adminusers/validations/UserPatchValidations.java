package uk.gov.pay.adminusers.validations;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static java.lang.String.format;
import static org.apache.commons.lang3.tuple.Pair.of;
import static uk.gov.pay.adminusers.model.PatchRequest.PATH_DISABLED;
import static uk.gov.pay.adminusers.model.PatchRequest.PATH_EMAIL;
import static uk.gov.pay.adminusers.model.PatchRequest.PATH_FEATURES;
import static uk.gov.pay.adminusers.model.PatchRequest.PATH_SESSION_VERSION;
import static uk.gov.pay.adminusers.model.PatchRequest.PATH_TELEPHONE_NUMBER;
import static uk.gov.pay.adminusers.validations.RequestValidations.isNotBoolean;
import static uk.gov.pay.adminusers.validations.RequestValidations.isNotNumeric;
import static uk.gov.pay.adminusers.validations.RequestValidations.isNotValidTelephoneNumber;

public class UserPatchValidations {

    private static final List<String> PATCH_ALLOWED_PATHS = 
            List.of(PATH_SESSION_VERSION, PATH_DISABLED, PATH_TELEPHONE_NUMBER, PATH_FEATURES, PATH_EMAIL);
    
    private static final Multimap<String, String> USER_PATCH_PATH_OPS = new ImmutableListMultimap.Builder<String, String>()
            .put(PATH_SESSION_VERSION, "append")
            .put(PATH_DISABLED, "replace")
            .put(PATH_TELEPHONE_NUMBER, "replace")
            .put(PATH_EMAIL, "replace")
            .put(PATH_FEATURES, "replace")
            .build();

    private static final Multimap<String, Pair<Function<JsonNode, Boolean>, String>> USER_PATCH_PATH_VALIDATIONS = new ImmutableListMultimap.Builder<String, Pair<Function<JsonNode, Boolean>, String>>()
            .put(PATH_SESSION_VERSION, of(isNotNumeric(), format("path [%s] must contain a value of positive integer", PATH_SESSION_VERSION)))
            .put(PATH_DISABLED, of(isNotBoolean(), format("path [%s] must be contain value [true | false]", PATH_DISABLED)))
            .put(PATH_TELEPHONE_NUMBER, of(isNotValidTelephoneNumber(), format("path [%s] must contain a valid telephone number", PATH_TELEPHONE_NUMBER)))
            .build();

    public static boolean isPathAllowed(String path){
        return PATCH_ALLOWED_PATHS.contains(path);
    }

    public static boolean isAllowedOpForPath(String path, String op) {
        return USER_PATCH_PATH_OPS.get(path).contains(op);
    }

    public static Collection<Pair<Function<JsonNode, Boolean>, String>> getUserPatchPathValidations(String path) {
        return USER_PATCH_PATH_VALIDATIONS.get(path);
    }
}
