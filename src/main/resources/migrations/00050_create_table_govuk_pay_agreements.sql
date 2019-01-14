--liquibase formatted sql

--changeset uk.gov.pay:create_table_govuk_pay_agreements
CREATE TABLE govuk_pay_agreements (
  id SERIAL PRIMARY KEY,
  service_id INT NOT NULL UNIQUE REFERENCES services (id),
  email VARCHAR(254) NOT NULL,
  agreement_time TIMESTAMP WITH TIME ZONE NOT NULL
);
--rollback drop table govuk_pay_agreements;