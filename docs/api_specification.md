#API Specification
These are the endpoints and methods available for managing users on GOV.UK Pay.

## POST /v1/api/users

This endpoint creates a new account in this connector.

### Request example

```
POST /v1/api/users
Content-Type: application/json

{
    "username": "abcd1234",
    "email": "email@email.com",
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
    "external_id": "7d19aff33f8948deb97ed16b2912dcd3",
    "username": "abcd1234",
    "email": "email@email.com",
    "gateway_account_ids": ["1"],
    "telephone_number": "49875792",
    "otp_key": "43c3c4t",
    "role": {"admin","Administrator"},
    "permissions":["perm-1","perm-2","perm-3"], 
    "_links": [{
        "href": "http://adminusers.service/v1/api/users/7d19aff33f8948deb97ed16b2912dcd3",
        "rel" : "self",
        "method" : "GET"
    }]
    
}
```

#### Response field description

| Field                    | always present | Description                                   |
| ------------------------ |:--------------:| --------------------------------------------- |
| `external_id`     | X              | External id for the user       |
| `username`     | X              | Username for the user       |
| `gateway_account_ids`     | X              | The account Ids created by the connector       |
| `email`                   | X              | email address     |
| `telephone_number`            | X              | user's mobile/phone number |
| `otp_key`           | X              | top key for this user      |
| `role`           | X              | Role assigned to this user      |
| `permissions`                  | X              | names of all the permissions granted by this role.     |
| `_links`                  | X              | Self link for this user.     |

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
    "external_id": "7d19aff33f8948deb97ed16b2912dcd3",
    "username": "abcd1234",
    "email": "email@email.com",
    "gateway_account_ids": ["1"],
    "telephone_number": "49875792",
    "otp_key": "43c3c4t",
    "role": {"admin","Administrator"},
    "sessionVersion": 0,
    "permissions":["perm-1","perm-2","perm-3"], 
    "_links": [{
        "href": "http://adminusers.service/v1/api/users/7d19aff33f8948deb97ed16b2912dcd3",
        "rel" : "self",
        "method" : "GET"
    }]
    
}
```

#### Response field description

| Field                    | always present | Description                                   |
| ------------------------ |:--------------:| --------------------------------------------- |
| `external_id`     | X              | External id for the user       |
| `username`     | X              | Username for the user       |
| `gateway_account_ids`     | X              | The account Ids created by the connector       |
| `email`                   | X              | email address     |
| `telephone_number`            | X              | user's mobile/phone number |
| `otp_key`           | X              | top key for this user      |
| `role`           | X              | Role assigned to this user      |
| `permissions`                  | X              | names of all the permissions granted by this role.     |
| `_links`                  | X              | Self link for this user.     |

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
    "external_id": "7d19aff33f8948deb97ed16b2912dcd3",
    "username": "abcd1234",
    "email": "email@email.com",
    "gateway_account_ids": ["1"],
    "telephone_number": "49875792",
    "otp_key": "43c3c4t",
    "sessionVersion": 2,
    "role": {"admin","Administrator"},
    "permissions":["perm-1","perm-2","perm-3"], 
    "_links": [{
        "href": "http://adminusers.service/v1/api/users/7d19aff33f8948deb97ed16b2912dcd3",
        "rel" : "self",
        "method" : "GET"
    }]
    
}
```

#### Request field description

