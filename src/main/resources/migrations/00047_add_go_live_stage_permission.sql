--liquibase formatted sql

--changeset uk.gov.pay:go_live_stage_update_permission
INSERT INTO permissions(id, name, description) VALUES (37, 'go-live-stage:update', 'Update Go Live stage');
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions WHERE name = 'go-live-stage:update'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'go-live-stage:update'), NOW(), NOW());

--changeset uk.gov.pay:go_live_stage_read_permission
INSERT INTO  permissions(id, name, description) VALUES (38, 'go-live-stage:read', 'View Go Live stage');
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions WHERE name = 'go-live-stage:read'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'go-live-stage:read'), NOW(), NOW());
