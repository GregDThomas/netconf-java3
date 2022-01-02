package net.juniper.netconf;

import net.juniper.netconf.exception.NetconfException;

/**
 * An interface that allows different SSH implementations to be used to communicate with devices.
 */
public interface NetconfSshSession extends AutoCloseable {

    /**
     * Opens the NETCONF SSH session to the specified device.
     *
     * @param device The device to connect to.
     * @throws NetconfException if a session could not be opened.
     */
    void openSession(final Device device) throws NetconfException;

    /**
     * Indicates if the NETCONF session is connected.
     *
     * @return {@code true} if the SSH session is fully connected, otherwise {@code false}.
     */
    boolean isConnected();

    /**
     * Sends a message over the NETCONF SSH session.
     *
     * @param message The message to send.
     * @return the response to the message from the device - excluding any message separator.
     * @throws NetconfException if the message could not be sent or the response received.
     */
    String sendMessage(final String message) throws NetconfException;

    /**
     * Closes the session to the device.
     */
    void close();
}
