package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import org.mindrot.jbcrypt.BCrypt;

public class PasswordHasher {

    private final String hashSalt;

    @Inject
    public PasswordHasher(String hashSalt) {
        this.hashSalt = hashSalt;
    }

    public String hash(String password) {
        return BCrypt.hashpw(password, hashSalt);
    }

}
