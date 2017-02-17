--liquibase formatted sql

--changeset uk.gov.pay:update_role_permission
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'super-admin'), (SELECT id FROM permissions WHERE name = 'transactions-card-type:read'), NOW(), NOW());
