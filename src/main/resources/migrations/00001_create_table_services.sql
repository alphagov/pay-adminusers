--liquibase formatted sql

--changeset uk.gov.pay:add_table-services
CREATE TABLE services (
    id SERIAL PRIMARY KEY
);
--rollback drop table services;

