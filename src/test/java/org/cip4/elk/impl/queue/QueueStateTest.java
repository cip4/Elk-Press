package org.cip4.elk.impl.queue;

import org.cip4.elk.ElkTestCase;
import org.cip4.elk.queue.QueueStatusEvent;
import org.cip4.elk.queue.QueueStatusListener;
import org.cip4.jdflib.jmf.JDFQueue;

/**
 * Tests QueueState. This test does not cover all possible states and state
 * transitions. 
 * <p>
 * <strong>TODO</strong> Add more tests to cover all possible states and state
 * transitions. To start with, calculate the number of tests needed ;) 
 * </p>
 * 
 * @author Claes Buckwalter (clabu@itn.liu.se)
 */
public class QueueStateTest extends ElkTestCase {

    /*
     * @see ElkTestCase#setUp()
     */
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Tests if the initial state is correct.
     */
    public void testQueueState() {
        QueueState qs = new QueueState(new DummyQueue());
        assertEquals(qs.getQueueStatus(), QueueState.WAITING);
        assertFalse(qs.isProcessFull());
        assertFalse(qs.getProcessFull());
        assertFalse(qs.isQueueFull());
        assertFalse(qs.getQueueFull());
    }
    

    /**
     * State: Waiting 
     * Event: Close Queue 
     * State: Closed 
     * Event: Close Queue 
     * State: Closed
     */
    public void testCloseQueueTwice() {
        QueueState qs = new QueueState(new DummyQueue());
        assertEquals(qs.getQueueStatus(), QueueState.WAITING);
        assertEquals(qs.closeQueue(), QueueState.CLOSED);
        assertEquals(qs.closeQueue(), QueueState.CLOSED);
    }
    
    /**
     * State: Waiting 
     * Event: Close Queue 
     * State: Closed
     */
    public void testCloseQueue_Waiting() {       
        QueueState qs = new QueueState(new DummyQueue());
        assertEquals(qs.getQueueStatus(), QueueState.WAITING);
        assertEquals(qs.closeQueue(), QueueState.CLOSED);
        assertEquals(qs.getQueueStatus(), QueueState.CLOSED);       
    }
    
    /**
     * State: Waiting 
     * Event: Close Queue 
     * State: Closed
     */   
    public void testCloseQueue_Held() {       
        QueueState qs = new QueueState(new DummyQueue());
        assertEquals(qs.getQueueStatus(), QueueState.WAITING);
        assertEquals(qs.holdQueue(), QueueState.HELD);
        assertEquals(qs.getQueueStatus(), QueueState.HELD);
        assertEquals(qs.closeQueue(), QueueState.BLOCKED);
        assertEquals(qs.getQueueStatus(), QueueState.BLOCKED);       
    }
    
    /**
     * State: Waiting 
     * Event: Close Queue 
     * State: Closed
     * Event: Open Queue
     * State: Waiting
     */
    public void testOpenQueue_Closed() {
        QueueState qs = new QueueState(new DummyQueue());
        assertEquals(qs.getQueueStatus(), QueueState.WAITING);
        assertEquals(qs.closeQueue(), QueueState.CLOSED);
        assertEquals(qs.getQueueStatus(), QueueState.CLOSED);
        assertEquals(qs.openQueue(), QueueState.WAITING);
        assertEquals(qs.getQueueStatus(), QueueState.WAITING);
    }

    /**
     * State: Waiting 
     * Event: Close Queue 
     * State: Closed
     * Event: Hold Queue
     * State: Blocked
     * Event: Open Queue
     * State: Held
     */
    public void testOpenQueue_Blocked() {
        QueueState qs = new QueueState(new DummyQueue());
        assertEquals(qs.getQueueStatus(), QueueState.WAITING);
        assertEquals(qs.closeQueue(), QueueState.CLOSED);
        assertEquals(qs.getQueueStatus(), QueueState.CLOSED);
        assertEquals(qs.holdQueue(), QueueState.BLOCKED);
        assertEquals(qs.getQueueStatus(), QueueState.BLOCKED);
        assertEquals(qs.openQueue(), QueueState.HELD);
        assertEquals(qs.getQueueStatus(), QueueState.HELD);
    }
    
