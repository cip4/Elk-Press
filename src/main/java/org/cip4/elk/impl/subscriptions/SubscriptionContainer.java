/*
 * Created on 2005-apr-27
 */
package org.cip4.elk.impl.subscriptions;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cip4.elk.jmf.Subscription;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;

/**
 * A class for manage {@link org.cip4.elk.impl.subscriptions.SubscriptionImpl}
 * elements.
 * 
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: SubscriptionContainer.java,v 1.4 2005/11/08 15:59:00 buckwalter Exp $
 */
public class SubscriptionContainer {

    private Map _subscriptions;

    private static Logger log;

    public SubscriptionContainer() {
        log = Logger.getLogger(this.getClass().getName());
        _subscriptions = new ConcurrentReaderHashMap();
        log.debug("Subscriptions element created");
    }

    /**
     * Adds a <code>Subscription</code> to this
     * <code>SubscriptionContainer</code>. If a <code>Subscription</code>
     * with the the the same url and the the same id (ChannelID) is added, the
     * old <code>Subscription</code> will be overwritten.
     * 
     * @param subscription the <code>Subscription</code> to be added.
     * @return 0 on success, 6 otherwise.
     * @throws NullPointerException if subscription is <code>null</code>.
     * @throws IllegalStateException if the internal representation of the
     *             <code>SubcriptionContainer</code> does not contain objects
     *             of type <code>java.util.Map</code> or its private field
     *             _subscriptions is <code>null</code>
     */
    public synchronized int addSubscription(Subscription subscription) {

        if (_subscriptions == null) {
            String msg = "_subscriptions element may not be null";
            log.error(msg);
            throw new IllegalStateException(msg);
        }
        String url = subscription.getUrl();
        if (url == null || url.equals("")) {
            log.debug("url was '', no subscription has been made");
            return 6;
        }

        Object oldURL = _subscriptions.get(url);
        String channelID = subscription.getId();

        if (oldURL == null) { // No other subscriptions at this URL
            log.debug("No previous subscriptions at " + url);

            Map quMap = new ConcurrentReaderHashMap();

            quMap.put(channelID, subscription);
            _subscriptions.put(url, quMap);
        } else if (oldURL instanceof Map) {
            SubscriptionImpl oldSub = (SubscriptionImpl) ((Map) oldURL).put(channelID,
                subscription);
            if (oldSub != null) {
                oldSub.cancelTimer();
                log.warn("Queries must be unique, old subscription overridden");
                return 6;
            }
        } else {
            String msg = "_subscriptions should contain elements of type Map, not: "
                    + oldURL.getClass().getName();
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        log.debug("Registered subscription with id='" + channelID
                + "' at url '" + url + "'");
        return 0;
    }

    /**
     * Removes the <code>Subscription</code> with the given channelID at the
     * given url. If the channelID does not exist at the given url no action is
     * taken and <code>false</code> is returned.
     * 
     * @param url The url where the <code>Subscription</code> was registered.
     * @param channelID the id of the <code>Subscription</code> to remove.
     * @return <code>true</code> if the <code>Subscription</code> was
     *         removed, <code>false</code> otherwise.
     */
    public synchronized boolean removeSubscription(String url, String channelID) {

        log.debug("Removing subscription '" + channelID + "' at '" + url
                + "' ...");
        Map subscriptionMap = getSubcritptionsMap(url);

        if (subscriptionMap == null) {
            return false; // Invalid parameters
        }

        SubscriptionImpl s = (SubscriptionImpl) subscriptionMap.remove(channelID);
        if (s == null) {
            log.warn("The channelID did not match any subscribing queries"
                    + ", no subscription unregistered");
            return false; // Invalid parameters

        } else {
            s.cancelTimer();
            log.debug("Unregistered subscription with id: " + channelID);
        }
        return true;
    }

    /**
     * Gets the {@link java.util.Map}which contains all
     * <code>Subscription</code> s for a specific url.
     * 
     * @param url the url
     * @return the {@link java.util.Map}which contains all
     *         <code>Subscription</code> s for a specific url.
     * @throws IllegalStateException if the private field _subscription did not
     *             contain a java.util.Map
     */
    private synchronized Map getSubcritptionsMap(String url) {
        String msg;
        Object o = _subscriptions.get(url);
        if (o == null) {
            log.debug("No Subscriptions at URL + " + url
                    + ", no unsubscriptions made");
        }
        if (!(o instanceof Map)) {
            msg = "Objects of _subscriptions must be of type Map";
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        return (Map) o;
    }

    /**
     * Removes all <code>Subscription</code> from the specified url.
     * 
     * @param url the url to remove all subscriptions from.
     * @return 0 if any Subscription were removed, 6 otherwise (the url did not
     *         contain any subscriptions).
     */
    public synchronized int removeSubscriptions(String url) {

        Map subscriptionMap = getSubcritptionsMap(url);
        if (subscriptionMap == null)
            return 6; // Invalid parameters
        for (Iterator it = subscriptionMap.keySet().iterator(); it.hasNext();) {
            SubscriptionImpl s = (SubscriptionImpl) subscriptionMap.remove(it.next());
            s.cancelTimer();
        }
        _subscriptions.remove(url);
        return 0;
    }

    /**
     * Returns <code>true</code> if there are any <code>Subscription</code>
     * s registered for the specified url.
     * 
     * @param url
     * @return <code>true</code> if there are any <code>Subscription</code>
     *         s registered for the specified url, <code>false</code>
     *         otherwise.
     */
    public synchronized boolean hasSubscriptionsAt(String url) {
        return !(_subscriptions.get(url) == null);
    }

    /**
     * Removes all <code>Subscription</code> elements of 'messageType' from
     * the specified 'url'.
     * 
     * @param url
     * @param messageType the <em>Type</em> of the message that was originally
     *            subscribed (i.e. <em>KnownMessages</em> or
     *            <em>QueueStatus</em>)
     * @return 0 if any <code>Subscription</code>s were removed, 6 otherwise.
     */
    public synchronized int removeSubscriptionsOfType(String url,
            String messageType) {

        int returnCode = 0;
        Map subscriptionsMap = getSubcritptionsMap(url);
        if (subscriptionsMap == null) {
            return 6; // Invalid parameters
        }

        for (Iterator it = subscriptionsMap.values().iterator(); it.hasNext();) {
            SubscriptionImpl sub = (SubscriptionImpl) it.next();
            if (sub.getMessageType().equals(messageType)) {
                subscriptionsMap.remove(sub.getId());
                sub.cancelTimer();
                log.debug("Unregistered message " + sub.getId()
                        + " successfully");
            }
        }
        return returnCode;
    }

    /**
     * Returns a {@link java.util.Collection} of all <code>Subscription</code>
     * objects in the <code>SubscriptionContainer</code>.
     * 
     * @return a {@link java.util.Collection}of all <code>Subscription</code>
     *         objects in the <code>SubscriptionContainer</code>.
     */
    public synchronized Collection getSubscriptions() {
        Collection ret = new Vector();
        log.debug("About to return all Subscriptions.");
        if(_subscriptions!=null)
        {
            for (Iterator subscriptionMaps = _subscriptions.values().iterator(); subscriptionMaps
            .hasNext();) {
                Map m = (Map) subscriptionMaps.next();
                ret.addAll(m.values());            
            }
        }
        log.debug("Total number of Subscriptions are " + ret.size());
        return Collections.unmodifiableCollection(ret);
    }

    /**
     * Clears the <code>SubscriptionContainer</code> and cancel all timer
     * threads.
     */
    public synchronized void cleanUp() {
        log.debug("About to destroy subscriptions.");
        Collection c = getSubscriptions();
        if(c!=null)
        {
            SubscriptionImpl s = null;
            for (Iterator it = c.iterator(); it.hasNext();) {
                s = (SubscriptionImpl) it.next();
                s.cancelTimer();
            }
        }
        if(_subscriptions!=null)
            _subscriptions.clear();
        _subscriptions = null;
        log.debug("Destroyed subscriptions.");

    }

}
