package uk.gov.pay.adminusers.model;

import static java.lang.String.format;

public enum InviteType {

    // Old values; these are still in use.
    USER("user"),
    SERVICE("service"),

    // New values; these are not used yet.
    EXISTING_USER_INVITED_TO_EXISTING_SERVICE("existing_user_invited_to_existing_service"),
    NEW_USER_INVITED_TO_EXISTING_SERVICE("new_user_invited_to_existing_service"),
    NEW_USER_AND_NEW_SERVICE_SELF_SIGNUP("new_user_new_service_self_signup");

    private String type;

    InviteType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static InviteType from(String inviteType) {
        if (USER.type.equals(inviteType)) {
            return USER;
        } else if (SERVICE.type.equals(inviteType)) {
            return SERVICE;
        } else {
            throw new RuntimeException(format("invalid invite type: [%s]", inviteType));
        }
    }
}
