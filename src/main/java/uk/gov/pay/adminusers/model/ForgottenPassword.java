package uk.gov.pay.adminusers.model;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;

public class ForgottenPassword {

    private Integer id;
    private String code;
    private ZonedDateTime date;
    private String username;

    public static ForgottenPassword forgottenPassword(String code, String username) {
        return forgottenPassword(randomInt(), code, username, ZonedDateTime.now(ZoneId.of("UTC")));
    }

    public static ForgottenPassword forgottenPassword(Integer id, String code, String username, ZonedDateTime date) {
        return new ForgottenPassword(id, code, date, username);
    }

    private ForgottenPassword(Integer id, String code, ZonedDateTime date, String username) {
        this.id = id;
        this.code = code;
        this.date = date;
        this.username = username;
    }

    public Integer getId() {
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
