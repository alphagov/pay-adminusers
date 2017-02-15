--liquibase formatted sql

--changeset uk.gov.pay:update_table-user_services_roles splitStatements:false
CREATE OR REPLACE FUNCTION populateUserServicesRolesModel() RETURNS void AS $$
DECLARE
    userAccount RECORD;
    roleAccount RECORD;
    serviceAccount RECORD;
    userId INTEGER;
    serviceId INTEGER;
BEGIN
    FOR userAccount IN SELECT id FROM users ORDER BY id LOOP
      userId := userAccount.id;
      EXECUTE format('SELECT service_id FROM users_services '
                     'WHERE user_id = $1') INTO serviceAccount USING userId;
      IF serviceAccount IS NOT NULL
        THEN
          serviceId := serviceAccount.service_id;
          EXECUTE format('SELECT role_id FROM user_role '
            'WHERE user_id = $1') INTO roleAccount USING userId;
          IF roleAccount IS NOT NULL
          THEN
            INSERT INTO user_services_roles (user_id, service_id, role_id) VALUES (userId, serviceId, roleAccount.role_id);
            RAISE NOTICE 'Added User [%] to a **NEW** Service [%] associating it to Role [%]', userId, serviceId, roleAccount.role_id;
          ELSE
            RAISE NOTICE 'User [%] does not have any Role assigned', userId;
          END IF;
        ELSE
          RAISE NOTICE 'GatewayAccount [%] does not have any Service associated with it', gatewayAccountId;
      END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

SELECT populateUserServicesRolesModel();
--rollback truncate table user_services_roles cascade;

DROP FUNCTION populateUserServicesRolesModel();
