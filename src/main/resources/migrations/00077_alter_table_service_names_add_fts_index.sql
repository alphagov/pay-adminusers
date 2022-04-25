--liquibase formatted sql

--changeset uk.gov.pay:add_fts_index_for_service_names_on_name
CREATE INDEX name_fts_index on service_names using gin (to_tsvector('english', name));