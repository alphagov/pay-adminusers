package uk.gov.pay.adminusers.client.ledger.exception;

import jakarta.ws.rs.core.Response;

public class LedgerException extends RuntimeException {
    private Integer status;

    public LedgerException(Response response) {
        super(response.toString());
        status = response.getStatus();
    }

    public LedgerException(Exception exception) {
        super(exception);
    }

    @Override
    public String toString() {
        return "LedgerException{" +
                "status=" + status +
                ", message=" + getMessage() +
                '}';
    }
}