    /**
     * State: Waiting 
     * Event: Hold Queue
     * State: Held
     */
    public void testHoldQueue_Waiting() {
        QueueState qs = new QueueState(new DummyQueue());
        assertEquals(qs.getQueueStatus(), QueueState.WAITING);
        assertEquals(qs.holdQueue(), QueueState.HELD);
        assertEquals(qs.getQueueStatus(), QueueState.HELD);
    }

    /**
     * State: Waiting 
     * Event: Close Queue 
     * State: Closed
     * Event: Hold Queue
     * State: Blocked
     */
    public void testHoldQueue_Closed() {
        QueueState qs = new QueueState(new DummyQueue());
        assertEquals(qs.getQueueStatus(), QueueState.WAITING);
        assertEquals(qs.closeQueue(), QueueState.CLOSED);
        assertEquals(qs.getQueueStatus(), QueueState.CLOSED);
        assertEquals(qs.holdQueue(), QueueState.BLOCKED);
        assertEquals(qs.getQueueStatus(), QueueState.BLOCKED);
    }
    
    /**
     * State: Waiting 
     * Event: Hold Queue
     * State: Held
     * Event: Resume Queue
     * State: Waiting
     */
    public void testResumeQueue_Held() {
        QueueState qs = new QueueState(new DummyQueue());
        assertEquals(qs.getQueueStatus(), QueueState.WAITING);
        assertEquals(qs.holdQueue(), QueueState.HELD);
        assertEquals(qs.getQueueStatus(), QueueState.HELD);
        assertEquals(qs.resumeQueue(), QueueState.WAITING);
        assertEquals(qs.getQueueStatus(), QueueState.WAITING);
    }
    
    /**
     * State: Waiting 
     * Event: Close Queue 
     * State: Closed
     * Event: Hold Queue
     * State: Blocked
     * Event: Resume Queue
     * State: Closed
     */
    public void testResumeQueue_Blocked() {
        QueueState qs = new QueueState(new DummyQueue());
        assertEquals(qs.getQueueStatus(), QueueState.WAITING);
        assertEquals(qs.closeQueue(), QueueState.CLOSED);
        assertEquals(qs.getQueueStatus(), QueueState.CLOSED);
        assertEquals(qs.holdQueue(), QueueState.BLOCKED);
        assertEquals(qs.getQueueStatus(), QueueState.BLOCKED);
        assertEquals(qs.resumeQueue(), QueueState.CLOSED);
        assertEquals(qs.getQueueStatus(), QueueState.CLOSED);
    }
    
    /**
     * State: Waiting 
     * Event: Queue Full 
     * State: Waiting (Queue full)
     */
    public void testSetQueueFull_QNotFull_PNotFull() {
        QueueState qs = new QueueState(new DummyQueue());
        assertEquals(qs.getQueueStatus(), QueueState.WAITING);
        assertFalse(qs.isQueueFull());
        assertFalse(qs.isProcessFull());
        qs.setQueueFull(true);
        assertEquals(qs.getQueueStatus(), QueueState.FULL);
        //assertEquals(qs.getQueueStatus(), QueueState.WAITING);
        assertTrue(qs.isQueueFull());
        assertFalse(qs.isProcessFull());
    }

