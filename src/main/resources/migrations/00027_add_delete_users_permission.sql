--liquibase formatted sql

--changeset uk.gov.pay:insert_users-service_delete_permission
INSERT INTO  permissions(id, name, description) VALUES (32, 'users-service:delete', 'Remove user from a service');
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions  WHERE name = 'users-service:delete'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions  WHERE name = 'users-service:delete'), NOW(), NOW());
