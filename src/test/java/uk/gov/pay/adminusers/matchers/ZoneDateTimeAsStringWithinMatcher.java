package uk.gov.pay.adminusers.matchers;

import org.exparity.hamcrest.date.ZonedDateTimeMatchers;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static uk.gov.pay.adminusers.utils.DateTimeUtils.toUTCZonedDateTime;

public class ZoneDateTimeAsStringWithinMatcher extends TypeSafeMatcher<String> {

    private final long period;
    private final ChronoUnit unit;
    private ZonedDateTime from;

    private ZoneDateTimeAsStringWithinMatcher(long period, ChronoUnit unit, ZonedDateTime from) {
        this.period = period;
        this.unit = unit;
        this.from = from;
    }

    public static ZoneDateTimeAsStringWithinMatcher isWithin(long period, ChronoUnit unit) {
        return new ZoneDateTimeAsStringWithinMatcher(period, unit, ZonedDateTime.now());
    }

    @Override
    protected boolean matchesSafely(String dateTimeAsString) {
        return ZonedDateTimeMatchers.within(period, unit, from).matches(toUTCZonedDateTime(dateTimeAsString).get());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("A date within " + period + " " + unit + " from " + from);
    }
}
