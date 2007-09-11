/*
 * Created on Sep 19, 2004
 */
package org.cip4.elk.impl.queue;

import java.io.InputStream;

import org.cip4.elk.Config;
import org.cip4.elk.DefaultConfig;
import org.cip4.elk.ElkTestCase;
import org.cip4.elk.impl.util.URLAccessTool;
import org.cip4.elk.queue.Queue;
import org.cip4.jdflib.core.JDFParser;
import org.cip4.jdflib.core.KElement.EnumValidationLevel;
import org.cip4.jdflib.jmf.JDFQueue;
import org.cip4.jdflib.jmf.JDFQueueEntry;
import org.cip4.jdflib.jmf.JDFQueueSubmissionParams;

/**
 * NOTE: To run this test, a folder named data containing the file:
 * SubmitQueueEntry.jmf must exists.
 * 
 * @author Claes Buckwalter (clabu@itn.liu.se)
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: MemoryQueueTest.java,v 1.11 2006/09/12 08:34:52 buckwalter Exp $
 */
public class MemoryQueueTest extends ElkTestCase {

    String fileAncectorPool0parts = _jdfFilesPath + "AncestorPoolTest0.jdf";
    String fileAncectorPool1parts = _jdfFilesPath + "AncestorPoolTest.jdf";
    String fileAncectorPool2parts = _jdfFilesPath + "AncestorPoolTest2.jdf";

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testInitialValues() {        
        int maxQueueSize = 10;
        Queue q = createQueue(maxQueueSize);        
        assertTrue(q.getQueueSize() == maxQueueSize);
        assertEquals(JDFQueue.EnumQueueStatus.Waiting, q.getQueueStatus());
        assertTrue(q.getQueueEntryCount() == 0);
        assertNull(q.getFirstRunnableQueueEntry());
        assertNull(q.getQueueEntry("100"));
    }

    public void testAddQueueEntry() {
        // Loads a JMF that references a JDF file /tmp/Approval.jdf
        JDFQueueSubmissionParams qsp = loadQueueSubmissionParams();
        assertTrue(qsp.isValid(EnumValidationLevel.RecursiveComplete));
        Queue q = createQueue(10);
        assertEquals(10, q.getQueueSize());
        assertEquals(0, q.getQueueEntryCount());
        // Adds a queue entry
        JDFQueueEntry qe1 = q.addQueueEntry(qsp);
        assertNotNull(qe1);
        assertTrue(qe1.isValid(EnumValidationLevel.RecursiveComplete));        
        assertEquals(1, q.getQueueEntryCount());
        // Tests that the queue entry is copied before it is returned
        JDFQueueEntry qe2 = q.getQueueEntry(qe1.getQueueEntryID());
        assertNotSame(qe1, qe2);
        assertNotSame(qe1.getOwnerDocument(), qe2.getOwnerDocument());
        assertEquals(qe1.toString(), qe2.toString());
        log.info(qe1);
        // Tests that the queue is copied before it is returned
        JDFQueue q1 = q.getQueue();
        JDFQueue q2 = q.getQueue();
        assertNotSame(q1, q2);
        assertNotSame(q1.getOwnerDocument(), q2.getOwnerDocument());
        assertEquals(q1.toString(), q2.toString());
        log.info(q1);
    }

    public void testPutQueueEntry() {
        JDFQueueSubmissionParams qsp = loadQueueSubmissionParams();
        Queue q = createQueue(10);
        // Adds a queue entry
        JDFQueueEntry qe = q.addQueueEntry(qsp);
        assertTrue(qe.isValid(EnumValidationLevel.RecursiveComplete));
        qe.setQueueEntryID("QueueEntryID");
        qe.setDescriptiveName("This queue entry was put");
        // Puts a new queue entry
        q.putQueueEntry(qe);
        assertTrue(q.getQueueEntryCount() == 2);
        // Replaces an old queue entry
        q.putQueueEntry(qe);
        assertTrue(q.getQueueEntryCount() == 2);
        log.info(q.getQueue());
    }

    public void testRemoveQueueEntry() {
        JDFQueueSubmissionParams qsp = loadQueueSubmissionParams();
        Queue q = createQueue(3);
        // Adds 3 queue entries
        JDFQueueEntry qe = q.addQueueEntry(qsp);
        q.addQueueEntry(qsp);
        q.addQueueEntry(qsp);
        assertEquals(3, q.getQueueSize());
        assertEquals(3, q.getQueueEntryCount());
        assertEquals(JDFQueue.EnumQueueStatus.Full, q.getQueueStatus());
        // Remove 1 queue entry
        q.removeQueueEntry(qe.getQueueEntryID());
        assertEquals(3, q.getQueueSize());
        assertEquals(2, q.getQueueEntryCount());
        assertEquals(JDFQueue.EnumQueueStatus.Waiting, q.getQueueStatus());
    }

    public void testAncestorPool() throws Exception {
    }


    public void testAddMaxEntries() {
        JDFQueueSubmissionParams qsp = loadQueueSubmissionParams();
        Queue q = createQueue(3);
        // Adds 3 queue entries
        assertNotNull(q.addQueueEntry(qsp));
        assertNotNull(q.addQueueEntry(qsp));
        assertNotNull(q.addQueueEntry(qsp));
        // Tests that the queue's size is exhausted
        assertEquals(q.getQueueSize(), q.getQueueEntryCount());        
        // XXXq.setQueueStatus(JDFQueue.EnumQueueStatus.Full);
        assertEquals(JDFQueue.EnumQueueStatus.Full, q.getQueueStatus());
        // Tests that another queue entry cannot be added
        assertNull(q.addQueueEntry(qsp));
        log.info("Max size: " + q.getQueueSize());
        log.info("Current size: " + q.getQueueEntryCount());
        assertEquals(q.getQueueSize(), q.getQueueEntryCount());
        assertEquals(JDFQueue.EnumQueueStatus.Full, q.getQueueStatus());
    }

    private JDFQueueSubmissionParams loadQueueSubmissionParams() {
        InputStream in = getResourceAsStream(_testDataPath + "SubmitQueueEntry.jmf");
        return new JDFParser().parseStream(in).getJMFRoot().getCommand()
                .getQueueSubmissionParams(0);
    }

    private Queue createQueue(int size) {
        return createQueue(null, size, null);
    }

    private Queue createQueue(Config config, int maxSize, URLAccessTool fileUtil) {
        if (config == null) {
            config = new DefaultConfig();
        }
        if (fileUtil == null) {
            fileUtil = new URLAccessTool(getResourceAsURL(".").toString());
        }
        return new MemoryQueue(config, maxSize, fileUtil);
    }
}
