# API Specification
These are the endpoints and methods available for managing users on GOV.UK Pay.

### The user object

| Field                            | always present | Description                                                        |
|----------------------------------|:--------------:|--------------------------------------------------------------------|
| `external_id`                    | X              | External id for the user                                           |
| `username`                       | X              | Username for the user                                              |
| `email`                          | X              | email address                                                      |
| `telephone_number`               | X              | user's mobile/phone number                                         |
| `otp_key`                        | X              | top key for this user                                              |
| `service_roles`                  | X              | The service roles assigned to this user                            |
| `service_roles[i].service`       | X              | The service that the user has a role assigned for                  |
| `service_roles[i].role`          | X              | The role that the user has for the associated service              |
| `features`                       | X              | the user's active feature flags                                    |
| `second_factor`                  | X              | the second factor authentication method                            |
| `provisional_otp_key`            | X              | an otp key that has been provisioned for use but not yet activated |
| `provisional_otp_key_created_at` | X              | the timestamp that the provisional otp key was issued at           |
| `last_logged_in_at`              | X              | the timestamp of the last login                                    |
| `disabled`                       | X              | indicates whether the user is disabled                             |
| `login_counter`                  | X              | the number of times the user has logged in                         |
| `sessionVersion`                 | X              | the session version                                                |
| `_links`                         | X              | Self link for this user.                                           |

#### Example
```
{
    "external_id": "7d19aff33f8948deb97ed16b2912dcd3",
    "username": "abcd1234",
    "email": "email@example.com",
    "telephone_number": "447700900000",
    "otp_key": "43c3c4t",
    "service_roles": [
            {
                "service": {
                    "id": 1,
                    "external_id": "73ba1ec4ed6a4238a59f16ad97b4fa12",
                    "name": "System Generated",
                    "gateway_account_ids": [
                        "1"
                    ],
                    "_links": [],
                    "service_name": {
                        "en": "System Generated"
                    },
                    "redirect_to_service_immediately_on_terminal_state": false,
                    "collect_billing_address": true,
                    "current_go_live_stage": "NOT_STARTED"
                },
                "role": {
                    "name": "admin",
                    "description": "Administrator",
                    "permissions": [
                        {
                            "name": "users-service:read",
                            "description": "Viewusersinservice"
                        }
                    ]
                }
            }
        ]
    "features": null,
    "second_factor": "SMS",
    "provisional_otp_key": null,
    "provisional_otp_key_created_at": null,
    "last_logged_in_at": null,
    "disabled": false,
    "login_counter": 0,
    "sessionVersion": 0,
    "_links": [{
        "href": "http://adminusers.service/v1/api/users/7d19aff33f8948deb97ed16b2912dcd3",
        "rel" : "self",
        "method" : "GET"
    }]
    
}
```

-----------------------------------------------------------------------------------------------------------

### POST /v1/api/users

This endpoint creates a new account in this connector.

### Request example

```
POST /v1/api/users
Content-Type: application/json

{
    "username": "abcd1234",
    "email": "email@example.com",
    "gateway_account_ids": ["1"],
    "telephone_number": "49875792",
    "otp_key": "43c3c4t",
    "role_name": "admin"
}
```

#### Request body description

| Field                    | required | Description                                                      | Supported Values     |
| ------------------------ |:--------:| ---------------------------------------------------------------- |----------------------|
| `username`       | X        | a username for the user. must be unique          |  |
| `email`                   |     X     | valid email address for a user.                                  |  |
| `gateway_account_ids`            |  X        | valid gateway account IDs from connector | |
| `telephone_number`           |   X       | Valid mobile/phone number      | |
| `otp_key`           |          | opt key (for 2FA)      | |
| `role_name`           |          | known role name for adminusers      | e.g. `admin` | 

### Response example

