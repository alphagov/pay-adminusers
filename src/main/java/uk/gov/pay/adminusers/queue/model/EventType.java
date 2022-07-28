package uk.gov.pay.adminusers.queue.model;

import java.util.Arrays;

import static org.apache.commons.lang3.StringUtils.isBlank;

public enum EventType {
    DISPUTE_CREATED,
    DISPUTE_LOST,
    DISPUTE_WON,
    DISPUTE_EVIDENCE_SUBMITTED,
    UNKNOWN;

    public static EventType byType(String type) {
        if (isBlank(type)) {
            return UNKNOWN;
        }
        return Arrays.stream(EventType.values())
                .filter(c -> c.name().equals(type.toUpperCase()))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
