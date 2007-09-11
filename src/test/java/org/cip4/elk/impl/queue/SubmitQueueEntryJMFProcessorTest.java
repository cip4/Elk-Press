/*
 * Created on Sep 19, 2004
 */
package org.cip4.elk.impl.queue;

import org.cip4.elk.Config;
import org.cip4.elk.DefaultConfig;
import org.cip4.elk.ElkTestCase;
import org.cip4.elk.impl.jmf.SubscribingIncomingJMFDispatcher;
import org.cip4.elk.impl.queue.jmf.SubmitQueueEntryJMFProcessor;
import org.cip4.elk.impl.util.URLAccessTool;
import org.cip4.elk.jmf.IncomingJMFDispatcher;
import org.cip4.elk.jmf.JMFProcessor;
import org.cip4.elk.queue.Queue;
import org.cip4.jdflib.auto.JDFAutoQueue.EnumQueueStatus;
import org.cip4.jdflib.core.KElement.EnumValidationLevel;
import org.cip4.jdflib.jmf.JDFJMF;


/**
 * NOTE: To run this test, the file "data/SubmitQueueEntry.jmf" must exist.

 * @author Claes Buckwalter (clabu@itn.liu.se)
 */
public class SubmitQueueEntryJMFProcessorTest extends ElkTestCase {

    private static final String JMF_FILE = "data/SubmitQueueEntry.jmf";
    
    /*
     * @see TestCase#setUp()
     */
    public void setUp() throws Exception {
        super.setUp();        
    }
    
    /**
     * Tests that the queue generates the correct response error
     * message when the queue is full (return code 112). 
     * 
     * Creates a queue of length 0 and submits a queue entry.
     */
    public void testSubmitQueueEntry_QueueFull() throws Exception
    {
        // Loads command
        JDFJMF jmfIn = (JDFJMF) getResourceAsJDF(JMF_FILE);
        assertTrue(jmfIn.isValid(EnumValidationLevel.RecursiveComplete));
        // Configures
        int maxSize = 0;
        Config config = new DefaultConfig();
        URLAccessTool fileUtil = new URLAccessTool(getResourceAsURL(".").toString());
        // Configure queue and test its initial state
        Queue q = new MemoryQueue(config, maxSize, fileUtil);
        assertEquals(q.getQueueStatus(),  EnumQueueStatus.Full);
        assertTrue(q.getMaxQueueSize() == 0);
        assertTrue(q.getQueueSize() == 0);
        // Configure JMF dispatcher and processor 
        JMFProcessor proc = 
            new SubmitQueueEntryJMFProcessor(config, q, fileUtil);
        IncomingJMFDispatcher disp = new SubscribingIncomingJMFDispatcher();
        disp.setConfig(config);
        disp.registerDefaultProcessor(proc);
        // Dispatches command 
        log.debug("In: " + jmfIn);
        JDFJMF jmfOut = disp.dispatchJMF(jmfIn);        
        log.debug("Out: " + jmfOut);
        assertTrue(jmfOut.isValid(EnumValidationLevel.RecursiveComplete));
        // Tests that the return code is 112, the queue is full
        assertTrue(jmfOut.getResponse(0).getReturnCode() == 112);
    }
    
    /**
     * Tests that the queue accepts SubmitQueueEntry commands and
     * replies with the correct return code (0).
     *
     * Creates a queue of length 5 and submits 5 jobs.
     */
    public void testSubmitQueueEntry() throws Exception
    {
        // Loads command
        JDFJMF jmfIn = (JDFJMF) getResourceAsJDF(JMF_FILE);
        assertTrue(jmfIn.isValid(EnumValidationLevel.RecursiveComplete));
        // Configures
        int maxSize = 5;
        Config config = new DefaultConfig();
        URLAccessTool fileUtil = new URLAccessTool(getResourceAsURL(".").toString());
        Queue q = new MemoryQueue(config, maxSize, fileUtil);
        JMFProcessor proc = 
            new SubmitQueueEntryJMFProcessor(config, q, fileUtil);
        IncomingJMFDispatcher disp = new SubscribingIncomingJMFDispatcher();
        disp.setConfig(config);
        disp.registerDefaultProcessor(proc);
        // Dispatches command
        for(int i=0; i<5; i++)
        {
            jmfIn.getCommand().setID("M" + i);
            log.debug(i + ". In: " + jmfIn);
            JDFJMF jmfOut = disp.dispatchJMF(jmfIn);            
            log.debug(i + ". Out: " + jmfOut);
            assertTrue(jmfOut.isValid(EnumValidationLevel.RecursiveComplete));
            // Tests that the return code is 0
            assertTrue(jmfOut.getResponse(0).getReturnCode() == 0);
        }
        assertTrue(q.getMaxQueueSize() == 5);
        assertTrue(q.getQueueSize() == 5);
    }

}
