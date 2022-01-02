package net.juniper.netconf.element;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * Class to represent a NETCONF rpc-error element - https://datatracker.ietf.org/doc/html/rfc6241#section-4.3
 */
@Value
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class RpcError {

    ErrorType errorType;
    ErrorTag errorTag;
    ErrorSeverity errorSeverity;
    String errorPath;
    String errorMessage;
    String errorMessageLanguage;
    RpcErrorInfo errorInfo;

    /**
     * The type of RPC error.
     */
    @Getter
    @RequiredArgsConstructor
    public enum ErrorType {
        TRANSPORT("transport"),
        RPC("rpc"),
        PROTOCOL("protocol"),
        APPLICATION("application");

        /**
         * The text content representing this error type.
         */
        private final String textContent;

        /**
         * Determine the error type from the text.
         *
         * @param textContent the text.
         * @return the error type - or null if the textContent is not recognised.
         */
        public static ErrorType from(final String textContent) {
            for (final ErrorType errorType : ErrorType.values()) {
                if (errorType.textContent.equals(textContent)) {
                    return errorType;
                }
            }
            return null;
        }
    }

    /**
     * The tag associated with the RPC error.
     */
    @Getter
    @RequiredArgsConstructor
    public enum ErrorTag {
        IN_USE("in-use"),
        INVALID_VALUE("invalid-value"),
        TOO_BIG("too-big"),
        MISSING_ATTRIBUTE("missing-attribute"),
        BAD_ATTRIBUTE("bad-attribute"),
        UNKNOWN_ATTRIBUTE("unknown-attribute"),
        MISSING_ELEMENT("missing-element"),
        BAD_ELEMENT("bad-element"),
        UNKNOWN_ELEMENT("unknown-element"),
        UNKNOWN_NAMESPACE("unknown-namespace"),
        ACCESS_DENIED("access-denied"),
        LOCK_DENIED("lock-denied"),
        DATA_EXISTS("data-exists"),
        DATA_MISSING("data-missing"),
        OPERATION_NOT_SUPPORTED("operation-not-supported"),
        OPERATION_FAILED("operation-failed"),
        PARTIAL_OPERATION("partial-operation"),
        MALFORMED_MESSAGE("malformed-message");

        /**
         * The text content representing this error tag.
         */
        private final String textContent;

        /**
         * Determine the error tag from the text.
         *
         * @param textContent the text.
         * @return the error tag - or null if the textContent is not recognised.
         */
        public static ErrorTag from(final String textContent) {
            for (final ErrorTag errorTag : ErrorTag.values()) {
                if (errorTag.textContent.equals(textContent)) {
                    return errorTag;
                }
            }
            return null;
        }
    }

    /**
     * The severity of the RPC error.
     */
    @Getter
    @RequiredArgsConstructor
    public enum ErrorSeverity {
        ERROR("error"), WARNING("warning");

        /**
         * The text content representing the error severity.
         */
        private final String textContent;

        /**
         * Determine the error tag from the text.
         *
         * @param textContent the text.
         * @return the error severity - or null if the textContent is not recognised.
         */
        public static ErrorSeverity from(final String textContent) {
            for (final ErrorSeverity errorSeverity : ErrorSeverity.values()) {
                if (errorSeverity.textContent.equals(textContent)) {
                    return errorSeverity;
                }
            }
            return null;
        }
    }

    /**
     * Class to represent a NETCONF rpc error-info element - https://datatracker.ietf.org/doc/html/rfc6241#section-4.3
     */
    @Value
    @Builder
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class RpcErrorInfo {

        String badAttribute;
        String badElement;
        String badNamespace;
        String sessionId;
        String okElement;
        String errElement;
        String noOpElement;

    }
}
