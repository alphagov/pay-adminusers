package uk.gov.pay.adminusers.logger;

import org.slf4j.Logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import static java.lang.String.format;
import static uk.gov.pay.adminusers.app.filters.LoggingFilter.currentRequestId;

/**
 * provides a pass-through proxy between slf4j.Logger instances and their proxied loggers
 * <P> appends request-id if bound to the current MDC context</p>
 */
class RequestIdLoggingHandler implements InvocationHandler {

    private final Logger target;

    RequestIdLoggingHandler(Logger target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (args[0] != null && (args[0] instanceof String)) {
            args[0] = format("[%s] - %s", currentRequestId(), args[0]);
        }
        return method.invoke(target, args);
    }

}
