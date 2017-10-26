--liquibase formatted sql

--changeset uk.gov.pay:merchant_details_read_permission
INSERT INTO  permissions(id, name, description) VALUES (33, 'merchant-details:read', 'View Merchant Details setting');
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions WHERE name = 'merchant-details:read'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'merchant-details:read'), NOW(), NOW());

--changeset uk.gov.pay:merchant_details_update_permission
INSERT INTO  permissions(id, name, description) VALUES (34, 'merchant-details:update', 'Edit Merchant Details setting');
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions WHERE name = 'merchant-details:update'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'merchant-details:update'), NOW(), NOW());
