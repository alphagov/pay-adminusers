--liquibase formatted sql

--changeset uk.gov.pay:stripe_account_details_update_permission
INSERT INTO  permissions(id, name, description) VALUES (48, 'stripe-account-details:update', 'UpdateStripeAccountDetails');
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions WHERE name = 'stripe-account-details:update'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'stripe-account-details:update'), NOW(), NOW());
