--liquibase formatted sql

--changeset uk.gov.pay:alter-table-invites-add-column-disabled
ALTER TABLE invites ADD COLUMN disabled BOOLEAN NOT NULL DEFAULT FALSE;

--rollback alter table invites drop column disabled;
