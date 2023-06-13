--liquibase formatted sql

--changeset uk.gov.pay:agreements_update_permission_for_refunds_roles
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'view-and-refund'), (SELECT id FROM permissions WHERE name = 'agreements:update'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'view-refund-and-initiate-moto'), (SELECT id FROM permissions WHERE name = 'agreements:update'), NOW(), NOW());