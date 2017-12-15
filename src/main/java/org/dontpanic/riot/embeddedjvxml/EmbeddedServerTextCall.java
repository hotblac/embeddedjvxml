package org.dontpanic.riot.embeddedjvxml;


import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.junit.Assert; // TODO remove this dependency on JUnit
import org.jvoicexml.DtmfInput;
import org.jvoicexml.JVoiceXml;
import org.jvoicexml.Session;
import org.jvoicexml.client.text.TextListener;
import org.jvoicexml.client.text.TextServer;
import org.jvoicexml.event.ErrorEvent;
import org.jvoicexml.event.JVoiceXMLEvent;
import org.jvoicexml.voicexmlunit.Call;
import org.jvoicexml.voicexmlunit.CallListener;
import org.jvoicexml.voicexmlunit.InputMonitor;
import org.jvoicexml.xml.ssml.Speak;
import org.jvoicexml.xml.ssml.SsmlDocument;

public class EmbeddedServerTextCall implements Call {
    /** Logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(EmbeddedServerTextCall.class);
    /** Known call listeners. */
    private final Collection<CallListener> listeners;
    /** The text server. */
    private TextServer textServer;
    /** Used port number. */
    private int portNumber;
    /** Buffered messages from JVoiceXml. */
    private OutputMessageBuffer outputBuffer;
    /** Monitor to wait until JVoiceXML is ready to accept input. */
    private InputMonitor inputMonitor;
    /** The last captured output. */
    private SsmlDocument lastOutput;
    /** The last observed error. */
    private JVoiceXMLEvent lastError;

    private JVoiceXml jvxml;

    /** The active session. */
    private Session session;


    /**
     * Constructs a new call.
     * @throws InterruptedException error initializing the output buffer
     */
    public EmbeddedServerTextCall(Session session, TextServer textServer) throws InterruptedException {
        this.session = session;
        this.textServer = textServer;

        outputBuffer = new OutputMessageBuffer();
        this.textServer.addTextListener(outputBuffer);
        inputMonitor = new InputMonitor();
        this.textServer.addTextListener(inputMonitor);
        listeners = new java.util.ArrayList<CallListener>();
    }


