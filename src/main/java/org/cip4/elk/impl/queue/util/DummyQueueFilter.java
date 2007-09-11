/*
 * Created on Aug 31, 2004
 */
package org.cip4.elk.impl.queue.util;

import org.cip4.elk.queue.util.QueueFilter;
import org.cip4.jdflib.jmf.JDFQueue;
import org.cip4.jdflib.jmf.JDFQueueFilter;

/**
 * A queue filter that does not perform any filtering, it just
 * creates a copy of the input queue and returns the copy.
 * 
 * @author Claes Buckwalter (clabu@itn.liu.se)
 */
public class DummyQueueFilter implements QueueFilter {

    /**
     * Filters all queue entries in a queue.
     * 
     * @see org.cip4.elk.queue.util.QueueFilter#filterQueue(com.heidelberg.JDFLib.jmf.JDFQueue, com.heidelberg.JDFLib.jmf.JDFQueueFilter)
     */
    public JDFQueue filterQueue(JDFQueue queue, JDFQueueFilter filter)
    {
        // TODO Create a new owner document and import the queue so that clients of this interface cannot obtain a reference to this queue's document
        return (JDFQueue) queue.cloneNode(true);
    }
   
    
    
    /*
    //
    if (filter == null)
    {
        return queue;
    }
    // Filter parameters
    int maxEntries = filter.getMaxEntries();
    // Filter by date
    // TODO JDFDate olderThan = filter.getOlderThan();
    // TODO JDFDate newerThan = filter.getNewerThan();
    // Filter by detail level: None, Brief (default), JobPhase, JDF, Full 
    // TODO String qeueEntryDetails = filter.getQueueEntryDetails(); 
    // Filter by list status
    JDFAttributeMap attrMap1 = new JDFAttributeMap();
    List statusList = filter.getStatusList().getEnumList();     
    for(int i=0, imax=statusList.size(); i<imax; i++)
    {
        attrMap1.put("Status", statusList.get(i));
    }
    List queueEntries1 = null;
    if (attrMap1.size() > 0)
    {
        queueEntries1 = _queue.getChildElementVector("QueueEntry", JDFConstants.NONAMESPACE, attrMap1, false, maxEntries, false);    
    }                
    // Filter by queue entries
    JDFAttributeMap attrMap2 = new JDFAttributeMap();
    List queueEntryDefs = filter.getChildElementVector("QueueEntryDef",JDFConstants.NONAMESPACE, new JDFAttributeMap(), false, 0, false);
    for(int i=0, imax=queueEntryDefs.size(); i<imax; i++)
    {
        attrMap2.put("QueueEntryID", ((JDFQueueEntry)queueEntryDefs.get(i)).getQueueEntryID());
    }
    List queueEntries2 = null;
    if (attrMap1.size() > 0)
    {
        queueEntries2 = _queue.getChildElementVector("QueueEntry", JDFConstants.NONAMESPACE, attrMap2, false, maxEntries, false);

    }
    // Filter by device ID
    // TODO String deviceID = filter.getDevice(0);
    
    // Intersect all filtered queue entry results
    queueEntries1.retainAll(queueEntries2);
    // TODO
    // Copy queue entries to new queue
    JDFQueue filteredQueue = getQueueTemplate();
    filteredQueue.setQueueSize(queueEntries1.size());
    log.debug("Queue size: " + filteredQueue.getQueueSize());
    for(int i=0, imax=queueEntries1.size(); i<imax; i++)
    {
        filteredQueue.copyElement((KElement)queueEntries1.get(i), null);
    }
    return filteredQueue;       
    
    
    
    return null;
    */

}
