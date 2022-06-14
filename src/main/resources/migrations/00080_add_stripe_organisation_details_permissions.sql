--liquibase formatted sql

--changeset uk.gov.pay:stripe_organisation_details_update_permission
INSERT INTO permissions(id, name, description) VALUES ((SELECT MAX(id) + 1 FROM permissions), 'stripe-organisation-details:update', 'Update Organisation details on Stripe');
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions WHERE name = 'stripe-organisation-details:update'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'stripe-organisation-details:update'), NOW(), NOW());

--changeset uk.gov.pay:stripe_organisation_details_read_permission
INSERT INTO  permissions(id, name, description) VALUES ((SELECT MAX(id) + 1 FROM permissions), 'stripe-organisation-details:read', 'View Organisation details on Stripe');
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions WHERE name = 'stripe-organisation-details:read'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'stripe-organisation-details:read'), NOW(), NOW());