--liquibase formatted sql

--changeset uk.gov.pay:alter_table-services-add-merchant-email

ALTER TABLE services ADD COLUMN merchant_email VARCHAR(255);

--rollback alter table services drop column merchant_email;
