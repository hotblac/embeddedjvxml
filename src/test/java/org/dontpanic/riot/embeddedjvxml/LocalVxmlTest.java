package org.dontpanic.riot.embeddedjvxml;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.jvoicexml.ConnectionInformation;
import org.jvoicexml.JVoiceXmlMain;
import org.jvoicexml.JVoiceXmlMainListener;
import org.jvoicexml.Session;
import org.jvoicexml.client.text.TextServer;
import org.jvoicexml.event.ErrorEvent;
import org.jvoicexml.event.JVoiceXMLEvent;

import java.net.URL;

public class LocalVxmlTest implements JVoiceXmlMainListener {

    private static final Logger LOGGER = Logger.getLogger(LocalVxmlTest.class);

    private TextServer server;
    private Session session;
    private JVoiceXmlMain jvxml;

    @Before
    public synchronized void startSession() throws JVoiceXMLEvent, Exception {
        final EmbeddedTextConfiguration config = new EmbeddedTextConfiguration();
        jvxml = new JVoiceXmlMain(config);
        jvxml.addListener(this);
        jvxml.start();

        wait();

        server = new TextServer(4242);
        server.start();
        server.waitStarted();
        final ConnectionInformation client = server.getConnectionInformation();
        session = jvxml.createSession(client);
    }

    @After
    public void endSession() throws ErrorEvent {
        if (session != null) {
            session.waitSessionEnd();
            session.hangup();
            server.stopServer(); // Expect a SocketException to be logged and swallowed!
        }

        if (jvxml != null) {
            jvxml.shutdown();
        }
    }

    @Test
    public void testLocalVxml() throws Exception, ErrorEvent {
        final URL vxmlFile = getClass().getClassLoader().getResource("hello.vxml");
        session.call(vxmlFile.toURI());
    }

    @Override
    public synchronized void jvxmlStarted() {
        notifyAll();
    }

    @Override
    public void jvxmlTerminated() {
    }

    @Override
    public void jvxmlStartupError(final Throwable exception) {
        LOGGER.error("error starting JVoiceML", exception);
        jvxmlStarted(); // cancel
    }
}
