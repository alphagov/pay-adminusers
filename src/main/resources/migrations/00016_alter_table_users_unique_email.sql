--liquibase formatted sql

--changeset uk.gov.pay:alter-table-users-update-column-email-unique-back
ALTER TABLE users ADD CONSTRAINT users_email_unique UNIQUE (email);

--rollback ALTER TABLE users DROP CONSTRAINT users_email_unique;