```
201 OK
Content-Type: application/json
{
  ..user object..
}
```
See [The user object](#the-user-object)

-----------------------------------------------------------------------------------------------------------

## GET /v1/api/users/`{externalId}`

This endpoint finds and return a user with the given external id.

### Request example

```
GET /v1/api/users/7d19aff33f8948deb97ed16b2912dcd3
```

### Response example

```
200 OK
Content-Type: application/json
{
    ..user object..
}
```
See [The user object](#the-user-object)

## GET /v1/api/users/?ids=`{externalId1}`,`{externalId2}`...

This endpoint finds and return users with the given external ids.

### Notes

Will return `404` if any of the provided external ids do not match a user.

### Request example

```
GET /v1/api/users?=ids=7d19aff33f8948deb97ed16b2912dcd3,4e89tlf59f9148deb79ed61b9212bhj7
```

### Response example

```
200 OK
Content-Type: application/json
[
    {
        ..user object..
    },
    ...
]
```
See [The user object](#the-user-object)

-----------------------------------------------------------------------------------------------------------


## PATCH /v1/api/users/`{externalId}`

This endpoint amends a specific attribute in user resource.

### Request example

```
PATCH /v1/api/users/7d19aff33f8948deb97ed16b2912dcd3
Content-Type: application/json
{
    "path": "sessionVersion",
    "op": "append",
    "value": "2",
}
```

### Response example

```
200 OK
Content-Type: application/json
{
  ..user object.
}
```
See [The user object](#the-user-object)

#### Request field description

| Field                    | required | Description                                                | Supported Values     |
| ------------------------ |:--------:| ---------------------------------------------------------- |----------------------|
| `path`                   |   X      | the name of the attribute to be adjusted                   | [sessionVersion &#124; disabled &#124; telephone_number] |
| `op`                     |   X      | type of adjustment to be performed on attribute            | [append &#124; replace]   |
| `value`                  |   X      | value to be used for performing the op                     |                      |

-----------------------------------------------------------------------------------------------------------

## POST /v1/api/users/authenticate

Authenticates the provided username / password combination. Counts failed login attempts as a side effect.

### Request example

```
POST /v1/api/users/authenticate
Content-Type: application/json

{
    "username": "abcd1234",
    "password": "a-password"
}
```

#### Request body description

| Field                    | required | Description                                                      | Supported Values     |
| ------------------------ |:--------:| ---------------------------------------------------------------- |----------------------|
| `username`       | X        | username of user          |  |
| `password`           |    X    | password of user      |  | 



### Response example

if authorised:

```
200 OK
Content-Type: application/json
{
    ..user object..
}
```
See [The user object](#the-user-object)

if un-authorised:
```
401 Unauthorized
Content-Type: application/json
{
  "errors": "invalid username/password combination"
}
```

if locked due to multiple login attempts:
```
401 Unauthorized
Content-Type: application/json
{
  "errors": "user [abcd1234] locked due to too many login attempts"
}
```

-----------------------------------------------------------------------------------------------------------

## POST /v1/api/forgotten-passwords

This endpoint creates a new forgotten password request

### Request example

```
POST /v1/api/forgotten-passwords
Content-Type: application/json

{
    "username": "abcd1234",
}
```

#### Request body description

| Field                    | required | Description                                                      | Supported Values     |
| ------------------------ |:--------:| ---------------------------------------------------------------- |----------------------|
| `username`       | X        |  username for the forgotten password to be created           |  |


### Response example

```
201 Created
Content-Type: application/json
{
    "username": "abcd1234",
    "code": "6fg77h67g497r5ivcdtdh",
    "date": "23-12-2015 13:23:12Z",
    "_links": [{
            "href": "http://adminusers.service/v1/api/forgotten-passwords/6fg77h67g497r5ivcdtdh",
            "rel" : "self",
            "method" : "GET"
          }]
}
```


-----------------------------------------------------------------------------------------------------------

## GET /v1/api/forgotten-passwords/`{code}`

Verify that the code is valid

### Request example

```
GET /v1/api/forgotten-passwords/xyz1234
```

### Response example (valid)

```
200 OK
Content-Type: application/json
{
    "username": "abcd1234",
    "code": "xyz1234",
    "date": "23-12-2015 13:23:12Z",
    "_links": [{
            "href": "http://adminusers.service/v1/api/forgotten-passwords/xyz1234",
            "rel" : "self",
            "method" : "GET"
          }]
}
```

### Response example (invalid)

```
404 OK
```

-----------------------------------------------------------------------------------------------------------

## PUT /v1/api/users/`{externalId}`/services/`{serviceId}`

This endpoint updates a service role of a particular user.

### Request example

```
PUT /v1/api/users/7d19aff33f8948deb97ed16b2912dcd3/services/111
Content-Type: application/json
{
    "role_name": "view-and-refund"
}
```

### Response example

```
200 OK
Content-Type: application/json
{
    ..user object.
}
```
See [The user object](#the-user-object)

if user not found:
```
404 Not found
```

if provided role name not valid:
```
400 Bad request
Content-Type: application/json
{
  "errors": "role [xyz] not recognised"
}
```

if provided the user does not have access to the given service id:
```
409 Conflict
Content-Type: application/json
{
  "errors": "user [7d19aff33f8948deb97ed16b2912dcd3] does not belong to service [123]"
}
```

if no of administrators for the given service is less than or equal to 1:
```
412 Precondition Failed
Content-Type: application/json
{
  "errors": "Service admin limit reached. At least 1 admin(s) required"
}
```
#### Request field description

| Field                    | required | Description                                                | Supported Values     |
| ------------------------ |:--------:| ---------------------------------------------------------- |----------------------|
| `role_name`              |   X      | the name of an existing valid role                         | e.g. admin           |

-----------------------------------------------------------------------------------------------------------

## POST /v1/api/users/`{externalId}`/services

This endpoint assigns a new service role to a user.

### Request example

```
POST /v1/api/users/7d19aff33f8948deb97ed16b2912dcd3/services
Content-Type: application/json
{
    "service_external_id": "ahq8745yq387"
    "role_name": "view-and-refund"
}
```

### Response example

```
200 OK
Content-Type: application/json
{
    ..user object..
}
```
See [The user object](#the-user-object)

if user not found:
```
404 Not found
```

if service id not found:
```
400 Bad request
Content-Type: application/json
{
  "errors": "Service ahq8745yq387 provided does not exist"
}
```


if provided role name not valid:
```
400 Bad request
Content-Type: application/json
{
  "errors": "role [xyz] not recognised"
}
```

if provided the user already has access to the given service
```
409 Conflict
Content-Type: application/json
{
  "errors": "Cannot assign service role. user [7d19aff33f8948deb97ed16b2912dcd3] already got access to service [ahq8745yq387]."
}
```

#### Request field description

| Field                    | required | Description                                                | Supported Values     |
| ------------------------ |:--------:| ---------------------------------------------------------- |----------------------|
| `service_external_id`    |   X      | the external id of an existing service                     |                      |
| `role_name`              |   X      | the name of an existing valid role                         | e.g. admin           |

-----------------------------------------------------------------------------------------------------------

## POST /v1/api/services

This endpoint creates a new service. And assigns to gateway account ids (Optional)

### Request example

```
POST /v1/api/services
Content-Type: application/json

{
    "name": "abcd1234",
    "gateway_account_ids": ["1"],
    "service_name": {
      "en": "abcd1234",
      "cy": "1234abcd"
    }
}
```

#### Request body description

| Field                    | required | Description                                                      | Supported Values     |
| ------------------------ |:--------:| ---------------------------------------------------------------- |----------------------|
| `name`       |         | a name for the service          |  |
| `gateway_account_ids`            |          | valid gateway account IDs from connector | |
| `service_name` | | object where keys are supported ISO-639-1 language codes and values are translated service names | key must be `"en"` or `"cy"` |

### Response example

```
201 OK
Content-Type: application/json
{
    "id": 123
    "external_id": "7d19aff33f8948deb97ed16b2912dcd3",
    "name": "abcd1234",
    "service_name": {
      "en": "abcd1234",
      "cy": "1234abcd"
    }
    "_links": [{
        "href": "http://adminusers.service/v1/api/services/123",
        "rel" : "self",
        "method" : "GET"
    }],
    "redirect_to_service_immediately_on_terminal_state": false,
    "collect_billing_address": true
}
```

## POST /v1/api/users/`{externalId}`/second-factor/provision

This endpoint provisions a new second-factor OTP key (secret used to generate OTP codes) for a user.

Provisioning a new key does not change immediately change the user’s current key. Use the [```/v1/api/users/second-factor/activate```](#post-v1apiusersexternalidsecondfactoractivate) endpoint to replace the user’s current key with the newly-provisioned one.

### Request example

```
POST /v1/api/users/7d19aff33f8948deb97ed16b2912dcd3/second-factor/provision
```

### Response example

```
200 OK
Content-Type: application/json
{
    ..user object.
}
```
See [The user object](#the-user-object)

## POST /v1/api/users/`{externalId}`/second-factor/activate

This endpoint activates the provisional OTP key for a user and configures their second-factor authentication method.

The user should have already a provisional OTP key created by the [```/v1/api/users/second-factor/provision```](#post-v1apiusersexternalidsecondfactorprovision) endpoint before this endpoint is called.

### Request example

```
POST /v1/api/users/7d19aff33f8948deb97ed16b2912dcd3/second-factor/activate
Content-Type: application/json
{
    "code": 123456,
    "second_factor": "APP"
}
```

#### Request body description

| Field           | Required | Description                                    | Supported Values |
| --------------- |:--------:| ---------------------------------------------- |------------------|
| `code`          |   X      | OTP code valid for the provisional OTP key     |                  |
| `second_factor` |   X      | the second-factor authentication method to use | `SMS`, `APP`     |

### Response example

```
200 OK
Content-Type: application/json
{
    ..user object..
}
```
See [The user object](#the-user-object)

-----------------------------------------------------------------------------------------------------------

## POST /v1/api/invites/service

This endpoint creates an invitation to allow self provisioning new service with Pay.

### Request example

```
POST /v1/api/invites/service
Content-Type: application/json
{
"telephone_number":"07700900000",
"email": "example@example.gov.uk",
"password" : "plain-txt-passsword"
}

```

#### Request body description

| Field                    | required | Description                                                      | Supported Values     |
| ------------------------ |:--------:| ---------------------------------------------------------------- |----------------------|
| `telephone_number`       |   X      | the phone number of the user                                     |   |
| `email`                  |   X      | the email (must be a public sector email)                         | |
| `password`               |   X      | password for the new user                                        | |

### Response example

```
201 OK
Content-Type: application/json
{  
   "type":"service",
   "email":"example@example.gov.uk",
   "telephone_number":"07700900000",
   "disabled":false,
   "attempt_counter":0,
   "_links":[  
      {  
         "rel":"invite",
         "method":"GET",
         "href":"https://selfservice.pymnt.localdomain/invites/04f431f18c3243f5bb29d10c01659e9c"
      },
      {  
         "rel":"self",
         "method":"GET",
         "href":"http://localhost:8080/v1/api/invites/04f431f18c3243f5bb29d10c01659e9c"
      }
   ]
}
```

-----------------------------------------------------------------------------------------------------------

## POST /v1/api/invites/user

This endpoint creates an invitation to allow a new team member to join an existing service.

### Request example

```
POST /v1/api/invites/user
Content-Type: application/json
{
"email": "example@example.gov.uk",
"sender": "sender@example.gov.uk",
"service_external_id": "674tqnc4b7q64",
"role_name": "view-only",
}

```

#### Request body description

| Field                    | required | Description                                                      | Supported Values     |
| ------------------------ |:--------:| ---------------------------------------------------------------- |----------------------|
| `email`                  |   X      | the email (mut be a public sector email)                         | |
| `sender`                 |   X      | external user id of the admin inviting                           | |
| `service_external_id`    |   X      | external user id of the service                                  | |
| `role_name`              |   X      | role to set for the invitee                                      | |

### Response example

```
201 OK
Content-Type: application/json
{  
   "type":"user",
   "email":"example@example.gov.uk",
   "disabled":false,
   "attempt_counter":0,
   "_links":[  
      {  
         "rel":"invite",
         "method":"GET",
         "href":"https://selfservice.pymnt.localdomain/invites/04f431f18c3243f5bb29d10c01659e9c"
      },
      {  
         "rel":"self",
         "method":"GET",
         "href":"http://localhost:8080/v1/api/invites/04f431f18c3243f5bb29d10c01659e9c"
      }
   ]
}
```

-----------------------------------------------------------------------------------------------------------
## GET /v1/api/services/`{serviceExternalId}`

Returns the service with the given external id

### Request example 
```
GET /v1/api/services/7d19aff33f8948deb97ed16b2912dcd3
```
### Response example

```
200 OK
Content-Type: application/json
{
    "id": 123
    "external_id": "7d19aff33f8948deb97ed16b2912dcd3",
    "name": "updated-service-name",
    "custom_branding": "some valid css class",
    "service_name": {
      "en": "abcd1234",
      "cy": "1234abcd"
    }
    "_links": [{
        "href": "http://adminusers.service/v1/api/services/123",
        "rel" : "self",
        "method" : "GET"
    }],
    "redirect_to_service_immediately_on_terminal_state": false,
    "collect_billing_address": true
}
```
-----------------------------------------------------------------------------------------------------------
## GET /v1/api/services/`{serviceExternalId}/users`

Returns the users for a service with the given external id

### Request example 
```
GET /v1/api/services/7d19aff33f8948deb97ed16b2912dcd3/users
```
### Response example

```
200 OK
Content-Type: application/json
[
  {
    ..user object..
  },
  ...
]
```
See [The user object](#the-user-object)

-----------------------------------------------------------------------------------------------------------
## GET /v1/api/services?gatewayAccountId={gateway_account_id}

Finds the service with the given gateway account id associated with

### Request example 
```
GET /v1/api/services?gatewayAccountId=123
```

#### Request query parameter description

| Query param              | required | Description                                                      | Supported Values     |
| ------------------------ |:--------:| ---------------------------------------------------------------- |----------------------|
| `gatewayAccountId`       |   X      | gateway account id                                               |      |

### Response example

```
200 OK
Content-Type: application/json
{
    "id": 123
    "external_id": "7d19aff33f8948deb97ed16b2912dcd3",
    "name": "service-name",
    "custom_branding": "some valid css class",
    "gateway_account_ids":["123"]
    "_links": [{
        "href": "http://adminusers.service/v1/api/services/123",
        "rel" : "self",
        "method" : "GET"
    }]
    
}
```

-----------------------------------------------------------------------------------------------------------

## PATCH /v1/api/services/`{serviceExternalId}`

This endpoint modifies updatable attributes of a service. Currently supports:
 - Update the name of a service
- Update the multilingual service names of a service
 - Add new gateway account(s) to a service
 - Update/replace the custom branding of a service

 Request can either be a single object or an array of objects. It’s similar to (but not 100% compliant with) [JSON Patch](http://jsonpatch.com/).

### Request example (for updating name)

```
PATCH /v1/api/services/7d19aff33f8948deb97ed16b2912dcd3
Content-Type: application/json
{
 "op": "replace",
 "path": "name", 
 "value": "updated-service-name" 
}

```

Updating `name` will also update `service_name/en` (see below).


### Request example (for updating the Welsh multilingual service name)

```
PATCH /v1/api/services/7d19aff33f8948deb97ed16b2912dcd3
Content-Type: application/json
{
 "op": "replace",
 "path": "service_name/cy", 
 "value": "updated-service-name" 
}

```

Updating `service_name/en` will also update `name` (see above).


### Request example (for assigning gateway accounts)

```
PATCH /v1/api/services/7d19aff33f8948deb97ed16b2912dcd3
Content-Type: application/json
{
 "op": "add",
 "path": "gateway_account_ids", 
 "value": ["1", "123"] 
}

```

### Request example (for updating custom branding)
Any valid JSON is allowed for value (including empty `{}`)

```
PATCH /v1/api/services/7d19aff33f8948deb97ed16b2912dcd3
Content-Type: application/json
{
 "op": "add",
 "path": "gateway_account_ids", 
 "value": {
    "css_path" : "/some.url/css.css",
    "image_path" : "/some.url/image.jpg",
 } 
}

```

### Request example (for replacing redirect_to_service_immediately_on_terminal_state)

Only boolean value allowed

```
PATCH /v1/api/services/7d19aff33f8948deb97ed16b2912dcd3
Content-Type: application/json
{
 "op":    "replace",
 "path":  "redirect_to_service_immediately_on_terminal_state", 
 "value": true
}

```

### Request example (for replacing collect_billing_address)

Only boolean value allowed

```
PATCH /v1/api/services/7d19aff33f8948deb97ed16b2912dcd3
Content-Type: application/json
{
 "op":    "replace",
 "path":  "collect_billing_address", 
 "value": false
}

```

#### Request body description

| Field                    | required | Description                                                      | Supported Values                                                                                                                                     |
| ------------------------ | -------- | ---------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| `op`                     |   X      | operation to perform on attribute                                | `replace`, `add`                                                                                                                                     |
| `path`                   |   X      | attribute that is affecting                                      | `gateway_account_ids` , `name`, `service_name/xx`, `custom_branding`, `redirect_to_service_immediately_on_terminal_state`, `collect_billing_address` |
| `value`                  |   X      | value to be replaced                                             |                                                                                                                                                      |

Note that in `service_name/xx`, `xx` must be replaced by a supported ISO-639-1 language code.


### Response example

```
200 OK
Content-Type: application/json
{
    "id": 123
    "external_id": "7d19aff33f8948deb97ed16b2912dcd3",
    "name": "updated-service-name",
    "service_name": {
      "en": "updated-service-name",
      "cy": "1234abcd"
    }
    "_links": [{
        "href": "http://adminusers.service/v1/api/services/123",
        "rel" : "self",
        "method" : "GET"
    }],
    "redirect_to_service_immediately_on_terminal_state": true
    "collect_billing_address": false
}
```

-----------------------------------------------------------------------------------------------------------

## POST /v1/api/services/`{serviceExternalId}`/stripe-agreement

This endpoint records that a Stripe terms have been accepted for the service.

### Request example
```
POST v1/api/services/7d19aff33f8948deb97ed16b2912dcd3/stripe-agreement
Content-Type: application/json
{
    "ip_address": "192.0.0.0"
}
```

### Request body description

| Field                    | required | Description                                                    | Supported Values     |
| ------------------------ |:--------:| -------------------------------------------------------------- |----------------------|
| `ip_address`             |   X      | the IP address the acceptance of terms request originated from |                      |

### Response Example

```
200 OK
```

-----------------------------------------------------------------------------------------------------------

### GET /v1/api/services/`{serviceExternalId}`/stripe-agreement

This endpoint retrieves the IP address and timestamp that the Stripe terms were accepted on for the service.

### Request example
```
GET v1/api/services/7d19aff33f8948deb97ed16b2912dcd3/stripe-agreement
```

### Response example
```
200 OK
Content-Type: application/json
{
    "ip_address": "192.0.0.0",
    "agreement_time": "2019-01-21T10:31:28.968Z"
}
```

-----------------------------------------------------------------------------------------------------------

### POST /v1/api/services/`{serviceExternalId}`/send-live-email

This endpoint will send an email to the user who signed the agreement with GOV.UK Pay for the service informing them that their service is now live.

### Request example
```
GET v1/api/services/7d19aff33f8948deb97ed16b2912dcd3/send-live-email
```

### Response example
```
200 OK
```

-----------------------------------------------------------------------------------------------------------

## POST /v1/api/invites/`{code}`/complete

This endpoint completes the invite by creating user/service and invalidating itself.
1. In the case of a `user` invite, this resource will assign the new service to the existing user and disables the invite
2. In the case of a `service` invite, this resource will create a new service, assign gateway account ids (if provided) and also creates a new user and assign to the service 

The response contains the user and the service id's affected as part of the invite completion in addition to the invite

### Request example

```
POST /v1/api/invites/wewe87325875c6/complete
```

Optional body (only in the case of invite type `service`)
```
Content-Type: application/json
{
"gateway_account_ids": ["1","78"]
}

```

#### Optional Request body description

| Field                    | required | Description                                                      | Supported Values     |
| ------------------------ |:--------:| ---------------------------------------------------------------- |----------------------|
| `gateway_account_ids`    |   X      | gateway accounts that needs to be associated for the new service | |


### Response example

```
200 OK
Content-Type: application/json
{  
   invite: { "type":"user",
             "email":"example@example.gov.uk",
             "disabled":false,
             "attempt_counter":0,
             "_links":[  
              {  
                 "rel":"invite",
                 "method":"GET",
                 "href":"https://selfservice.pymnt.localdomain/invites/04f431f18c3243f5bb29d10c01659e9c"
              },
              {  
                 "rel":"self",
                 "method":"GET",
                 "href":"http://localhost:8080/v1/api/invites/04f431f18c3243f5bb29d10c01659e9c"
              }]
            },
   service_external_id: "89wi6il2364328",
   user_external_id: "287cg75v3737" 
}
```

-----------------------------------------------------------------------------------------------------------

## POST /v1/api/invites/`{code}`/otp/generate

This endpoint generates and sends otp verification code to the phone number registered in the invite.

### `user` invite

#### Request example (`user` invite)

```
POST /v1/api/invites/265f39f63d8347f3bc5bf2d401b5e3ec/otp/generate
Content-Type: application/json
{
 "telephone_number": "07451234567",
 "password": "a-password"
}
```

#### Request body description (`user` invite)

| Field                    | required | Description                                                      | Supported Values     |
| ------------------------ |:--------:| ---------------------------------------------------------------- |----------------------|
| `telephone_number`       |   X      | the phone number of the user                                     | |
| `password`               |   X      | password for the new user                                        | |

#### Response example (`user` invite)

```
200 OK
```

### `service` invite

#### Request example (`service` invite)

```
POST /v1/api/invites/265f39f63d8347f3bc5bf2d401b5e3ec/otp/generate
```

#### Response example (`service` invite)

```
200 OK
```
