package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.dropwizard.logback.shaded.checkerframework.checker.nullness.qual.Nullable;

import jakarta.validation.Valid;


@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CompleteInviteRequest {

    @Valid
    @Nullable
    private SecondFactorMethod secondFactor;

    public CompleteInviteRequest() {
        // for Jackson deserialisation
    }

    public SecondFactorMethod getSecondFactor() {
        return secondFactor;
    }

    public void setSecondFactor(SecondFactorMethod secondFactor) {
        this.secondFactor = secondFactor;
    }
}
