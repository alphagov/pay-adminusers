# pay-adminusers

The GOV.UK Pay Admin Users Module in Java (Dropwizard)

## API Specification

The [API Specification](/openapi/adminusers_spec.yaml) provides more detail on the paths and operations including
examples.

[View the API specification for adminusers in Swagger Editor](https://editor.swagger.io/?url=https://raw.githubusercontent.com/alphagov/pay-adminusers/master/openapi/adminusers_spec.yaml).

## Environment Variables

| NAME                                                                          | DESCRIPTION                                                                                                                                                                                                                                |
|-------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ADMIN_PORT`                                                                  | The port number to listen for Dropwizard admin requests on. Defaults to `8081`.                                                                                                                                                            |
| `BIND_HOST`                                                                   | The IP address for the application to bind to. Defaults to `127.0.0.1`                                                                                                                                                                     |
| `BASE_URL`                                                                    | This is the publicly visible URL for the pay admin users root. Defaults to `http://localhost:8080`.                                                                                                                                        |
| `DB_HOST`                                                                     | The hostname of the database server.                                                                                                                                                                                                       |
| `DB_NAME`                                                                     | The name of the database on `DB_HOST`. Defaults to `adminusers`.                                                                                                                                                                           |
| `DB_PASSWORD`                                                                 | The password for the `DB_USER` user.                                                                                                                                                                                                       |
| `DB_PORT`                                                                     | The port number it use when connecting to the database server. Defaults to `5432`.                                                                                                                                                         |
| `DB_SSL_OPTION`                                                               | To turn TLS on this value must be set as `ssl=true`. Otherwise must be empty.                                                                                                                                                              |
| `DB_USER`                                                                     | The username to log into the database as.                                                                                                                                                                                                  |
| `FORGOTTEN_PASSWORD_EXPIRY_MINUTES`                                           | The number of minutes password reset tokens are valid for. Defaults to `90`.                                                                                                                                                               |
| `JAVA_HOME`                                                                   | The location of the JRE.                                                                                                                                                                                                                   |
| `JAVA_OPTS`                                                                   | Commandline arguments to pass to the java runtime. Optional.                                                                                                                                                                               |
| `JPA_LOG_LEVEL`                                                               | The logging level to set for JPA. Defaults to `WARNING`.                                                                                                                                                                                   |
| `JPA_SQL_LOG_LEVEL`                                                           | The logging level to set for JPA SQL logging. Defaults to `WARNING`.                                                                                                                                                                       |
| `LOGIN_ATTEMPT_CAP`                                                           | The number of consecutive failed logins a user can have before their account is disabled. Defaults to `10`.                                                                                                                                |
| `NOTIFY_SIGN_IN_OTP_SMS_TEMPLATE_ID`                                          | The GOV.UK Notify template ID to use for sending OTP codes via SMS for signing in. Defaults to `pay-notify-sign-in-otp-sms-template-id`.                                                                                                   |
| `NOTIFY_CHANGE_SIGN_IN_2FA_TO_SMS_OTP_SMS_TEMPLATE_ID`                        | The GOV.UK Notify template ID to use for sending OTP codes via SMS for changing the sign-in method to text messages. Defaults to `pay-notify-change-sign-in-2fa-to-sms-otp-sms-template-id`.                                               |
| `NOTIFY_SELF_INITIATED_CREATE_USER_AND_SERVICE_OTP_SMS_TEMPLATE_ID`           | The GOV.UK Notify template ID to use for sending OTP codes via SMS for self-initiated user and service creation. Defaults to `pay-notify-self-initiated-create-user-and-service-otp-sms-template-id`.                                      |
| `NOTIFY_CREATE_USER_IN_RESPONSE_TO_INVITATION_TO_SERVICE_OTP_SMS_TEMPLATE_ID` | The GOV.UK Notify template ID to use for sending OTP codes via SMS for creating a user in response to an invitation to join a service. Defaults to `pay-notify-create-user-in-response-to-invitation-to-service-otp-sms-template-id`.      |
| `NOTIFY_API_KEY`                                                              | The GOV.UK Notify API key to use when sending card payment messages. Defaults to `api_key-pay-notify-service-id-pay-notify-secret-needs-to-be-32-chars-fsghdngfhmhfkrgsfs`.                                                                |
| `NOTIFY_BASE_URL`                                                             | The URL of GOV.UK Notify's API. Defaults to `https://stubs.pymnt.localdomain/notify`.                                                                                                                                                      |
| `NOTIFY_FORGOTTEN_PASSWORD_EMAIL_TEMPLATE_ID`                                 | The GOV.UK Notify template ID to use when sending a password reset email to a user of the admin tool. Defaults to `pay-notify-forgotten-password-email-template-id`.                                                                       |
| `NOTIFY_INVITE_SERVICE_EMAIL_TEMPLATE_ID`                                     | The GOV.UK Notify template ID to use when sending a confirmation email to a user registering for an admin tool account. Defaults to `pay-notify-invite-service-email-template-id`.                                                         |
| `NOTIFY_INVITE_SERVICE_USER_DISABLED_EMAIL_TEMPLATE_ID`                       | The GOV.UK Notify template ID to use when sending an email to a user who is trying to register for an admin tool account but already has a disabled one. Defaults to `pay-notify-invite-service-user-disabled-email-template-id`.          |
| `NOTIFY_INVITE_SERVICE_USER_EXITS_EMAIL_TEMPLATE_ID`                          | The GOV.UK Notify template ID to use when sending an email to a user who is trying to register for an admin tool account but already has one. Defaults to `pay-notify-invite-service-user-exists-email-template-id`.                       |
| `NOTIFY_INVITE_USER_EMAIL_TEMPLATE_ID`                                        | The GOV.UK Notify template ID to use when sending an invitation to collaborate on a service to a user who does not yet have an admin tool account. Defaults to `pay-notify-invite-user-email-template-id`.                                 |
| `NOTIFY_INVITE_USER_EXISTING_EMAIL_TEMPLATE_ID`                               | The GOV.UK Notify template ID to use when sending an invitation to collaborate on service to a user who already has an admin tool account. Defaults to `pay-notify-invite-user-existing-email-template-id`.                                |
| `NOTIFY_LIVE_ACCOUNT_CREATED_EMAIL_TEMPLATE_ID`                               | The GOV.UK Notify template ID to use when sending an email to an admin tool user who has requested to go live using our procured payment provider. Defaults to `pay-notify-live-account-created-email-template-id`.                        |
| `NOTIFY_MANDATE_CANCELLED_EMAIL_TEMPLATE_ID`                                  | The GOV.UK Notify template ID to use when sending an email to a paying user to inform them their Direct Debit mandate has been cancelled. Defaults to `pay-mandate-cancelled-email-template-id`.                                           |
| `NOTIFY_MANDATE_FAILED_EMAIL_TEMPLATE_ID`                                     | The GOV.UK Notify template ID to use when sending an email to a paying user to inform them their request to set up an on-demand Direct Debit mandate failed. Defaults to `pay-mandate-failed-email-template-id`.                           |
| `NOTIFY_ONE_OFF_MANDATE_AND_PAYMENT_CREATED_EMAIL_TEMPLATE_ID`                | The GOV.UK Notify template ID to use when sending an email to a paying user to inform them their request to set up a one-off Direct Debit payment was successful. Defaults to `pay-one-off-mandate-and-payment-created-email-template-id`. |
| `NOTIFY_ON_DEMAND_MANDATE_CREATED_EMAIL_TEMPLATE_ID`                          | The GOV.UK Notify template ID to use when sending an email to a paying user to inform them their request to set up an on-demand Direct Debit mandate was successful. Defaults to `pay-on-demand-mandate-created-email-template-id`.        |
| `NOTIFY_ON_DEMAND_PAYMENT_CONFIRMED_EMAIL_TEMPLATE_ID`                        | The GOV.UK Notify template ID to use when sending an email to a paying user to inform them that an on-demand Direct Debit payment will be taken. Defaults to `pay-on-demand-payment-confirmed-email-template-id`.                          |
| `NOTIFY_PAYMENT_FAILED_EMAIL_TEMPLATE_ID`                                     | The GOV.UK Notify template ID to use when sending an email to a paying user to inform them that a Direct Debit payment failed to be taken. Defaults to `pay-payment-failed-email-template-id`.                                             |
| `PORT`                                                                        | The port number to listen for requests on. Defaults to `8080`.                                                                                                                                                                             |
| `RUN_APP`                                                                     | Set to `true` to run the application. Defaults to `true`.                                                                                                                                                                                  |
| `RUN_MIGRATION`                                                               | Set to `true` to run a database migration. Defaults to `false`.                                                                                                                                                                            |
| `SELFSERVICE_URL`                                                             | The URL to the admin portal. Defaults to `https://selfservice.pymnt.localdomain`.                                                                                                                                                          |
| `SUPPORT_URL`                                                                 | The URL users can visit to get support. Defaults to `https://frontend.pymnt.localdomain/contact/`.                                                                                                                                         |
| `EXPUNGE_AND_ARCHIVE_HISTORICAL_DATA_ENABLED`                                 | Set to `true` to enable expunging (user related data) and archiving (services) historical data. Default is `false`.                                                                                                                        |
| `EXPUNGE_USER_DATA_AFTER_DAYS`                                                | Number of days after which users (not attached to any service) not logged in or created but never logged in deleted. Default is `1460` (4 years).                                                                                          |
| `EXPUNGE_ARCHIVE_SERVICES_AFTER_DAYS`                                         | Number of days after which services without transactions archived. Default is `2555` (7 years).                                                                                                                                            |

-----------------------------------------------------------------------------------------------------------

## Maven profiles

### Default profile

By default, maven will run all the tests excluding contract tests
`mvn clean install`

### Contract tests profile

By specifying this profile, maven will run *only* the contract tests
`mvn clean install -DrunContractTests -DPACT_BROKER_USERNAME=username -DPACT_BROKER_PASSWORD=password -DPACT_CONSUMER_TAG=tag`

## Licence

[MIT License](LICENCE)

## Vulnerability Disclosure

GOV.UK Pay aims to stay secure for everyone. If you are a security researcher and have discovered a security
vulnerability in this code, we appreciate your help in disclosing it to us in a responsible manner. Please refer to
our [vulnerability disclosure policy](https://www.gov.uk/help/report-vulnerability) and
our [security.txt](https://vdp.cabinetoffice.gov.uk/.well-known/security.txt) file for details.
