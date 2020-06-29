--liquibase formatted sql

--changeset uk.gov.pay:add_services_column_internal
ALTER TABLE services ADD COLUMN internal BOOLEAN NULL;
--rollback ALTER TABLE services DROP COLUMN internal;

--changeset uk.gov.pay:add_services_column_archived
ALTER TABLE services ADD COLUMN archived BOOLEAN NULL;
ALTER TABLE services ALTER COLUMN archived SET DEFAULT false;
--rollback ALTER TABLE services DROP COLUMN archived;

--changeset uk.gov.pay:add_services_column_created_date
ALTER TABLE services ADD COLUMN created_date TIMESTAMP WITH TIME ZONE;
ALTER TABLE services ALTER COLUMN created_date SET DEFAULT (now() at time zone 'utc');
--rollback ALTER TABLE services DROP COLUMN created_date;

--changeset uk.gov.pay:add_services_column_went_live_date
ALTER TABLE services ADD COLUMN went_live_date TIMESTAMP WITH TIME ZONE;
--rollback ALTER TABLE services DROP COLUMN went_live_date;