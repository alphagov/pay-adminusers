--liquibase formatted sql

--changeset uk.gov.pay:alter-table-invites-add-column-login_counter
ALTER TABLE invites ADD COLUMN login_counter INTEGER NOT NULL DEFAULT 0;

--rollback alter table invites drop column login_counter;
