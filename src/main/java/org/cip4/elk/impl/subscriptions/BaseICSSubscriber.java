/*
 * Created on 2005-apr-27
 */
package org.cip4.elk.impl.subscriptions;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.cip4.elk.impl.jmf.util.Messages;
import org.cip4.elk.impl.subscriptions.TimeEvent;
import org.cip4.elk.impl.subscriptions.TimeEventListener;
import org.cip4.elk.impl.subscriptions.TimeEventListenerNotifier;
import org.cip4.elk.jmf.Subscription;
import org.cip4.elk.jmf.SubscriptionManager;
import org.cip4.jdflib.jmf.JDFQuery;
import org.cip4.jdflib.jmf.JDFSubscription;

/**
 * Implements a Base ICS level 3 subscriber class. This class implements the
 * semantic meaning the
 * {@link org.cip4.elk.jmf.SubscriptionManager#registerSubscription(JDFQuery)}
 * in its method {@link #subscribe(SubscriptionContainer, JDFQuery)}except that
 * it registers the Query in the <code>SubscriptionContainer</code> and
 * returns a range of error codes instead of just <code>true</code> or
 * <code>false</code>.
 * 
 * 
 * @see <a
 *      href="http://www.cip4.org/document_archive/documents/ICS-Base-1.0.pdf">ICS-Base-1.0
 *      Specification, Table 22 Query - Abstract</a>
 * @see <a
 *      href="http://www.cip4.org/documents/jdf_specifications/JDF1.2.pdf">JDF
 *      Specification Release 1.2, Table 5-13 Contents of the Subscription
 *      Element</a>
 * 
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: BaseICSSubscriber.java,v 1.5 2006/08/15 09:09:05 prosi Exp $
 */
public class BaseICSSubscriber {

    private Logger log;

    private TimeEventListenerNotifier _notifier;

    private String _deviceID;

    /**
     * Creates a subscriber.
     * 
     * @param deviceID The DeviceID of Device that this subscriber belongs to.
     * @param subscriptionManager The SubcriptionManager that handles the
     *            Device's subscriptions. This is also the manager that will be
     *            notified if a time-event is triggered. If this class is not a
     *            <code>TimeEventListener</code> a
     *            <code>ClassCastException</code> will be thrown.
     * 
     * @throws <code>ClassCastException</code> if subscriptionManager does not
     *             implement the <code>TimeEventListener</code> interface.
     */
    public BaseICSSubscriber(String deviceID,
            SubscriptionManager subscriptionManager) {
        _notifier = new TimeEventListenerNotifier();
        _notifier.addListener((TimeEventListener) subscriptionManager);
        _deviceID = deviceID;
        log = Logger.getLogger(this.getClass().getName());
    }

    /**
     * Registers the subscription in the incoming <em>Query</em> if it has a
     * valid <em>Subscription</em> element and its ID is unique for the given
     * URL, if the ID is not the old subscription will be cancelled.
     * 
     * Checks that:
     * <ul>
     * <li><em>Query</em> exists</li>
     * <li><em>Query/Subscription</em> exists</li>
     * <li><em>Subscription/@URL</em> is set and not equal to ''.</li>
     * </ul>
     * <p>
     * If the <em>Subscription/@RepeatTime</em> is set, this class will fire
     * <code>TimeEvent</code> s to this class' subscription manager
     * {@link #BaseICSSubscriber(String, SubscriptionManager)}at the given time
     * interval. The <em>RepeatTime</em> is measured in seconds.
     * </p>
     * <p>
     * If the <em>Subscription/@RepeatStep</em> is set, this method will
     * register the value. It is up to the implementor of a
     * <em>SubscriptionManager</em> to listen to the Device's (Process) Amount
     * and make necessary actions.
     * </p>
     * <p>
     * Other attributes/elements of the <em>Subscription</em> elements are
     * ignored.
     * </p>
     * 
     * @param subscriptions The container of current subscriptions to which the
     *            incomingQuery will be added.
     * @param incomingQuery The <em>Query</em> that contains a
     *            <em>Subscription</em> element and will be subscribed.
     * @return 0 if the Subscription was successful, 6 otherwise.
     * @throws NullPointerException if subscriptions is <code>null</code>
     */

