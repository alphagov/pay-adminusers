package uk.gov.pay.adminusers.utils.date;

import java.time.ZonedDateTime;

public class DisputeEvidenceDueByDateUtil {

    public static ZonedDateTime getPayDueByDate(ZonedDateTime evidenceDueDate) {
        return evidenceDueDate.minusWeeks(1L);
    }
}
