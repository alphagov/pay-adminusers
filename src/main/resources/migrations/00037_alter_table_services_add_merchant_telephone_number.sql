--liquibase formatted sql

--changeset uk.gov.pay:alter_table-services-add-merchant-telephone-number

ALTER TABLE services ADD COLUMN merchant_telephone_number VARCHAR(255);

--rollback alter table services drop column merchant_telephone_number;
