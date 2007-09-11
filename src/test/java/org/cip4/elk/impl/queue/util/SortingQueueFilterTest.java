/*
 * Created on Sep 15, 2004
 */
package org.cip4.elk.impl.queue.util;

import java.io.InputStream;

import org.cip4.elk.ElkTestCase;
import org.cip4.elk.queue.util.QueueFilter;
import org.cip4.jdflib.core.JDFParser;
import org.cip4.jdflib.jmf.JDFQueue;

/**
 * @author Claes Buckwalter (clabu@itn.liu.se)
 */
public class SortingQueueFilterTest extends ElkTestCase {

    /*
     * @see ElkTestCase#setUp()
     */
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testFilterQueue() throws Exception {
        QueueFilter filter = new SortingQueueFilter();
        InputStream stream = getResourceAsStream("data/Queue_unsorted.xml");
        JDFQueue qIn =  new JDFParser().parseStream(stream).getJMFRoot().getResponse().getQueue(0);
        log.debug("Input queue: " + qIn);
        long t0 = System.currentTimeMillis();
        JDFQueue qOut = filter.filterQueue(qIn, null);
        long t1 = System.currentTimeMillis();
        // Tests that a copy was created
        assertNotSame(qIn, qOut);        
        log.debug("Output queue: " + qOut);
        // TODO Test that the order is correct
        stream = getResourceAsStream("data/Queue_sorted.xml");
        JDFQueue sortedQu =  new JDFParser().parseStream(stream).getJMFRoot().getResponse().getQueue(0);
        assertEquals(sortedQu.toString(), qOut.toString());
        log.debug("Filter time: " + (t1-t0) + " ms");
        
    }
    
    public void testFilterQueue_noCopying() throws Exception {
        SortingQueueFilter filter = new SortingQueueFilter();
        InputStream stream = getResourceAsStream("data/Queue.xml");
        JDFQueue qIn =  new JDFParser().parseStream(stream).getJMFRoot().getResponse().getQueue(0);
        log.debug("Input queue: " + qIn);
        long t0 = System.currentTimeMillis();
        JDFQueue qOut = filter.filterQueue(qIn, null, false);
        long t1 = System.currentTimeMillis();
        // Tests that a copy was created
        assertEquals(qIn, qOut);
        log.debug("Output queue: " + qOut);
        // TODO Test that the order is correct
        log.debug("Filter time: " + (t1-t0) + " ms");
    }  
}
