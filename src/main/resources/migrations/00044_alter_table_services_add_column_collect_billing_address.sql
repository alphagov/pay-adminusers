--liquibase formatted sql

--changeset uk.gov.pay:alter_table_services_add_column_collect_billing_address
ALTER TABLE services ADD COLUMN collect_billing_address BOOLEAN NULL;
ALTER TABLE services ALTER COLUMN collect_billing_address SET DEFAULT true;
UPDATE services SET collect_billing_address = true WHERE collect_billing_address IS NULL;

--rollback ALTER TABLE services DROP COLUMN collect_billing_address;
