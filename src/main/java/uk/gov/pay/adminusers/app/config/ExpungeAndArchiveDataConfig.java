package uk.gov.pay.adminusers.app.config;

import jakarta.validation.constraints.NotNull;

public class ExpungeAndArchiveDataConfig {

    @NotNull
    private boolean expungeAndArchiveHistoricalDataEnabled;

    @NotNull
    private int expungeUserDataAfterDays;

    @NotNull
    private int archiveServicesAfterDays;

    public boolean isExpungeAndArchiveHistoricalDataEnabled() {
        return expungeAndArchiveHistoricalDataEnabled;
    }

    public int getExpungeUserDataAfterDays() {
        return expungeUserDataAfterDays;
    }

    public int getArchiveServicesAfterDays() {
        return archiveServicesAfterDays;
    }
}
