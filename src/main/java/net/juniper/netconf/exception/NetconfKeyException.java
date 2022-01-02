package net.juniper.netconf.exception;

/**
 * This exception is raised if the supplied private key cannot be decoded.
 */
public class NetconfKeyException extends NetconfException {

    public NetconfKeyException() {
        super();
    }

    public NetconfKeyException(final String message) {
        super(message);
    }

    public NetconfKeyException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public NetconfKeyException(final Throwable cause) {
        super(cause);
    }

    public NetconfKeyException(
        final String message,
        final Throwable cause,
        final boolean enableSuppression,
        final boolean writableStackTrace
    ) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
