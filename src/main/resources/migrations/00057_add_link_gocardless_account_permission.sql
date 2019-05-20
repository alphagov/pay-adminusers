--liquibase formatted sql

--changeset uk.gov.pay:connected_gocardless_account_update_permission
INSERT INTO permissions(id, name, description) VALUES (45, 'connected-gocardless-account:update', 'Update Connected Go Cardless Account');
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions WHERE name = 'connected-gocardless-account:update'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'connected-gocardless-account:update'), NOW(), NOW());

--changeset uk.gov.pay:connected_gocardless_account_read_permission
INSERT INTO  permissions(id, name, description) VALUES (46, 'connected-gocardless-account:read', 'View Connected Go Cardless Account');
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions WHERE name = 'connected-gocardless-account:read'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'connected-gocardless-account:read'), NOW(), NOW());
