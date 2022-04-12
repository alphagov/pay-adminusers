--liquibase formatted sql

--changeset uk.gov.pay:add_fts_index_for_services_on_merchant_name
CREATE INDEX merchant_name_fts_index on services using gin (to_tsvector('english', merchant_name));
