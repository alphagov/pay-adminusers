--liquibase formatted sql

--changeset uk.gov.pay:insert_tokens_read_permission
INSERT INTO  permissions(id, name, description) VALUES (29, 'tokens:read', 'View keys');
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions  WHERE name = 'tokens:read'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions  WHERE name = 'tokens:read'), NOW(), NOW());
