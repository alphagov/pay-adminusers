--liquibase formatted sql

--changeset uk.gov.pay:alter-table-services-alter-column-name
ALTER TABLE services ALTER COLUMN name TYPE VARCHAR(50);

--rollback ALTER TABLE services ALTER COLUMN name TYPE VARCHAR(255);
