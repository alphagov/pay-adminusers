--liquibase formatted sql

--changeset uk.gov.pay:update_table-services splitStatements:false
CREATE OR REPLACE FUNCTION populateServicesModel() RETURNS void AS $$
DECLARE
    userGatewayAccount RECORD;
    serviceGatewayAccount RECORD;
    service RECORD;
    userId INTEGER;
    gatewayAccountId VARCHAR(255);
BEGIN
    FOR userGatewayAccount IN SELECT id,gateway_account_id FROM users ORDER BY gateway_account_id LOOP
      userId := userGatewayAccount.id;
      gatewayAccountId := userGatewayAccount.gateway_account_id;
      EXECUTE format('SELECT service_id FROM service_gateway_accounts '
        'WHERE gateway_account_id = $1') INTO serviceGatewayAccount USING gatewayAccountId;
      IF serviceGatewayAccount IS NULL
      THEN
        EXECUTE format('INSERT INTO services values(default)');
        SELECT currval('services_id_seq') INTO service;
        INSERT INTO service_gateway_accounts (service_id, gateway_account_id) VALUES (service.currval, gatewayAccountId);
        INSERT INTO users_services (user_id, service_id) VALUES (userId, service.currval);
        RAISE NOTICE 'Added User [%] to a **NEW** created Service [%] associating it to Gateway account [%]', userId, service.currval, gatewayAccountId;
      ELSE
        INSERT INTO users_services (user_id, service_id) VALUES (userId, serviceGatewayAccount.service_id);
        RAISE NOTICE 'Added User [%] to existing Service [%] associated to Gateway account [%]', userId, serviceGatewayAccount.service_id, gatewayAccountId;
      END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

SELECT populateServicesModel();
--rollback truncate table services cascade;

DROP FUNCTION populateServicesModel();
