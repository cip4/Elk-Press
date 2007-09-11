/*
 * Created on Sep 19, 2004
 */
package org.cip4.elk.impl.queue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cip4.elk.Config;
import org.cip4.elk.JDFElementFactory;
import org.cip4.elk.JDFElementFactoryLoaderException;
import org.cip4.elk.device.process.Process;
import org.cip4.elk.device.process.ProcessQueueEntryEvent;
import org.cip4.elk.device.process.ProcessQueueEntryEventListener;
import org.cip4.elk.device.process.ProcessStatusEvent;
import org.cip4.elk.device.process.ProcessStatusListener;
import org.cip4.elk.impl.queue.util.AttributeQueueFilter;
import org.cip4.elk.impl.queue.util.BaseICSQueueFilter;
import org.cip4.elk.impl.queue.util.SortingQueueFilter;
import org.cip4.elk.impl.util.URLAccessTool;
import org.cip4.elk.queue.Queue;
import org.cip4.elk.queue.QueueStatusEvent;
import org.cip4.elk.queue.QueueStatusListener;
import org.cip4.elk.queue.util.QueueFilter;
import org.cip4.jdflib.auto.JDFAutoJobPhase.EnumActivation;
import org.cip4.jdflib.auto.JDFAutoQueue.EnumQueueStatus;
import org.cip4.jdflib.core.ElementName;
import org.cip4.jdflib.core.JDFDoc;
import org.cip4.jdflib.core.JDFElement.EnumNodeStatus;
import org.cip4.jdflib.datatypes.JDFAttributeMap;
import org.cip4.jdflib.jmf.JDFDeviceInfo;
import org.cip4.jdflib.jmf.JDFJobPhase;
import org.cip4.jdflib.jmf.JDFQueue;
import org.cip4.jdflib.jmf.JDFQueueEntry;
import org.cip4.jdflib.jmf.JDFQueueFilter;
import org.cip4.jdflib.jmf.JDFQueueSubmissionParams;
import org.cip4.jdflib.node.JDFNode;
import org.cip4.jdflib.pool.JDFAncestorPool;
import org.cip4.jdflib.util.JDFDate;

/**
 * An implementation of a queue.
 * <p>
 * <strong>Note: </strong> This implementation makes defensive copies of all JDF
 * objects (<code>JDFQueue</code>,<code>JDFQueueEntry</code>, etc)
 * before setting or returning them. In other words, modifying a JDFQueueEntry
 * returned by this class will not modify the internals of this class. In
 * addition, none of the returned JDF objects share the same owner
 * <code>org.w3c.dom.Document</code>.
 * </p>
 * 
 * @author Claes Buckwalter (clabu@itn.liu.se)
 * @author Ola Stering (olst6875@student.uu.se)
 * @author Jos Potargent (jos.potargent@agfa.com)
 * 
 * @see org.cip4.elk.queue.Queue
 * @see org.cip4.elk.impl.queue.QueueState
 * @version $Id: MemoryQueue.java,v 1.15 2006/09/12 08:34:52 buckwalter Exp $
 */
