package org.cip4.elk.impl.queue;

import org.apache.log4j.Logger;
import org.cip4.elk.queue.Queue;
import org.cip4.elk.queue.QueueStatusEvent;
import org.cip4.elk.queue.QueueStatusListener;
import org.cip4.jdflib.auto.JDFAutoQueue.EnumQueueStatus;
import org.cip4.jdflib.jmf.JDFQueue;

/**
 * This class models the state of a JDF queue. Whenever the state changes events
 * are sent to all registered listeners.
 * <p>
 * This implementation recalculates the queue's status each time any of the methods
 * that potentially modify the queue's status are called. Calling a method that 
 * results in the same status as the queue had prior to the method call does not 
 * result in a status change. For example, if the queue already is closed, calling
 * {@link #closeQueue() closeQueue} will not modify the queue's status (it will 
 * however cause the queue's status to be recalculated).
 * </p>
 * @author Claes Buckwalter (clabu@itn.liu.se)
 * @see org.cip4.jdflib.jmf.JDFQueue.EnumQueueStatus
 * @see <a href="http://www.cip4.org/documents/jdf_specifications/JDF1.2.pdf">JDF Specification Release 1.2, 5.6 Queue Support</a> 
 * @see <a href="http://www.cip4.org/documents/jdf_specifications/JDF1.2.pdf">JDF Specification Release 1.2, 5.6.4 Global Queue Handling</a>
 * @version $Id: QueueState.java,v 1.5 2006/08/30 15:48:12 buckwalter Exp $
 */
public class QueueState {

    public static final JDFQueue.EnumQueueStatus BLOCKED = JDFQueue.EnumQueueStatus.Blocked;
    public static final JDFQueue.EnumQueueStatus CLOSED = JDFQueue.EnumQueueStatus.Closed;
    public static final JDFQueue.EnumQueueStatus FULL = JDFQueue.EnumQueueStatus.Full;
    public static final JDFQueue.EnumQueueStatus HELD = JDFQueue.EnumQueueStatus.Held;
    public static final JDFQueue.EnumQueueStatus RUNNING = JDFQueue.EnumQueueStatus.Running;
    public static final JDFQueue.EnumQueueStatus WAITING = JDFQueue.EnumQueueStatus.Waiting;

    private Logger log;
    private boolean _queueClosed;
    private boolean _queueHeld;
    private boolean _queueFull;
    private boolean _processFull;
    private JDFQueue.EnumQueueStatus _queueStatus=EnumQueueStatus.Blocked;
    private QueueStatusListenerNotifier _notifier;
    private Queue _queue;
    
    /**
     * Creates a new queue state object for the specified queue. The queue's 
     * state that has status <em>Waiting</em>, is not full, and has a process 
     * that is not full.
     */
    public QueueState(Queue owner) {
        log = Logger.getLogger(this.getClass());
        _notifier = new  QueueStatusListenerNotifier();
        _queue = owner;
        _queueStatus = owner.getQueueStatus();
        _queueClosed = false;
        _queueHeld = false;
        _queueFull = false;
        _processFull = false;
        _queueStatus = recalculateQueueStatus();
    }
    
    /**
     * Sets the queue's status.
     * @param status    the queue's new status
     * @return the queue's old status
     * @see org.cip4.jdflib.jmf.JDFQueue.EnumQueueStatus
     */
    private synchronized JDFQueue.EnumQueueStatus setQueueStatus(final JDFQueue.EnumQueueStatus status) {
        final JDFQueue.EnumQueueStatus oldStatus = _queueStatus;
        _queueStatus = status;
        final String msg = "Status Change: " + oldStatus.getName() + " -> " + _queueStatus.getName();
        log.debug(msg);
        fireQueueStatusEvent(new QueueStatusEvent(QueueStatusEvent.EVENT, status, _queue, msg));        
        return oldStatus;
    }
    
    /**
     * Returns the queue's status. 
     * @return the queue's status
     * @see org.cip4.jdflib.jmf.JDFQueue.EnumQueueStatus
     */
    public synchronized JDFQueue.EnumQueueStatus getQueueStatus() {
        return _queueStatus;
    }
    
    /**
     * Recalculates the queue's status.
     * @return the queue's status
     * @see org.cip4.jdflib.jmf.JDFQueue.EnumQueueStatus
     */
    public final synchronized JDFQueue.EnumQueueStatus recalculateQueueStatus() {        
        final JDFQueue.EnumQueueStatus status;
        if (_queueClosed && _queueHeld) {
            status = BLOCKED;
        } else if (_queueClosed && !_queueHeld) {
            status = CLOSED;
        } else if (!_queueClosed && _queueHeld) {
            status = HELD;
        } else {
            if (!_queueFull && _processFull) {
                status = RUNNING;
            } else if (_queueFull && _processFull) {
                status = FULL;
            } else if (_queueFull) {
                // This does not conform with the JDF spec, table 5-92
                status = FULL;
            } else { 
                status = WAITING; 
            }
        }
        log.debug("Recalculated Status: " + status==null ? "unknown" : status.getName());
        if (!getQueueStatus().equals(status)) {            
            setQueueStatus(status);
        } 
        return status;
    }
    
