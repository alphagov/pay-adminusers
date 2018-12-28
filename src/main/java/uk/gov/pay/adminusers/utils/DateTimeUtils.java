package uk.gov.pay.adminusers.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public class DateTimeUtils {

    private static final ZoneId UTC = ZoneId.of("Z");
    private static final DateTimeFormatter dateTimeFormatterUTC = DateTimeFormatter.ISO_INSTANT.withZone(UTC);
    private static final DateTimeFormatter dateTimeFormatterAny = DateTimeFormatter.ISO_ZONED_DATE_TIME;

    /**
     * Converts any valid ZonedDateTime String (ISO_8601) representation to a UTC ZonedDateTime
     * <p>
     * e.g. <br/>
     * 1. 2010-01-01T12:00:00+01:00[Europe/Paris] ==>  ZonedDateTime("2010-12-31T22:59:59.132Z") <br/>
     * 2. 2010-12-31T22:59:59.132Z ==>  ZonedDateTime("2010-12-31T22:59:59.132Z") <br/>
     * </p>
     *
     * @param dateString e.g.
     * @return @ZonedDateTime instance represented by dateString in UTC ("Z") or
     * @Optional.empty() if the dateString does not represent a valid ISO_8601 zone string
     * @see "https://docs.oracle.com/javase/8/docs/api/java/time/ZonedDateTime.html"
     */
    public static Optional<ZonedDateTime> toUTCZonedDateTime(String dateString) {
        try {
            ZonedDateTime utcDateTime = ZonedDateTime
                    .parse(dateString, dateTimeFormatterAny)
                    .withZoneSameInstant(UTC);
            return Optional.of(utcDateTime);
        } catch (DateTimeParseException ex) {
            return Optional.empty();
        }
    }

    /**
     * Converts a ZonedDateTime to a UTC ISO_8601 string representation
     * <p>
     * e.g. <br/>
     * 1. ZonedDateTime("2010-01-01T12:00:00+01:00[Europe/Paris]") ==> "2010-12-31T22:59:59.132Z" <br/>
     * 2. ZonedDateTime("2010-12-31T22:59:59.132Z") ==> "2010-12-31T22:59:59.132Z" <br/>
     * </p>
     *
     * @param dateTime
     * @return UTC ISO_8601 date string
     */
    public static String toUTCDateString(ZonedDateTime dateTime) {
        return dateTime.format(dateTimeFormatterUTC);
    }


    public static String toLondonZone(ZonedDateTime zonedDateTime) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        LocalDateTime localDateTime = zonedDateTime.withZoneSameInstant(ZoneId.of("Europe/London")).toLocalDateTime();

        return localDateTime.format(dateTimeFormatter);
    }

    public static String toUserFriendlyDate(ZonedDateTime zonedDateTime) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy - HH:mm:ss");

        LocalDateTime localDateTime = zonedDateTime.withZoneSameInstant(ZoneId.of("Europe/London")).toLocalDateTime();

        return localDateTime.format(dateTimeFormatter);
    }
}
