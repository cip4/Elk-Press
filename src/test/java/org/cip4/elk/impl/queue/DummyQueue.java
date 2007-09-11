/*
 * Created on Oct 1, 2004
 */
package org.cip4.elk.impl.queue;

import org.cip4.elk.queue.Queue;
import org.cip4.elk.queue.QueueStatusListener;
import org.cip4.jdflib.jmf.JDFJobPhase;
import org.cip4.jdflib.jmf.JDFQueue;
import org.cip4.jdflib.jmf.JDFQueueEntry;
import org.cip4.jdflib.jmf.JDFQueueFilter;
import org.cip4.jdflib.jmf.JDFQueueSubmissionParams;

/**
 * @author Claes Buckwalter (clabu@itn.liu.se)
 */
public class DummyQueue implements Queue {

    /* (non-Javadoc)
     * @see org.cip4.elk.queue.Queue#setMaxQueueSize(int)
     */
    public void setQueueSize(int size) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.cip4.elk.queue.Queue#getMaxQueueSize()
     */
    public int getMaxQueueSize() {
        // TODO Auto-generated method stub
        return getQueueSize();
    }

    /* (non-Javadoc)
     * @see org.cip4.elk.queue.Queue#getQueueSize()
     */
    public int getQueueSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see org.cip4.elk.queue.Queue#getTotalQueueSize()
     */
    public int getQueueEntryCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see org.cip4.elk.queue.Queue#addQueueEntry(com.heidelberg.JDFLib.jmf.JDFQueueSubmissionParams)
     */
    public JDFQueueEntry addQueueEntry(JDFQueueSubmissionParams params) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.cip4.elk.queue.Queue#getQueueEntry(java.lang.String)
     */
    public JDFQueueEntry getQueueEntry(String queueEntryId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.cip4.elk.queue.Queue#getQueueSubmissionParams(java.lang.String)
     */
    public JDFQueueSubmissionParams getQueueSubmissionParams(String queueEntryId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.cip4.elk.queue.Queue#removeQueueEntry(java.lang.String)
     */
    public JDFQueueEntry removeQueueEntry(String queueEntryId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.cip4.elk.queue.Queue#putQueueEntry(com.heidelberg.JDFLib.jmf.JDFQueueEntry)
     */
    public JDFQueueEntry putQueueEntry(JDFQueueEntry queueEntry) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.cip4.elk.queue.Queue#getQueue()
     */
    public JDFQueue getQueue() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.cip4.elk.queue.Queue#getQueue(com.heidelberg.JDFLib.jmf.JDFQueueFilter)
     */
    public JDFQueue getQueue(JDFQueueFilter filter) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.cip4.elk.queue.Queue#getQueueStatus()
     */
    public JDFQueue.EnumQueueStatus getQueueStatus() {
        // TODO Auto-generated method stub
        return JDFQueue.EnumQueueStatus.Waiting;
    }

    /* (non-Javadoc)
     * @see org.cip4.elk.queue.Queue#getFirstRunnableQueueEntry()
     */
    public JDFQueueEntry getFirstRunnableQueueEntry() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.cip4.elk.queue.Queue#addQueueStatusListener(org.cip4.elk.queue.QueueStatusListener)
     */
    public void addQueueStatusListener(QueueStatusListener listener) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.cip4.elk.queue.Queue#removeQueueStatusListener(org.cip4.elk.queue.QueueStatusListener)
     */
    public void removeQueueStatusListener(QueueStatusListener listener) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.cip4.elk.queue.Queue#closeQueue()
     */
    public void closeQueue() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.cip4.elk.queue.Queue#openQueue()
     */
    public void openQueue() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.cip4.elk.queue.Queue#holdQueue()
     */
    public void holdQueue() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.cip4.elk.queue.Queue#resumeQueue()
     */
    public void resumeQueue() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.cip4.elk.queue.Queue#flushQueue()
     */
    public void flushQueue() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.cip4.elk.queue.Queue#getJobPhase(java.lang.String, boolean)
     */
    public JDFJobPhase getJobPhase(String queueEntryId, boolean includeJDF) {
        // TODO Auto-generated method stub
        return null;
    }

    public void abortQueueEntry(String queueEntryId) {
        // TODO Auto-generated method stub
        
    }

    public int getTotalQueueSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    public void setMaxQueueSize(int size) {
        // TODO Auto-generated method stub
        
    }

}
