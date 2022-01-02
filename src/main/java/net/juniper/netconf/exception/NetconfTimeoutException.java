package net.juniper.netconf.exception;

/**
 * This exception is raised if there is a timeout while communicating with a device.
 */
public class NetconfTimeoutException extends NetconfException {

    public NetconfTimeoutException() {
        super();
    }

    public NetconfTimeoutException(final String message) {
        super(message);
    }

    public NetconfTimeoutException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public NetconfTimeoutException(final Throwable cause) {
        super(cause);
    }

    public NetconfTimeoutException(
        final String message,
        final Throwable cause,
        final boolean enableSuppression,
        final boolean writableStackTrace
    ) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
