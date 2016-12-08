package uk.gov.pay.adminusers.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;

/**
 * Factory providing proxy Logger instances.
 * <p>Provided loggers always appends <b>request-id</b> to the begining of log message
 * if bound to the MDC context </p>
 *
 * @see RequestIdLoggingHandler
 */
public class PayLoggerFactory {

    public static Logger getLogger(Class clazz) {
        return proxiedLogger(LoggerFactory.getLogger(clazz));
    }

    public static Logger getLogger(String name) {
        return proxiedLogger(LoggerFactory.getLogger(name));
    }

    private static Logger proxiedLogger(Logger logger) {
        return (Logger) Proxy.newProxyInstance(PayLoggerFactory.class.getClassLoader(),
                new Class<?>[]{Logger.class},
                new RequestIdLoggingHandler(logger)
        );
    }
}
