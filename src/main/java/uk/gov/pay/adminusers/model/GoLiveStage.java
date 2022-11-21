package uk.gov.pay.adminusers.model;

public enum GoLiveStage {
    NOT_STARTED,
    ENTERED_ORGANISATION_NAME,
    ENTERED_ORGANISATION_ADDRESS,
    CHOSEN_PSP_STRIPE,
    CHOSEN_PSP_WORLDPAY,
    CHOSEN_PSP_SMARTPAY,
    CHOSEN_PSP_EPDQ,
    CHOSEN_PSP_GOV_BANKING_WORLDPAY,
    GOV_BANKING_MOTO_OPTION_COMPLETED,
    TERMS_AGREED_STRIPE,
    TERMS_AGREED_WORLDPAY,
    TERMS_AGREED_SMARTPAY,
    TERMS_AGREED_EPDQ,
    TERMS_AGREED_GOV_BANKING_WORLDPAY,
    DENIED,
    LIVE
}
