--liquibase formatted sql

--changeset uk.gov.pay:alter-table-invites-add-column-type
ALTER TABLE invites ADD COLUMN type VARCHAR(255) NOT NULL DEFAULT 'user';

--rollback alter table invites drop column type;
