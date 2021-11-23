package net.juniper.netconf.exception;

/**
 * This exception is raised if there is an error communicating with a device.
 */
public class NetconfConnectException extends NetconfException {

    public NetconfConnectException() {
        super();
    }

    public NetconfConnectException(final String message) {
        super(message);
    }

    public NetconfConnectException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public NetconfConnectException(final Throwable cause) {
        super(cause);
    }

    public NetconfConnectException(
        final String message,
        final Throwable cause,
        final boolean enableSuppression,
        final boolean writableStackTrace
    ) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
