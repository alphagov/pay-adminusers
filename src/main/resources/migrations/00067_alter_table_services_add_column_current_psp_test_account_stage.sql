--liquibase formatted sql

--changeset uk.gov.pay:alter_table_services_add_column_current_psp_test_account_stage
ALTER TABLE services ADD COLUMN current_psp_test_account_stage VARCHAR(50) NULL;
ALTER TABLE services ALTER COLUMN current_psp_test_account_stage SET DEFAULT 'NOT_STARTED';
UPDATE services SET current_psp_test_account_stage = 'NOT_STARTED' WHERE current_go_live_stage IS NULL;

--rollback ALTER TABLE services DROP COLUMN current_psp_test_account_stage;
