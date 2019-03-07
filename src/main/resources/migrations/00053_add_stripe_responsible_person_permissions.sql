--liquibase formatted sql

--changeset uk.gov.pay:stripe_responsible_person_update_permission
INSERT INTO permissions(id, name, description) VALUES (41, 'stripe-responsible-person:update', 'Update Stripe responsible person');
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions WHERE name = 'stripe-responsible-person:update'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'stripe-responsible-person:update'), NOW(), NOW());

--changeset uk.gov.pay:stripe_responsible_person_read_permission
INSERT INTO  permissions(id, name, description) VALUES (42, 'stripe-responsible-person:read', 'View Stripe responsible person');
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions WHERE name = 'stripe-responsible-person:read'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'stripe-responsible-person:read'), NOW(), NOW());