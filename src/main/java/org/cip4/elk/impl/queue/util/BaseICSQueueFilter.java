/*
 * Created on Sep 15, 2004
 */
package org.cip4.elk.impl.queue.util;

import java.util.List;

import org.apache.log4j.Logger;
import org.cip4.elk.queue.Queue;
import org.cip4.elk.queue.util.QueueFilter;
import org.cip4.jdflib.auto.JDFAutoQueueFilter.EnumQueueEntryDetails;
import org.cip4.jdflib.core.ElementName;
import org.cip4.jdflib.core.JDFConstants;
import org.cip4.jdflib.datatypes.JDFAttributeMap;
import org.cip4.jdflib.jmf.JDFJobPhase;
import org.cip4.jdflib.jmf.JDFQueue;
import org.cip4.jdflib.jmf.JDFQueueEntry;
import org.cip4.jdflib.jmf.JDFQueueFilter;

/**
 * A filter that sorts queues. This implementation of <code>QueueFilter</code>
 * will sort the queue entries according to the documentation of
 * {@link #filterQueue(JDFQueue, JDFQueueFilter)}.
 * 
 * @see <a
 *      href="http://www.cip4.org/documents/jdf_specifications/JDF1.2.pdf">JDF
 *      Specification Release 1.2, Table 5.105 Contents of the QueueFilter
 *      Element </a>
 * @see <a
 *      href="http://www.cip4.org/document_archive/documents/ICS-Base-1.0.pdf">ICS-Base-1.0
 *      Specification, Table 49 QueueFilter </a>
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: BaseICSQueueFilter.java,v 1.4 2005/09/08 23:57:09 ola.stering Exp $
 */
public class BaseICSQueueFilter implements QueueFilter {

    private static Logger log;
    private Queue _queue;

    /**
     * Creates a BaseICSQueueFilter.
     * 
     * @param owner the <code>Queue</code> that owns this
     *            <code>QueueFilter</code>.
     */
    public BaseICSQueueFilter(Queue owner) {
        log = Logger.getLogger(this.getClass().getName());
        _queue = owner;
        log.debug("Instance of BaseICSQueueFilter instantiated");
    }

    /**
     * Filters all queue entries in a queue. This implementation of
     * <code>QueueFilter</code> handles these filter attributes:
     * <ul>
     * <li>MaxEntries, Maximum number of QueueEntries to provide in the Queue
     * element. If not specified, fill in all matching QueueEntrys. If 0 is
     * specified ALL entries will be returned</li>
     * <li>QueueEntryDetails, for details see JDF 1.2.</li>
     * </ul>
     * <p>
     * NOTE: The <em>Full</em> option is no longer a valild value of Queue
     * <em>QueneEntryDetails</em>.
     * </p>
     * <p>
     * NOTE: An incoming <em>Queue</em> is assumed to be sorted BEFORE it
     * enters this filter.
     * </p>
     * <p>
     * Other attributes of <em>QueueFilter</em> are ignored.
     * </p>
     * <p>
     * A new queue is returned, with the same owner document as the incoming
     * queue.
     * </p>
     * 
     * @param queue the queue to filter
     * @param filter the <em>QueueFilter</em> to be used to filter the
     *            <em>Queue</em>. If the filter is <code>null</code> it is
     *            ignored and the returned <em>Queue</em> will be an exact
     *            copy of the incoming <em>Queue</em>.
     * @return a new filtered queue
     * @see org.cip4.elk.queue.util.QueueFilter#filterQueue(org.cip4.jdflib.jmf.JDFQueue,
     *      org.cip4.jdflib.jmf.JDFQueueFilter)
     * @throws NullPointerException if the <code>queue</code> is
     *             <code>null</code>
     */
    public JDFQueue filterQueue(JDFQueue queue, JDFQueueFilter filter) {

        if (filter == null) {
            log.debug("No filter applied, queue returned unmodified");
            return (JDFQueue) queue.cloneNode(true);
        }
        log.debug("About to filter the queue using filter " + filter);
        int max = -1;

        JDFQueue retQueue = (JDFQueue) queue.cloneNode(false);

        // The filtering!
        boolean includeJobPhase = false; // default
        boolean includeJDF = false; // default
        EnumQueueEntryDetails qed = filter.getQueueEntryDetails();
        // See table 5.105 p. 189 of JDF 1.2
        // No null checking needed
        if (qed.equals(EnumQueueEntryDetails.None)) {
            max = 0; // No Queue entries should be added
            log.debug("QueueFilter/QueueEntryDetails/@None filter"
                    + " is applied, No QueueEntries will be returned.");
        } else if (qed.equals(EnumQueueEntryDetails.Brief)) {
            // Default is that No JobPhase is included.
            log.debug("QueueFilter/QueueEntryDetails/@Brief filter"
                    + " is applied");
            includeJobPhase = false;
            includeJDF = false;
        } else if (qed.equals(EnumQueueEntryDetails.JobPhase)) {
            log.debug("QueueFilter/QueueEntryDetails/@JobPhase filter"
                    + " applied.");
            includeJobPhase = true;
            includeJDF = false;
        } else if (qed.equals(EnumQueueEntryDetails.JDF)) {

            log
                    .debug("QueueFilter/QueueEntryDetails/@JDF filter "
                            + "applied.");
            includeJobPhase = true;
            includeJDF = true;   
        } else {
            log.debug("Incorrect filter value, using default (Brief)");
        }

        List qEntries = queue.getChildElementVector(ElementName.QUEUEENTRY,
            JDFConstants.NONAMESPACE, new JDFAttributeMap(), false, 0, false);
        // MaxEntries defaults to 0 if not given, therefore ALL elements
        // if Max is 0
        int queueSize = qEntries.size();
        if (max != 0) { // if 0 then the None filter is applied.
            max = filter.getMaxEntries(); // 0 default which equals ALL!
            if (max <= 0 || max > queueSize) {
                max = queueSize;
            }
        }
        log.debug("There are " + queueSize + " QueueEntries in the Queue, "
                + max + " are returned.");
        for (int i = 0; i < max; i++) {
            JDFQueueEntry qe = (JDFQueueEntry) qEntries.get(i);
            JDFJobPhase phase = null;
            if (includeJobPhase) {
                phase = _queue.getJobPhase(qe.getQueueEntryID(), includeJDF);
                if (phase != null) {
                    qe.copyElement(phase, null);
                }
            }
            retQueue.copyElement(qe, null);
        }
        return retQueue;
    }
}
