package net.juniper.netconf;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import net.juniper.netconf.exception.NetconfException;

/**
 * A ByteArrayOutputStream that allows you to add a listener which is invoked when new data
 * is written to it.
 */
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

    public void waitForEnding(final String ending, final Duration timeout) throws NetconfException {
        final Semaphore semaphore = new Semaphore(0);
        final WaitingByteArrayOutputStream.Listener listener = () -> {
            if (streamEndsWith(ending)) {
                semaphore.release();
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
                throw new NetconfException("Timeout waiting for device to respond");
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NetconfException("Interrupted waiting for device to respond", e);
        } finally {
            removeListener(listener);
        }
    }

    private boolean streamEndsWith(final String ending) {
        return this.toString().trim().endsWith(ending);
    }

    public interface Listener {
        /**
         * This method is invoked whenever any data is written to the output stream.
         */
        void onWrite();
    }
}
