--liquibase formatted sql

--changeset uk.gov.pay:drop-gateway_account_id-column
ALTER TABLE users DROP COLUMN IF EXISTS gateway_account_id;
