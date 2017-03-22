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
    "username": "abcd1234",
    "email": "email@email.com",
    "gateway_account_ids": ["1"],
    "telephone_number": "49875792",
    "otp_key": "43c3c4t",
    "role": {"admin","Administrator"},
    "permissions":["perm-1","perm-2","perm-3"], 
    "_links": [{
        "href": "http://adminusers.service/v1/api/users/abcd1234",
        "rel" : "self",
        "method" : "GET"
    }]
    
}
```

#### Response field description

| Field                    | always present | Description                                   |
| ------------------------ |:--------------:| --------------------------------------------- |
| `username`     | X              | Username for the user       |
| `gateway_account_ids`     | X              | The account Ids created by the connector       |
| `email`                   | X              | email address     |
| `telephone_number`            | X              | user's mobile/phone number |
| `otp_key`           | X              | top key for this user      |
| `role`           | X              | Role assigned to this user      |
| `permissions`                  | X              | names of all the permissions granted by this role.     |
| `_links`                  | X              | Self link for this user.     |

-----------------------------------------------------------------------------------------------------------

## GET /v1/api/users/{username}

This endpoint finds and return a user with the given username.

### Request example

```
GET /v1/api/users/abcd1234
```

### Response example

```
200 OK
Content-Type: application/json
{
    "username": "abcd1234",
    "email": "email@email.com",
    "gateway_account_ids": ["1"],
    "telephone_number": "49875792",
    "otp_key": "43c3c4t",
    "role": {"admin","Administrator"},
    "sessionVersion": 0,
    "permissions":["perm-1","perm-2","perm-3"], 
    "_links": [{
        "href": "http://adminusers.service/v1/api/users/abcd1234",
        "rel" : "self",
        "method" : "GET"
    }]
    
}
```

#### Response field description

| Field                    | always present | Description                                   |
| ------------------------ |:--------------:| --------------------------------------------- |
| `username`     | X              | Username for the user       |
| `gateway_account_ids`     | X              | The account Ids created by the connector       |
| `email`                   | X              | email address     |
| `telephone_number`            | X              | user's mobile/phone number |
| `otp_key`           | X              | top key for this user      |
| `role`           | X              | Role assigned to this user      |
| `permissions`                  | X              | names of all the permissions granted by this role.     |
| `_links`                  | X              | Self link for this user.     |

-----------------------------------------------------------------------------------------------------------

## PATCH /v1/api/users/{username}

This endpoint amends a specific attribute in user resource.

### Request example

```
PATCH /v1/api/users/abcd1234
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
    "username": "abcd1234",
    "email": "email@email.com",
    "gateway_account_ids": ["1"],
    "telephone_number": "49875792",
    "otp_key": "43c3c4t",
    "sessionVersion": 2,
    "role": {"admin","Administrator"},
    "permissions":["perm-1","perm-2","perm-3"], 
    "_links": [{
        "href": "http://adminusers.service/v1/api/users/abcd1234",
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
    "username": "abcd1234",
    "email": "email@email.com",
    "gateway_account_ids": ["1"],
    "telephone_number": "49875792",
    "otp_key": "43c3c4t",
    "role": {"admin","Administrator"},
    "permissions":["perm-1","perm-2","perm-3"], 
    "_links": [{
        "href": "http://adminusers.service/v1/api/users/abcd1234",
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

## GET /v1/api/forgotten-passwords/{code}

This endpoint creates a new forgotten password request

### Request example

```
GET /v1/api/forgotten-passwords/xyz1234
```

### Response example

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

-----------------------------------------------------------------------------------------------------------

## PUT /v1/api/users/{username}/services/{service-id}

This endpoint updates a service role of a perticular user.

### Request example

```
PUT /v1/api/users/abcd1234/services/111
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
    "username": "abcd1234",
    "email": "email@email.com",
    "gateway_account_ids": ["1"],
    "telephone_number": "49875792",
    "otp_key": "43c3c4t",
    "sessionVersion": 2,
    "role": {"view-and-refund","View and Refund"},
    "permissions":["perm-1","perm-2","perm-3"], 
    "_links": [{
        "href": "http://adminusers.service/v1/api/users/abcd1234",
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
  "errors": "user [abcd1234] does not belong to service [123]"
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
