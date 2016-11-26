package uk.gov.pay.adminusers.model;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.newLongId;

public class ForgottenPassword {

    private Long id;
    private String code;
    private ZonedDateTime date;
    private String username;

    public static ForgottenPassword forgottenPassword(String code, String username) {
        return forgottenPassword(newLongId(), code, username, ZonedDateTime.now(ZoneId.of("UTC")));
    }

    public static ForgottenPassword forgottenPassword(Long id, String code, String username, ZonedDateTime date) {
        return new ForgottenPassword(id, code, date, username);
    }

    private ForgottenPassword(Long id, String code, ZonedDateTime date, String username) {
        this.id = id;
        this.code = code;
        this.date = date;
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public ZonedDateTime getDate() {
        return date;
    }

    public String getUsername() {
        return username;
    }
}
