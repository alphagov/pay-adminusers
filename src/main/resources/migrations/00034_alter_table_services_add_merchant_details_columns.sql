--liquibase formatted sql

--changeset uk.gov.pay:alter_table-services-add-merchant-details

ALTER TABLE services
ADD COLUMN merchant_name VARCHAR(255),
ADD COLUMN merchant_address_line1 VARCHAR(255),
ADD COLUMN merchant_address_line2 VARCHAR(255),
ADD COLUMN merchant_address_city VARCHAR(255),
ADD COLUMN merchant_address_postcode VARCHAR(25),
ADD COLUMN merchant_address_country VARCHAR(10);

--rollback alter table services drop column merchant_name, drop column merchant_address_line1, drop column merchant_address_line2, drop column merchant_address_city, drop column merchant_address_postcode, drop column merchant_address_country;
