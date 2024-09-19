--liquibase formatted sql

--changeset uk.gov.pay:add_column_is_worldpay_test_service
ALTER TABLE services ADD COLUMN is_worldpay_test_service BOOLEAN DEFAULT FALSE;

--rollback ALTER TABLE services DROP COLUMN is_worldpay_test_service;
