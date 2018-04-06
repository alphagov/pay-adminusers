package uk.gov.pay.adminusers.persistence.entity;

import org.junit.Test;
import uk.gov.pay.adminusers.model.SecondFactorMethod;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class SecondFactorMethodConverterTest {

    private final SecondFactorMethodConverter secondFactorMethodConverter = new SecondFactorMethodConverter();

    @Test
    public void smsEnumVariantConvertToDatabaseColumnReturnsSmsString() {
        String databaseColumnValue = secondFactorMethodConverter.convertToDatabaseColumn(SecondFactorMethod.SMS);
        assertThat(databaseColumnValue, is("sms"));
    }

    @Test
    public void appEnumVariantConvertToDatabaseColumnReturnsAppString() {
        String databaseColumnValue = secondFactorMethodConverter.convertToDatabaseColumn(SecondFactorMethod.APP);
        assertThat(databaseColumnValue, is("app"));
    }

    @Test
    public void smsStringConvertToEntityAttributeReturnsSmsEnumVariant() {
        SecondFactorMethod entityAttribute = secondFactorMethodConverter.convertToEntityAttribute("sms");
        assertThat(entityAttribute, is(SecondFactorMethod.SMS));
    }

    @Test
    public void appStringConvertToEntityAttributeReturnsAppEnumVariant() {
        SecondFactorMethod entityAttribute = secondFactorMethodConverter.convertToEntityAttribute("app");
        assertThat(entityAttribute, is(SecondFactorMethod.APP));
    }

    @Test
    public void unhandledStringConvertToEntityAttributeReturnsSmsEnumVariant() {
        SecondFactorMethod entityAttribute = secondFactorMethodConverter.convertToEntityAttribute("Someone went wild in the DB!");
        assertThat(entityAttribute, is(SecondFactorMethod.SMS));
    }

}
