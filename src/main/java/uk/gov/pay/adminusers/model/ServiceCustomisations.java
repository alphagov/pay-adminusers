package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class ServiceCustomisations {
    private final String bannerColour;
    private final String logoUrl;

    public ServiceCustomisations(@JsonProperty("banner_colour") String bannerColour, @JsonProperty("logo_url") String logoUrl) {
        this.bannerColour = bannerColour;
        this.logoUrl = logoUrl;
    }

    public String getBannerColour() {
        return bannerColour;
    }

    public String getLogoUrl() {
        return logoUrl;
    }
}
