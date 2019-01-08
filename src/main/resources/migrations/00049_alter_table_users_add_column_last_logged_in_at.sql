--liquibase formatted sql

--changeset uk.gov.pay:alter_table_users_add_column_last_logged_in_at

ALTER TABLE users ADD COLUMN last_logged_in_at TIMESTAMP WITH TIME ZONE;

--rollback ALTER TABLE users DROP COLUMN last_logged_in_at;
