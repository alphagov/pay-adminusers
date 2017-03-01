--liquibase formatted sql

--changeset uk.gov.pay:insert_toggle_3ds_read_permission
INSERT INTO  permissions(id, name, description) VALUES (30, 'toggle-3ds:read', 'View 3D Secure setting');
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions WHERE name = 'toggle-3ds:read'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'toggle-3ds:read'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'view-and-refund'), (SELECT id FROM permissions WHERE name = 'toggle-3ds:read'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'view-only'), (SELECT id FROM permissions WHERE name = 'toggle-3ds:read'), NOW(), NOW());

--changeset uk.gov.pay:insert_toggle_3ds_update_permission
INSERT INTO  permissions(id, name, description) VALUES (31, 'toggle-3ds:update', 'Edit 3D Secure setting');
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'toggle-3ds:update'), NOW(), NOW());
