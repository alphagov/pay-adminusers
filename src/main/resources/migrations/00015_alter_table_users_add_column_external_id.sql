--liquibase formatted sql

--changeset uk.gov.pay:alter-table-users-add-column-external_id
ALTER TABLE users ADD COLUMN external_id VARCHAR(32) NOT NULL UNIQUE DEFAULT replace(uuid_generate_v4()::VARCHAR,'-','');

--rollback alter table users drop column external_id;
