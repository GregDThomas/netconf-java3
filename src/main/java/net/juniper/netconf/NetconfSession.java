package net.juniper.netconf;

import static java.lang.String.format;

import java.io.IOException;
import lombok.extern.log4j.Log4j2;
import net.juniper.netconf.exception.NetconfAuthenticationException;
import net.juniper.netconf.exception.NetconfConnectException;
import net.juniper.netconf.exception.NetconfException;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;

/**
 * Manages the NETCONF session to a device.
 */
@Log4j2
public class NetconfSession implements AutoCloseable {

    private final Device device;
    private final SshClient client;
    private ClientSession clientSession;

    NetconfSession(final Device device) {
        this.device = device;
        client = SshClient.setUpDefaultClient();
    }

    void connect() throws NetconfException {

        client.start();
        try {
            log.debug("Connecting to device at {}:{}",
                device::getAddress, device::getPort
            );
            clientSession = client
                .connect(device.getUsername(), device.getAddress(), device.getPort())
                .verify(device.getConnectTimeout())
                .getSession();
        } catch (final IOException e) {
            close();
            throw new NetconfConnectException(
                format("Unable to connect to device at %s:%d",
                    device.getAddress(), device.getPort()
                ), e);
        }

        if (device.getPassword() != null) {
            loginWithPassword();
        } else if (device.getPrivateKey() != null) {
            loginWithPrivateKey();
        } else {
            loginWithIdentityFiles();
        }
    }

    private void loginWithPassword() throws NetconfAuthenticationException {
        try {
            log.debug("Logging in with password to device with username '{}'",
                device.getUsername()
            );
            clientSession.addPasswordIdentity(device.getPassword());
            clientSession.auth().verify(device.getLoginTimeout());
        } catch (final IOException e) {
            close();
            throw new NetconfAuthenticationException(
                "Unable to login with username " + device.getUsername(),
                e
            );
        }
        log.info("Connected using password to {}@{}:{}",
            device::getUsername, device::getAddress, device::getPort);
    }

    private void loginWithPrivateKey() throws NetconfException {
        try {
            log.debug("Logging in with private key to device with username '{}'",
                device.getUsername()
            );
            clientSession.addPublicKeyIdentity(CertUtils.getGetPair(device.getPrivateKey()));
            clientSession.auth().verify(device.getLoginTimeout());
        } catch (final IOException e) {
            close();
            throw new NetconfAuthenticationException(
                "Unable to login with username " + device.getUsername(),
                e
            );
        }
        log.info("Connected using private key to {}@{}:{}",
            device::getUsername, device::getAddress, device::getPort);
    }

    private void loginWithIdentityFiles() throws NetconfAuthenticationException {
        try {
            log.debug("Logging in with identify files to device with username '{}'",
                device.getUsername());
            clientSession.auth().verify(device.getLoginTimeout());
        } catch (final IOException e) {
            close();
            throw new NetconfAuthenticationException(
                "Unable to login with username " + device.getUsername(),
                e
            );
        }
        log.info("Connected using password to {}@{}:{}",
            device::getUsername, device::getAddress, device::getPort);
    }

    /**
     * Indicates if the NETCONF session is connected.
     *
     * @return {@code true} if the NETCONF session is connected, otherwise {@code false}.
     */
    public boolean isConnected() {
        return clientSession != null && clientSession.isOpen() && clientSession.isAuthenticated();
    }

    /**
     * Closes the session to the device.
     */
    @Override
    public void close() {
        if (clientSession != null && clientSession.isOpen()) {
            try {
                log.debug("Closing client session to {}:{}", device::getAddress, device::getPort);
                clientSession.close();
            } catch (final IOException e) {
                log.warn("Unable to close client session", e);
            }
        }
        clientSession = null;
        if (client.isOpen()) {
            try {
                log.debug("Closing client to {}:{}", device::getAddress, device::getPort);
                client.close();
            } catch (final IOException e) {
                log.warn("Unable to close client", e);
            }
        }
        log.debug("Disconnected from {}:{}", device::getAddress, device::getPort);
    }
}
