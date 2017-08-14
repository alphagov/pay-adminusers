--liquibase formatted sql

--changeset uk.gov.pay:add_column_custom_branding
ALTER TABLE services ADD COLUMN custom_branding json NULL;
