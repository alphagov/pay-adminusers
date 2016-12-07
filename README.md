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
 |[```/v1/api/users/{username}```](#get-v1apiusers)              | GET    |  Gets a user with the associated username            |


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
| `role_name`           |          | known role name for adminusers      | | e.g. `admin`

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
