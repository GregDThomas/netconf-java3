package net.juniper.netconf;

import net.juniper.netconf.exception.NetconfException;

/**
 * Primarily used for testing.
 */
public class NetconfSessionFactory {

    // TODO not sure about the usefulness of this class?

    NetconfSessionFactory() {
    }

    NetconfSession createSessionUsingCertificate(final Device device) throws NetconfException {
        final NetconfSession netconfSession = new NetconfSession(device);
        netconfSession.connectUsingCertificate();
        return netconfSession;
    }

    NetconfSession createSessionUsingPassword(final Device device) throws NetconfException {
        final NetconfSession netconfSession = new NetconfSession(device);
        netconfSession.connectUsingUsernameAndPassword();
        return netconfSession;
    }
}
