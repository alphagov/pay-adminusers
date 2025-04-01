--liquibase formatted sql dbms:postgresql splitStatements:false

--changeset uk.gov.pay:create_user_mfa_method
CREATE TABLE user_mfa_method
(
    id           SERIAL PRIMARY KEY,
    user_id      INTEGER REFERENCES users (id) ON DELETE CASCADE,
    invite_id    INTEGER REFERENCES invites (id),
    external_id  VARCHAR(50)                 DEFAULT 'mfa_' || replace(uuid_generate_v4()::VARCHAR, '-', ''),
    description  VARCHAR(200),
    method       VARCHAR(50) NOT NULL, -- 'app', 'sms', 'backup_code'
    otp_key      VARCHAR(200),
    backup_code  VARCHAR(200),
    phone_number VARCHAR(200),
    is_primary   BOOLEAN                     DEFAULT FALSE,
    is_active    BOOLEAN                     DEFAULT FALSE,
    created_at   TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version      INTEGER                     DEFAULT 0
);

insert into user_mfa_method (user_id, method, otp_key, is_active, created_at, updated_at)
select id, 'sms', otp_key, true, now(), now()
from users
where second_factor = 'sms';

insert into user_mfa_method (user_id, method, otp_key, is_active, created_at, updated_at)
select id, 'app', otp_key, true, now(), now()
from users
where second_factor = 'app';

--rollback drop table user_mfa_method;
