--liquibase formatted sql

--changeset uk.gov.pay:agreements_read_update_permission
INSERT INTO  permissions(id, name, description) VALUES ((SELECT MAX(id) + 1 FROM permissions), 'agreements:read', 'View agreements');
INSERT INTO  permissions(id, name, description) VALUES ((SELECT MAX(id) + 1 FROM permissions), 'agreements:update', 'Update agreements');
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions WHERE name = 'agreements:read'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'agreements:read'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'view-and-refund'), (SELECT id FROM permissions WHERE name = 'agreements:read'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'view-only'), (SELECT id FROM permissions WHERE name = 'agreements:read'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'view-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'agreements:read'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'view-refund-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'agreements:read'), NOW(), NOW());

INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions WHERE name = 'agreements:update'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'agreements:update'), NOW(), NOW());