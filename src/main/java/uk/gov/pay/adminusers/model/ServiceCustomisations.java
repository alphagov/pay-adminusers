package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class ServiceCustomisations {

    public static final String FIELD_BANNER_COLOUR = "banner_colour";
    public static final String FIELD_LOGO_URL = "logo_url";

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

    public static ServiceCustomisations from(JsonNode payload) {
        String bannerColour = payload.get(FIELD_BANNER_COLOUR) != null ? payload.get(FIELD_BANNER_COLOUR).asText() : "";
        String logoUrl = payload.get(FIELD_LOGO_URL) != null ? payload.get(FIELD_LOGO_URL).asText() : "";
        return new ServiceCustomisations(bannerColour, logoUrl);
    }
}
