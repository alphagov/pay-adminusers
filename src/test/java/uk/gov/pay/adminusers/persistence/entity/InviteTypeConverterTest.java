package uk.gov.pay.adminusers.persistence.entity;

import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.model.InviteType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InviteTypeConverterTest {

    private final InviteTypeConverter inviteTypeConverter = new InviteTypeConverter();

    @Test
    public void userEnumConstantConvertToDatabaseColumnReturnsUserString() {
        String databaseColumnValue = inviteTypeConverter.convertToDatabaseColumn(InviteType.USER);
        assertThat(databaseColumnValue, is("user"));
    }

    @Test
    public void serviceEnumConstantConvertToDatabaseColumnReturnsServiceString() {
        String databaseColumnValue = inviteTypeConverter.convertToDatabaseColumn(InviteType.SERVICE);
        assertThat(databaseColumnValue, is("service"));
    }

    @Test
    public void existingUserInvitedToExistingServiceEnumConstantConvertToDatabaseColumnReturnsExistingUserInvitedToExistingServiceString() {
        String databaseColumnValue = inviteTypeConverter.convertToDatabaseColumn(InviteType.EXISTING_USER_INVITED_TO_EXISTING_SERVICE);
        assertThat(databaseColumnValue, is("existing_user_invited_to_existing_service"));
    }

    @Test
    public void newUserInvitedToExistingServiceEnumConstantConvertToDatabaseColumnReturnsNewUserInvitedToExistingServiceString() {
        String databaseColumnValue = inviteTypeConverter.convertToDatabaseColumn(InviteType.NEW_USER_INVITED_TO_EXISTING_SERVICE);
        assertThat(databaseColumnValue, is("new_user_invited_to_existing_service"));
    }

    @Test
    public void newUserAndNewServiceSelfSignupEnumConstantConvertToDatabaseColumnReturnsnewUserAndNewServiceSelfSignupString() {
        String databaseColumnValue = inviteTypeConverter.convertToDatabaseColumn(InviteType.NEW_USER_AND_NEW_SERVICE_SELF_SIGNUP);
        assertThat(databaseColumnValue, is("new_user_and_new_service_self_signup"));
    }

    @Test
    public void userStringConvertToEntityAttributeReturnsUserEnumConstant() {
        InviteType entityAttribute = inviteTypeConverter.convertToEntityAttribute("user");
        assertThat(entityAttribute, is(InviteType.USER));
    }

    @Test
    public void serviceStringConvertToEntityAttributeReturnsServiceEnumConstant() {
        InviteType entityAttribute = inviteTypeConverter.convertToEntityAttribute("service");
        assertThat(entityAttribute, is(InviteType.SERVICE));
    }

    @Test
    public void existingUserInvitedToExistingServiceStringConvertToEntityAttributeReturnsExistingUserInvitedToExistingServiceEnumConstant() {
        InviteType entityAttribute = inviteTypeConverter.convertToEntityAttribute("existing_user_invited_to_existing_service");
        assertThat(entityAttribute, is(InviteType.EXISTING_USER_INVITED_TO_EXISTING_SERVICE));
    }

    @Test
    public void newUserInvitedToExistingServiceStringConvertToEntityAttributeReturnsNewUserInvitedToExistingServiceEnumConstant() {
        InviteType entityAttribute = inviteTypeConverter.convertToEntityAttribute("new_user_invited_to_existing_service");
        assertThat(entityAttribute, is(InviteType.NEW_USER_INVITED_TO_EXISTING_SERVICE));
    }

    @Test
    public void newUserAndNewServiceSelfSignupStringConvertToEntityAttributeReturnsNewUserAndNewServiceSelfSignupEnumConstant() {
        InviteType entityAttribute = inviteTypeConverter.convertToEntityAttribute("new_user_and_new_service_self_signup");
        assertThat(entityAttribute, is(InviteType.NEW_USER_AND_NEW_SERVICE_SELF_SIGNUP));
    }

    @Test
    public void unhandledStringConvertToEntityAttributeThrowsException() {
        assertThrows(RuntimeException.class, () ->
                inviteTypeConverter.convertToEntityAttribute("Someone went wild in the DB!"));
    }
}
