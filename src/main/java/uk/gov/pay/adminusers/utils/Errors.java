package uk.gov.pay.adminusers.utils;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

public class Errors {

    private List<String> errors;

    private Errors(@JsonProperty("errors") List<String> errors) {
        this.errors = errors;
    }

    public static Errors from(String error) {
        return new Errors(Collections.singletonList(error));
    }

    public static Errors from(List<String> errorList) {
        return new Errors(errorList);
    }

    @JsonGetter
    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
