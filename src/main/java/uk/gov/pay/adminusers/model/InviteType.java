package uk.gov.pay.adminusers.model;

public enum InviteType {

    // Old values; these are still in use.
    USER,
    SERVICE;

    public String getType() {
        return name().toLowerCase();
    }

    public static InviteType from(String typeString) {
        return InviteType.valueOf(typeString.toUpperCase());
    }
}
