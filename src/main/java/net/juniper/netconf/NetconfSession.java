package net.juniper.netconf;

import java.io.IOException;
import java.util.Arrays;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import lombok.extern.log4j.Log4j2;
import net.juniper.netconf.element.Hello;
import net.juniper.netconf.element.RpcCloseSession;
import net.juniper.netconf.exception.NetconfException;
import org.apache.logging.log4j.CloseableThreadContext;
import org.xml.sax.SAXException;

/**
 * Manages the NETCONF session to a device.
 */
@Log4j2
public class NetconfSession implements AutoCloseable {

    private static final String NSI = "NSI";
    private static long nextNetconfSessionId = 1;
    private final String currentNetconfSessionId;
    private final Device device;
    private final NetconfSshSession netconfSshSession;
    private long nextMessageId = 1;
    private Hello serverHello;

    private static String getNextNetconfSessionId() {
        return String.valueOf(nextNetconfSessionId++);
    }

    NetconfSession(final Device device) {
        this.currentNetconfSessionId = getNextNetconfSessionId();
        try (final CloseableThreadContext.Instance ignored
                 = CloseableThreadContext.put(NSI, currentNetconfSessionId)) {
            this.device = device;
            try {
                this.netconfSshSession = device.getSshImplementation().newInstance();
            } catch (final InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(
                    "Unable to instantiate instance of class "
                        + device.getSshImplementation().getName(),
                    e
                );
            }
            log.info("New NetconfSession created");
        }
    }

    void connect() throws NetconfException {
        try (final CloseableThreadContext.Instance ignored
                 = CloseableThreadContext.put(NSI, currentNetconfSessionId)) {
            netconfSshSession.openSession(device);
            sendHello();
        }
    }

    /**
     * Indicates if the NETCONF session is connected.
     *
     * @return {@code true} if the NETCONF session is connected, otherwise {@code false}.
     */
    public boolean isConnected() {
        try (final CloseableThreadContext.Instance ignored
                 = CloseableThreadContext.put(NSI, currentNetconfSessionId)) {
            return serverHello != null && netconfSshSession.isConnected();
        }
    }

    @Override
    public void close() throws NetconfException {
        try (final CloseableThreadContext.Instance ignored
                 = CloseableThreadContext.put(NSI, currentNetconfSessionId)) {
            if (netconfSshSession.isConnected()) {
                netconfSshSession.sendMessage(RpcCloseSession.builder()
                    .messageId(String.valueOf(nextMessageId++))
                    .build()
                    .getXml());
            }
            netconfSshSession.close();
            log.info("Disconnected from {}:{}", device::getAddress, device::getPort);
        }
    }

    private void sendHello() throws NetconfException {
        final Hello clientHello = Hello.builder().capabilities(
            Arrays.asList(Hello.URN_IETF_PARAMS_NETCONF_BASE_1_0,
                Hello.URN_IETF_PARAMS_NETCONF_BASE_1_0 + "#candidate",
                Hello.URN_IETF_PARAMS_NETCONF_BASE_1_0 + "#confirmed-commit",
                Hello.URN_IETF_PARAMS_NETCONF_BASE_1_0 + "#validate",
                Hello.URN_IETF_PARAMS_NETCONF_BASE_1_0 + "#url?protocol=http,ftp,file")).build();
        final String serverHello = netconfSshSession.sendMessage(clientHello.getXml());
        try {
            this.serverHello = Hello.from(serverHello);
        } catch (final ParserConfigurationException
            | IOException
            | SAXException
            | XPathExpressionException e
        ) {
            log.warn("Unexpected response received from server: {}", serverHello, e);
            throw new NetconfException("Unable to parse response from server", e);
        }
    }

}
