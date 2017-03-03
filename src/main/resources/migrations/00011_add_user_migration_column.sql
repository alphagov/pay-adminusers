--liquibase formatted sql

--changeset uk.gov.pay:alter_table-users-add-is_new

ALTER TABLE users ADD COLUMN is_new INTEGER NOT NULL DEFAULT 0;

--rollback alter table users drop column is_new;
