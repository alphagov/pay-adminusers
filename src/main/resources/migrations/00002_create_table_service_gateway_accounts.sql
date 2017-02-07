--liquibase formatted sql

--changeset uk.gov.pay:add_table-service_gateway_accounts
CREATE TABLE service_gateway_accounts (
    id SERIAL PRIMARY KEY,
    service_id INTEGER,
    gateway_account_id VARCHAR(255) UNIQUE
);
--rollback drop table service_gateway_accounts;

--changeset uk.gov.pay:add_fk-service_id
ALTER TABLE service_gateway_accounts ADD CONSTRAINT fk_service_gateway_accounts_services FOREIGN KEY (service_id) REFERENCES services (id);

