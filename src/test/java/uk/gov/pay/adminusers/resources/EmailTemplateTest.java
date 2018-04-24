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

        assertThat(mandateCancelled, is(EmailTemplate.MANDATE_CANCELLED));
        assertThat(mandateFailed, is(EmailTemplate.MANDATE_FAILED));
        assertThat(paymentConfirmed, is(EmailTemplate.PAYMENT_CONFIRMED));
    }

    @Test
    public void shouldDeserialiseEnumFromLowercaseString() {
        EmailTemplate mandateCancelled = EmailTemplate.fromString("mandate_cancelled");
        EmailTemplate mandateFailed = EmailTemplate.fromString("mandate_failed");
        EmailTemplate paymentConfirmed = EmailTemplate.fromString("payment_confirmed");

        assertThat(mandateCancelled, is(EmailTemplate.MANDATE_CANCELLED));
        assertThat(mandateFailed, is(EmailTemplate.MANDATE_FAILED));
        assertThat(paymentConfirmed, is(EmailTemplate.PAYMENT_CONFIRMED));
    }

}
