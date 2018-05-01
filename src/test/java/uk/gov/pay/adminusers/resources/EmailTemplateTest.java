package uk.gov.pay.adminusers.resources;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class EmailTemplateTest {

    @Test
    public void shouldDeserialiseEnumFromUppercaseString() {
        EmailTemplate mandateCancelled = EmailTemplate.fromString("MANDATE_CANCELLED");
        EmailTemplate mandateFailed = EmailTemplate.fromString("MANDATE_FAILED");
        EmailTemplate paymentConfirmed = EmailTemplate.fromString("PAYMENT_CONFIRMED");
        EmailTemplate paymentFailed = EmailTemplate.fromString("PAYMENT_FAILED");

        assertThat(mandateCancelled, is(EmailTemplate.MANDATE_CANCELLED));
        assertThat(mandateFailed, is(EmailTemplate.MANDATE_FAILED));
        assertThat(paymentConfirmed, is(EmailTemplate.PAYMENT_CONFIRMED));
        assertThat(paymentFailed, is(EmailTemplate.PAYMENT_FAILED));
    }

    @Test
    public void shouldDeserialiseEnumFromLowercaseString() {
        EmailTemplate mandateCancelled = EmailTemplate.fromString("mandate_cancelled");
        EmailTemplate mandateFailed = EmailTemplate.fromString("mandate_failed");
        EmailTemplate paymentConfirmed = EmailTemplate.fromString("payment_confirmed");
        EmailTemplate paymentFailed = EmailTemplate.fromString("payment_failed");

        assertThat(mandateCancelled, is(EmailTemplate.MANDATE_CANCELLED));
        assertThat(mandateFailed, is(EmailTemplate.MANDATE_FAILED));
        assertThat(paymentConfirmed, is(EmailTemplate.PAYMENT_CONFIRMED));
        assertThat(paymentFailed, is(EmailTemplate.PAYMENT_FAILED));
    }

}
