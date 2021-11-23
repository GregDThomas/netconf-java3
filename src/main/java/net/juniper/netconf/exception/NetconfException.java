package net.juniper.netconf.exception;

/**
 * This exception is raised if there is an error communicating with a device.
 */
public class NetconfException extends Exception {

    public NetconfException() {
        super();
    }

    public NetconfException(final String message) {
        super(message);
    }

    public NetconfException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public NetconfException(final Throwable cause) {
        super(cause);
    }

    public NetconfException(final String message, final Throwable cause,
                               final boolean enableSuppression, final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
