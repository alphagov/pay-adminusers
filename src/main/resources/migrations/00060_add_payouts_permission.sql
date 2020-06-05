--liquibase formatted sql

--changeset uk.gov.pay:payouts_read_permission
INSERT INTO  permissions(id, name, description) VALUES (47, 'payouts:read', 'View Payouts');
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions WHERE name = 'payouts:read'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'payouts:read'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'view-and-refund'), (SELECT id FROM permissions WHERE name = 'payouts:read'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'view-only'), (SELECT id FROM permissions WHERE name = 'payouts:read'), NOW(), NOW());
