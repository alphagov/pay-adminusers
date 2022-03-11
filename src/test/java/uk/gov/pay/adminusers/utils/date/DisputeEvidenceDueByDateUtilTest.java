package uk.gov.pay.adminusers.utils.date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.adminusers.utils.date.DisputeEvidenceDueByDateUtil.getPayDueByDateForEpoch;
import static uk.gov.pay.adminusers.utils.date.DisputeEvidenceDueByDateUtil.getZDTForEpoch;

class DisputeEvidenceDueByDateUtilTest {
    
    @Test
    void shouldReturnZonedDateTimeForEpoch() {
        var expectedZDT = Instant.ofEpochSecond(1646830800).atZone(ZoneOffset.UTC);
        assertThat(getZDTForEpoch(1646830800L), is(expectedZDT));
    }

    @ParameterizedTest
    @MethodSource("epochProvider")
    void shouldCorrectlyCalculatePayDueByDate(Long epoch, DayOfWeek expected) {
        assertThat(getPayDueByDateForEpoch(epoch).getDayOfWeek(), is(expected));
    }

    private static Stream<Arguments> epochProvider() {
        return Stream.of(
                Arguments.of(1646658000L, DayOfWeek.FRIDAY), // Monday, 7 March 2022 13:00:00
                Arguments.of(1646744400L, DayOfWeek.FRIDAY), //  Tuesday, 8 March 2022 13:00:00
                Arguments.of(1646830800L, DayOfWeek.MONDAY), //  Wednesday, 9 March 2022 13:00:00
                Arguments.of(1646917200L, DayOfWeek.TUESDAY), // Thursday, 10 March 2022 13:00:00
                Arguments.of(1647003600L, DayOfWeek.WEDNESDAY) // Friday, 11 March 2022 13:00:00
        );
    }
}
