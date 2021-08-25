--liquibase formatted sql

--changeset uk.gov.pay:alter_table_services_add_column_default_billing_address_country

ALTER TABLE services ADD COLUMN default_billing_address_country VARCHAR(2) NULL;
ALTER TABLE services ALTER COLUMN default_billing_address_country SET DEFAULT 'GB';
UPDATE services SET default_billing_address_country = 'GB' WHERE default_billing_address_country IS NULL;

--rollback ALTER TABLE services DROP COLUMN default_billing_address_country;
