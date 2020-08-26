--liquibase formatted sql

--changeset uk.gov.pay:moto_mask_input_update_permission
INSERT INTO permissions(id, name, description) VALUES (49, 'moto-mask-input:update', 'Update MOTO mask input settings');
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions WHERE name = 'moto-mask-input:update'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'moto-mask-input:update'), NOW(), NOW());

--changeset uk.gov.pay:moto_mask_input_read_permission
INSERT INTO  permissions(id, name, description) VALUES (50, 'moto-mask-input:read', 'View MOTO mask input settings');
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions WHERE name = 'moto-mask-input:read'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'moto-mask-input:read'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'view-and-refund'), (SELECT id FROM permissions WHERE name = 'moto-mask-input:read'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'view-only'), (SELECT id FROM permissions WHERE name = 'moto-mask-input:read'), NOW(), NOW());

