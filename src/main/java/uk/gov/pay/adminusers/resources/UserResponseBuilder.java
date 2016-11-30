package uk.gov.pay.adminusers.resources;

import uk.gov.pay.adminusers.model.User;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public class UserResponseBuilder {

    private final Response.ResponseBuilder responseBuilder;
    private User user;
    private String baseUrl;

    private UserResponseBuilder(int status) {
        responseBuilder = Response.status(status).type(APPLICATION_JSON);
    }

    public static UserResponseBuilder created() {
        return new UserResponseBuilder(201);
    }

    public UserResponseBuilder withUser(User user) {
        this.user = user;
        return this;
    }

    public UserResponseBuilder withBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public Response build() {
        //setLinks here
        return responseBuilder.entity(user).build();
    }
}
