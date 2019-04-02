--liquibase formatted sql

--changeset uk.gov.pay:backfill_english_service_names
INSERT INTO service_names (service_id, language, name)
SELECT id, 'en', name
FROM services
WHERE NOT EXISTS (
    SELECT *
    FROM service_names
    WHERE service_id = services.id AND language = 'en'
);
