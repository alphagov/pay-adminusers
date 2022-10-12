--liquibase formatted sql

--changeset uk.gov.pay:alter_table_users_set_column_telephone_number_to_nullable
ALTER TABLE users ALTER column telephone_number DROP NOT NULL;

-- rollback ALTER TABLE users ALTER column telephone_number SET NOT NULL;