package uk.gov.pay.adminusers.app.filters;

import com.google.common.base.Stopwatch;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.MDC;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public class LoggingFilter implements Filter {

    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    private static final Logger logger = PayLoggerFactory.getLogger(LoggingFilter.class);

    public static String currentRequestId() {
        return MDC.get(HEADER_REQUEST_ID);
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {

        Stopwatch stopwatch = Stopwatch.createStarted();

        String requestURL = ((HttpServletRequest) servletRequest).getRequestURI();
        String requestMethod = ((HttpServletRequest) servletRequest).getMethod();
        String requestId = StringUtils.defaultString(((HttpServletRequest) servletRequest).getHeader(HEADER_REQUEST_ID),"");

        MDC.put(HEADER_REQUEST_ID, requestId);

        logger.info(format("%s to %s began", requestMethod, requestURL));
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } catch (Throwable throwable) {
            logger.error("Exception - adminusers request - " + requestURL + " - exception - " + throwable.getMessage(), throwable);
        } finally {
            logger.info(format("%s to %s ended - total time %dms", requestMethod, requestURL,
                    stopwatch.elapsed(TimeUnit.MILLISECONDS)));
            stopwatch.stop();
        }
    }

    @Override
    public void destroy() {
    }
}
