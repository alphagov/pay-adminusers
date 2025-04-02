package uk.gov.pay.adminusers.expungeandarchive.resource;

import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import uk.gov.pay.adminusers.expungeandarchive.service.ExpungeAndArchiveHistoricalDataService;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static uk.gov.service.payments.logging.LoggingKeys.MDC_REQUEST_ID_KEY;

@Path("/v1/tasks")
@Tag(name = "Tasks")
public class ExpungeAndArchiveHistoricalDataResource {

    private final ExpungeAndArchiveHistoricalDataService expungeAndArchiveHistoricalDataService;

    @Inject
    public ExpungeAndArchiveHistoricalDataResource(ExpungeAndArchiveHistoricalDataService expungeAndArchiveHistoricalDataService) {
        this.expungeAndArchiveHistoricalDataService = expungeAndArchiveHistoricalDataService;
    }
    @POST
    @Produces(APPLICATION_JSON)
    @Operation(
            summary = "Deletes and archives historical data based on `expungeAndArchiveDataConfig`. " +
                    "Currently,  " +
                    " 1. deletes historical users not attached to any service" +
                    " 2. deletes historical invites" +
                    " 3. deletes forgotten passwords" +
                    " 4. archives services without transactions within the configured number of days and detaches users from service",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
            }
    )
    @Path("/expunge-and-archive-historical-data")
    public Response expungeAndArchiveHistoricalData() {
        String correlationId = MDC.get(MDC_REQUEST_ID_KEY) == null ? "ExpungeAndArchiveHistoricalDataResource-" + UUID.randomUUID() : MDC.get(MDC_REQUEST_ID_KEY);
        MDC.put(MDC_REQUEST_ID_KEY, correlationId);

        expungeAndArchiveHistoricalDataService.expungeAndArchiveHistoricalData();

        MDC.remove(MDC_REQUEST_ID_KEY);
        return Response.ok().build();
    }
}
