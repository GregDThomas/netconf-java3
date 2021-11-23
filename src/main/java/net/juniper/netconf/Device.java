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
 *     .hostKeysFileName("hostKeysFileName")
 *     .build();
 *
 * try(final NetconfSession session = device.openSession()) {
 *     ....
 * }
 * {@code}
 * </pre>
 */

@Value
@Log4j2
public class Device {

    /**
     * The DNS or IP address of the device to connect to. Must be specified.
     */
    String address;

    /**
     * The port number to connect to. Defaults to 830 if not supplied.
     */
    int port;

    /**
     * The username used to log in with. Will be used in conjunction with the password if a private
     * certificate is not supplied, or the supplied certificate does not work.
     *
     * @see #password
     * @see #certificate
     */
    String username;

    /**
     * The password used to log in with. Will be used in conjunction with the username if a private
     * certificate is not supplied, or the supplied certificate does not work.
     *
     * @see #username
     * @see #certificate
     */
    @ToString.Exclude
    String password;

    @Getter(AccessLevel.NONE)
    @ToString.Include(name = "password")
    String maskedPassword;

    /**
     * The private key certificate used to log in with, in PEM format (this normally starts
     * <code>-----BEGIN RSA PRIVATE KEY-----</code>). Used in preference to the username and
     * password, though if they are supplied they will be used as a backup if this certificate
     * does not work.
     *
     * @see #username
     * @see #certificate
     */
    @ToString.Exclude
    String certificate;

    @ToString.Include(name = "certificate")
    String maskedCertificate;

    /**
     * The maximum amount of time to wait when attempting to connect to the device. Defaults to
     * five seconds.
     */
    Duration connectTimeout;

    /**
     * The maximum amount of time to wait when reading data the device. Defaults to five seconds.
     */
    Duration readTimeout;

    /**
     * This is used for test purposes only. Code using this library should not access the
     * {@link NetconfSessionFactory} directly.
     */
    @ToString.Exclude
    @Getter(AccessLevel.NONE)
    NetconfSessionFactory netconfSessionFactory;

    @Builder
    private Device(
        @NonNull final String address,
        final Integer port,
        final String username,
        final String password,
        final String certificate,
        final NetconfSessionFactory netconfSessionFactory,
        final Duration connectTimeout,
        final Duration readTimeout
    ) {
        this.address = address;
        this.port = ofNullable(port).orElse(830);
        this.username = username;
        this.password = password;
        this.maskedPassword = password == null ? null : "********";
        this.certificate = certificate;
        this.maskedCertificate = certificate == null ? null : "****************";
        this.netconfSessionFactory = ofNullable(netconfSessionFactory)
            .orElseGet(NetconfSessionFactory::new);
        this.connectTimeout = ofNullable(connectTimeout).orElseGet(() -> Duration.ofSeconds(5));
        this.readTimeout = ofNullable(readTimeout).orElseGet(() -> Duration.ofSeconds(5));

        if (username == null && password == null && certificate == null) {
            throw new IllegalArgumentException("Credentials in the form of a client certificate"
                + " and/or username and password must be supplied");
        }
        if ((username == null && password != null) || (username != null && password == null)) {
            throw new IllegalArgumentException("A username must be supplied with a password");
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
        if (certificate != null) {
            try {
                return netconfSessionFactory.createSessionUsingCertificate(this);
            } catch (final NetconfException e) {
                if (username == null) {
                    throw e;
                } else {
                    log.warn("Unable to connect to {}:{} using certificate,"
                        + " trying username and password", address, port, e);
                }
            }
        }
        return netconfSessionFactory.createSessionUsingPassword(this);
    }

}
