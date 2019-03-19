--liquibase formatted sql

--changeset uk.gov.pay:stripe_vat_number_company_number_update_permission
INSERT INTO permissions(id, name, description) VALUES (43, 'stripe-vat-number-company-number:update', 'Update Stripe vat number company number');
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions WHERE name = 'stripe-vat-number-company-number:update'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'stripe-vat-number-company-number:update'), NOW(), NOW());

--changeset uk.gov.pay:stripe_vat_number_company_number_read_permission
INSERT INTO  permissions(id, name, description) VALUES (44, 'stripe-vat-number-company-number:read', 'View Stripe vat number company number');
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions WHERE name = 'stripe-vat-number-company-number:read'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'stripe-vat-number-company-number:read'), NOW(), NOW());
