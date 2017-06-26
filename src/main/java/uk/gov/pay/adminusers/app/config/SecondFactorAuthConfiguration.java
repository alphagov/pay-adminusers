package uk.gov.pay.adminusers.app.config;

import javax.validation.constraints.NotNull;

public class SecondFactorAuthConfiguration {
    @NotNull
    private int timeWindowInSeconds;
    
    @NotNull
    private int validTimeWindows;
    
    public int getTimeWindowInSeconds() {
        return timeWindowInSeconds;
    }

    public int getValidTimeWindows() {
        return validTimeWindows;
    }

    public long getTimeWindowInMillis() {
        return timeWindowInSeconds * 1000L;
    }

}
