/*
 * Created on Aug 28, 2004
 */
package org.cip4.elk.impl.jmf;

import java.io.InputStream;

import org.cip4.elk.ElkTestCase;
import org.cip4.elk.jmf.JMFProcessor;
import org.cip4.jdflib.core.ElementName;
import org.cip4.jdflib.core.JDFDoc;
import org.cip4.jdflib.core.JDFParser;
import org.cip4.jdflib.jmf.JDFJMF;
import org.cip4.jdflib.jmf.JDFMessage;
import org.cip4.jdflib.jmf.JDFResponse;

/**
 * @author clabu
 */
public class NotImplementedJMFProcessorTest extends ElkTestCase
{            
    private static final String jmfInFile = "JMFProcessor.jmf";
 
    public void setUp() throws Exception {
        super.setUp();
    }
    
    /**
     * Tests that the processor returns the correct return code: 5
     * @throws Exception
     */
    public void testProcessJMF() throws Exception
    {
        // Load command/query
        InputStream stream = getResourceAsStream(_testDataPath + jmfInFile);
        log.debug("The " + jmfInFile + "is supposed to be any message that is not implemented by Elk");
        JDFJMF jmfIn = new JDFParser().parseStream(stream).getJMFRoot();
        JDFMessage msgIn = jmfIn.getMessage(0);
        log.info("In: " + jmfIn);
        // Create response
        JDFJMF jmfOut = new JDFDoc(ElementName.JMF).getJMFRoot();
        jmfOut.setSenderID(this.getName());
        JDFResponse msgOut = jmfOut.appendResponse();
        
        msgOut.setID("R" + msgIn.getID());
        msgOut.setrefID(msgIn.getID());
        msgOut.setType(msgIn.getType());    
        
        JMFProcessor proc = new NotImplementedJMFProcessor();
        int returnCode = proc.processJMF(msgIn, msgOut);
        log.info("Return code: " + returnCode);
        log.info("Out: " + jmfOut);
        assertTrue(returnCode == 5);
        log.debug("Finished testing NotImplementedJMFProcessor");
    }

}
