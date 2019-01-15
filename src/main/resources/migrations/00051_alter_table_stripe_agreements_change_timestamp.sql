--liquibase formatted sql

--changeset uk.gov.pay:create_table-stripe_agreements
ALTER TABLE stripe_agreements ALTER COLUMN agreement_time TYPE TIMESTAMP WITH TIME ZONE;
--rollback alter table stripe_agreements alter column agreement_time TYPE TIMESTAMP WITHOUT TIME ZONE;