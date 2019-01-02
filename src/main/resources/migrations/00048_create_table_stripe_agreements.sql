--liquibase formatted sql

--changeset uk.gov.pay:create_table-stripe_agreements
CREATE TABLE stripe_agreements (
  id SERIAL PRIMARY KEY,
  service_id INT NOT NULL REFERENCES services (id),
  ip_address VARCHAR(45) NOT NULL,
  agreement_time TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
--rollback drop table stripe_agreements;