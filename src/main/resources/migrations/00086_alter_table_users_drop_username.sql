--liquibase formatted sql

--changeset uk.gov.pay:alter-table-users-drop_username
ALTER TABLE users DROP COLUMN username;
