/*
 * Created on Sep 15, 2004
 */
package org.cip4.elk.impl.queue.util;

import java.util.Collections;
import java.util.List;

import org.cip4.elk.queue.util.QueueFilter;
import org.cip4.jdflib.core.ElementName;
//import org.cip4.jdflib.core.JDFConstants;
import org.cip4.jdflib.datatypes.JDFAttributeMap;
import org.cip4.jdflib.jmf.JDFQueue;
import org.cip4.jdflib.jmf.JDFQueueEntry;
import org.cip4.jdflib.jmf.JDFQueueFilter;

/**
 * A filter that sorts queues. This implementation of <code>QueueFilter</code>
 * ignores the specified filter and simply sorts all queue entries in the input
 * queue.
 * 
 * @author Claes Buckwalter (clabu@itn.liu.se)
 */
public class SortingQueueFilter implements QueueFilter {

    private QueueEntryComparator _comp;

    public SortingQueueFilter() {
        // Sort descending, highest priority first, not last
        _comp = new QueueEntryComparator(false);
    }

    /**
     * Sorts all queue entries in a queue. This implementation of
     * <code>QueueFilter</code> ignores the specified filter and simply sorts
     * all queue entries in the original queue.
     * 
     * @param queue
     *            the queue to sort
     * @param filter
     *            the filter is ignored by this implementation, it may be
     *            <code>null</code>
     * @return a new queue with sorted queue entries
     * @see org.cip4.elk.queue.util.QueueFilter#filterQueue(com.heidelberg.JDFLib.jmf.JDFQueue,
     *      com.heidelberg.JDFLib.jmf.JDFQueueFilter)
     */
    public JDFQueue filterQueue(JDFQueue queue, JDFQueueFilter filter) {
        return filterQueue(queue, filter, true);
    }

    /**
     * Sorts all queue entries in a queue. This implementation of
     * <code>QueueFilter</code> ignores the specified filter and simply sorts
     * all queue entries in the original queue.
     * 
     * @todo Make a defensive copy of the owner document 
     * @param queue
     *            the queue to filter
     * @param filter
     *            the filter will be ignored, it may be <code>null</code>
     * @param copy
     *            if <code>true</code> then a copy of the input queue and its
     *            queue entries will be created; if <code>false</code> the
     *            input queue will be modified
     * @return a queue with sorted queue entries
     */
    public JDFQueue filterQueue(JDFQueue queue, JDFQueueFilter filter,
            boolean copy) {
        // Sort the original queue's queue entries
        List qEntries = queue.getChildElementVector(ElementName.QUEUEENTRY,
                null, new JDFAttributeMap(), false, 0, false);
        Collections.sort(qEntries, _comp);
        // Copy the sorted queue entries to the new queue
        if (copy) {
            // TODO Create a new owner document and import the queue so that
            // clients of this interface cannot obtain a reference to this
            // queue's document
            // Clones the Queue node without children
            queue = (JDFQueue) queue.cloneNode(false);
            for (int i = 0, imax = qEntries.size(); i < imax; i++) {
                queue.copyElement((JDFQueueEntry) qEntries.get(i), null);
            }
        } else {
            JDFQueueEntry qe;
            for (int i = 0, imax = qEntries.size(); i < imax; i++) {
                qe = (JDFQueueEntry) qEntries.get(i);
                queue.removeChild(qe);
                queue.appendChild(qe);
            }
        }
        return queue;
    }
}
