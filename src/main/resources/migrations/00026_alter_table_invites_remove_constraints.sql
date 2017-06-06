--liquibase formatted sql

--changeset uk.gov.pay:alter-table-invites-remove-constraints
ALTER TABLE invites ALTER COLUMN sender_id DROP NOT NULL;
ALTER TABLE invites ALTER COLUMN service_id DROP NOT NULL;

--rollback ALTER TABLE INVITES ALTER COLUMN SENDER_ID SET NOT NULL;
--rollback ALTER TABLE INVITES ALTER COLUMN SERVICE_ID SET NOT NULL;
