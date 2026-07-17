--liquibase formatted sql

--changeset uk.gov.pay:create_table_service_features
CREATE TABLE service_features (
    id         SERIAL PRIMARY KEY,
    service_id INT NOT NULL REFERENCES services (id) ON DELETE CASCADE,
    feature    VARCHAR(255) NOT NULL
);

--rollback drop table service_features;