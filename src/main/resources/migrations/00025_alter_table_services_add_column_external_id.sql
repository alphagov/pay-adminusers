--liquibase formatted sql

--changeset uk.gov.pay:alter-table-service-add-column-external_id
ALTER TABLE services ADD COLUMN external_id VARCHAR(32) NOT NULL UNIQUE DEFAULT replace(uuid_generate_v4()::VARCHAR,'-','');

--rollback alter table invites drop column external_id;
