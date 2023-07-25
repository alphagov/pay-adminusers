--liquibase formatted sql

--changeset uk.gov.pay:alter-table-users-remove-username-not-null-constraint
ALTER TABLE users ALTER COLUMN username DROP NOT NULL;

--rollback ALTER TABLE users ALTER COLUMN username SET NOT NULL;
