package uk.gov.pay.adminusers.utils.date;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.adminusers.utils.date.DisputeEvidenceDueByDateUtil.getPayDueByDate;

class DisputeEvidenceDueByDateUtilTest {
    @Test
    void shouldCorrectlyCalculatePayDueByDate() {
        ZonedDateTime dateTime = ZonedDateTime.parse("2023-01-03T13:00:00.000Z");
        ZonedDateTime expectedDueByDate = ZonedDateTime.parse("2022-12-27T13:00:00.000Z");

        ZonedDateTime actualDueByDate = getPayDueByDate(dateTime);

        assertThat(actualDueByDate, is(expectedDueByDate));
    }
}
