package uk.gov.pay.adminusers.model;

public enum InviteType {

    // Old values; these are still in use.
    USER,
    SERVICE,

    // New values; these are not used yet.
    EXISTING_USER_INVITED_TO_EXISTING_SERVICE,
    NEW_USER_INVITED_TO_EXISTING_SERVICE,
    NEW_USER_AND_NEW_SERVICE_SELF_SIGNUP;

    public String getType() {
        return name().toLowerCase();
    }

    public static InviteType from(String typeString) {
        return InviteType.valueOf(typeString.toUpperCase());
    }
}
