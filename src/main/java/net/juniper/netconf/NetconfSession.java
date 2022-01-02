package net.juniper.netconf;

import java.io.IOException;
import java.util.Arrays;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import lombok.extern.log4j.Log4j2;
import net.juniper.netconf.element.Hello;
import net.juniper.netconf.exception.NetconfException;
import org.xml.sax.SAXException;

/**
 * Manages the NETCONF session to a device.
 */
@Log4j2
public class NetconfSession implements AutoCloseable {

    private final Device device;
    private final NetconfSshSession netconfSshSession;
    private Hello serverHello;

    NetconfSession(final Device device) {
        this.device = device;
        try {
            netconfSshSession = device.getSshImplementation().newInstance();
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(
                "Unable to instantiate instance of class "
                    + device.getSshImplementation().getName(),
                e
            );
        }
    }

    void connect() throws NetconfException {
        netconfSshSession.openSession(device);
        sendHello();
    }

    /**
     * Indicates if the NETCONF session is connected.
     *
     * @return {@code true} if the NETCONF session is connected, otherwise {@code false}.
     */
    public boolean isConnected() {
        return serverHello != null && netconfSshSession.isConnected();
    }

    @Override
    public void close() {
        netconfSshSession.close();
        log.info("Disconnected from {}:{}", device::getAddress, device::getPort);
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
