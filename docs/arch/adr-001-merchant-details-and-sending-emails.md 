# ADR 001 - Merchant details and sending emails 

## Status

Proposed

## Context

For card payments, we are required — by Visa/Mastercard rules — to display the name and address of the merchant (provider of the service) on the user-facing payment pages.

We are now implementing support for Direct Debit payments. For these payments, we also display the merchant’s name and address on the payment pages.

The Direct Debit scheme rules state that we must email paying users in certain circumstances and these emails must include the merchant’s telephone number.

Currently, merchant details (name, address and telephone number) are stored on a per service basis (rather than on a per gateway account basis) in the adminusers database. We require all services to provide a name and address but we only require a telephone number if the service has a Direct Debit gateway account. 

As a result, both the card frontend and the Direct Debit frontend microservices have to talk to adminusers to get the merchant details in order to display them on the payment pages (they also have to talk to adminusers to get custom branding if the service uses it).

At present, emails to paying users are sent via GOV.UK Notify by card connector and Direct Debit connector (adminusers also sends emails via GOV.UK Notify to manage selfservice invitations etc.). This has lead to some code duplication. The two connectors do not have to talk to adminusers to send emails at present but the requirement to include the merchant’s telephone number in Direct Debit emails means that this may have to change.

Given we are going to have to make changes in order to include the merchant’s telephone number in Direct Debit emails, two questions arise:

1. Are merchant details stored in the right place?
2. Are we sending emails from the right place?

We discussed several ideas:

* Keep the merchant details in adminusers and have Direct Debit connector talk to adminusers in order to get the merchant’s telephone numbers when sending emails. This further involves adminusers — a microservice ostensibly intended to support selfservice — in making payments.

* Move the merchant details to be per gateway account, rather than per service. This would mean storing them in the connector database rather than the adminusers database. This would then mean that both frontends would not have to talk to adminusers to display the merchant’s name and address on the payment pages, and Direct Debit connector would not have to talk to adminusers to get the merchant’s telephone number to include it in emails. Apart from custom branding (which could, potentially, also be moved), adminusers would be removed from involvement in making payments. However, moving the merchant details to be per gateway account can be considered a step backwards, as they conceptually belong to the service. We could try to preserve this by updating the merchant details on all the service’s gateway accounts when they are changed — we currently do this with the service name and it adds complexity.

* Keep the merchant details in adminusers and send all emails from adminusers. Whenever they need to send emails, the connectors would talk to adminusers, which would already have the merchant details. This would centralise our email sending, reducing duplication, but would involve adminusers — still ostensibly intended to support selfservice — in making payments.

* Create a separate microservice for sending emails. Whenever they need to send emails, the connectors would talk to this microservice. Either the new microservice or the connectors would still have to get the merchant details from wherever they are stored (probably adminusers, but we could combine this with the idea of moving the merchant details to be per gateway account and stored in the connector database).

## Decision

We will keep the merchant details in adminusers and send Direct Debit emails from adminusers.

We will continue to send card payment emails from card connector for now but we will look to move this to adminusers in the future.

While we won’t make any changes immediately, we intend to remove the service name from the gateway accounts (in the connectors) and store them only in adminusers.

As part of future design work, we will change selfservice so that it is clear that the merchant details belong to the service, rather than a specific gateway account.

## Consequences

By making this change, we are embracing (rather than resisting) the fact that adminusers is becoming the “service microservice” (we should probably rename it at some point in the future).

For the first time, adminusers will process personal information from paying users. This is not considered to be a concern. Cardholder information will not flow through adminusers, which will continue to live outside of the cardholder data environment as defined by PCI DSS.

Centralising email sending in adminusers will not make it more difficult to move this functionality to a new microservice in the future if we decide to go down this route.