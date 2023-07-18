--liquibase formatted sql

--changeset uk.gov.pay:alter-table-users-add-lower-email-unique-index

CREATE UNIQUE INDEX lower_case_email_index ON users ((LOWER(email)));

--rollback DROP INDEX lower_case_email_index;
