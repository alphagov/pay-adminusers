--liquibase formatted sql

--changeset uk.gov.pay:add_column_expiry_date_invites
ALTER TABLE services ADD COLUMN custom_branding TEXT NULL;
