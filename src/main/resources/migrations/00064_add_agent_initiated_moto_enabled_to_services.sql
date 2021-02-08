--liquibase formatted sql

--changeset uk.gov.pay:add_agent_initiated_moto_enabled_to_services
ALTER TABLE services ADD COLUMN agent_initiated_moto_enabled BOOLEAN NOT NULL DEFAULT false;
