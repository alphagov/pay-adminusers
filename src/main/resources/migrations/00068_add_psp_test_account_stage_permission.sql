--liquibase formatted sql

--changeset uk.gov.pay:psp_test_account_stage_update_permission
INSERT INTO permissions(id, name, description) VALUES ((SELECT MAX(id) + 1 FROM permissions), 'psp_test_account_stage:update', 'Update PSP Test Account stage');
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions WHERE name = 'psp_test_account_stage:update'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'psp_test_account_stage:update'), NOW(), NOW());

