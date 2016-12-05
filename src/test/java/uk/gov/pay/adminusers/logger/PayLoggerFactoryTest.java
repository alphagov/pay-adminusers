package uk.gov.pay.adminusers.logger;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.adminusers.app.filters.LoggingFilter.HEADER_REQUEST_ID;

public class PayLoggerFactoryTest {

    ByteArrayOutputStream baos;
    PrintStream printStream;

    @Before
    public void before() throws Exception {
        baos = new ByteArrayOutputStream();
        printStream = new PrintStream(baos);
        System.setOut(printStream);
    }

    @Test
    @Ignore
    public void shouldAddRequestIdToAnyLoggerEvent() throws Exception {
        MDC.put(HEADER_REQUEST_ID,"some-header-id");
        Logger logger = PayLoggerFactory.getLogger(PayLoggerFactoryTest.class);

        logger.error("logline");
        String loggerOutput = baos.toString();

        assertThat(loggerOutput, containsString("[some-header-id] - logline"));
    }


}
