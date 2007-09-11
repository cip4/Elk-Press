/*
 * Created on Aug 31, 2004
 */
package org.cip4.elk.impl.queue.util;

import java.io.InputStream;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.cip4.elk.queue.util.QueueFilter;
import org.cip4.jdflib.core.JDFParser;
import org.cip4.jdflib.jmf.JDFQueue;

/**
 * @author Claes Buckwalter (clabu@itn.liu.se)
 */
public class DummyQueueFilterTest extends TestCase {

    private static Logger log = Logger.getLogger(DummyQueueFilterTest.class);
    
    private static String file = "data/DummyQueueFilterTest.jmf";

    public void testFilter() throws Exception
    {
        QueueFilter filter = new DummyQueueFilter();
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream(file);
        JDFQueue qIn =  new JDFParser().parseStream(stream).getJMFRoot().getResponse().getQueue(0);
        JDFQueue qOut = filter.filterQueue(qIn, null);
        // Tests that a copy was created
        assertNotSame(qIn, qOut);
        assertEquals(qIn.toString(), qOut.toString());
        log.info(qOut);
    }
    
}
