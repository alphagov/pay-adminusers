--liquibase formatted sql

--changeset uk.gov.pay:alter_table-service-add-name

ALTER TABLE services ADD COLUMN name VARCHAR(255) NOT NULL DEFAULT 'System Generated';

--rollback alter table services drop column name;
