package uk.gov.pay.adminusers.utils.date;

import java.time.ZonedDateTime;

public class DisputeEvidenceDueByDateUtil {

    public static ZonedDateTime getPayDueByDate(ZonedDateTime evidenceDueDate) {
        switch (evidenceDueDate.getDayOfWeek()) {
            case MONDAY:
                return evidenceDueDate.minusDays(3L);
            case TUESDAY:
                return evidenceDueDate.minusDays(4L);
            default:
                return evidenceDueDate.minusDays(2L);
        }
    }
}
