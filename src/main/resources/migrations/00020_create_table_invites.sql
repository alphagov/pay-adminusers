--liquibase formatted sql

--changeset uk.gov.pay:add_table-invites
CREATE TABLE invites (
    id SERIAL PRIMARY KEY,
    service_id INTEGER NOT NULL,
    role_id INTEGER NOT NULL,
    sender_id INTEGER NOT NULL,
    email VARCHAR(255) NOT NULL,
    telephone_number VARCHAR(255),
    code VARCHAR(255) NOT NULL UNIQUE,
    otp_key VARCHAR(255) NOT NULL,
    date TIMESTAMP WITH TIME ZONE DEFAULT (now() AT TIME ZONE 'utc') NOT NULL,
    version INTEGER DEFAULT 0 NOT NULL
);
--rollback drop table invites;

--changeset uk.gov.pay:add_invites_roles_fk-role_id
ALTER TABLE invites ADD CONSTRAINT fk_invites_roles FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE;

--changeset uk.gov.pay:add_invites_services_fk-service_id
ALTER TABLE invites ADD CONSTRAINT fk_invites_services FOREIGN KEY (service_id) REFERENCES services (id) ON DELETE CASCADE;

--changeset uk.gov.pay:add_invites_users_fk-user_id
ALTER TABLE invites ADD CONSTRAINT fk_invites_users FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE CASCADE;

--changeset uk.gov.pay:add_invites_code_index
CREATE INDEX invites_code_idx ON invites (code);
