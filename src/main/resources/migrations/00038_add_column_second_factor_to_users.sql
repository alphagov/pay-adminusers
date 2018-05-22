--liquibase formatted sql

--changeset uk.gov.pay:add_column_second_factor_to_users

-- This will lock the users table (because NOT NULL and DEFAULT) but
-- it only has ~650 rows and gets fairly light usage, so it shouldn’t
-- cause any noticable impact
ALTER TABLE users ADD COLUMN second_factor VARCHAR(16) NOT NULL DEFAULT 'sms';

--rollback ALTER TABLE users DROP COLUMN second_factor;
