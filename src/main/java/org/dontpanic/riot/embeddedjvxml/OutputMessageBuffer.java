package org.dontpanic.riot.embeddedjvxml;

import java.net.InetSocketAddress;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jvoicexml.client.text.TextListener;
import org.jvoicexml.event.GenericVoiceXmlEvent;
import org.jvoicexml.event.JVoiceXMLEvent;
import org.jvoicexml.event.plain.ConnectionDisconnectHangupEvent;
import org.jvoicexml.xml.ssml.SsmlDocument;

public class OutputMessageBuffer implements TextListener {
    /** The last received output message. */
    private SsmlDocument output;
    /** Synchronization. */
    private final Semaphore receiveSem;
    private final Semaphore readSem;
    /** Caught event while waiting for an input. */
    private JVoiceXMLEvent event;

    /**
     * Constructs a new object.
     * @throws InterruptedException error initializing the semaphores
     */
    public OutputMessageBuffer() throws InterruptedException {
        receiveSem = new Semaphore(1);
        receiveSem.acquire();
        readSem = new Semaphore(1);
        readSem.release();
    }

    /**
     * Retrieves the next message.
     * @return next message, <code>null</code> if the call was interrupted.
     * @throws InterruptedException
     *          interrupted while waiting
     * @throws JVoiceXMLEvent
     *          error waiting
     */
    public SsmlDocument nextMessage()
            throws InterruptedException, JVoiceXMLEvent {
        try {
            receiveSem.acquire();
            if (event != null) {
                throw event;
            }
            return output;
        } finally {
            output = null;
            event = null;
            readSem.release();
        }
    }

    /**
     * Retrieves the next message.
     * @return next message, <code>null</code> if the call was interrupted.
     * @param timeout the timeout to wait at max in msec, waits forever, if
     *          timeout is zero
     * @throws InterruptedException
     *         waiting interrupted
     * @throws TimeoutException
     *         waiting time exceeded
     * @throws JVoiceXMLEvent
     *          error waiting
     */
    public SsmlDocument nextMessage(final long timeout)
            throws InterruptedException, TimeoutException, JVoiceXMLEvent {
        try {
            final boolean success =
                    receiveSem.tryAcquire(timeout, TimeUnit.MILLISECONDS);
            if (event != null) {
                throw event;
            }
            if (!success || (output == null)) {
                throw new TimeoutException("timeout of '" + timeout
                        + "' msec exceeded while waiting for next message");
            }
            return output;
        } finally {
            output = null;
            event = null;
            readSem.release();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void started() {
    }

    @Override
    public void connected(final InetSocketAddress remote) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void outputSsml(final SsmlDocument document) {
        try {
            readSem.acquire();
            output = document;
        } catch (InterruptedException e) {
            event = new GenericVoiceXmlEvent("interrupted", e.getMessage());
        } finally {
            receiveSem.release();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void expectingInput() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void inputClosed() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disconnected() {
        event = new ConnectionDisconnectHangupEvent();
        receiveSem.release();
    }
}
