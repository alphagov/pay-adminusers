--liquibase formatted sql

--changeset uk.gov.pay:stripe_director_update_permission
INSERT INTO permissions(id, name, description) VALUES ((SELECT MAX(id) + 1 FROM permissions), 'stripe-director:update', 'Update Stripe director');
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions WHERE name = 'stripe-director:update'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'stripe-director:update'), NOW(), NOW());

--changeset uk.gov.pay:stripe_director_read_permission
INSERT INTO  permissions(id, name, description) VALUES ((SELECT MAX(id) + 1 FROM permissions), 'stripe-director:read', 'View Stripe director');
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions WHERE name = 'stripe-director:read'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'stripe-director:read'), NOW(), NOW());