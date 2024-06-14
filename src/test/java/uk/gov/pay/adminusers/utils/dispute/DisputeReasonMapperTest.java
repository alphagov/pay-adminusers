package uk.gov.pay.adminusers.utils.dispute;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class DisputeReasonMapperTest {

    @ParameterizedTest
    @ValueSource(strings = {"duplicate", "fraudulent", "general"})
    void shouldReturnPassedThroughUnmodifiedValues(String stripeReason) {
        String mappedValue = DisputeReasonMapper.mapToNotifyEmail(stripeReason);
        assertThat(mappedValue, is(stripeReason));
    }

    @Test
    void shouldReturnCreditNotProcessed() {
        String mappedValue = DisputeReasonMapper.mapToNotifyEmail("credit_not_processed");
        assertThat(mappedValue, is("credit not processed"));
    }

    @Test
    void shouldReturnProductNotReceived() {
        String mappedValue = DisputeReasonMapper.mapToNotifyEmail("product_not_received");
        assertThat(mappedValue, is("product not received"));
    }

    @Test
    void shouldReturnProductUnacceptable() {
        String mappedValue = DisputeReasonMapper.mapToNotifyEmail("product_unacceptable");
        assertThat(mappedValue, is("product unacceptable"));
    }

    @Test
    void shouldReturnProductSubscriptionCancelled() {
        String mappedValue = DisputeReasonMapper.mapToNotifyEmail("subscription_canceled");
        assertThat(mappedValue, is("subscription cancelled"));
    }

    @Test
    void shouldReturnUnrecognised() {
        String mappedValue = DisputeReasonMapper.mapToNotifyEmail("unrecognized");
        assertThat(mappedValue, is("unrecognised"));
    }

    @Test
    void shouldReturnOther() {
        String mappedValue = DisputeReasonMapper.mapToNotifyEmail("insufficient_funds");
        assertThat(mappedValue, is("other"));
    }

    @Test
    void shouldHandleNullValue() {
        String mappedValue = DisputeReasonMapper.mapToNotifyEmail(null);
        assertThat(mappedValue, is("unknown"));
    }

    @Test
    void shouldHandleEmptyValue() {
        String mappedValue = DisputeReasonMapper.mapToNotifyEmail("");
        assertThat(mappedValue, is("unknown"));
    }

    @Test
    void shouldHandleAllWhitespaceValue() {
        String mappedValue = DisputeReasonMapper.mapToNotifyEmail(" ");
        assertThat(mappedValue, is("unknown"));
    }

}
