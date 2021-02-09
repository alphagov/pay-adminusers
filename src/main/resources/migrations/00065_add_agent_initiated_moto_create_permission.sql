--liquibase formatted sql

--changeset uk.gov.pay:agent_initiated_moto_permission
INSERT INTO permissions(id, name, description) VALUES ((SELECT MAX(id) + 1 FROM permissions), 'agent-initiated-moto:create', 'Create payment from agent-initiated MOTO product');
INSERT INTO role_permission (role_id, permission_id) VALUES ((SELECT id FROM roles WHERE name = 'admin'), (SELECT id FROM permissions WHERE name = 'agent-initiated-moto:create'));