    /**
     * State: Waiting 
     * Event: Process Full 
     * State: Running (Process Full)
     * Event: Queue Full
     * State: Full (Process Full, Queue Full) 
     */
    public void testSetQueueFull_QNotFull_PFull() {
        QueueState qs = new QueueState(new DummyQueue());
        assertEquals(qs.getQueueStatus(), QueueState.WAITING);
        assertFalse(qs.isQueueFull());
        assertFalse(qs.isProcessFull());
        qs.setProcessFull(true);
        assertTrue(qs.isProcessFull());
        assertEquals(qs.getQueueStatus(), QueueState.RUNNING);                
        qs.setQueueFull(true);
        assertTrue(qs.isQueueFull());
        assertTrue(qs.isProcessFull());
        assertEquals(qs.getQueueStatus(), QueueState.FULL);        
    }
    
    public void testGetQueueFull() {
        QueueState qs = new QueueState(new DummyQueue());
        assertTrue(qs.isQueueFull() == qs.getQueueFull());
    }

    /**
     * State: Waiting 
     * Event: Process Full 
     * State: Running (Process Full)
     */
    public void testSetProcessFull_QNotFull_PFull() {
        QueueState qs = new QueueState(new DummyQueue());
        assertEquals(qs.getQueueStatus(), QueueState.WAITING);
        assertFalse(qs.isQueueFull());
        assertFalse(qs.isProcessFull());
        qs.setProcessFull(true);
        assertFalse(qs.isQueueFull());
        assertTrue(qs.isProcessFull());
        assertEquals(qs.getQueueStatus(), QueueState.RUNNING);
    }

    /**
     * State: Waiting 
     * Event: Queue Full 
     * State: Waiting (Queue Full)
     * Event: Process Full
     * State: Full (Process Full, Queue Full) 
     */
    public void testSetProcessFull_QFull_PNotFull() {
        QueueState qs = new QueueState(new DummyQueue());
        assertEquals(qs.getQueueStatus(), QueueState.WAITING);
        assertFalse(qs.isQueueFull());
        assertFalse(qs.isProcessFull());
        qs.setQueueFull(true);
        assertTrue(qs.isQueueFull());
        assertEquals(qs.getQueueStatus(), QueueState.FULL);
        //assertEquals(qs.getQueueStatus(), QueueState.WAITING);                
        qs.setProcessFull(true);
        assertTrue(qs.isQueueFull());
        assertTrue(qs.isProcessFull());
        assertEquals(qs.getQueueStatus(), QueueState.FULL);        

    }
    
    public void testGetProcessFull() {
        QueueState qs = new QueueState(new DummyQueue());
        assertTrue(qs.isProcessFull() == qs.getProcessFull());
    }

    /**
     * Test that QueueStatusEvents are fired to a listener when the state changes.
     * 
     * State: Waiting 
     * Event: Close Queue 
     * State: Closed
     * Event: Open Queue
     * State: Waiting
     * Event: Hold Queue
     * State: Held
     * Event: Close Queue
     * State: Blocked
     */
    public void testQueueStatusEvents() {
        QueueState qs = new QueueState(new DummyQueue());
        // Inline class
        QueueStatusAdaptor qsa = new QueueStatusAdaptor();
        qs.addQueueStatusListener(qsa);
        qsa.expectedStatus = QueueState.CLOSED;
        qs.closeQueue();
        qsa.expectedStatus = QueueState.WAITING;
        qs.openQueue();
        qsa.expectedStatus = QueueState.HELD;
        qs.holdQueue();
        qsa.expectedStatus = QueueState.BLOCKED;
        qs.closeQueue();        
    }
    
    /**
     * Listens for QueueStatusEvents. Compares the fired event to the expected event. 
     * @author Claes Buckwalter (clabu@itn.liu.se)
     */
    public class QueueStatusAdaptor implements QueueStatusListener {
        /**
         * Set this to the expected event before triggering the firing of an event.
         */
        public JDFQueue.EnumQueueStatus expectedStatus = null;
        
        public void queueStatusChanged(QueueStatusEvent event) {
            log.debug("Received QueueStatusEvent: " + event);          
            assertEquals(event.getQueueStatus(), expectedStatus);
        }
    }    
}
