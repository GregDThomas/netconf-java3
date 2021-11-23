package net.juniper.netconf;

import lombok.extern.log4j.Log4j2;
import net.juniper.netconf.exception.NetconfException;

/**
 * Manages the NETCONF session to a device.
 */
@Log4j2
public class NetconfSession implements AutoCloseable {

    /**
     * The device to which this session is connected.
     */
    private final Device device;

    NetconfSession(final Device device) {
        this.device = device;
        log.info("Connecting to {}:{}", device::getAddress, device::getPort);
    }

    void connectUsingCertificate() throws NetconfException {

    }

    void connectUsingUsernameAndPassword() throws NetconfException {

    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("Not yet disconnecting from " + device);
    }
}
