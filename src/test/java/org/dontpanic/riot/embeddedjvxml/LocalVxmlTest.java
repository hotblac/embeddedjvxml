package org.dontpanic.riot.embeddedjvxml;

import org.apache.log4j.Logger;
import org.junit.*;
import org.jvoicexml.ConnectionInformation;
import org.jvoicexml.JVoiceXmlMain;
import org.jvoicexml.JVoiceXmlMainListener;
import org.jvoicexml.Session;
import org.jvoicexml.client.text.TextServer;
import org.jvoicexml.event.ErrorEvent;
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
import java.util.concurrent.CountDownLatch;

import static junit.framework.TestCase.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotNull;

public class LocalVxmlTest {

    private static final Logger LOGGER = Logger.getLogger(LocalVxmlTest.class);

    private static final long TEST_TIMEOUT_MS = 4000;
    private static final int TEXT_SERVER_PORT = 4242;

    private Session session;
    private Call call;
    private static TextServer textServer;
    private static JVoiceXmlMain jvxml;

    private static final CountDownLatch startupLatch = new CountDownLatch(1);
    private static final CountDownLatch shutdownLatch = new CountDownLatch(1);

    @BeforeClass
    public static void startJvxml() throws Exception {
        final EmbeddedTextConfiguration config = new EmbeddedTextConfiguration();
        jvxml = new JVoiceXmlMain(config);
        jvxml.addListener(new JVoiceXmlMainListener() {
            @Override
            public void jvxmlStarted() {
                startupLatch.countDown();
            }

            @Override
            public void jvxmlTerminated() {
                shutdownLatch.countDown();
            }

            @Override
            public void jvxmlStartupError(final Throwable exception) {
                LOGGER.error("error starting JVoiceML", exception);
                startupLatch.countDown(); // cancel
            }
        });
        jvxml.start();
        startupLatch.await();
    }

    @Before
    public void startCall() throws Exception, ErrorEvent {

        // Note: A TextServer is attached to a single session on construction
        // and so must be started for every new session.
        textServer = new TextServer(TEXT_SERVER_PORT);
        textServer.start();
        textServer.waitStarted();

        // A new session must be created for each call
        final ConnectionInformation info = textServer.getConnectionInformation();
        session = jvxml.createSession(info);

        call = new EmbeddedServerTextCall(session, textServer);
    }

    @After
    public void endSession() throws ErrorEvent {
        textServer.stopServer();
    }

    @AfterClass
    public static void stopJvxml() {
        jvxml.getDocumentServer().stop();
        jvxml.shutdown();
    }

    @Test(timeout = TEST_TIMEOUT_MS)
    public void testLocalVxml() throws Exception {
        call.call(fileUri("hello.vxml"));
        call.hears("Hello World!");
        call.hears("Goodbye!");
        //call.hangup();
    }

    @Test(timeout = TEST_TIMEOUT_MS)
    public void testAudioResponse() throws Exception {
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
        call.call(fileUri("dtmf.vxml"));
        call.hears("Do you like this example? Please enter 1 for yes or 2 for no");
        call.enter("1");
        call.hears("You like this example.");
    }

    @Test(timeout = TEST_TIMEOUT_MS)
    public void testDtmfInvalidInput() throws Exception {
        call.call(fileUri("dtmf.vxml"));
        call.hears("Do you like this example? Please enter 1 for yes or 2 for no");
        call.enter("9");
        // Expect reprompt
        call.hears("Do you like this example? Please enter 1 for yes or 2 for no");
    }

    @Test(timeout = TEST_TIMEOUT_MS)
    public void testSpokenInput() throws Exception {
        call.call(fileUri("input.vxml"));
        call.hears("Do you like this example?");
        call.say("yes");
        call.hears("You like this example.");
    }

    @Test(timeout = TEST_TIMEOUT_MS)
    public void testSpokenInvalidInput() throws Exception {
        call.call(fileUri("input.vxml"));
        call.hears("Do you like this example?");
        call.say("um...");
        // Expect reprompt
        call.hears("Do you like this example?");
    }

    @Test(timeout = TEST_TIMEOUT_MS)
    public void testGoto() throws Exception {
        call.call(fileUri("goto1.vxml"));
        call.hears("Prompt from goto1.vxml");
        call.hears("Prompt from goto2.vxml");
    }

    @Test(timeout = TEST_TIMEOUT_MS)
    public void testSubmit() throws Exception {
        call.call(fileUri("submit1.vxml"));
        call.hears("Prompt from submit1.vxml");
        call.hears("Prompt from submit2.vxml");
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
