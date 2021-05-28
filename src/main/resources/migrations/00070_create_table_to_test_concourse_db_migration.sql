--liquibase formatted sql

--changeset uk.gov.pay:test_concourse_db_migration
CREATE TABLE test_concourse_db_migration (
    id BIGSERIAL PRIMARY KEY
);
--rollback drop table test_concourse_db_migration;