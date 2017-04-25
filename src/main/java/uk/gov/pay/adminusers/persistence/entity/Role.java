package uk.gov.pay.adminusers.persistence.entity;

public enum Role {
    ADMIN(2);

    private final int id;

    Role(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
