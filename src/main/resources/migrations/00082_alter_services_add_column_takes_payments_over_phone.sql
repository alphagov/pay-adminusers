--liquibase formatted sql

--changeset uk.gov.pay:alter_services_add_column_takes_payments_over_phone
ALTER TABLE services ADD COLUMN takes_payments_over_phone BOOLEAN NOT NULL DEFAULT false;

--rollback ALTER TABLE services DROP COLUMN takes_payments_over_phone;