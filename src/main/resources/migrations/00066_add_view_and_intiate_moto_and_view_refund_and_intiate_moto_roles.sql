--liquibase formatted sql

--changeset uk.gov.pay:view_and_initiate_moto_role
INSERT INTO roles(id, name, description) VALUES ((SELECT MAX(id) + 1 FROM roles), 'view-and-initiate-moto', 'View and create MOTO payments');
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'transactions:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'transactions-by-date:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'transactions-by-fields:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'transactions-download:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'transactions-details:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'transactions-events:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'transactions-amount:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'transactions-description:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'transactions-email:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'transactions-card-type:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'service-name:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'toggle-3ds:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'payment-types:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'email-notification-template:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'toggle-billing-address:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'payouts:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'moto-mask-input:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'agent-initiated-moto:create'));

--changeset uk.gov.pay:view_refund_and_initiate_moto_role
INSERT INTO roles(id, name, description) VALUES ((SELECT MAX(id) + 1 FROM roles), 'view-refund-and-initiate-moto', 'View, refund and create MOTO payments');
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-refund-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'transactions:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-refund-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'transactions-by-date:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-refund-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'transactions-by-fields:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-refund-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'transactions-download:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-refund-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'transactions-details:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-refund-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'transactions-events:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-refund-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'refunds:create'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-refund-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'transactions-amount:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-refund-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'transactions-description:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-refund-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'transactions-email:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-refund-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'transactions-card-type:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-refund-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'service-name:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-refund-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'toggle-3ds:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-refund-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'payment-types:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-refund-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'email-notification-template:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-refund-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'toggle-billing-address:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-refund-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'payouts:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-refund-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'moto-mask-input:read'));
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'view-refund-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'agent-initiated-moto:create'));
