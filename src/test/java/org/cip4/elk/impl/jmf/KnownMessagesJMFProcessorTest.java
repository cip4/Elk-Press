/*
 * Created on Sep 12, 2004
 */
package org.cip4.elk.impl.jmf;

import java.io.InputStream;
import java.util.Arrays;

import org.cip4.elk.Config;
import org.cip4.elk.DefaultConfig;
import org.cip4.elk.ElkTestCase;
import org.cip4.elk.jmf.IncomingJMFDispatcher;
import org.cip4.jdflib.core.JDFParser;
import org.cip4.jdflib.jmf.JDFJMF;
import org.cip4.jdflib.jmf.JDFMessageService;

/**
 * @author Claes Buckwalter (clabu@itn.liu.se)
 */
public class KnownMessagesJMFProcessorTest extends ElkTestCase {

    private static final String JMF_FILE = "KnownMessages.jmf";
    
    public void setUp() throws Exception {
        super.setUp();          
    }
    
    public void testProcessJMF() {
        // Configure dispatcher
        IncomingJMFDispatcher disp = new SubscribingIncomingJMFDispatcher();
        Config config = new DefaultConfig();
        config.setID("KnownMessagesJMFProcessorTest");
        disp.setConfig(config);
        // Register processors
        disp.registerProcessor("KnownMessages1", new KnownMessagesJMFProcessor());
        disp.registerProcessor("KnownMessages2", new KnownMessagesJMFProcessor());        
        KnownMessagesJMFProcessor proc = new KnownMessagesJMFProcessor();
        proc.setIncomingJMFDispatcher(disp);
        disp.registerProcessor("KnownMessages", proc);
        // Load and dispatch KnownMessages query
        InputStream stream = getResourceAsStream(_testDataPath + JMF_FILE);        
        JDFJMF jmfIn = new JDFParser().parseStream(stream).getJMFRoot();
        assertTrue(jmfIn.isValid());
        log.info(jmfIn.toString());
        JDFJMF jmfOut = disp.dispatchJMF(jmfIn);
        assertTrue(jmfOut.isValid());
        
        log.debug(jmfOut.toString());
        assertTrue(jmfOut.getResponse().getReturnCode() == 0);
    }

    
    public void testGetMessageServices() {
        KnownMessagesJMFProcessor proc = new KnownMessagesJMFProcessor();
        JDFMessageService[] services = proc.getMessageServices();
        assertTrue(services.length == 1);
        assertEquals(services[0].getType(), "KnownMessages");
        assertTrue(services[0].getQuery());
        assertTrue(services[0].isValid());
        log.debug(Arrays.asList(services).toString());
    }
}
