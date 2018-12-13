--liquibase formatted sql

--changeset uk.gov.pay:alter_table_services_add_column_current_go_live_stage
ALTER TABLE services ADD COLUMN current_go_live_stage VARCHAR(50) NULL;
ALTER TABLE services ALTER COLUMN current_go_live_stage SET DEFAULT 'NOT_STARTED';
UPDATE services SET current_go_live_stage = 'NOT_STARTED' WHERE current_go_live_stage IS NULL;

--rollback ALTER TABLE services DROP COLUMN current_go_live_stage;
