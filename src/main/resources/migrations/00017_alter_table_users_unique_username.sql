--liquibase formatted sql

--changeset uk.gov.pay:alter-table-users-update-column-username-unique-back
ALTER TABLE users ADD CONSTRAINT users_username_unique UNIQUE (username);

--rollback ALTER TABLE users DROP CONSTRAINT users_username_unique;
