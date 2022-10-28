package uk.gov.pay.adminusers.service;

import uk.gov.pay.adminusers.model.InviteOtpRequest;

public abstract class InviteOtpDispatcher {

    /* default */ static final String SIX_DIGITS_WITH_LEADING_ZEROS = "%06d";

    /* default */ InviteOtpRequest inviteOtpRequest = null;

    public abstract void dispatchOtp(String inviteCode);

    public InviteOtpDispatcher withData(InviteOtpRequest data){
        this.inviteOtpRequest = data;
        return this;
    }
}
