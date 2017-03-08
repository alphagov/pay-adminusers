--liquibase formatted sql

--changeset uk.gov.pay:drop_table-user_role
DROP TABLE IF EXISTS user_role;