    /**
     * Closes the queue. If the queue already is closed no status change will occur.
     * @return the queue's new status
     * @see #openQueue()
     */
    public synchronized JDFQueue.EnumQueueStatus closeQueue() {        
        log.debug("Queue closed...");
        _queueClosed = true;         
        return recalculateQueueStatus();
    }
    
    /**
     * Opens the queue. If the queue already is open no status change will occur. 
     * @return the queue's new status
     * @see #closeQueue()
     */
    public synchronized JDFQueue.EnumQueueStatus openQueue() {
        log.debug("Queue opened...");
        _queueClosed = false;
        return recalculateQueueStatus();
    }
    
    /**
     * Holds the queue. If the queue already is held no status change will occur.
     * @return the queue's new status
     * @see #resumeQueue()
     */
    public synchronized JDFQueue.EnumQueueStatus holdQueue() {
        log.debug("Queue held...");
        _queueHeld = true;
        return recalculateQueueStatus();        
    }

    /**
     * Resumes the queue. If the queue is not held no status change will occur.
     * @return the queue's new status
     * @see #holdQueue()
     */
    public synchronized JDFQueue.EnumQueueStatus resumeQueue() {
        log.debug("Queue resumed...");
        _queueHeld = false;
        return recalculateQueueStatus();        
    }
    
    /**
     * Sets whether the queue is full or not. If the queue is full it cannot 
     * accept any new queue entries.
     * @param queueFull <code>true</code> if the queue is full; <code>false</code> otherwise
     */    
    public synchronized void setQueueFull(final boolean queueFull) {
        log.debug("Set Queue Full to " + queueFull + "...");
        _queueFull = queueFull;
        recalculateQueueStatus();
    }
    
    /**
     * Returns whether the queue is full or not. When the queue is full it cannot 
     * accept any new queue entries. This method returns the same value
     * as {@link isQueueFull() isQueueFull}.
     * @return <code>true</code> if the queue is full; <code>false</code> otherwise
     * @see #isQueueFull()
     */
    public synchronized boolean getQueueFull() {
        return isQueueFull();
    }

    /**
     * Returns whether the queue is full or not. When the queue is full it cannot 
     * accept any new queue entries. This method returns the same value
     * as {@link getQueueFull() getQueueFull}.
     * @return <code>true</code> if the queue is full; <code>false</code> otherwise
     * @see #getQueueFull()
     */
    public synchronized boolean isQueueFull() {
        return _queueFull;
    }
    
    /**
     * Sets whether the process is full or not. The process is full when it is 
     * processing a queue entry. 
     * @param processFull <code>true</code> if the process is full; <code>false</code> otherwise
     */ 
    public synchronized void setProcessFull(final boolean processFull) {
        log.debug("Set Process Full to " + processFull + "...");
        _processFull = processFull;
        recalculateQueueStatus();
    }
    
    /**
     * Returns whether the process is full or not. The process is full when it is 
     * processing a queue entry. This method returns the same value
     * as {@link getProcessFull() getProcessFull}.
     * @param processFull <code>true</code> if the process is full; <code>false</code> otherwise
     * @see #getProcessFull()
     */    
    public synchronized boolean isProcessFull() {
        return _processFull;
    }

    /**
     * Returns whether the process is full or not. The process is full when it is 
     * processing a queue entry. This method returns the same value
     * as {@link isProcessFull() isProcessFull}.
     * @param processFull <code>true</code> if the process is full; <code>false</code> otherwise
     * @see #isProcessFull()
     */    
    public synchronized boolean getProcessFull() {
        return isProcessFull();
    }
    
    /**
     * Adds a listener that listens to status changes.
     * @param listener
     */
    public void addQueueStatusListener(final QueueStatusListener listener) {
        _notifier.addListener(listener);
        log.debug("Added QueueStatusListener: " + listener);
    }

    /**
     * Removes a listener.
     */
    public void removeQueueStatusListener(final QueueStatusListener listener) {
        _notifier.removeListener(listener);
        log.debug("Removed QueueStatusListener: " + listener);
    }

    /**
     * Sends the status change to all listeners.
     * @param event the event to fire
     */
    protected void fireQueueStatusEvent(final QueueStatusEvent event) {
        _notifier.fireEvent(event);
        log.debug("Fired QueueStatusEvent.");
    }
}
