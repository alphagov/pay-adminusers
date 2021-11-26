--liquibase formatted sql

--changeset uk.gov.pay:stripe_government_entity_document_update_permission
INSERT INTO permissions(id, name, description) VALUES ((SELECT MAX(id) + 1 FROM permissions), 'stripe-government-entity-document:update', 'Stripe - Upload government entity document ');
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions WHERE name = 'stripe-government-entity-document:update'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'stripe-government-entity-document:update'), NOW(), NOW());

