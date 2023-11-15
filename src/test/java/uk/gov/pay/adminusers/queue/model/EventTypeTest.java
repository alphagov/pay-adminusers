package uk.gov.pay.adminusers.queue.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.pay.adminusers.queue.model.EventType.DISPUTE_CREATED;
import static uk.gov.pay.adminusers.queue.model.EventType.UNKNOWN;

class EventTypeTest {

    @Test
    void shouldGetCorrectEventTypeForValidValue() {
        assertThat(EventType.byType("DISPUTE_CREATED"), is(DISPUTE_CREATED));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "some-random-string"
    })
    @NullSource
    void shouldReturnUnknownForEmptyValue(String value) {
        assertThat(EventType.byType(value), is(UNKNOWN));
    }
}
