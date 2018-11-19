--liquibase formatted sql

--changeset uk.gov.pay:insert_toggle_billing_address_read_permission
INSERT INTO permissions(id, name, description) VALUES (35, 'toggle-billing-address:read', 'View Billing Address setting');
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions WHERE name = 'toggle-billing-address:read'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'toggle-billing-address:read'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'view-and-refund'), (SELECT id FROM permissions WHERE name = 'toggle-billing-address:read'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'view-only'), (SELECT id FROM permissions WHERE name = 'toggle-billing-address:read'), NOW(), NOW());

--changeset uk.gov.pay:insert_toggle_billing_address_update_permission
INSERT INTO  permissions(id, name, description) VALUES (36, 'toggle-billing-address:update', 'Edit Billing Address setting');
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'toggle-billing-address:update'), NOW(), NOW());
