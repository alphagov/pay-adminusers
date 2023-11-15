package uk.gov.pay.adminusers.queue.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class ServiceArchivedTaskData {

    @JsonProperty("service_external_id")
    private String serviceId;

    public ServiceArchivedTaskData() {
    }

    public String getServiceId() {
        return serviceId;
    }

    public ServiceArchivedTaskData(String serviceId) {
        this.serviceId = serviceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceArchivedTaskData that = (ServiceArchivedTaskData) o;
        return serviceId.equals(that.serviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceId);
    }
}
