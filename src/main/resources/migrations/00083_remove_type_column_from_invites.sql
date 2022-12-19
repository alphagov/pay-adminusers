--liquibase formatted sql

--changeset uk.gov.pay:remove_type_column_from_invites

ALTER TABLE invites DROP COLUMN "type";
