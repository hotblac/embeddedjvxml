package org.dontpanic.riot.embeddedjvxml;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.jvoicexml.JVoiceXmlMain;
import org.jvoicexml.JVoiceXmlMainListener;
import org.jvoicexml.client.text.TextServer;
import org.jvoicexml.event.plain.ConnectionDisconnectHangupEvent;
import org.jvoicexml.voicexmlunit.Call;
import org.jvoicexml.xml.ssml.Speak;
import org.jvoicexml.xml.ssml.SsmlDocument;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static junit.framework.TestCase.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotNull;

public class LocalVxmlTest implements JVoiceXmlMainListener {

    private static final Logger LOGGER = Logger.getLogger(LocalVxmlTest.class);

    private static final long TEST_TIMEOUT_MS = 4000;
    private static final int TEXT_SERVER_PORT = 4242;

    private TextServer server;
    private JVoiceXmlMain jvxml;

    @Before
    public synchronized void startSession() throws Exception {
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
    public synchronized void endSession() throws Exception {
        server.stopServer();
        jvxml.getDocumentServer().stop();
        jvxml.shutdown();

        wait();
    }

    @Test(timeout = TEST_TIMEOUT_MS)
    public void testLocalVxml() throws Exception {
        Call call = new EmbeddedServerTextCall(jvxml, server);
        call.call(fileUri("hello.vxml"));
        call.hears("Hello World!");
        call.hears("Goodbye!");
        //call.hangup();
    }

    @Test(timeout = TEST_TIMEOUT_MS)
    public void testAudioResponse() throws Exception {
        Call call = new EmbeddedServerTextCall(jvxml, server);
        call.call(fileUri("audio.vxml"));

        // Expect two audio responses
        final String audioSrc1 = audioSrc(call.getNextOutput());
        assertThat(audioSrc1, endsWith("audio-in-block.wav"));


        final String audioSrc2 = audioSrc(call.getNextOutput());
        assertThat(audioSrc2, endsWith("audio-in-prompt.wav"));

        try {
            call.getNextOutput();
            fail("No further output expected");
        } catch (AssertionError e) {
            // Expected event
            assertThat(e.getCause(), instanceOf(ConnectionDisconnectHangupEvent.class));
        }
    }

    @Test(timeout = TEST_TIMEOUT_MS)
    public void testDtmfInput() throws Exception {
        Call call = new EmbeddedServerTextCall(jvxml, server);
        call.call(fileUri("dtmf.vxml"));
        call.hears("Do you like this example? Please enter 1 for yes or 2 for no");
        call.enter("1");
        call.hears("You like this example.");
    }

    @Test(timeout = TEST_TIMEOUT_MS)
    public void testDtmfInvalidInput() throws Exception {
        Call call = new EmbeddedServerTextCall(jvxml, server);
        call.call(fileUri("dtmf.vxml"));
        call.hears("Do you like this example? Please enter 1 for yes or 2 for no");
        call.enter("9");
        // Expect reprompt
        call.hears("Do you like this example? Please enter 1 for yes or 2 for no");
    }

    @Test(timeout = TEST_TIMEOUT_MS)
    public void testSpokenInput() throws Exception {
        Call call = new EmbeddedServerTextCall(jvxml, server);
        call.call(fileUri("input.vxml"));
        call.hears("Do you like this example?");
        call.say("yes");
        call.hears("You like this example.");
    }

    @Test(timeout = TEST_TIMEOUT_MS)
    public void testSpokenInvalidInput() throws Exception {
        Call call = new EmbeddedServerTextCall(jvxml, server);
        call.call(fileUri("input.vxml"));
        call.hears("Do you like this example?");
        call.say("um...");
        // Expect reprompt
        call.hears("Do you like this example?");
    }

    @Test(timeout = TEST_TIMEOUT_MS)
    public void testGoto() throws Exception {
        Call call = new EmbeddedServerTextCall(jvxml, server);
        call.call(fileUri("goto1.vxml"));
        call.hears("Prompt from goto1.vxml");
        call.hears("Prompt from goto2.vxml");
    }

    @Test(timeout = TEST_TIMEOUT_MS)
    public void testSubmit() throws Exception {
        Call call = new EmbeddedServerTextCall(jvxml, server);
        call.call(fileUri("submit1.vxml"));
        call.hears("Prompt from submit1.vxml");
        call.hears("Prompt from submit2.vxml");
    }

    @Override
    public synchronized void jvxmlStarted() {
        notifyAll();
    }

    @Override
    public synchronized void jvxmlTerminated() {
        notifyAll();
    }

    @Override
    public void jvxmlStartupError(final Throwable exception) {
        LOGGER.error("error starting JVoiceML", exception);
        jvxmlStarted(); // cancel
    }

    private URI fileUri(String filename) throws URISyntaxException {
        final URL vxmlFile = getClass().getClassLoader().getResource(filename);
        assertNotNull("File not found: " + filename, vxmlFile);
        return vxmlFile.toURI();
    }

    private String audioSrc(SsmlDocument document) throws XPathExpressionException {
        NamespaceContext ns = new SimpleNamespaceContext("ssml", Speak.DEFAULT_XMLNS);

        final XPathFactory xpathFactory = XPathFactory.newInstance();
        final XPath xpath = xpathFactory.newXPath();
        xpath.setNamespaceContext(ns);

        return (String)xpath.evaluate("/ssml:speak/ssml:audio/@src", document.getDocument(), XPathConstants.STRING);
    }
}
