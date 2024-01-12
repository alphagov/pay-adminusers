--liquibase formatted sql

--changeset uk.gov.pay:add_table-user_roles
CREATE TABLE user_roles (
    id SERIAL PRIMARY KEY,
    user_id INTEGER,
    role_id INTEGER
);
--rollback drop table user_roles;

--changeset uk.gov.pay:add_user_roles_fk-user_id
ALTER TABLE user_roles ADD CONSTRAINT fk_user_roles_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

--changeset uk.gov.pay:add_user_roles_fk-role_id
ALTER TABLE user_roles ADD CONSTRAINT fk_user_roles_roles FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE;
