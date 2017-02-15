--liquibase formatted sql

--changeset uk.gov.pay:add_table-user_services_roles
CREATE TABLE user_services_roles (
    user_id INTEGER,
    service_id INTEGER,
    role_id INTEGER
);
--rollback drop table user_services_roles;

--changeset uk.gov.pay:add_services_roles_fk-user_id
ALTER TABLE user_services_roles ADD CONSTRAINT fk_services_roles_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

--changeset uk.gov.pay:add_services_roles_fk-service_id
ALTER TABLE user_services_roles ADD CONSTRAINT fk_services_roles_services FOREIGN KEY (service_id) REFERENCES services (id) ON DELETE CASCADE;

--changeset uk.gov.pay:add_services_roles_fk-role_id
ALTER TABLE user_services_roles ADD CONSTRAINT fk_services_roles_roles FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE;

--changeset uk.gov.pay:add_pk-user_id-service_id_role_id
ALTER TABLE user_services_roles ADD PRIMARY KEY (user_id, service_id, role_id);
