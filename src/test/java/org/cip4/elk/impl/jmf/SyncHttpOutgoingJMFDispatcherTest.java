/*
 * Created on Sep 21, 2004
 */
package org.cip4.elk.impl.jmf;

import java.io.InputStream;

import org.cip4.elk.Config;
import org.cip4.elk.DefaultConfig;
import org.cip4.elk.ElkTestCase;
import org.cip4.elk.jmf.OutgoingJMFDispatcher;
import org.cip4.jdflib.core.JDFParser;
import org.cip4.jdflib.jmf.JDFJMF;

/**
 * Tests if JMF can be sent to
 * <code>org.cip4.elk.impl.jmf.LoggingJMFServlet</code>. Requires that
 * <code>LoggingJMFServlet</code> is deployed and listening at
 * <em>http://localhost:8080/elk/logJmf</code>.
 * <p>
 * <strong>TODO</strong> Embed a HTTP server that the JMF message can be sent to
 * and verify the result. 
 * </p>
 * @see org.cip4.elk.impl.testtools.servlet.LoggingJMFServlet
 * @author Claes Buckwalter (clabu@itn.liu.se)
 */
public class SyncHttpOutgoingJMFDispatcherTest extends ElkTestCase {

    private static final String JMF_FILE = "data/Status.jmf";
    private static final String JMF_URL = "http://localhost:8080/elk/logJmf";

    /*
     * @see ElkTestCase#setUp()
     */
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * See Javadoc comments for this class.
     * 
     * @see SyncHttpOutgoingJMFDispatcherTest
     * @throws Exception
     */
    public void testDispatchJMF() {
        // Configures dispatcher
        Config config = new DefaultConfig();
        OutgoingJMFDispatcher disp = new SyncHttpOutgoingJMFDispatcher(config);
        // Loads JMF message
        InputStream stream = getResourceAsStream(JMF_FILE);
        JDFJMF jmf = new JDFParser().parseStream(stream).getJMFRoot();
        log.debug(jmf);
        // Sends JMF message
        try {
            disp.dispatchJMF(jmf, JMF_URL);
        } catch(Exception e){
            log.error("TJJOOOOOOOOOOOOOOOOOOOOOO");
        }
        log.info("At the moment the http://localhost:8080/elk/logJmf does "
                + "not respond with a JMF message. Therefore some exceptions"
                + " will be thrown for the call above.");
    }

}
