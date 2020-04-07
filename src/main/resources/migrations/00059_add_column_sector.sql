--liquibase formatted sql

--changeset uk.gov.pay:add_column_sector
ALTER TABLE services ADD COLUMN sector varchar(50)
