/*
 * Created on Aug 29, 2004
 */
package org.cip4.elk.impl.jmf;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.cip4.elk.ElkTestCase;
import org.cip4.elk.jmf.IncomingJMFDispatcher;
import org.cip4.elk.jmf.JMFProcessor;
import org.cip4.jdflib.jmf.JDFJMF;

/**
 * @author clabu
 */
public class AbstractIncomingJMFDispatcherTest extends ElkTestCase {
    
    /**
     * Registers 2 processors
     * Tests that the processors can be correctly retrieved
     * Tests that the processors are replaced when new processors with for the same types are registered 
     */
    public void testRegisterProcessor() {
        log.debug("Testing AbstractIncomingJMFDispatcher");
        IncomingJMFDispatcher disp = new DummyDispatcher();
        // Creates 2 processors
        String type1 = "KnownDevices";
        JMFProcessor proc1 = new NotImplementedJMFProcessor();
        JMFProcessor oldProc1;
        String type2 = "Status";
        JMFProcessor proc2 = new NotImplementedJMFProcessor();
        JMFProcessor oldProc2;
        // Registers processor 1
        oldProc1 = disp.registerProcessor(type1, proc1);
        assertTrue(oldProc1 == null);
        assertEquals(disp.getProcessor(type1), proc1);
        // Registers processor 2
        oldProc2 = disp.registerProcessor(type2, proc2);
        assertTrue(oldProc2 == null);
        assertEquals(disp.getProcessor(type2), proc2);
        // Overwrites processor 1
        oldProc1 = disp.registerProcessor(type1, proc1);
        assertEquals(oldProc1, proc1);
        // Tests that the latest registered processor 1 is still registered
        assertEquals(disp.getProcessor(type1), proc1);
    }

    /**
     * Registers 1 processor
     * Unregisters the processor
     * Tests that the processor has been unregistered
     */
    public void testUnregisterProcessor()
    {
        IncomingJMFDispatcher disp = new DummyDispatcher();
        JMFProcessor proc = new NotImplementedJMFProcessor();
        JMFProcessor oldProc;
        String type = "KnownDevices";
        // Registers processor
        disp.registerProcessor(type, proc);
        // Unregisters processor
        oldProc = disp.unregisterProcessor(type);
        assertEquals(oldProc, proc);
        // Tests that no processor is registered
        assertTrue(disp.getProcessor(type) == null);                
    }

    /**
     * Registers 2 processors
     * Tests that all types are in the list of message types
     */
    public void testGetMessageTypes()
    {
        IncomingJMFDispatcher disp = new DummyDispatcher();
        // Creates 2 processors
        String type1 = "KnownDevices";
        JMFProcessor proc1 = new NotImplementedJMFProcessor();
        //JMFProcessor oldProc1;
        String type2 = "Status";
        JMFProcessor proc2 = new NotImplementedJMFProcessor();
        //JMFProcessor oldProc2;
        // Registers processor 1
        disp.registerProcessor(type1, proc1);
        // Registers processor 2
        disp.registerProcessor(type2, proc2);
        Set types = disp.getMessageTypes();
        assertTrue(types.contains(type1));
        assertTrue(types.contains(type2));
        assertTrue(types.size() == 2);
    }

    public void testGetProcessor()
    {
        IncomingJMFDispatcher disp = new DummyDispatcher();        
        JMFProcessor proc = new NotImplementedJMFProcessor();
        String type = "KnownDevices";
        // Registers a new processor
        disp.registerProcessor(type, proc);
        // Tests that the processor was registered
        assertEquals(disp.getProcessor(type), proc);        
    }
    
    public void testGetDefaultProcessor()
    {
        IncomingJMFDispatcher disp = new DummyDispatcher();        
        JMFProcessor proc = new NotImplementedJMFProcessor();              
        // Registers a new processor
        disp.registerDefaultProcessor(proc);
        assertEquals(disp.getDefaultProcessor(), proc);
        // Tests that the default processor is returned for unknown message types
        assertEquals(disp.getProcessor("KnownDevices"), proc);
    }

    public void testRegisterDefaultProcessor()
    {
        IncomingJMFDispatcher disp = new DummyDispatcher();        
        JMFProcessor proc = new NotImplementedJMFProcessor();
        JMFProcessor oldProc;
        // Registers a new processor
        oldProc = disp.registerDefaultProcessor(proc);
        assertTrue(oldProc == null);
        assertEquals(disp.getDefaultProcessor(), proc);
        // Overwrites an already registered processor
        oldProc = disp.registerDefaultProcessor(proc);
        assertEquals(oldProc, proc);
        // Tests that the latest registered processor is still registered
        assertEquals(disp.getDefaultProcessor(), proc);
    }

    public void testUnregisterDefaultProcessor()
    {
        IncomingJMFDispatcher disp = new DummyDispatcher();        
        JMFProcessor proc = new NotImplementedJMFProcessor();
        JMFProcessor oldProc;
        // Registers processor
        disp.registerDefaultProcessor(proc);
        // Unregisters processor
        oldProc = disp.unregisterDefaultProcessor();
        assertEquals(oldProc, proc);
        // Tests that no processor is registered
        assertTrue(disp.getDefaultProcessor() == null);
    }
    
    /**
     * Tests that a Map of processors are registered correctly. Tests that
     * the map overwrites 
     *
     */
    public void testSetProcessors() {
        IncomingJMFDispatcher disp = new DummyDispatcher();
        // Create a map of processors
        Map map = new HashMap();
        String msg1 = "default";
        JMFProcessor proc1 = new NotImplementedJMFProcessor();        
        map.put(msg1, proc1);
        String msg2 = "SubmitQueueEntry";
        JMFProcessor proc2 = new NotImplementedJMFProcessor();
        map.put(msg2, proc2);
        String msg3 = "KnownDevices";
        JMFProcessor proc3 = new NotImplementedJMFProcessor();
        map.put(msg3, proc3);
        // Register a single processor
        JMFProcessor proc4 = new NotImplementedJMFProcessor();
        disp.registerProcessor(msg3, proc4);
        // Registers a default process
        JMFProcessor proc5 = new NotImplementedJMFProcessor();
        disp.registerDefaultProcessor(proc5);        
        // Register processors in map
        disp.setProcessors(map);
        // Test that the map processors are registers and that the single 
        // processor has been overwritten
        assertEquals(proc1, disp.getDefaultProcessor());
        assertEquals(proc2, disp.getProcessor(msg2));
        assertEquals(proc3, disp.getProcessor(msg3));
        // Register single processor
        disp.registerProcessor(msg3, proc4);
        // Test that single processor overwrote map
        assertEquals(proc4, disp.getProcessor(msg3));
        log.debug("Finished testing AbstractIncomingJMFDispatcher");
    }
    
    

    /**
     * A dummy class that implementes all of IncomingJMFDispatcher so that
     * we can test AbstractIncomingJMFDispatcher.
     * @author clabu
     */
    private static class DummyDispatcher extends AbstractIncomingJMFDispatcher
    {
        public void init()
        {          
        }
        
        public JDFJMF dispatchJMF(JDFJMF jmfIn)
        {
            return null;
        }
    }
}
