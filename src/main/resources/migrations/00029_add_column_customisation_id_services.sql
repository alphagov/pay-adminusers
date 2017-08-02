--liquibase formatted sql

--changeset uk.gov.pay:add_column_expiry_date_invites
ALTER TABLE services ADD COLUMN customisations_id INTEGER NULL REFERENCES service_customisations(id);

