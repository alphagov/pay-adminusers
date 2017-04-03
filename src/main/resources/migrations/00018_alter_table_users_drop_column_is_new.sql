--liquibase formatted sql

--changeset uk.gov.pay:alter-table-users-drop-column-is_new
ALTER TABLE users DROP COLUMN is_new;

--rollback ALTER TABLE users ADD COLUMN is_new INTEGER NOT NULL DEFAULT 1;
