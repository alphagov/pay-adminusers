#pay-adminusers
The GOV.UK Pay Admin Users Module in Java (Dropwizard)

## Environment Variables

`BASE_URL`:  This is the publicly visible URL for the pay admin users root. Defaults to http://localhost:8080 if not set.

`INITIAL_MIGRATION_REQUIRED`: Defaults to false. Must set to `true` for an environment where you bring up adminusers connecting to a empty database. 

`DB_USER`: database username for adminusers DB. 
`DB_PASSWORD`: database password for adminusers DB.
 
 
 ## API NAMESPACE
 
 | Path                          | Supported Methods | Description                        |
 | ----------------------------- | ----------------- | ---------------------------------- |
 |[```/v1/api/users```](#post-v1apiusers)              | POST    |  Creates a new user            |
 |[```/v1/api/users/{username}```](#get-v1apiusersusername)              | GET    |  Gets a user with the associated username            |
 |[```/v1/api/users/authenticate```](#get-v1apiusersauthenticate)              | POST    |  Authenticate a given username/password            |
 |[```/v1/api/users/{username}/attempt-login```](#get-v1apiusersusernameattempt-login)              | POST    |  Records login attempts and locks account if necessary`            |
 |[```/v1/api/users/{username}/attempt-login?action=reset```](#get-v1apiusersusernameattemptLoginActionReset)              | POST    |  Resets login attempts to `0` and enables the user account            |
 |[```/v1/api/forgotten-passwords```](#get-v1apiforgottenpasswords)              | POST    |  Create a new forgotten password request            |


-----------------------------------------------------------------------------------------------------------

### POST /v1/api/users

This endpoint creates a new account in this connector.

#### Request example

```
POST /v1/api/users
Content-Type: application/json

{
    "username": "abcd1234",
    "email": "email@email.com",
    "gateway_account_id": "1",
    "telephone_number": "49875792",
    "otp_key": "43c3c4t",
    "role_name": "admin"
}
```

##### Request body description

| Field                    | required | Description                                                      | Supported Values     |
| ------------------------ |:--------:| ---------------------------------------------------------------- |----------------------|
| `username`       | X        | a username for the user. must be unique          |  |
| `email`                   |     X     | valid email address for a user.                                  |  |
| `gateway_account_id`            |  X        | valid gateway account ID from connector | |
| `telephone_number`           |   X       | Valid mobile/phone number      | |
| `otp_key`           |          | opt key (for 2FA)      | |
| `role_name`           |          | known role name for adminusers      | e.g. `admin` | 

#### Response example

```
201 OK
Content-Type: application/json
{
    "username": "abcd1234",
    "email": "email@email.com",
    "gateway_account_id": "1",
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

##### Response field description

| Field                    | always present | Description                                   |
| ------------------------ |:--------------:| --------------------------------------------- |
| `username`     | X              | Username for the user       |
| `gateway_account_id`     | X              | The account Id created by the connector       |
| `email`                   | X              | email address     |
| `telephone_number`            | X              | user's mobile/phone number |
| `otp_key`           | X              | top key for this user      |
| `role`           | X              | Role assigned to this user      |
| `permissions`                  | X              | names of all the permissions granted by this role.     |
| `_links`                  | X              | Self link for this user.     |

-----------------------------------------------------------------------------------------------------------

### GET /v1/api/users/{username}

This endpoint creates a new account in this connector.

#### Request example

```
GET /v1/api/users/abcd1234
```

#### Response example

```
200 OK
Content-Type: application/json
{
    "username": "abcd1234",
    "email": "email@email.com",
    "gateway_account_id": "1",
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

##### Response field description

| Field                    | always present | Description                                   |
| ------------------------ |:--------------:| --------------------------------------------- |
| `username`     | X              | Username for the user       |
| `gateway_account_id`     | X              | The account Id created by the connector       |
| `email`                   | X              | email address     |
| `telephone_number`            | X              | user's mobile/phone number |
| `otp_key`           | X              | top key for this user      |
| `role`           | X              | Role assigned to this user      |
| `permissions`                  | X              | names of all the permissions granted by this role.     |
| `_links`                  | X              | Self link for this user.     |

-----------------------------------------------------------------------------------------------------------

### POST /v1/api/users/authenticate

Authenticates the provided username / password combination. Counts failed login attempts as a side effect.

#### Request example

```
POST /v1/api/users/authenticate
Content-Type: application/json

{
    "username": "abcd1234",
    "password": "a-password"
}
```

##### Request body description

| Field                    | required | Description                                                      | Supported Values     |
| ------------------------ |:--------:| ---------------------------------------------------------------- |----------------------|
| `username`       | X        | username of user          |  |
| `password`           |    X    | password of user      |  | 



#### Response example

if authorised:

```
200 OK
Content-Type: application/json
{
    "username": "abcd1234",
    "email": "email@email.com",
    "gateway_account_id": "1",
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

### POST /v1/api/users/{username}/attempt-login

Records a login attempt (increase count). If attempt count is over 3, the account will be locked

#### Request example

```
POST /v1/api/users/abcd1234/attempt-login
```

#### Response examples

if successful and account not locked:
```
200 OK
```

if successful and account is locked:
```
401 Unauthorized
Content-Type: application/json
{
  "errors": "user [abcd1234] locked due to too many login attempts"
}
```

if user not found
```
404 Not Found
```


-----------------------------------------------------------------------------------------------------------

### POST /v1/api/users/{username}/attempt-login?action=reset

Resets login attempts to `0` and enables the user account

```
POST /v1/api/users/abcd1234/attempt-login?action=reset
```

#### Response examples

if successful and account is un-locked:
```
200 OK
```

if user not found
```
404 Not Found
```

-----------------------------------------------------------------------------------------------------------

### POST /v1/api/forgotten-passwords

This endpoint creates a new forgotten password request

#### Request example

```
POST /v1/api/forgotten-passwords
Content-Type: application/json

{
    "username": "abcd1234",
}
```

##### Request body description

| Field                    | required | Description                                                      | Supported Values     |
| ------------------------ |:--------:| ---------------------------------------------------------------- |----------------------|
| `username`       | X        |  username for the forgotten password to be created           |  |


#### Response example

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
