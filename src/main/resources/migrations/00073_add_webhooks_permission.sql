--liquibase formatted sql

--changeset uk.gov.pay:webhooks_permission
INSERT INTO permissions(id, name, description) VALUES ((SELECT MAX(id) + 1 FROM permissions), 'webhooks:read', 'View webhooks');
INSERT INTO permissions(id, name, description) VALUES ((SELECT MAX(id) + 1 FROM permissions), 'webhooks:update', 'Update webhooks');
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'webhooks:read'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'webhooks:update'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions WHERE name = 'webhooks:read'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions WHERE name = 'webhooks:update'), NOW(), NOW());