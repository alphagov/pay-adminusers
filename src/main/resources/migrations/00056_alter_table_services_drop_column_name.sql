--liquibase formatted sql

--changeset uk.gov.pay:alter-table-services-drop-column-name
ALTER TABLE services DROP COLUMN name;
