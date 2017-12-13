package org.dontpanic.riot.embeddedjvxml;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.jvoicexml.JVoiceXmlMain;
import org.jvoicexml.JVoiceXmlMainListener;
import org.jvoicexml.client.text.TextServer;
import org.jvoicexml.event.ErrorEvent;
import org.jvoicexml.event.JVoiceXMLEvent;
import org.jvoicexml.voicexmlunit.Call;

import java.net.URL;

public class LocalVxmlTest implements JVoiceXmlMainListener {

    private static final Logger LOGGER = Logger.getLogger(LocalVxmlTest.class);

    private static final int TEXT_SERVER_PORT = 4242;

    private TextServer server;
    private JVoiceXmlMain jvxml;

    @Before
    public synchronized void startSession() throws JVoiceXMLEvent, Exception {
        final EmbeddedTextConfiguration config = new EmbeddedTextConfiguration();
        jvxml = new JVoiceXmlMain(config);
        jvxml.addListener(this);
        jvxml.start();

        wait();

        server = new TextServer(TEXT_SERVER_PORT);
        server.start();
        server.waitStarted();
    }

    @After
    public void endSession() throws ErrorEvent {
        if (jvxml != null) {
            jvxml.shutdown();
        }
    }

    @Test
    public void testLocalVxml() throws Exception, ErrorEvent {
        final URL vxmlFile = getClass().getClassLoader().getResource("hello.vxml");

        Call call = new EmbeddedServerTextCall(jvxml, server);
        call.call(vxmlFile.toURI());
        call.hears("Hello World!");
        call.hears("Goodbye!");
        //call.hangup();
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
