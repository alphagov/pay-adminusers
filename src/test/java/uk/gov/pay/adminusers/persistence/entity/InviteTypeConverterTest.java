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
    public void unhandledStringConvertToEntityAttributeThrowsException() {
        assertThrows(RuntimeException.class, () ->
                inviteTypeConverter.convertToEntityAttribute("Someone went wild in the DB!"));
    }
}