| Field                    | required | Description                                                | Supported Values     |
| ------------------------ |:--------:| ---------------------------------------------------------- |----------------------|
| `path`                   |   X      | the name of the attribute to be adjusted                   | [sessionVersion|disabled]] |
| `op`                     |   X      | type of adjustment to be performed on attribute            | [append | replace]   |
| `value`                  |   X      | value to be used for performing the op                     |                      |


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
    "external_id": "7d19aff33f8948deb97ed16b2912dcd3",
    "username": "abcd1234",
    "email": "email@email.com",
    "gateway_account_ids": ["1"],
    "telephone_number": "49875792",
    "otp_key": "43c3c4t",
    "role": {"admin","Administrator"},
    "permissions":["perm-1","perm-2","perm-3"], 
    "_links": [{
        "href": "http://adminusers.service/v1/api/users/7d19aff33f8948deb97ed16b2912dcd3",
        "rel" : "self",
        "method" : "GET"
    }]
    
}
```

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

This endpoint updates a service role of a perticular user.

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
    "external_id": "7d19aff33f8948deb97ed16b2912dcd3",
    "username": "abcd1234",
    "email": "email@email.com",
    "gateway_account_ids": ["1"],
    "telephone_number": "49875792",
    "otp_key": "43c3c4t",
    "sessionVersion": 2,
    "role": {"view-and-refund","View and Refund"},
    "permissions":["perm-1","perm-2","perm-3"], 
    "_links": [{
        "href": "http://adminusers.service/v1/api/users/7d19aff33f8948deb97ed16b2912dcd3",
        "rel" : "self",
        "method" : "GET"
    }]
    
}
```

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
    "external_id": "7d19aff33f8948deb97ed16b2912dcd3",
    "username": "abcd1234",
    "email": "email@email.com",
    "gateway_account_ids": ["1"],
    "telephone_number": "49875792",
    "otp_key": "43c3c4t",
    "sessionVersion": 2,
    "service_role":[{
        service:{external_id: "ahq8745yq387",name:"service-name"},
        role:{
            name:"view-and-refund", 
            permissions:[{name:"perm-1", description:"perm-description"}]
        }  
    }],
    "_links": [{
        "href": "http://adminusers.service/v1/api/users/7d19aff33f8948deb97ed16b2912dcd3",
        "rel" : "self",
        "method" : "GET"
    }]
    
}
```

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
}
```

#### Request body description

| Field                    | required | Description                                                      | Supported Values     |
| ------------------------ |:--------:| ---------------------------------------------------------------- |----------------------|
| `name`       |         | a name for the service          |  |
| `gateway_account_ids`            |          | valid gateway account IDs from connector | |

### Response example

```
201 OK
Content-Type: application/json
{
    "id": 123
    "external_id": "7d19aff33f8948deb97ed16b2912dcd3",
    "name": "abcd1234",
    "_links": [{
        "href": "http://adminusers.service/v1/api/services/123",
        "rel" : "self",
        "method" : "GET"
    }]
    
}
```

-----------------------------------------------------------------------------------------------------------

## POST /v1/api/invites/service

This endpoint creates an invitation to allow self provisioning new service with Pay.

### Request example

```
POST /v1/api/invites/service
Content-Type: application/json
{
"telephone_number":"088882345689",
"email": "example@example.gov.uk",
"password" : "plain-txt-passsword"
}

```

#### Request body description

| Field                    | required | Description                                                      | Supported Values     |
| ------------------------ |:--------:| ---------------------------------------------------------------- |----------------------|
| `telephone_number`       |   X      | the phone number of the user                                     |   |
| `email`                  |   X      | the email (mut be a public sector email)                         | |
| `password`               |   X      | password for the new user                                        | |

### Response example

```
201 OK
Content-Type: application/json
{  
   "type":"service",
   "email":"example@example.gov.uk",
   "telephone_number":"088882345689",
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

## PATCH /v1/api/services/`{serviceExternalId}`

This endpoint modifies updatable attributes of a Service. Currently supports
 - Update the name of a service
 - Add new gateway account(s) to a service.

### Request example

```
PATCH /v1/api/services/7d19aff33f8948deb97ed16b2912dcd3
Content-Type: application/json
{
 "op": "replace",
 "path": "name", 
 "value": "updated-service-name" 
}

```

#### Request body description

| Field                    | required | Description                                                      | Supported Values     |
| ------------------------ |:--------:| ---------------------------------------------------------------- |----------------------|
| `op`                     |   X      | operation to perform on attribute                                | `replace`, `add`     |
| `path`                   |   X      | attribute that is affecting                                      | `gateway_account_ids` , `name` |
| `value`                  |   X         | value to be replaced                                             |                      |

### Response example

```
200 OK
Content-Type: application/json
{
    "id": 123
    "external_id": "7d19aff33f8948deb97ed16b2912dcd3",
    "name": "updated-service-name",
    "_links": [{
        "href": "http://adminusers.service/v1/api/services/123",
        "rel" : "self",
        "method" : "GET"
    }]
    
}
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
