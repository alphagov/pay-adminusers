--liquibase formatted sql

--changeset uk.gov.pay:add_column_experimental_features_enabled
ALTER TABLE services ADD COLUMN experimental_features_enabled boolean NULL;