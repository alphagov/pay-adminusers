package uk.gov.pay.adminusers.app.config;

import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class NotifyDirectDebitConfiguration extends Configuration {

    @Valid
    @NotNull
    private String mandateCancelledEmailTemplateId;

    @Valid
    @NotNull
    private String mandateFailedEmailTemplateId;

    @Valid
    @NotNull
    private String paymentFailedEmailTemplateId;

    @Valid
    @NotNull
    private String oneOffMandateAndPaymentCreatedEmailTemplateId;

    @Valid
    @NotNull
    private String onDemandMandateCreatedEmailTemplateId;

    @Valid
    @NotNull
    private String onDemandPaymentConfirmedEmailTemplateId;


    public String getMandateCancelledEmailTemplateId() {
        return mandateCancelledEmailTemplateId;
    }

    public String getMandateFailedEmailTemplateId() {
        return mandateFailedEmailTemplateId;
    }

    public String getPaymentFailedEmailTemplateId() {
        return paymentFailedEmailTemplateId;
    }

    public String getOneOffMandateAndPaymentCreatedEmailTemplateId() {
        return oneOffMandateAndPaymentCreatedEmailTemplateId;
    }

    public String getOnDemandMandateCreatedEmailTemplateId() {
        return onDemandMandateCreatedEmailTemplateId;
    }

    public String getOnDemandPaymentConfirmedEmailTemplateId() {
        return onDemandPaymentConfirmedEmailTemplateId;
    }

}
