/*
 * Created on 2005-mar-23
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cip4.elk.impl.subscriptions;

import java.util.Timer;

import org.apache.log4j.Logger;
import org.cip4.elk.jmf.Subscription;
import org.cip4.jdflib.jmf.JDFQuery;

/**
 * Implements an internal representation of a subscription. The class is used to
 * store information about a subscription. A <code>Subscription</code> element
 * consists of:
 * <ul>
 * <li>The original <em>Query</em> that contained the <em>Subscription</em>
 * element (or was of <em>Query/@Type='Event'</em>).
 * <li>An URL to which the <em>Signal</em> containing the body of the
 * <em>Response</em> of the original <em>Query</em> is sent. This should
 * also be the same as the <em>Query/Subscription/@URL</em>.
 * <li>A {@link java.util.Timer}if the <em>Subscription</em> contained a
 * <em>TimeRepeat</em> attribute.</li>
 * </ul>
 * NOTE: The <em>Query/@ID</em> is the same as the {@link SubscriptionImpl#getId()}which is
 * also the same as the channelID.
 * 
 * @version $Id$
 * @author Ola Stering, olst6875@student.uu.se
 */
public class SubscriptionImpl implements Subscription {

    private static Logger log;
    
    private String _url;

    private String _deviceID;

    private JDFQuery _query;

    private Timer _timer = null;

    private String _type;

    private String _channelID;

    private String _messageType;

    private int _repeatStep;

    public String toString() {
        return "[ChannelID: " + _channelID + " Query type: " + _messageType
                + " " + _type + " URL: " + _url + " deviceID " + _deviceID
                + "]";

    }

    /**
     * Creates a <code>Subscription</code> element.
     * 
     * @param deviceID The if of the <em>Device</em> that this <em>Query</em>
     *            was sent.
     * @param q A <em>Query</em> containing a <em>Subscription</em> element.
     * @param url The url to which the <em>Signal</em>s should be sent.
     */
    public SubscriptionImpl(String deviceID, JDFQuery q, String url) {
        log = Logger.getLogger(this.getClass().getName());
        _query = q;
        if (q == null){
            String msg = "Incoming Query may not be null."; 
            log.error(msg);
            throw new NullPointerException(msg);
        }

        _channelID = q.getID();
        _messageType = q.getType();
        _url = url;
        _deviceID = deviceID;
        _type = EVENT_TYPE; // Default

    }

    /* (non-Javadoc)
     * @see org.cip4.elk.impl.subscriptions.SubscriptionIF#getMessageType()
     */
    public String getMessageType() {
        return _messageType;
    }

    /* (non-Javadoc)
     * @see org.cip4.elk.impl.subscriptions.SubscriptionIF#getType()
     */
    public String getType() {
        return _type;
    }

    /**
     * Sets the type of the subscription.
     * 
     * @see #getType()
     * @param type the type. use {@link SubscriptionImpl#EVENT_TYPE}and
     *            {@link SubscriptionImpl#TIME_TYPE}to set this value.
     * @throws IllegalArgumentException if type is not
     *             {@link SubscriptionImpl#EVENT_TYPE}or
     *             {@link SubscriptionImpl#TIME_TYPE}.
     */
    public void setType(String type) {
        if (EVENT_TYPE.equals(type) || TIME_TYPE.equals(type)){
            _type = type;
        } else {
            throw new IllegalArgumentException("type must be " + TIME_TYPE
                    + " or " + EVENT_TYPE);
        }
    }

    /* (non-Javadoc)
     * @see org.cip4.elk.impl.subscriptions.SubscriptionIF#getQuery()
     */
    public JDFQuery getQuery() {
        return _query;
    }

    /**
     * Sets a {@link java.util.Timer} for this <code>Subscription</code>.
     * 
     * @param t the {@link java.util.Timer} to be set.
     */
    public void setTimer(Timer t) {
        _timer = t;
    }

    /**
     * Cancel this <code>Subscription</code>'s Timer.
     */
    public void cancelTimer() {
        if (_timer != null)
            _timer.cancel();
    }

    /* (non-Javadoc)
     * @see org.cip4.elk.impl.subscriptions.SubscriptionIF#getId()
     */
    public String getId() {

        return _channelID;
    }

    /* (non-Javadoc)
     * @see org.cip4.elk.impl.subscriptions.SubscriptionIF#getUrl()
     */
    public String getUrl() {
        return _url;
    }

    /**
     * Sets the <em>RepeatStep</em> attribute of this
     * <code>Subscription</code>.
     * 
     * @param repeatStep
     */
    public void setRepeatStep(int repeatStep) {
        _repeatStep = repeatStep;
    }

    /* (non-Javadoc)
     * @see org.cip4.elk.impl.subscriptions.SubscriptionIF#isRepeatStepSubscription()
     */
    public boolean isRepeatStepSubscription() {
        return _repeatStep != 0;
    }

    /* (non-Javadoc)
     * @see org.cip4.elk.impl.subscriptions.SubscriptionIF#getRepeatStep()
     */
    public int getRepeatStep() {
        return _repeatStep;
    }

}
