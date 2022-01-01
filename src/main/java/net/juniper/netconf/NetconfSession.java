package net.juniper.netconf;

import static java.lang.String.format;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
 * Manages the NETCONF session to a device.
 */
@Log4j2
public class NetconfSession implements AutoCloseable {

    private final Device device;
    private final SshClient sshClient;
    private ClientSession clientSession;
    private WaitingByteArrayOutputStream responseStream;
    private ClientChannel clientChannel;
    private OutputStream requestStream;

    NetconfSession(final Device device) {
        this.device = device;
        sshClient = SshClient.setUpDefaultClient();
    }

    void connect() throws NetconfException {

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

        if (device.getPassword() != null) {
            loginWithPassword();
        } else if (device.getPrivateKey() != null) {
            loginWithPrivateKey();
        } else {
            loginWithIdentityFiles();
        }
        setupChannel();
        sendHello();
    }

    private void setupChannel() throws NetconfException {
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

        close(requestStream, "request stream");
        requestStream = null;

        close(responseStream, "response stream");
        responseStream = null;

        close(clientChannel, "client channel");
        clientChannel = null;

        close(clientSession, "client session");
        clientSession = null;

        close(sshClient, "SSH client");
        log.info("Disconnected from {}:{}", device::getAddress, device::getPort);
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

    private void sendHello() throws NetconfException {
        final Hello clientHello = Hello.builder()
            .capabilities(Arrays.asList(
                Hello.URN_IETF_PARAMS_NETCONF_BASE_1_0,
                Hello.URN_IETF_PARAMS_NETCONF_BASE_1_0 + "#candidate",
                Hello.URN_IETF_PARAMS_NETCONF_BASE_1_0 + "#confirmed-commit",
                Hello.URN_IETF_PARAMS_NETCONF_BASE_1_0 + "#validate",
                Hello.URN_IETF_PARAMS_NETCONF_BASE_1_0 + "#url?protocol=http,ftp,file"
            ))
            .build();
        final String xml = clientHello.getXml();
        sendXml(xml);
        sendXml(
            "<?xml version='1.0' encoding='utf-8'?><rpc xmlns='urn:ietf:params:xml:ns:netconf:base:1.0' message-id='3'><get-chassis-inventory/></rpc>");
    }

    private void sendXml(final String xml) throws NetconfException {
        try {
            responseStream.reset();
            log.warn("Sending:\n{}", xml);
            requestStream.write((xml + Hello.DEVICE_PROMPT).getBytes(StandardCharsets.UTF_8));
            requestStream.flush();
            responseStream.waitForEnding(Hello.DEVICE_PROMPT, device.getReadTimeout());
            log.warn("Received:\n{}", responseStream.toString());
        } catch (final IOException e) {
            throw new NetconfException("I/O Exception communicating with device to respond", e);
        }
    }

}
