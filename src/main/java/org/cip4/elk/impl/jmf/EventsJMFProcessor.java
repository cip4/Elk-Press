/*
 * Created on Sep 19, 2004
 */
package org.cip4.elk.impl.jmf;

import org.cip4.elk.jmf.SubscriptionManager;
import org.cip4.jdflib.jmf.JDFMessage;
import org.cip4.jdflib.jmf.JDFNotificationDef;
import org.cip4.jdflib.jmf.JDFQuery;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.resource.process.JDFNotificationFilter;

/**
 * A processor for QueueStatus queries.
 * 
 * @author Claes Buckwalter (clabu@itn.liu.se)
 * @version $Id: EventsJMFProcessor.java,v 1.4 2005/05/06 16:49:30 buckwalter Exp $
 */
public class EventsJMFProcessor extends AbstractJMFProcessor {

    private static final String MESSAGE_TYPE = "Events";       
    private SubscriptionManager _subscriptions;
    
    public EventsJMFProcessor(SubscriptionManager subscriptions) {
        super();
        _subscriptions = subscriptions;
        setMessageType(MESSAGE_TYPE);
        setQueryProcessor(true);
    }
    
    /* (non-Javadoc)
     * @see org.cip4.elk.jmf.JMFProcessor#processJMF(com.heidelberg.JDFLib.jmf.JDFMessage, com.heidelberg.JDFLib.jmf.JDFResponse)
     */
    public int processMessage(JDFMessage input, JDFResponse output) {
        return processEvents((JDFQuery)input, output);
    }

    /**
     * Processes QueueStatus queries.
     * @param query
     * @param response
     * @return
     */
    private int processEvents(JDFQuery query, JDFResponse response) {
        int returnCode = 0;
        JDFNotificationFilter filter = query.getNotificationFilter(0);
        JDFNotificationDef[] notDefs = _subscriptions.getNotificationDefs(filter);
        for (int i=0; i<notDefs.length; i++) {
            response.copyElement(notDefs[i], null);
        }
        response.setReturnCode(returnCode);
        return returnCode;
    }   
}
