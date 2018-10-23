--liquibase formatted sql

--changeset uk.gov.pay:add_redirect_to_service_immediately_flag
ALTER TABLE services ADD COLUMN redirect_to_service_immediately_on_terminal_state boolean NULL;
ALTER TABLE services ALTER COLUMN redirect_to_service_immediately_on_terminal_state SET DEFAULT false;
UPDATE services SET redirect_to_service_immediately_on_terminal_state = false WHERE redirect_to_service_immediately_on_terminal_state IS NULL;

--rollback ALTER TABLE services DROP COLUMN redirect_to_service_immediately_on_terminal_state;