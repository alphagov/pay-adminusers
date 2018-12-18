package uk.gov.pay.adminusers.model;

import static java.lang.String.format;

public enum InviteType {

    USER("user"), SERVICE("service");

    private final String type;

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
