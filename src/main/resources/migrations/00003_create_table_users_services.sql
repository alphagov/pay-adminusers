--liquibase formatted sql

--changeset uk.gov.pay:add_table-users_services
CREATE TABLE users_services (
    user_id INTEGER,
    service_id BIGINT
);
--rollback drop table users_services;

--changeset uk.gov.pay:add_fk-user_id
ALTER TABLE users_services ADD CONSTRAINT fk_users_services_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

--changeset uk.gov.pay:add_fk-service_id
ALTER TABLE users_services ADD CONSTRAINT fk_users_services_services FOREIGN KEY (service_id) REFERENCES services (id) ON DELETE CASCADE;

--changeset uk.gov.pay:add_pk-user_id-service_id
ALTER TABLE users_services ADD PRIMARY KEY (user_id, service_id);
