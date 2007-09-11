/*
 * Created on 2005-jun-28
 */
package org.cip4.elk.impl.queue.util;

import java.io.InputStream;

import org.cip4.elk.ElkTestCase;
import org.cip4.jdflib.core.JDFElement;
import org.cip4.jdflib.core.JDFParser;
import org.cip4.jdflib.jmf.JDFQueue;
import org.cip4.jdflib.jmf.JDFQueueEntry;
import org.cip4.jdflib.util.JDFDate;

/**
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: QueueEntryComparatorTest.java,v 1.3 2006/08/30 15:55:23 buckwalter Exp $
 */
public class QueueEntryComparatorTest extends ElkTestCase {

    public void testCompare() {
        log.info("Testing: QueueEntryComaparator...");
        QueueEntryComparator comp = new QueueEntryComparator();        
        // QueueFilter filter = new SortingQueueFilter();
        InputStream stream = getResourceAsStream(_testDataPath + "Queue_unsorted.xml");
        // JDFQueue qIn =  new JDFParser().parseStream(stream).getJMFRoot().getResponse().getQueue(0);        
        stream = getResourceAsStream(_testDataPath + "Queue_sorted.xml");
        JDFQueue sortedQu =  new JDFParser().parseStream(stream).getJMFRoot().getResponse().getQueue(0);
        log.debug("Calling qe.getEndTime() returns null.");
        JDFQueueEntry qe = sortedQu.getQueueEntry(0);
        //assertNull(qe.getEndTime());
        qe.setStatus(JDFElement.EnumNodeStatus.Completed);
        log.debug("Calling qe.getEndTime() returns null.");
        
        qe.setEndTime(new JDFDate());
        JDFQueueEntry qe2 = sortedQu.getQueueEntry(1);
        log.debug("Test that if EndTime is given for qe1 but not for qe2.");
        assertTrue(comp.compareEndTimes(qe,qe2) > 0);
        assertTrue(comp.compareEndTimes(qe2,qe) < 0);
        qe2.setEndTime(qe2.getSubmissionTime());
        qe2.setStatus(JDFElement.EnumNodeStatus.Completed);
        int result = comp.compare(qe,qe2);
        if(result > 0){
            log.debug(" QueueEntry 1 is before QueueEntry 2");
        }else if (result < 0){
            log.debug(" QueueEntry 2 is before QueueEntry 1");
        }else{
            log.debug(" QueueEntry 2 is equal to QueueEntry 1");
        }        
        assertTrue(comp.compareEndTimes(qe,qe2) > 0);
        assertTrue(comp.compareEndTimes(qe2,qe) < 0);
        qe.setEndTime(new JDFDate());
    }

}
