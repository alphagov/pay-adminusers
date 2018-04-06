package uk.gov.pay.adminusers.model;

public enum SecondFactorMethod {

    SMS {
        @Override
        public String toString() {
            return "sms";
        }
    },

    APP {
        @Override
        public String toString() {
            return "app";
        };
    }

}
