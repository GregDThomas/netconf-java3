package net.juniper.netconf.exception;

/**
 * This exception is raised if there is an error communicating with a device.
 */
public class NetconfCertificateException extends NetconfException {

    public NetconfCertificateException() {
        super();
    }

    public NetconfCertificateException(final String message) {
        super(message);
    }

    public NetconfCertificateException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public NetconfCertificateException(final Throwable cause) {
        super(cause);
    }

    public NetconfCertificateException(
        final String message,
        final Throwable cause,
        final boolean enableSuppression,
        final boolean writableStackTrace
    ) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
