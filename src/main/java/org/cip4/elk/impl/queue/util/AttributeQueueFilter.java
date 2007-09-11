/*
 * Created on Sep 19, 2004
 */
package org.cip4.elk.impl.queue.util;

import java.util.List;

import org.cip4.elk.queue.util.QueueFilter;
import org.cip4.jdflib.core.ElementName;
import org.cip4.jdflib.core.JDFConstants;
import org.cip4.jdflib.datatypes.JDFAttributeMap;
import org.cip4.jdflib.jmf.JDFQueue;
import org.cip4.jdflib.jmf.JDFQueueEntry;
import org.cip4.jdflib.jmf.JDFQueueFilter;

/**
 * A queue filter that filters queue entries based on the attributes specified when
 * constructing this class. Only queue entries matching the attributes will be 
 * returned.
 * @author Claes Buckwalter (clabu@itn.liu.se)
 */
public class AttributeQueueFilter implements QueueFilter {
    
    private SortingQueueFilter _sortingFilter;
    private JDFAttributeMap _attrMap;
    
    /**
     * Constructs an attribute filter
     * @param attrMap   a map of the attributes to filter
     */
    public AttributeQueueFilter(JDFAttributeMap attrMap) {
        _sortingFilter = new SortingQueueFilter();
        _attrMap = attrMap;
    }
     
    /**
     * Returns a sorted queue with queue entries that match the attributes that were
     * specified when constructing this class. 
     * @param queue the queue to filter
     * @param filter    the filter will be ignored, it may be <code>null</code>
     * @return a queue with sorted queue entries
     */
    public JDFQueue filterQueue(JDFQueue queue, JDFQueueFilter filter) {
        // Filters the queue        
        List qEntries = queue.getChildElementVector(ElementName.QUEUEENTRY, JDFConstants.NONAMESPACE, _attrMap, true, 0, false);
        // Creates a copy of the original queue
        queue = (JDFQueue) queue.cloneNode(false);
        // Copies the filtered queue entries to the new queue
        for(int i=0, imax=qEntries.size(); i<imax; i++) {
            queue.copyElement((JDFQueueEntry)qEntries.get(i), null);
        }
        // Sorts the queue
        return _sortingFilter.filterQueue(queue, null, false); 
    }
    
//    /**
//     * Adds all attribute values in a list into a <code>JDFAttributeMap</code> 
//     * @param attrName      the name of the attribute
//     * @param attrValueList the values of the attribute
//     * @param attrMap       the map to put the attributes into
//     * @return 
//     */
//    private void addListToAttributeMap(String attrName, List attrValueList, JDFAttributeMap attrMap) {
//        for(int i=0, imax=attrValueList.size(); i<imax; i++) {
//            attrMap.put(attrName, attrValueList.get(i));
//        }
//    }
}
