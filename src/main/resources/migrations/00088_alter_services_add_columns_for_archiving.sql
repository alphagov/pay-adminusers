--liquibase formatted sql

--changeset uk.gov.pay:alter_services_add_column_first_checked_for_archival_date
ALTER TABLE services ADD COLUMN first_checked_for_archival_date TIMESTAMP WITH TIME ZONE;

--changeset uk.gov.pay:alter_services_add_column_skip_checking_for_archival_until_date
ALTER TABLE services ADD COLUMN skip_checking_for_archival_until_date TIMESTAMP WITH TIME ZONE;

--rollback ALTER TABLE services DROP COLUMN first_checked_for_archival_date;
--rollback ALTER TABLE services DROP COLUMN skip_checking_for_archival_until_date;