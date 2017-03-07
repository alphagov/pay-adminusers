--liquibase formatted sql

--changeset uk.gov.pay:alter_table-service_gateway_accounts-drop-constraint

ALTER TABLE service_gateway_accounts DROP CONSTRAINT service_gateway_accounts_gateway_account_id_key;

--rollback ALTER TABLE service_gateway_accounts ADD CONSTRAINT service_gateway_accounts_gateway_account_id_key UNIQUE (gateway_account_id);
