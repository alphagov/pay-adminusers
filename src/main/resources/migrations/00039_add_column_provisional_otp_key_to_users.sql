--liquibase formatted sql

--changeset uk.gov.pay:add_column_provisional_otp_key

ALTER TABLE users ADD COLUMN provisional_otp_key VARCHAR(255);

--rollback ALTER TABLE users DROP COLUMN provisional_otp_key;
