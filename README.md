# pay-adminusers
The GOV.UK Pay Admin Users Module in Java (Dropwizard)

## Environment Variables

* `BASE_URL`:  This is the publicly visible URL for the pay admin users root. Defaults to http://localhost:8080 if not set.
* `DB_USER`: database username for adminusers DB.
* `DB_PASSWORD`: database password for adminusers DB.
 
## API Specification
 
 The [API Specification](/docs/api_specification.md) provides more detail on the paths and operations including examples.
 
| Path                          | Supported Methods | Description                        |
| ----------------------------- | ----------------- | ---------------------------------- |
| [```/v1/api/users```](/docs/api_specification.md#post-v1apiusers)              | POST    |  Creates a new user            |
| [```/v1/api/users/{username}```](/docs/api_specification.md#get-v1apiusersusername)              | GET    |  Gets a user with the associated username            |
| [```/v1/api/users/{username}```](/docs/api_specification.md#patch-v1apiusersusername)              | PATCH    |  amend a specific user attribute            |
| [```/v1/api/users/authenticate```](/docs/api_specification.md#post-v1apiusersauthenticate)              | POST    |  Authenticate a given username/password            |
| [```/v1/api/forgotten-passwords```](/docs/api_specification.md#post-v1apiforgottenpasswords)              | POST    |  Create a new forgotten password request            |
| [```/v1/api/forgotten-passwords/{code}```](/docs/api_specification.md#get-v1apiforgottenpasswordscode)              | GET    |  GETs a forgotten password record by code            |


-----------------------------------------------------------------------------------------------------------

## Licence

[MIT License](LICENCE)

## Responsible Disclosure

GOV.UK Pay aims to stay secure for everyone. If you are a security researcher and have discovered a security vulnerability in this code, we appreciate your help in disclosing it to us in a responsible manner. We will give appropriate credit to those reporting confirmed issues. Please e-mail gds-team-pay-security@digital.cabinet-office.gov.uk with details of any issue you find, we aim to reply quickly.

