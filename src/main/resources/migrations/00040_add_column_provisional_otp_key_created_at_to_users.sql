--liquibase formatted sql

--changeset uk.gov.pay:add_column_provisional_otp_key_created_at

ALTER TABLE users ADD COLUMN provisional_otp_key_created_at TIMESTAMP WITH TIME ZONE;

--rollback ALTER TABLE users DROP COLUMN provisional_otp_key_created_at;
