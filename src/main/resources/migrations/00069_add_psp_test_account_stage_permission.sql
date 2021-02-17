--liquibase formatted sql

--changeset uk.gov.pay:psp_test_account_stage_update_permission
UPDATE permissions SET name = 'psp-test-account-stage:update' WHERE name = 'psp_test_account_stage:update';
