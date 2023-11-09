--liquibase formatted sql

--changeset uk.gov.pay:alter_services_add_column_archived_date
ALTER TABLE services ADD COLUMN archived_date TIMESTAMP WITH TIME ZONE;

--rollback ALTER TABLE services DROP COLUMN alter_services_add_column_archived_date;