--liquibase formatted sql

--changeset uk.gov.pay:add_table-service_customisations
CREATE TABLE service_customisations (
    id SERIAL PRIMARY KEY,
    banner_colour VARCHAR(50) NULL,
    logo_url TEXT NULL,
    updated TIMESTAMP WITH TIME ZONE DEFAULT (now() AT TIME ZONE 'utc') NOT NULL,
    version INTEGER DEFAULT 0 NOT NULL
);
--rollback drop table service_customisations;
