package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum SecondFactorMethod {

    @JsonProperty("SMS")
    SMS {
        @Override
        public String toString() {
            return "sms";
        }
    },

    @JsonProperty("APP")
    APP {
        @Override
        public String toString() {
            return "app";
        }
    },
    
    @JsonProperty("BACKUP_CODE")
    BACKUP_CODE {
        @Override
        public String toString() {
            return "backup_code";
        }
    }

}