    public int subscribe(SubscriptionContainer subscriptions,
            JDFQuery incomingQuery) {
        int returnCode = 0;

        if (checkIncomingParameters(subscriptions, incomingQuery) != 0) {
            log.warn("Error in incoming parameters, no subscription made");
            return 6;
        }

        JDFSubscription sub = incomingQuery.getSubscription();

        JDFQuery query = (JDFQuery) incomingQuery.cloneNode(true);
        String url = sub.getURL();
        SubscriptionImpl subElement = new SubscriptionImpl(_deviceID, query, url);
        returnCode = subscriptions.addSubscription(subElement);
        if (returnCode == 0) {
            log.debug("Registration was successful, about to check arguments.");

            double period = sub.getRepeatTime();
            if (period != 0.0) { // Time triggered event

                try { // Register time-triggered subscriptions
                    if (period > 0.0 && period < Double.MAX_VALUE / 1000) {
                        Timer timer = new Timer();
                        timer.scheduleAtFixedRate(new TimeTrigger(query), 0,
                            (long) (period * 1000));
                        subElement.setTimer(timer);
                        subElement.setType(Subscription.TIME_TYPE);
                        log.debug("Subscription/@RepeatTime= " + period
                                + " enabled");
                    } else {
                        log.debug("Unable to register time-triggered "
                                + "subscription. Values must be > 0.0 and < "
                                + (Double.MAX_VALUE / 1000)
                                + ". Incoming value: " + period);
                        subscriptions.removeSubscription(url, subElement
                                .getId());
                        returnCode = 6;
                    }
                } catch (Exception e) {
                    log.debug("Unable to register time-triggered "
                            + "subscription for query: " + query);
                    subscriptions.removeSubscription(url, subElement.getId());
                    returnCode = 6;
                }
            }

            // Register RepeatStep subscriptions
            int repeatStep = sub.getRepeatStep();
            if (repeatStep != 0) { // RepeatStep defaults to 0
                if (repeatStep < 0 || repeatStep > Integer.MAX_VALUE) {
                    log.warn("RepeatStep parameter is out of bounds. Values"
                            + " must be > 0 and < " + Integer.MAX_VALUE
                            + ". Incoming value: " + repeatStep);
                    subscriptions.removeSubscription(url, subElement.getId());
                    returnCode = 6;
                } else {
                    log.debug("Registering subscription with RepeatStep: "
                            + repeatStep);
                    subElement.setRepeatStep(repeatStep);
                }
            }
        }

        if (returnCode != 0) {
            log.debug("An error occurred when registering query with id '"
                    + query.getID() + "' to '" + url);
        } else {
            log.debug("Subscription of the original query with id '"
                    + query.getID() + "' to '" + url + "' was successful.");
        }
        return returnCode;

    }

    /**
     * @see #subscribe(Subscriptions, JDFQuery)
     * 
     * @param subscriptions
     * @param incomingQuery
     * @throws NullPointerException if subscriptions is <code>null</code>
     */
    private int checkIncomingParameters(SubscriptionContainer subscriptions,
            JDFQuery incomingQuery) {
        String msg;

        if (subscriptions == null) {
            msg = "Subscriptions element may can not be null.";
            log.error(msg);
            throw new NullPointerException(msg);
        }

        return Messages.checkSubscriptionParameters(log,incomingQuery);
    }

    /**
     * Simple class for implementing the <em>Subscription/@TimeRepeat</em>.
     * Fires a TimeEvent at the given interval. It is up to the implementor of
     * the <code>SubscriptionManager</code> to handle the event.
     * <p>
     * A new instance of the <code>TimeTrigger</code> class is created for
     * each subscription that is time-based.
     * </p>
     * 
     * @author Ola Stering, olst6875@student.uu.se
     */
    public class TimeTrigger extends TimerTask {

        private JDFQuery _q;

        public TimeTrigger(JDFQuery q) {
            super();
            _q = q;
        }

        /**
         * Fires a <code>TimeEvent</code> every time it is invoked.
         */
        public void run() {
            // This notifier is defined in the BaseICSSubscriber class above
            _notifier.fireEvent(new TimeEvent(this, _q));
        }

    }
}
