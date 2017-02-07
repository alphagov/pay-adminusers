package uk.gov.pay.adminusers.validations;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static java.lang.String.format;
import static org.apache.commons.lang3.tuple.Pair.of;
import static uk.gov.pay.adminusers.model.PatchRequest.PATH_DISABLED;
import static uk.gov.pay.adminusers.model.PatchRequest.PATH_SESSION_VERSION;
import static uk.gov.pay.adminusers.validations.RequestValidations.isNotBoolean;
import static uk.gov.pay.adminusers.validations.RequestValidations.isNotNumeric;

public class UserPatchValidations {

    private static final List<String> PATCH_ALLOWED_PATHS = ImmutableList.of(PATH_SESSION_VERSION, PATH_DISABLED);
    private static final Multimap<String, String> USER_PATCH_PATH_OPS = new ImmutableListMultimap.Builder<String, String>()
            .put(PATH_SESSION_VERSION, "append")
            .put(PATH_DISABLED, "replace")
            .build();

    private static final Multimap<String, Pair<Function<JsonNode, Boolean>, String>> USER_PATCH_PATH_VALIDATIONS = new ImmutableListMultimap.Builder<String, Pair<Function<JsonNode, Boolean>, String>>()
            .put(PATH_SESSION_VERSION, of(isNotNumeric(), format("path [%s] must contain a value of positive integer", PATH_SESSION_VERSION)))
            .put(PATH_DISABLED, of(isNotBoolean(), format("path [%s] must be contain value [true | false]", PATH_DISABLED)))
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
