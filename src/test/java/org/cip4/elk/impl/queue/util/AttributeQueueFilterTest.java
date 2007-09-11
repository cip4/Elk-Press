/*
 * Created on Sep 19, 2004
 */
package org.cip4.elk.impl.queue.util;

import java.io.InputStream;

import org.cip4.elk.ElkTestCase;
import org.cip4.elk.queue.util.QueueFilter;
import org.cip4.jdflib.core.JDFParser;
import org.cip4.jdflib.datatypes.JDFAttributeMap;
import org.cip4.jdflib.jmf.JDFQueue;
import org.cip4.jdflib.jmf.JDFQueueEntry;


/**
 * @author Claes Buckwalter (clabu@itn.liu.se)
 */
public class AttributeQueueFilterTest extends ElkTestCase {

    /*
     * @see TestCase#setUp()
     */
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testFilterQueue() {
        JDFAttributeMap attrMap = new JDFAttributeMap();
        attrMap.put("Status", JDFQueueEntry.EnumQueueEntryStatus.Waiting.getName());
        QueueFilter filter = new AttributeQueueFilter(attrMap);
        InputStream stream = getResourceAsStream("data/Queue.xml");
        JDFQueue qIn =  new JDFParser().parseStream(stream).getJMFRoot().getResponse().getQueue(0);
        log.info("Input queue: " + qIn);
        long t0 = System.currentTimeMillis();
        JDFQueue qOut = filter.filterQueue(qIn, null);
        long t1 = System.currentTimeMillis();
        // Tests that a copy was created
        assertNotSame(qIn, qOut);
        log.info("Output queue: " + qOut);        
        // TODO Test that the order is correct
        log.info("Filter time: " + (t1-t0) + " ms");
    }

}
