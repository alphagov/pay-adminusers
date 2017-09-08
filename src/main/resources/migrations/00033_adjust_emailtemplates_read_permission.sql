--liquibase formatted sql

--changeset uk.gov.pay:adjust_emailtemplates_read_permission
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'view-and-refund'), (SELECT id FROM permissions  WHERE name = 'email-notification-template:read'), NOW(), NOW());
INSERT INTO  role_permission VALUES ((SELECT id FROM roles WHERE name = 'view-only'), (SELECT id FROM permissions  WHERE name = 'email-notification-template:read'), NOW(), NOW());
