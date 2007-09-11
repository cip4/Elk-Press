package org.cip4.elk.impl.jmf;

import java.util.List;
import java.util.Set;

import org.cip4.elk.Config;
import org.cip4.elk.DefaultConfig;
import org.cip4.elk.ElkTestCase;
import org.cip4.elk.jmf.IncomingJMFDispatcher;
import org.cip4.elk.jmf.JMFProcessor;
import org.cip4.jdflib.jmf.JDFJMF;
import org.cip4.jdflib.jmf.JDFMessage;

/**
 * @author clabu    
 */
public class SubscribingIncomingJMFDispatcherTest extends ElkTestCase {
          
    //private static final String jmfInFile = "data/IncomingJMFDispatcher.jmf";
    
    public void setUp() throws Exception {
        super.setUp();
    }
    
    /**
     * Tests that for each incoming message (in) there is a response (out) that fulfills the following:
     * <ul>
     * <li>in/@ID == out/@refID</li>
     * <li>in/@Type == out/@Type</li>
     * </ul>
     */
    public void testDispatchJMF() throws Exception
    {
        JDFJMF jmfIn = (JDFJMF) getResourceAsJDF(_testDataPath + "IncomingJMFDispatcher.jmf");
        log.info("In: " + jmfIn);
      
        // Sets up the dispatcher with a default processor
        Config config = new DefaultConfig();
        config.setID("Elk");
        IncomingJMFDispatcher disp = new SubscribingIncomingJMFDispatcher();
        disp.registerDefaultProcessor(new NotImplementedJMFProcessor());
        disp.setConfig(config);
        
        JDFJMF jmfOut = disp.dispatchJMF(jmfIn);
        
        // Tests number of response messages
        log.info("Message count: " + jmfIn.getMessageVector().size());
        assertTrue(jmfIn.getMessageVector().size() == jmfOut.getMessageVector().size());
        // Tests response messages        
        JDFMessage msgIn;
        JDFMessage msgOut;
        List msgsIn = jmfIn.getMessageVector();
        for(int i=0, imax=msgsIn.size(); i<imax; i++)
        {
            msgIn = (JDFMessage) msgsIn.get(i);           
            String idIn = msgIn.getID();
            String typeIn = msgIn.getType();
            log.info("Expecting response for: ID='" + idIn + "' Type='" + typeIn + "'");
            boolean typeAndRefIdMatch = false;
            List msgsOut = jmfOut.getMessageVector();
            for(int j=0, jmax=msgsOut.size(); j<jmax; j++)
            {
                msgOut = (JDFMessage) msgsOut.get(j);
                // Checks ref ID
                if(idIn.equals(msgOut.getrefID()))
                {
                    // Checks type
                   typeAndRefIdMatch = typeIn.equals(msgOut.getType());
                   break;
                }
            }
            // Test if ID and type match
            assertTrue(typeAndRefIdMatch);
        }
    }

    public void testDispatchSignal() throws Exception {
        JDFJMF jmfIn = (JDFJMF) getResourceAsJDF("data/Signal.jmf");
        log.info("Signal: " + jmfIn);
      
        // Sets up the dispatcher with a default processor
        Config config = new DefaultConfig();
        config.setID("Elk");
        IncomingJMFDispatcher disp = new SubscribingIncomingJMFDispatcher();
        disp.registerDefaultProcessor(new NotImplementedJMFProcessor());
        disp.setConfig(config);
        
        JDFJMF jmfOut = disp.dispatchJMF(jmfIn);
        assertNull(jmfOut);
        
        // Tests number of response messages
        log.info("Message count: " + jmfIn.getMessageVector().size());
        assertNull(jmfOut);
    }
    
    
    
    public void testRegisterProcessor()
    {
        IncomingJMFDispatcher disp = new SubscribingIncomingJMFDispatcher();
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

    public void testUnregisterProcessor()
    {
        IncomingJMFDispatcher disp = new SubscribingIncomingJMFDispatcher();
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

    public void testGetMessageTypes()
    {
        IncomingJMFDispatcher disp = new SubscribingIncomingJMFDispatcher();
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
        IncomingJMFDispatcher disp = new SubscribingIncomingJMFDispatcher();        
        JMFProcessor proc = new NotImplementedJMFProcessor();
        String type = "KnownDevices";
        // Registers a new processor
        disp.registerProcessor(type, proc);
        // Tests that the processor was registered
        assertEquals(disp.getProcessor(type), proc);        
    }
    
    public void testGetDefaultProcessor()
    {
        IncomingJMFDispatcher disp = new SubscribingIncomingJMFDispatcher();        
        JMFProcessor proc = new NotImplementedJMFProcessor();              
        // Registers a new processor
        disp.registerDefaultProcessor(proc);
        assertEquals(disp.getDefaultProcessor(), proc);
        // Tests that the default processor is returned for unknown message types
        assertEquals(disp.getProcessor("KnownDevices"), proc);
    }

    public void testRegisterDefaultProcessor()
    {
        IncomingJMFDispatcher disp = new SubscribingIncomingJMFDispatcher();        
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
        IncomingJMFDispatcher disp = new SubscribingIncomingJMFDispatcher();        
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

}
