--liquibase formatted sql

--changeset uk.gov.pay:add_column_merchant_url_to_services
ALTER TABLE services ADD COLUMN merchant_url TEXT NULL;

--rollback ALTER TABLE services DROP COLUMN merchant_url;
