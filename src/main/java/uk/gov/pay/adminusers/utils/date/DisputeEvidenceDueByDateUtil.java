package uk.gov.pay.adminusers.utils.date;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class DisputeEvidenceDueByDateUtil {
    
    public static ZonedDateTime getZDTForEpoch(Long epoch) {
        return Instant.ofEpochSecond(epoch).atZone(ZoneOffset.UTC);
    }
    
    public static ZonedDateTime getPayDueByDateForEpoch(Long epoch) {
        var zdt = getZDTForEpoch(epoch);
        switch (zdt.getDayOfWeek()) {
            case MONDAY:
                return zdt.minusDays(3L);
            case TUESDAY:
                return zdt.minusDays(4L);
            default:
                return zdt.minusDays(2L);
        }
    }
}
