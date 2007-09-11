/*
 * Created on 2005-apr-29
 */
package org.cip4.elk.impl.subscriptions;

import org.cip4.elk.ElkEvent;
import org.cip4.jdflib.jmf.JDFQuery;
import org.cip4.jdflib.resource.JDFNotification;

/**
 * An event that is generated with a certain time interval.
 * 
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: TimeEvent.java,v 1.1 2005/05/27 19:27:33 ola.stering Exp $
 */
public class TimeEvent extends ElkEvent {

    JDFQuery _query;

    /**
     * Creates a <code>TimeEvent</code> where the 'source' is the class that
     * generated this event and the Query is the <em>Query</em> that contained
     * a <em>Subscription/@TimeRepeat</em> attribute.
     * 
     * @param source is the class that generated this event
     * @param query the <em>Query</em> that contained a
     *            <em>Subscription/@TimeRepeat</em> attribute.
     */
    public TimeEvent(Object source, JDFQuery query) {
        super(JDFNotification.EnumClass.Event, source, "Time-triggered event");
        _query = query;
    }

    /**
     * Returns this <code>TimeEvent</code>'s <em>Query</em>.
     * 
     * @return this <code>TimeEvent</code>'s <em>Query</em>.
     */
    public JDFQuery getQuery() {
        return _query;
    }

}
