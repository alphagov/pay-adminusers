--liquibase formatted sql

--changeset uk.gov.pay:alter_users_add_column_time_zone
ALTER TABLE users ADD COLUMN time_zone VARCHAR(50) default 'Europe/London';
