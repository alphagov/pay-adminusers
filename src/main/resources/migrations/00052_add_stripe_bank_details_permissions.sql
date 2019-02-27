--liquibase formatted sql

--changeset uk.gov.pay:stripe_bank_details_update_permission
INSERT INTO permissions(id, name, description) VALUES (39, 'stripe-bank-details:update', 'Update Stripe bank details');
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions WHERE name = 'stripe-bank-details:update'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'stripe-bank-details:update'), NOW(), NOW());

--changeset uk.gov.pay:stripe_bank_details_read_permission
INSERT INTO  permissions(id, name, description) VALUES (40, 'stripe-bank-details:read', 'View Stripe bank details');
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions WHERE name = 'stripe-bank-details:read'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'stripe-bank-details:read'), NOW(), NOW());
