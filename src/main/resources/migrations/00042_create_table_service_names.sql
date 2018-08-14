--liquibase formatted sql

--changeset uk.gov.pay:create_table-service_names
CREATE TABLE service_names (
  id SERIAL PRIMARY KEY,
  service_id BIGINT NOT NULL REFERENCES services (id),
  language VARCHAR(2) NOT NULL,
  name VARCHAR(255) NOT NULL,
  UNIQUE(service_id, language)
);
--rollback drop table service_names;

