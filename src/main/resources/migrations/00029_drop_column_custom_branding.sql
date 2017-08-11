--liquibase formatted sql

--changeset uk.gov.pay:drop_column_custom_branding
ALTER TABLE services DROP COLUMN IF EXISTS custom_branding;
