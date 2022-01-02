package net.juniper.netconf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import lombok.extern.log4j.Log4j2;
import net.juniper.netconf.exception.NetconfException;
import net.juniper.netconf.exception.NetconfTimeoutException;

/**
 * A ByteArrayOutputStream that allows you to add a listener which is invoked when new data
 * is written to it.
 */
@Log4j2
public class WaitingByteArrayOutputStream extends ByteArrayOutputStream {

    final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    private void addListener(final Listener listener) {
        listeners.add(listener);
    }

    private void removeListener(final Listener listener) {
        listeners.remove(listener);
    }

    @Override
    public synchronized void write(final int b) {
        super.write(b);
        notifyListeners();
    }

    @Override
    public synchronized void write(final byte[] b, final int off, final int len) {
        super.write(b, off, len);
        notifyListeners();
    }

    private void notifyListeners() {
        listeners.forEach(Listener::onWrite);
    }

    /**
     * Waits until the stream - ignoring any whitespace - ends with the specified text.
     *
     * @param ending  the text to wait for.
     * @param timeout the maximum amount of time to wait.
     * @throws NetconfException if the expected text does not arrive in time.
     */
    public void waitForEnding(final String ending, final Duration timeout)
        throws NetconfException {
        final Semaphore semaphore = new Semaphore(0);
        final WaitingByteArrayOutputStream.Listener listener = () -> {
            try {
                if (streamEndsWith(ending)) {
                    semaphore.release();
                }
            } catch (final UnsupportedEncodingException e) {
                log.warn("Unexpected exception waiting for stream to end", e);
            }
        };
        try {
            addListener(listener);
            if (streamEndsWith(ending)) {
                // If we've already received it, don't bother waiting for it to arrive
                return;
            }
            final boolean acquired =
                semaphore.tryAcquire(1, timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!streamEndsWith(ending) && !acquired) {
                throw new NetconfTimeoutException("Timeout waiting for device to respond");
            }
        } catch (final IOException e) {
            throw new NetconfException("I/O Exception waiting for device to respond", e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NetconfException("Interrupted waiting for device to respond", e);
        } finally {
            removeListener(listener);
        }
    }

    private boolean streamEndsWith(final String ending) throws UnsupportedEncodingException {
        return this.toString(StandardCharsets.UTF_8.name()).trim().endsWith(ending);
    }

    private interface Listener {
        /**
         * This method is invoked whenever any data is written to the output stream.
         */
        void onWrite();
    }
}
