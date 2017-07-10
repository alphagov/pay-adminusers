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
| [```/v1/api/users/{externalId}```](/docs/api_specification.md#get-v1apiusersexternalid)              | GET    |  Gets a user with the associated external id            |
| [```/v1/api/users/{externalId}```](/docs/api_specification.md#patch-v1apiusersexternalid)              | PATCH    |  amend a specific user attribute            |
| [```/v1/api/users/{externalId}/services/{serviceId}```](/docs/api_specification.md#put-v1apiusersexternalidservicesserviceid)  | PUT    |  update user's role for a service            |
| [```/v1/api/users/{externalId}/services```](/docs/api_specification.md#post-v1apiusersexternalidservicesserviceid)  | POST    |  assign a new service along with role to a user        |
| [```/v1/api/users/authenticate```](/docs/api_specification.md#post-v1apiusersauthenticate)              | POST    |  Authenticate a given username/password            |
| [```/v1/api/forgotten-passwords```](/docs/api_specification.md#post-v1apiforgottenpasswords)              | POST    |  Create a new forgotten password request            |
| [```/v1/api/forgotten-passwords/{code}```](/docs/api_specification.md#get-v1apiforgottenpasswordscode)              | GET    |  GETs a forgotten password record by code            |
| [```/v1/api/services```](/docs/api_specification.md#post-v1apiservices)              | POST   |  Creates a new service           |
| [```/v1/api/invites/service```](/docs/api_specification.md#post-v1apiinvitesservice)               | POST   |  Creates a invitation for a new service     |
| [```/v1/api/invites/user```](/docs/api_specification.md#post-v1apiinvitesuser)               | POST   |  Creates a user invitation     |
| [```/v1/api/service/{externalId}```](/docs/api_specification.md#patch-v1apiservicesserviceexternalid)               | PATCH   |  Updates the value of a service attribute     |


-----------------------------------------------------------------------------------------------------------

## Licence

[MIT License](LICENCE)

## Responsible Disclosure

GOV.UK Pay aims to stay secure for everyone. If you are a security researcher and have discovered a security vulnerability in this code, we appreciate your help in disclosing it to us in a responsible manner. We will give appropriate credit to those reporting confirmed issues. Please e-mail gds-team-pay-security@digital.cabinet-office.gov.uk with details of any issue you find, we aim to reply quickly.

