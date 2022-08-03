package uk.gov.pay.adminusers.utils.dispute;


import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class DisputeReasonMapperTest {

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
}