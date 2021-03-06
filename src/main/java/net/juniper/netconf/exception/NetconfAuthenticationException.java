package net.juniper.netconf.exception;

/**
 * This exception is raised if the device rejects the supplied credentials.
 */
public class NetconfAuthenticationException extends NetconfException {

    public NetconfAuthenticationException() {
        super();
    }

    public NetconfAuthenticationException(final String message) {
        super(message);
    }

    public NetconfAuthenticationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public NetconfAuthenticationException(final Throwable cause) {
        super(cause);
    }

    public NetconfAuthenticationException(
        final String message,
        final Throwable cause,
        final boolean enableSuppression,
        final boolean writableStackTrace
    ) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