public class MemoryQueue implements Queue, ProcessStatusListener,
        ProcessQueueEntryEventListener {

    private int _maxQueueSize;
    private SortingQueueFilter _sortingFilter;
    private BaseICSQueueFilter _baseICSFilter;
    private QueueFilter _statusWaitingFilter;
    private JDFQueue _queue;
    private Map _queueEntriesMap;
    private Map _queueSubmissionParamsMap;
    private Config _config;
    private URLAccessTool _fileUtil;
    private int _queueEntryIdIterator;
    private static Logger log;
    private QueueState _state;
    private Process _process;

    public MemoryQueue(Config config, int maxQueueSize, URLAccessTool fileUtil) {
        log = Logger.getLogger(this.getClass().getName());
        log.debug("Attempting to create MemoryQueue");
        // Create internal objects
        _state = new QueueState(this);
        _config = config;
        _fileUtil = fileUtil;
        _queueEntryIdIterator = 0;
        _queueEntriesMap = new HashMap();
        _queueSubmissionParamsMap = new HashMap();
        _queue = createQueue();
        // Configure queue filters
        JDFAttributeMap attrMap = new JDFAttributeMap();
        attrMap.put("Status", JDFQueueEntry.EnumQueueEntryStatus.Waiting
                .getName());
        _statusWaitingFilter = new AttributeQueueFilter(attrMap);
        _sortingFilter = new SortingQueueFilter();
        _baseICSFilter = new BaseICSQueueFilter(this);
        // Set queue size
        setQueueSize(maxQueueSize);
        log.debug("Instance of MemoryQueue created.");
    }

    /**
     * Sets this Queue's process.
     * 
     * @param process the <code>Process</code> to set.
     */
    public void setProcess(Process process) {
        _process = process;
        log.debug("Set the Process of the queue with class '"
                + process.getClass().getName() + "'.");
    }

    public void init() {
    }

    public void destroy() {
        _state.holdQueue();
    }

    /**
     * @see org.cip4.elk.queue.Queue#setMaxQueueSize(int)
     */
    public void setQueueSize(int size) {
        _maxQueueSize = size;
        _state.setQueueFull(getQueueEntryCount() >= _maxQueueSize);
    }

    /**
     * @see org.cip4.elk.queue.Queue#getMaxQueueSize()
     * @deprecated Use {@link #getQueueSize()} instead
     */
    public int getMaxQueueSize() {
        return getQueueSize();
    }

    /**
     * Returns the number of queue entries allowed in this queue. 
     * 
     * @return the number of queue entries allowed in this queue
     * @see org.cip4.elk.queue.Queue#getQueueSize()
     */
    public synchronized int getQueueSize() {
        return _maxQueueSize;
    }

    public synchronized int getQueueEntryCount() {
        return _queueEntriesMap.size();
    }

    /**
     * Returns this queue's status.
     * 
     * @see com.heidelberg.JDFLib.jmf.JDFQueue.EnumQueueStatus;
     */
    public synchronized JDFQueue.EnumQueueStatus getQueueStatus() {
        return _state==null ? EnumQueueStatus.Blocked : _state.getQueueStatus();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cip4.elk.queue.Queue#addQueueEntry(org.cip4.jdflib.jmf.JDFQueueSubmissionParams)
     */
    public synchronized JDFQueueEntry addQueueEntry(
            JDFQueueSubmissionParams params) {
        log.debug("Adding queue entry: " + params);
        JDFQueueEntry qe;
        // Check if the queue is accepting new queue entries
        JDFQueue.EnumQueueStatus status = _state.getQueueStatus();
        if (status.equals(JDFQueue.EnumQueueStatus.Full)) {
            qe = null;
        } else if (status.equals(JDFQueue.EnumQueueStatus.Blocked)) {
            qe = null;
        } else if (status.equals(JDFQueue.EnumQueueStatus.Closed)) {
            qe = null;
        } else {
            qe = createQueueEntry(params);
            putQueueEntry(qe);
        
            // Keep a copy of submission parameters
            // TODO Make a defensive copy first
            _queueSubmissionParamsMap.put(qe.getQueueEntryID(), params);
            // Check if the queue was filled
            _state.setQueueFull(getQueueEntryCount() >= getQueueSize());
        }
        return qe;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cip4.elk.queue.Queue#getQueueEntry(java.lang.String)
     */
    public synchronized JDFQueueEntry getQueueEntry(String queueEntryId) {
        JDFQueueEntry qe = (JDFQueueEntry) _queueEntriesMap.get(queueEntryId);
        if (qe != null) {
            // Creates a copy of the queue entry
            JDFDoc owner = new JDFDoc();
            qe = (JDFQueueEntry) owner.importNode(qe, true);
        }
        return qe;
    }

    public JDFQueueSubmissionParams getQueueSubmissionParams(String queueEntryId) {
        // TODO Make defensive copy before returning
        return (JDFQueueSubmissionParams) _queueSubmissionParamsMap
                .get(queueEntryId);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cip4.elk.queue.Queue#abortQueueEntry(java.lang.String)
     */
    public synchronized void abortQueueEntry(String queueEntryId) {
        log.debug("Aborting queue entry '" + queueEntryId + "'...");
        
        // Get the queue entry element.
        JDFQueueEntry qe = (JDFQueueEntry) _queueEntriesMap.get(queueEntryId);
        if (qe == null) {
        	log.error("Aborting error: cannot find queue entry '" + queueEntryId + "'.");
        }
        else {
        	qe.setStatus(EnumNodeStatus.Aborted);
        }
    }

    
    /**
     * @see org.cip4.elk.queue.Queue#removeQueueEntry(java.lang.String)
     */
    public synchronized JDFQueueEntry removeQueueEntry(String queueEntryId) {
        log.debug("Removing queue entry '" + queueEntryId + "'...");

        _queueSubmissionParamsMap.remove(queueEntryId);
        // Creates a copy of the removed queue entry and returns the copy
        JDFQueueEntry qe = (JDFQueueEntry) _queueEntriesMap
                .remove(queueEntryId);

        if (qe != null) {
            // Detaches the queue entry from the queue
            _queue.removeChild(qe);
            // Creates a copy of the removed queue entry and returns the copy
            JDFDoc owner = new JDFDoc();
            qe = (JDFQueueEntry) owner.importNode(qe, true);
        }

        _state.setQueueFull(getQueueEntryCount() >= getQueueSize());
        return qe;
    }

    /**
     * @see org.cip4.elk.queue.Queue#putQueueEntry(JDFQueueEntry)
     */
    public synchronized JDFQueueEntry putQueueEntry(JDFQueueEntry queueEntry) {
        String qeId = queueEntry.getQueueEntryID();
        log.debug("Putting queue entry '" + qeId + "'...");
        // Checks if the queue is full
        if (!_queueEntriesMap.containsKey(qeId)
                && getQueueSize() == getQueueEntryCount()) {
            return null;
        }
        // Creates a copy of the queue entry and appends it to the queue
        queueEntry = (JDFQueueEntry) _queue.copyElement(queueEntry, null);
        // Puts the queue entry in the ID->QueueEntry map
        queueEntry = (JDFQueueEntry) _queueEntriesMap.put(qeId, queueEntry);
        // Creates a copy of the old queue entry and returns it
        if (queueEntry != null) {
            JDFDoc owner = new JDFDoc();
            owner.importNode(queueEntry, true);
        }
        return queueEntry;
    }

    /**
     * Returns a sorted representation of this queue.
     * <p>
     * TODO The performance of this method most likely needs to be optimized.
     * See {@link MemoryQueue#getQueue(QueueFilter)}that this method delegates
     * to.
     * </p>
     * 
     * @see #getQueue(org.cip4.elk.impl.queue.util.QueueFilter)
     * @see org.cip4.elk.queue.Queue#getQueue()
     */
    public synchronized JDFQueue getQueue() {
        return getQueue(_sortingFilter);
    }

    /**
     * Returns a filtered queue.
     * 
     * The queue is sorted and filtered according to the requirements of Base
     * ICS.
     * 
     * @see org.cip4.impl.queue.util.SortingQueueFilter
     * @return the filtered and sorted queue
     */

    public synchronized JDFQueue getQueue(JDFQueueFilter filter) {
        // TODO Map the JDFQueueFilter to a QueueFilter
        // Creates a copy of the queue element
        JDFDoc owner = new JDFDoc();
        JDFQueue q = (JDFQueue) owner.importNode(_queue, false);

        q.setDeviceID(_config.getID());
        q.setQueueSize(getQueueSize());
        q.setQueueStatus(getQueueStatus());
        // Appends all queue entries to the queue copy
        for (Iterator it = _queueEntriesMap.values().iterator(); it.hasNext();) {
            q.copyElement((JDFQueueEntry) it.next(), null);
        }

        log.debug("About to sort the Queue.");
        // First sort the queue according to specification
        q = _sortingFilter.filterQueue(q, filter, false); // TODO test

        log.debug("About to filter the Queue using "
                + _baseICSFilter.getClass().getName());
        q = _baseICSFilter.filterQueue(q, filter);

        return q;
    }

    /**
     * Returns a filtered queue.
     * 
     * @todo The performance of this method most likely needs to be optimized.
     *       There is a lot of cloning and iterating over Nodes nodes going on
     *       here.
     * @see org.cip4.elk.queue.Queue#getQueue(org.cip4.elk.impl.queue.util.QueueFilter)
     */
    public synchronized JDFQueue getQueue(QueueFilter filter) {
        // Creates a copy of the queue element
        JDFDoc owner = new JDFDoc();
        JDFQueue q = (JDFQueue) owner.importNode(_queue, false);
        q.setQueueSize(getQueueSize());
        q.setQueueStatus(getQueueStatus());
        // Appends all queue entries to the queue copy
        for (Iterator it = _queueEntriesMap.values().iterator(); it.hasNext();) {
            q.copyElement((JDFQueueEntry) it.next(), null);
        }
        if (filter instanceof SortingQueueFilter) {
            // Sorts without copying and cloning
            q = ((SortingQueueFilter) filter).filterQueue(q, null, false);
        } else {
            // Default filtering
            q = filter.filterQueue(q, null);
        }
        return q;
    }

    /**
     * Returns the first queue entry that is runnable.
     */
    public synchronized JDFQueueEntry getFirstRunnableQueueEntry() {
        // log.debug("Getting first runnable queue entry: " +
        // _queue.getQueueEntry(0));
        JDFQueueEntry qe = null;
        JDFQueue.EnumQueueStatus status = getQueueStatus();
        if (!status.equals(JDFQueue.EnumQueueStatus.Held)
                && !status.equals(JDFQueue.EnumQueueStatus.Blocked)) {
            JDFQueue q = getQueue(_statusWaitingFilter);
            qe = q.getQueueEntry(0);
        }
        return qe;
    }

    /**
     * Creates a new queue entry based on the submission parameters. The queue
     * entry is represented by a <em>QueueEntry</em> element that is owned by
     * this queue's owner document. When creating the queue entry the JDF job
     * file that the queue entry refers to is downloaded so that job ID (
     * <em>JDF/@JobID</em>) and job part ID (<em>JDF/@JobPartID</em>) can
     * be retrieved.
     * 
     * @param params
     * @return
     */
    private JDFQueueEntry createQueueEntry(JDFQueueSubmissionParams params) {
        JDFQueueEntry qe = (JDFQueueEntry) JDFElementFactory.getInstance()
                .createJDFElement(ElementName.QUEUEENTRY);
        qe.setDeviceID(getDeviceID());
        // XXX qe.setEndTime(); Done in BaseProcess.
        qe.setPriority(params.getPriority());
        qe.setQueueEntryID(createQueueEntryID());
        // XXX qe.setStartTime(); Done in BaseProcess.
        qe.setQueueEntryStatus(JDFQueueEntry.EnumQueueEntryStatus.Waiting);
        // XXX qe.setStatus();
        qe.setSubmissionTime(new JDFDate());
        // XXX qe.setJobPhase();
        // Get JDF file
        String url = params.getURL();
        JDFNode jdf = _fileUtil.getURLAsJDF(url);
        // TODO Cache the JDF file to temp dir so that the device does not have
        // to download it again
        if (jdf != null) {
            String jobID = jdf.getJobID(true);
            if (jobID.equals("")) {
                log.warn("No JobID set");
                jobID = "Unknown jobID";
            }
            qe.setJobID(jobID);
            qe.setJobPartID(jdf.getJobPartID(false));

            JDFAncestorPool ancestorPool = jdf.getAncestorPool();
            if (ancestorPool != null) {
                Vector partVector = ancestorPool.getPartMapVector().getVector();
                int vsize = partVector.size();
                log.debug("The JDF Node contained an AncestorPool with "
                        + vsize + " Part elements.");
                for (int i = 0; i < vsize; i++) {
                    // Add qe/Part elements.
                    qe.appendPart().setAttributes(
                        (JDFAttributeMap) partVector.get(i));
                }
            }

        } else {
            log.warn("This QueueEntry's JDF Node was null");
        }
        return qe;
    }

    /**
     * Returns the ID of the device that owns this queue. The device's ID is
     * retrieved from this queue's configuration.
     * 
     * @see MemoryQueue#MemoryQueue(Config, int, URLAccessTool)
     * @return the owning device's ID
     */
    private String getDeviceID() {
        return _config.getID();
    }

    /**
     * Creates a new queue entry ID (<em>QueueEntry/@QueueEntryID</em>).
     * This returns the next integer value from a counter which is reset each
     * time the queue is restarted.
     * 
     * @return
     */
    private synchronized String createQueueEntryID() {
        _queueEntryIdIterator++;
        return "" + _queueEntryIdIterator;
    }

    /**
     * Creates a new <em>Queue</em> element that represents this queue.
     * 
     * @return a <em>Queue</em> element that represents this queue
     */
    private JDFQueue createQueue() {
        JDFQueue queue = null;
        try {
            queue = (JDFQueue) JDFElementFactory.getInstance()
                    .createJDFElement(ElementName.QUEUE);
        } catch (JDFElementFactoryLoaderException e) {
            log.error("Unable to load JDFElementFactory. Do you have the"
                    + " JDFElementFactory.properties in your class path?", e);
        }
        queue.setQueueSize(getQueueSize());
        queue.setQueueStatus(getQueueStatus());
        queue.setDeviceID(getDeviceID());
        return queue;
    }

    public void openQueue() {
        _queue.setQueueStatus(_state.openQueue());
    }

    public void closeQueue() {
        _queue.setQueueStatus(_state.closeQueue());
    }

    public void holdQueue() {
        // TODO Hold currently running processes
        _queue.setQueueStatus(_state.holdQueue());
    }

    public void resumeQueue() {
        _queue.setQueueStatus(_state.resumeQueue());
    }

    public void flushQueue() {
        // TODO Implement flush queue
    }

    /**
     * Adds a listener that listens for events from this queue.
     * 
     * @see org.cip4.elk.queue.Queue#addQueueStatusListener(QueueStatusListener)
     */
    public void addQueueStatusListener(QueueStatusListener listener) {
        _state.addQueueStatusListener(listener);
    }

    /**
     * Removes a listener that listened for events from this queue.
     * 
     * @see org.cip4.elk.queue.Queue#removeQueueStatusListener(QueueStatusListener)
     */
    public void removeQueueStatusListener(QueueStatusListener listener) {
        _state.addQueueStatusListener(listener);
    }

    /**
     * Sends the event to all listeners. This method delegates to the underlying
     * queue state object.
     * 
     * @param event the event to fire
     */
    protected void fireQueueStatusEvent(QueueStatusEvent event) {
        _state.fireQueueStatusEvent(event);
    }

    /**
     * Updates the queue's state. ProcessStatusListener interface.
     */
    public void processStatusChanged(ProcessStatusEvent processEvent) {
        JDFDeviceInfo.EnumDeviceStatus status = processEvent.getProcessStatus();
        if (status.equals(JDFDeviceInfo.EnumDeviceStatus.Running)
                || status.equals(JDFDeviceInfo.EnumDeviceStatus.Setup)
                || status.equals(JDFDeviceInfo.EnumDeviceStatus.Cleanup)) {
            _state.setProcessFull(true);
            _state.setQueueFull(getQueueSize() == getQueueEntryCount());
        } else if (status.equals(JDFDeviceInfo.EnumDeviceStatus.Idle)
                || status.equals(JDFDeviceInfo.EnumDeviceStatus.Stopped)
                || status.equals(JDFDeviceInfo.EnumDeviceStatus.Down)) {
            _state.setProcessFull(false);
        }
    }

    /**
     * <strong>NOTE:</strong> When getting the JDF (If
     * QueueFilter/QueueEntryDetails/@JDF) it will not work correctly yet,
     * because the Queue will fetch the JDFs from the URL at the moment (which
     * means unprocessed).
     * 
     * @see org.cip4.elk.queue.Queue#getJobPhase(java.lang.String, boolean)
     */
    public synchronized JDFJobPhase getJobPhase(String queueEntryId,
            boolean includeJDF) {

        if (queueEntryId == null) {
            throw new NullPointerException("queueEntryId may not be null");
        }

        JDFQueueEntry qe = getQueueEntry(queueEntryId);
        if (qe == null) {
            log.debug("No queue entry with id '" + queueEntryId
                    + "' exists, no JobPhase returned.");
            return null;
        }

        JDFJobPhase phase = null;
        if (qe.getQueueEntryStatus().equals(
            JDFQueueEntry.EnumQueueEntryStatus.Running)) {
            log.debug("This QueueEntry is running, getting its JobPhase from"
                    + " the Process running it.");
            // If the QueueEntry has state running the JobPhase must be
            // fetched from the Process.

            if (_process == null) {
                log.warn("This Queue's Process is null, use the "
                        + "setProcess(Process process) method to set its"
                        + " Process.");
                return null;
            }
            phase = _process.getJobPhase();
            if (phase == null) { // Programming error
                String msg = "The QueueEntry with id '" + qe.getQueueEntryID()
                        + "' state was Running. The Process that is "
                        + "running this QueueEntry is supposed to provide"
                        + " a JobPhase element for running QueueEntries but did not. "
                        + "No JobPhase element of this QueueEntry "
                        + "attached.";
                log.warn(msg);
                return null;
            } 
            // copy the element to the JobPhase element to the QueueEntry
            JDFJobPhase qephase = qe.getJobPhase();
            if (qephase != null) {
                qe.replaceChild(phase, qephase);
            } else {
                qe.copyElement(phase, null);
            }
        } else {
            log.debug("The QueueEntryID='" + qe.getQueueEntryID()
                    + "', JobID='" + qe.getJobID() + "'," + "JobPartID='"
                    + qe.getJobPartID() + "', and Status='"
                    + qe.getQueueEntryStatus()
                    + "' is not running, no JobPhase added.");
        }

        if (includeJDF && phase != null) { // if false, no JDF will be
            // included.
            // The JDF must be fetched somehow.
            // This will become the repository's duty
            // for now download it from URL.
            // TODO Change to use the Repository.
            JDFQueueSubmissionParams params = (JDFQueueSubmissionParams) _queueSubmissionParamsMap
                    .get(qe.getQueueEntryID());
            String url = params.getURL();
            JDFNode jdf = _fileUtil.getURLAsJDF(url);
            if (jdf == null) {
                log.warn("Unable to fetch the jdf for queueEntry with id "
                        + qe.getQueueEntryID()
                        + ". No JDF attached to the JobPhase."
                        + " Is the url '" + url + "' correct?");
            } else {
                phase.setActivation(EnumActivation.getEnum(jdf
                        .getActivation(true).getName()));
                jdf.setActivation(JDFNode.EnumActivation.Informative);
                phase.copyElement(jdf, null);
            }
        }

        return phase;
    }

    /**
     * @see org.cip4.elk.queue.ProcessQueueEntryEventListener#queueEntryStatusChanged(org.cip4.elk.queue.ProcessQueueEntryEvent)
     */
    public void queueEntryStatusChanged(ProcessQueueEntryEvent event) {
        log.debug("Received ProcessQueueEntryEvent.");
        JDFQueueEntry qe = event.getQueueEntry();
        putQueueEntry(qe);
    }

    /**
     * @deprecated Use {@link #getQueueEntryCount()} instead
     * @see org.cip4.elk.queue.Queue#getTotalQueueSize()
     */
    public int getTotalQueueSize() {
        return getQueueEntryCount();
    }

    /**
     * @deprecated Use {@link #getQueueSize()} instead
     * @see org.cip4.elk.queue.Queue#getMaxQueueSize()
     */
    public void setMaxQueueSize(int size) {
        setQueueSize(size);
    }
}