    /**
     * Adds the given listener of messages received from the JVoiceXML.
     * This allows for further investigation of the behavior.
     * @param listener the listener to add
     */
    public void addTextListener(final TextListener listener) {
        textServer.addTextListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void call(final URI uri) {
        LOGGER.info("calling '" + uri + "'");
        try {
            lastError = null;

            // run the dialog
            session.call(uri);
            for (CallListener listener : listeners) {
                listener.called(uri);
            }
        } catch (Exception | ErrorEvent e) {
            final AssertionError error = new AssertionError(e);
            notifyError(error);
            throw error;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SsmlDocument getNextOutput() {
        Assert.assertNotNull("no active session", session);
        try {
            lastOutput = outputBuffer.nextMessage();
            for (CallListener listener : listeners) {
                listener.heard(lastOutput);
            }
            LOGGER.info("heard '" + lastOutput + "'");
            return lastOutput;
        } catch (InterruptedException | JVoiceXMLEvent e) {
            try {
                lastError = session.getLastError();
            } catch (ErrorEvent ex) {
                final AssertionError error = new AssertionError(ex);
                notifyError(error);
                throw error;
            }
            if (lastError != null) {
                final AssertionError error = new AssertionError(lastError);
                notifyError(error);
                throw error;
            }
            final AssertionError error = new AssertionError(e);
            notifyError(error);
            throw error;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SsmlDocument getNextOutput(final long timeout) {
        Assert.assertNotNull("no active session", session);
        try {
            lastOutput = outputBuffer.nextMessage(timeout);
            for (CallListener listener : listeners) {
                listener.heard(lastOutput);
            }
            LOGGER.info("heard '" + lastOutput + "'");
            return lastOutput;
        } catch (InterruptedException | TimeoutException | JVoiceXMLEvent e) {
            try {
                lastError = session.getLastError();
            } catch (ErrorEvent ex) {
                final AssertionError error = new AssertionError(ex);
                notifyError(error);
                throw error;
            }
            if (lastError != null) {
                final AssertionError error = new AssertionError(lastError);
                notifyError(error);
                throw error;
            }
            final AssertionError error = new AssertionError(e);
            notifyError(error);
            throw error;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SsmlDocument getLastOutput() {
        return lastOutput;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void hears(final String utterance) {
        Assert.assertNotNull("no active session", session);
        final SsmlDocument document = getNextOutput();
        final Speak speak = document.getSpeak();
        final String output = speak.getTextContent();
        Assert.assertEquals(utterance, output);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void hears(final String utterance, final long timeout) {
        Assert.assertNotNull("no active session", session);
        final SsmlDocument document = getNextOutput(timeout);
        final Speak speak = document.getSpeak();
        final String output = speak.getTextContent();
        Assert.assertEquals(utterance, output);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void say(final String utterance) {
        say(utterance, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void say(final String utterance, final long timeout) {
        Assert.assertNotNull("no active session", session);
        try {
            if (timeout == 0) {
                inputMonitor.waitUntilExpectingInput();
            } else {
                inputMonitor.waitUntilExpectingInput(timeout);
            }
            textServer.sendInput(utterance);
            for (CallListener listener : listeners) {
                listener.said(utterance);
            }
            LOGGER.info("say '" + utterance + "'");
        } catch (InterruptedException | IOException | TimeoutException
                | JVoiceXMLEvent e) {
            try {
                lastError = session.getLastError();
            } catch (ErrorEvent ex) {
                final AssertionError error = new AssertionError(ex);
                notifyError(error);
                throw error;
            }
            if (lastError != null) {
                final AssertionError error = new AssertionError(lastError);
                notifyError(error);
                throw error;
            }
            final AssertionError error = new AssertionError(e);
            notifyError(error);
            throw error;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enter(final String digits) {
        Assert.assertNotNull("no active session", session);
        DtmfInput input = null;
        try {
            inputMonitor.waitUntilExpectingInput();
            input = session.getDtmfInput();
        } catch (JVoiceXMLEvent | InterruptedException e) {
            throw new AssertionError(e);
        }
        for (int i = 0; i < digits.length(); i++) {
            final char ch = digits.charAt(i);
            input.addDtmf(ch);
        }
        for (CallListener listener : listeners) {
            listener.entered(digits);
        }

        // Ignore spurious output update - take it from the buffer and discard
        try {
            outputBuffer.nextMessage(1000);
        } catch (InterruptedException | JVoiceXMLEvent | TimeoutException e) {
            LOGGER.warn("Interrupted while waiting for next message", e);
        }

        LOGGER.info("entered '" + digits + "'");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enter(final String digits, final long timeout) {
        Assert.assertNotNull("no active session", session);
        DtmfInput input = null;
        try {
            if (timeout == 0) {
                inputMonitor.waitUntilExpectingInput();
            } else {
                inputMonitor.waitUntilExpectingInput(timeout);
            }
            input = session.getDtmfInput();
        } catch (JVoiceXMLEvent | InterruptedException | TimeoutException e) {
            throw new AssertionError(e);
        }
        for (int i = 0; i < digits.length(); i++) {
            final char ch = digits.charAt(i);
            input.addDtmf(ch);
        }
        for (CallListener listener : listeners) {
            listener.entered(digits);
        }
        LOGGER.info("entered '" + digits + "'");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void waitUnitExpectingInput() {
        Assert.assertNotNull("no active session", session);
        try {
            inputMonitor.waitUntilExpectingInput();
        } catch (InterruptedException | JVoiceXMLEvent e) {
            JVoiceXMLEvent lastError;
            try {
                lastError = session.getLastError();
            } catch (ErrorEvent ex) {
                final AssertionError error = new AssertionError(ex);
                notifyError(error);
                throw error;
            }
            if (lastError != null) {
                final AssertionError error = new AssertionError(lastError);
                notifyError(error);
                throw error;
            }
            final AssertionError error = new AssertionError(e);
            notifyError(error);
            throw error;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void waitUnitExpectingInput(final long timeout) {
        Assert.assertNotNull("no active session", session);
        try {
            if (timeout == 0) {
                inputMonitor.waitUntilExpectingInput();
            } else {
                inputMonitor.waitUntilExpectingInput(timeout);
            }
        } catch (InterruptedException | TimeoutException
                | JVoiceXMLEvent e) {
            JVoiceXMLEvent lastError;
            try {
                lastError = session.getLastError();
            } catch (ErrorEvent ex) {
                final AssertionError error = new AssertionError(ex);
                notifyError(error);
                throw error;
            }
            if (lastError != null) {
                final AssertionError error = new AssertionError(lastError);
                notifyError(error);
                throw error;
            }
            final AssertionError error = new AssertionError(e);
            notifyError(error);
            throw error;
        }
    }

    /**
     * Notifies all listeners about the given error.
     *
     * @param error
     *            the caught error
     */
    private void notifyError(final AssertionError error) {
        for (CallListener listener : listeners) {
            listener.error(error);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void hangup() {
        if (session != null) {
            session.hangup();
            for (CallListener listener : listeners) {
                listener.hungup();
            }
            LOGGER.info("hungup");
            session = null;
        }

        textServer.stopServer();
        LOGGER.info("textServer stopped");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JVoiceXMLEvent getLastError() {
        if (session != null) {
            try {
                return session.getLastError();
            } catch (ErrorEvent e) {
                return lastError;
            }
        }
        return lastError;
    }
}
