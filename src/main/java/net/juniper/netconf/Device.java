package net.juniper.netconf;

import static java.util.Optional.ofNullable;

import java.time.Duration;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import net.juniper.netconf.exception.NetconfException;

/**
 * A <code>Device</code> is used to define a Netconf server. It is immutable, but can be used to
 * create new sessions.
 *
 * <p>Example:
 * <pre>
 * {@code}
 * final Device device = Device.builder()
 *     .hostname("hostname")
 *     .username("username")
 *     .password("password")
 *     .build();
 *
 * try(final NetconfSession session = device.openSession()) {
 *     ....
 * }
 * {@code}
 * </pre>
 * <h2>Authentication</h2>
 * There are three ways the client can authenticate to the device;
 * <ul>
 *     <li>With a password - see {@link #password}</li>
 *     <li>With a private key - see {@link #privateKey}</li>
 *     <li>Using standard SSH identity files, stored in the current users {@code ~/.ssh} folder
 *     (e.g., {@code id_rsa}, {@code id_ecdsa}). This mechanism will be used if neither a password
 *     or a private key is supplied.</li>
 * </ul>
 */

@Value
@Log4j2
public class Device {

    // TODO: Add option to verify server RSA keys

    /**
     * The DNS or IP address of the device to connect to. Must be specified.
     */
    @NonNull
    String address;

    /**
     * The port number to connect to. Defaults to 830 if not supplied.
     */
    int port;

    /**
     * The username used to log in with.
     *
     * @see #password
     * @see #privateKey
     */
    @NonNull
    String username;

    /**
     * If supplied, the password used to log in with.
     *
     * @see #username
     * @see #privateKey
     */
    @ToString.Exclude
    String password;

    @Getter(AccessLevel.NONE)
    @ToString.Include(name = "password")
    String maskedPassword;

    /**
     * If supplied, the private key used to log in with.  This should be supplied in PEM format
     * (this normally starts <code>-----BEGIN RSA PRIVATE KEY-----</code>).
     *
     * @see #username
     * @see #password
     */
    @ToString.Exclude
    String privateKey;

    @Getter(AccessLevel.NONE)
    @ToString.Include(name = "privateKey")
    String maskedPrivateKey;

    /**
     * The maximum amount of time to wait when attempting to connect to the device. Defaults to
     * five seconds.
     */
    Duration connectTimeout;

    /**
     * The maximum amount of time to wait when attempting to log in to the device. Defaults to
     * the {@link #connectTimeout}
     */
    Duration loginTimeout;

    /**
     * The maximum amount of time to wait when reading data the device. Defaults to five seconds.
     */
    Duration readTimeout;

    @Builder
    private Device(
        @NonNull final String address,
        final Integer port,
        @NonNull final String username,
        final String password,
        final String privateKey,
        final Duration connectTimeout,
        final Duration loginTimeout,
        final Duration readTimeout
    ) {
        this.address = address;
        this.port = ofNullable(port).orElse(830);
        this.username = username;
        this.password = password;
        this.maskedPassword = password == null ? null : "********";
        this.privateKey = privateKey;
        this.maskedPrivateKey = privateKey == null ? null : "****************";
        this.connectTimeout = ofNullable(connectTimeout).orElseGet(() -> Duration.ofSeconds(5));
        this.loginTimeout = ofNullable(loginTimeout).orElse(this.connectTimeout);
        this.readTimeout = ofNullable(readTimeout).orElseGet(() -> Duration.ofSeconds(5));

        if (password != null && privateKey != null) {
            throw new IllegalArgumentException(
                "A privateKey cannot be supplied with a password"
            );
        }
        log.info("New device created: {}", this);
    }

    /**
     * Creates a new NETCONF session to this device, and sends the &lt;hello&gt; element.
     *
     * @return a new NETCONF session.
     * @throws NetconfException if a session could not be created.
     */
    public NetconfSession openSession() throws NetconfException {
        final NetconfSession session = new NetconfSession(this);
        session.connect();
        return session;
    }

}
