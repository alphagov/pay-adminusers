package uk.gov.pay.adminusers.app.config;

import javax.validation.constraints.NotNull;

public class ExpungeAndArchiveDataConfig {

    @NotNull
    private boolean expungeAndArchiveHistoricalDataEnabled;

    @NotNull
    private int expungeUserDataAfterDays;

    public boolean isExpungeAndArchiveHistoricalDataEnabled() {
        return expungeAndArchiveHistoricalDataEnabled;
    }

    public int getExpungeUserDataAfterDays() {
        return expungeUserDataAfterDays;
    }
}
