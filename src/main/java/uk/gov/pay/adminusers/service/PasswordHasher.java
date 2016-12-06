package uk.gov.pay.adminusers.service;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordHasher {

    private static final int HASH_PASSWORD_SALT_ROUNDS = 10;

    public String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(HASH_PASSWORD_SALT_ROUNDS));
    }

    public boolean isEqual(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }
}
