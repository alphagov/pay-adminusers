--liquibase formatted sql

--changeset uk.gov.pay:add_column_expiry_date_invites
ALTER TABLE invites ADD COLUMN expiry_date TIMESTAMP WITH TIME ZONE NOT NULL;

--changeset uk.gov.pay:add_invites_code_index
CREATE INDEX invites_expiry_date_idx ON invites (expiry_date DESC);
