package net.juniper.netconf;

import static java.lang.String.format;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import lombok.extern.log4j.Log4j2;
import net.juniper.netconf.element.Hello;
import net.juniper.netconf.exception.NetconfAuthenticationException;
import net.juniper.netconf.exception.NetconfConnectException;
import net.juniper.netconf.exception.NetconfException;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.channel.Channel;

/**
 * An implementation of {@link NetconfSshSession} that uses the
 * <a href="https://mina.apache.org/sshd-project/">Apache MINA SSHD</a> library.
 */
@Log4j2
public class MinaSshSession implements NetconfSshSession {

    private Device device;
    private SshClient sshClient;
    private ClientSession clientSession;
    private WaitingByteArrayOutputStream responseStream;
    private ClientChannel clientChannel;
    private OutputStream requestStream;

    public MinaSshSession() {
    }

    @Override
    public void openSession(final Device device) throws NetconfException {
        this.device = device;
        this.sshClient = SshClient.setUpDefaultClient();
        connect();
        login();
        createChannel();
    }

    @Override
    public boolean isConnected() {
        return clientSession != null
            && clientSession.isOpen()
            && clientSession.isAuthenticated()
            && clientChannel != null
            && clientSession.isOpen();
    }

    private void connect() throws NetconfConnectException {
        sshClient.start();
        try {
            log.debug("Connecting to device at {}:{}",
                device::getAddress, device::getPort
            );
            clientSession = sshClient
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
    }

    private void login() throws NetconfException {
        if (device.getPassword() != null) {
            loginWithPassword();
        } else if (device.getPrivateKey() != null) {
            loginWithPrivateKey();
        } else {
            loginWithIdentityFiles();
        }
    }

    private void createChannel() throws NetconfException {
        try {
            clientChannel = clientSession.createChannel(Channel.CHANNEL_SUBSYSTEM, "netconf");
            responseStream = new WaitingByteArrayOutputStream();
            clientChannel.setOut(responseStream);
            clientChannel.open().verify(device.getReadTimeout());
            requestStream = clientChannel.getInvertedIn();
        } catch (final IOException e) {
            throw new NetconfException("Unable to create netconf channel to device", e);
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
            clientSession.addPublicKeyIdentity(KeyUtils.getGetPair(device.getPrivateKey()));
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

    @Override
    public String sendMessage(final String message) throws NetconfException {
        try {
            responseStream.reset();
            log.debug("Sending:\n{}", message);
            requestStream.write((message + Hello.DEVICE_PROMPT).getBytes(StandardCharsets.UTF_8));
            requestStream.flush();
            responseStream.waitForEnding(Hello.DEVICE_PROMPT, device.getReadTimeout());
            final String response = responseStream.toString(StandardCharsets.UTF_8.name());
            log.debug("Received:\n{}", response);
            return response;
        } catch (final IOException e) {
            throw new NetconfException("I/O Exception communicating with device to respond", e);
        }
    }

    @Override
    public void close() {

        close(requestStream, "request stream");
        requestStream = null;

        close(responseStream, "response stream");
        responseStream = null;

        close(clientChannel, "client channel");
        clientChannel = null;

        close(clientSession, "client session");
        clientSession = null;

        close(sshClient, "SSH client");
    }

    private void close(final Closeable closeable, final String what) {
        if (closeable != null) {
            try {
                log.debug("Closing {} to {}:{}", () -> what, device::getAddress, device::getPort);
                closeable.close();
            } catch (final IOException e) {
                log.warn(format("Unable to close %s", what), e);
            }
        }
    }

    private void close(final org.apache.sshd.common.Closeable closeable, final String what) {
        if (closeable != null && closeable.isOpen()) {
            close((Closeable) closeable, what);
        }
    }

}
