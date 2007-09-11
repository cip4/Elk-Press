/*
 * Created on Sep 29, 2004
 */
package org.cip4.elk.impl.jmf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.cip4.elk.ElkEvent;
import org.cip4.elk.ElkEventListener;
import org.cip4.elk.JDFElementFactory;
import org.cip4.elk.device.DeviceConfig;
import org.cip4.elk.device.process.ProcessStatusEvent;
import org.cip4.elk.device.process.ProcessStatusListener;
import org.cip4.elk.jmf.IncomingJMFDispatcher;
import org.cip4.elk.jmf.JMFProcessor;
import org.cip4.elk.jmf.OutgoingJMFDispatcher;
import org.cip4.elk.jmf.SubscriptionManager;
import org.cip4.elk.queue.QueueStatusEvent;
import org.cip4.elk.queue.QueueStatusListener;
import org.cip4.jdflib.core.JDFComment;
import org.cip4.jdflib.core.JDFDoc;
import org.cip4.jdflib.core.KElement;
import org.cip4.jdflib.jmf.JDFJMF;
import org.cip4.jdflib.jmf.JDFMessage;
import org.cip4.jdflib.jmf.JDFNotificationDef;
import org.cip4.jdflib.jmf.JDFQuery;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.jmf.JDFSignal;
import org.cip4.jdflib.jmf.JDFStopPersChParams;
import org.cip4.jdflib.jmf.JDFSubscription;
import org.cip4.jdflib.resource.JDFNotification;
import org.cip4.jdflib.resource.process.JDFNotificationFilter;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;

/**
 * A simple implementation of a subscription manager. It has the following
 * limitations:
 * <ul>
 * <li>Only supports event-based subscriptions. Ignores time-based
 * subscriptions.</li>
 * <li>Only matches an <em>Events</em> subscription's parameters against
 * event classes (<em>NotificationFilter/@Classes</em>)</li>
 * <li>Only one subscription can be registered per URL (
 * <em>Query/Subscription/@URL</em>). If a subscription is registered with a
 * URL that is used by a previously registered subscription, the new
 * subscription will overwrite the old one.</li>
 * </ul>
 * <p>
 * Subscriptions that do not match any of the limitations above will not be
 * registered.
 * </p>
 * 
 * @see <a
 *      href="http://www.cip4.org/documents/jdf_specifications/JDF1.2.pdf">JDF
 *      Specification Release 1.2, 5.2.1.3 Signal </a>
 * @see <a
 *      href="http://www.cip4.org/documents/jdf_specifications/JDF1.2.pdf">JDF
 *      Specification Release 1.2, 5.2.2.3 Persistent Channels </a>
 * @author Claes Buckwalter (clabu@itn.liu.se)
 */
public class SimpleSubscriptionManager implements SubscriptionManager,
        ElkEventListener, QueueStatusListener, ProcessStatusListener {

    public static final String EVENTS_TYPE = JDFMessage.EnumType.Events
            .getName();

    private transient Logger log;

    private transient JDFElementFactory _factory;

    private Map _eventSubscriptions;

    private Map _eventQueryMapping;

    private JDFDoc _ownerDocument;

    private OutgoingJMFDispatcher _outgoingDispatcher;

    private IncomingJMFDispatcher _incomingDispatcher;
    
    // private DeviceConfig _deviceConfig;

    public SimpleSubscriptionManager(OutgoingJMFDispatcher outgoingDispatcher,
            IncomingJMFDispatcher incomingDispatcher, DeviceConfig deviceConfig) {
        this(outgoingDispatcher, incomingDispatcher, deviceConfig, new HashMap());
    }

    public SimpleSubscriptionManager(OutgoingJMFDispatcher outgoingDispatcher,
            IncomingJMFDispatcher incomingDispatcher, DeviceConfig deviceConfig, Map eventQueryMapping) {
        super();
        log = Logger.getLogger(this.getClass().getName());
        _eventSubscriptions = new ConcurrentReaderHashMap();
        _ownerDocument = new JDFDoc("SubscriptionManagerPlaceHolder");
        _outgoingDispatcher = outgoingDispatcher;
        _incomingDispatcher = incomingDispatcher;
        // _deviceConfig = deviceConfig;
        _eventQueryMapping = eventQueryMapping;
        _factory = JDFElementFactory.getInstance();
    }

    /**
     * Not implemented.
     * @return an <code>Collection</code> of size <code>0</code> (zero)
     * @todo Implement this method
     */
    public Collection getSubscriptions() {
        return new ArrayList();
    }
    
    /**
     * Sets the mapping between <code>ElkEvent</code> s and JDF query types.
     * The map's keys must be strings that are fully qualified names of
     * subclasses of <code>ElkEvent</code>; the map's values must be strings
     * that are JMF query types.
     * 
     * @todo Make defensive copy and validate <code>eventQueryMapping</code>
     *       parameter
     * @param eventQueryMapping
     * @throws IllegalArgumentException
     *             if the map's keys are not <code>String</code> s that are
     *             the fully qualified names of subclasses of
     *             <code>ElkEvent</code> and if the map's values are not
     *             <code>String</code>s. No restriction is placed on the
     *             latter since custom JMF query types must be supported.
     * @see com.heidelberg.JDFLib.jmf.JDFMessage.EnumType
     */
    protected synchronized void setEventQueryMapping(Map eventQueryMapping) {
        // TODO Validate and make defensive copy
        if (eventQueryMapping == null) {
            throw new IllegalArgumentException(
                    "eventQueryMapping must not be null");
        }
        _eventQueryMapping = eventQueryMapping;
    }

    /**
     * Tests if the subscriptions to the specified query are supported. For a
     * query to be supported it must be of type <em>Events</em> or must have
     * been registered using {@link #setEventQueryMapping(Map)}.
     * 
     * @param query
     *            the query to test
     * @return <code>true</code> if the query is supported; <code>false</code>
     *         otherwise
     * @see #setEventQueryMapping(Map)
     */
    private boolean isQuerySupported(JDFQuery query) {
        String queryType = query.getType();
        return (_eventQueryMapping.containsValue(queryType) || queryType
                .equals(EVENTS_TYPE));
    }

    /**
     * Test if a <code>ElkEvent</code> has been mapped to a query type. This
     * means that each time the specified <code>ElkEvent</code> is received be
     * this subscription manager a JMF Signal should be sent to all subscribers
     * of the query that the event is mapped to.
     * 
     * @param event
     * @return <code>true</code> if th
     */
//    private boolean isEventSupported(ElkEvent event) {
//        return _eventQueryMapping.containsKey(event.getClass().getName());
//    }

    /**
     * Registers a subscription. This implementation only supports one
     * subscription per URL. In other words, queries that have the same URLs (
     * <em>Query/Subscription/@URL</em>) will overwrite each others
     * subscriptions.
     */
    public boolean registerSubscription(JDFQuery query) {
        log.debug("Registering subscription: " + query);
        boolean subscribed = false;
        if (query != null) {
            JDFSubscription sub = query.getSubscription();
            if (sub != null && isQuerySupported(query)) {
                query = (JDFQuery) _ownerDocument.importNode(query, true);
                // Create a key for the subscription
                Object key = query.getSubscription().getURL();
                _eventSubscriptions.put(key, query);
                subscribed = true;
                log.debug("Registered subscription: " + query);
            }
        }

        if (!subscribed) {
            log.debug("Subscription not registered: " + query);
        }
        return subscribed;
    }

    /**
     * Unregisters the subscription that has the same URL as the specified
     * <em>StopPersChParams/@URL</em>.
     * 
     * @see #registerSubscription(JDFQuery)
     */
    public int unregisterSubscription(JDFStopPersChParams stopParams) {
        if (stopParams == null) {
            throw new IllegalArgumentException(
                    "JDFStopPersChParams may not be null.");
        }
        Object key = stopParams.getURL();
        // Remove subscription from map
        JDFQuery query = (JDFQuery) _eventSubscriptions.remove(key);
        log.debug("Unregistered subscription: " + query);
        return 0; // XXX Hack. Should return a meaningful return code
    }

    /**
     * Handles events. Delegates to {@link #broadcastEvent(ElkEvent)}.
     * 
     * @see #broadcastEvent(ElkEvent)
     */
    public void eventGenerated(ElkEvent event) {
        log.debug("Received event: " + event);
        broadcastEvent(event);
    }

    /**
     * Handles queue status events. Broadcasts JMF Signals to all subscribers
     * who have subscribed to QueueStatus queries.
     */
    public void queueStatusChanged(QueueStatusEvent queueEvent) {
        log.debug("Received queue status event: " + queueEvent);
        broadcastEvent(queueEvent);
    }

    /**
     * Handles process status events. Broadcasts JMF Signals to all subscribers
     * who have subscribed to Status queries.
     */
    public void processStatusChanged(ProcessStatusEvent processEvent) {
        log.debug("Received process status event: " + processEvent);
        broadcastEvent(processEvent);
    }

    /**
     * Broadcasts the event to all subscribers whose subscriptions match the
     * event.
     * <p>
     * For <em>Events</em> queries, if the event's class is in the
     * subscriptions <em>NotificationFilter/@Classes</em> attribute a signal
     * will be sent to the subscriber. No matching against other
     * NotificationFilter attributes/parameters is performed.
     * </p>
     * 
     * @todo Implement NotificationFilter matching.
     * 
     * @param event
     *            the event to broadcast
     * @see #isEventsSubscriptionAndMatchesEvent(JDFQuery, ElkEvent)
     * @see #doesEventMatchSubscription(ElkEvent, JDFQuery)
     */
    public void broadcastEvent(ElkEvent event) {
        log.debug("Broadcasting signals to subscribers...");
        // For each subscription
        Iterator subscriberUrls = _eventSubscriptions.keySet().iterator();
        while (subscriberUrls.hasNext()) {
            // Get subscriber's URL (map key)
            String url = (String) subscriberUrls.next();
            JDFQuery query = (JDFQuery) _eventSubscriptions.get(url);
            if (isEventsSubscriptionAndMatchesEvent(query, event)) {
                // Events subscription
                sendEventsSignal(event, query);
            } else if (doesEventMatchSubscription(event, query)) {
                // Query subscription
                sendQuerySignal(event, query);
            }
        }
        log.debug("Broadcasted signals to subscribers.");
    }

    /**
     * Sends an <em>Events</em> signal to the subscriber specified in the
     * original query.
     * 
     * @param event
     *            the triggering event
     * @param query
     *            the original query that initiated the subscription
     * @return <code>true</code> if a signal was sent; <code>false</code>
     *         otherwise
     */
    private boolean sendEventsSignal(ElkEvent event, JDFQuery query) {
        log
                .debug("Sending signal to Events subscriber whose original query was: "
                        + query);
        JDFJMF jmf = buildSignal(query, event);
        String url = query.getSubscription().getURL();
        log.debug("Sending signal to " + url + ": " + jmf);
        _outgoingDispatcher.dispatchSignal(jmf, url);
        return true;
    }

    /**
     * Sends a signal to the subscriber specified in the original query. The
     * signal will contain the response of the query.
     * 
     * @param event
     *            the triggering event
     * @param query
     *            the original query that initiated the subscription
     * @return <code>true</code> if a signal was sent; <code>false</code>
     *         otherwise
     */
    private boolean sendQuerySignal(ElkEvent event, JDFQuery query) {
        boolean signalSent = false;
        log.debug("Sending signal to subscriber whose original query was: "
                + query);
        JDFJMF jmf = buildSignal(query, event);
        JDFSignal signal = jmf.getSignal();
        // Create dummy query for JMFProcessor
        JDFQuery dummyQuery = (JDFQuery) query.cloneNode(false);
        // Copy original query's children
        KElement[] queryChildren = query.getChildElementArray();
        for (int i = 0; i < queryChildren.length; i++) {
            if (!(queryChildren[i] instanceof JDFSubscription || queryChildren[i] instanceof JDFComment)) {
                signal.copyElement(queryChildren[i], null);
                dummyQuery.copyElement(queryChildren[i], null);
            }
        }
        // Create dummy response for JMFProcessor
        String queryType = query.getType();
        JDFJMF dummyJmf = _factory.createJMF();
        JDFResponse dummyResponse = dummyJmf.appendResponse();
        dummyResponse.setType(queryType);
        // Send dummies to JMFProcessor
        JMFProcessor processor = _incomingDispatcher.getProcessor(queryType);
        int returnCode = processor.processJMF(dummyQuery, dummyResponse);
        String url = query.getSubscription().getURL();
        if (returnCode == 0) { // Query okay
            signal.copyElement(dummyResponse.getChildElementArray()[0], null);
            //TODO
            // Vsignal.copyElement((KElement)dummyResponse.getChildElementVector(JDFConstants.WILDCARD,
            // JDFConstants.NONAMESPACE, new JDFAttributeMap(), false, 0, false).elementAt(0),
            // null);
            log.debug("Sending signal to " + url + ": " + jmf);
            _outgoingDispatcher.dispatchSignal(jmf, url);
            signalSent = true;
        } else { // Query failed
            log.warn("Could not send signal because the subscription's "
                    + "query could not be executed. Return code was "
                    + returnCode + ". Query was: " + query);
            signalSent = false;
        }        
        return signalSent;
    }

    /**
     * Tests if an event matches a query containing a subscription.
     * 
     * @param event
     * @param query
     * @return <code>true</code> if the event matches the subscription;
     *         <code>false</code> otherwise
     */
    private boolean doesEventMatchSubscription(ElkEvent event, JDFQuery query) {
        boolean match = false;
        Object queryType = _eventQueryMapping.get(event.getClass().getName());
        if (queryType != null && queryType.equals(query.getType())) {
            match = true;
        } else {
            log.warn("Event '" + event.getClass().getName() + "' "
                    + "does not match subscription of query type: "
                    + query.getType());
        }
        return match;
    }

    /**
     * Tests if a subscribed query is an <em>Events</em> query with a
     * <em>Subscription</em> element and if the query's
     * <em>NotificationFilter</em> matches the event.
     * <p>
     * If the event's class ({@link ElkEvent#getEventClass()}) is in the
     * subscribed query's <em>NotificationFilter/@Classes</em> attribute a
     * signal will be sent to the subscriber. No matching against other
     * NotificationFilter attributes/parameters is performed.
     * </p>
     * 
     * @todo Implement <em>NotificationFilter</em> matching.
     * @param subscribedQuery
     * @param event
     * @return <code>true</code> if the query is an <em>Events</em> query
     *         with a subscription that matches the event; <code>false</code>
     *         otherwise
     */
    private boolean isEventsSubscriptionAndMatchesEvent(
            JDFQuery subscribedQuery, ElkEvent event) {
        boolean match = false;
        if (subscribedQuery.getType().equals(EVENTS_TYPE)) {
            JDFNotificationFilter nf = subscribedQuery.getNotificationFilter(0);
            if (nf != null) {
                // Query[@Type="Events"]/NotificationFilter/@Classes
                String eventClass = event.getEventClass().getName();
                if (nf.getAttribute("Classes") == null
                        || nf.getAttribute("Classes").length() == 0
                        || nf.getAttribute("Classes").indexOf(eventClass) != -1) {
                    match = true;
                } else {
                    // TODO Test against the rest of the NotificationFilter
                    log
                            .warn("Ignored Events subscription because the "
                                    + "NotificationFilter parameters are not supported yet. "
                                    + "The original query was: "
                                    + subscribedQuery);
                }
            }
        }
        return match;
    }

    /**
     * Builds a JMF Signal based on the original query and the event that
     * triggered this signal.
     * 
     * @todo Add <em>Signal/NotificationDetails</em> elements
     * @todo Add <em>Signal/Trigger</em> elements
     * @param originalQuery
     *            the subscription's original query
     * @param event
     *            the event the generated this signal
     * @return a JMF Signal
     */
    private JDFJMF buildSignal(JDFQuery originalQuery, ElkEvent event) {
        // Build JMF Signal message
        JDFJMF jmf = _factory.createJMF();
        jmf.init();
        JDFSignal signal = jmf.appendSignal(); // JMF/Signal
        signal.init();
        signal.setType(originalQuery.getType()); // JMF/Signal/@Type
        signal.setrefID(originalQuery.getID()); // JMF/Signal/@refID
        JDFNotification notification = signal.appendNotification(); // JMF/Signal/Notification
        notification.setClass(event.getEventClass()); // JMF/Signal/Notification/@Class
        // TODO Set Type
        // TODO Attach concrete NotificationDetails element
        notification.appendComment().appendText(event.getDescription()); // JMF/Signal/Notification/Comment
        notification.appendComment().appendText(event.toString());
        // JMF/Signal/Trigger
        // TODO Add triggers
        return jmf;
    }

    /**
     * Returns the <em>Notification</em> types known by this subscription
     * manager. This implementation ignores the <em>NotificationFilter</em>.
     * 
     * @todo Interpret <code>JDFNotificationFilter</code> and filter the
     *       returned <code>JDFNotificationDef</code> array
     * @param filter
     * @return an array of the known notification types
     */
    public JDFNotificationDef[] getNotificationDefs(JDFNotificationFilter filter) {
        // TODO Interpret filter
        log.debug("Getting NotificationDefs...");
        String[] supportedQueryTypes = new String[0];
        if (_eventQueryMapping.size() != 0) {
            synchronized (_eventQueryMapping) {
                supportedQueryTypes = (String[]) _eventQueryMapping.values()
                        .toArray(new String[_eventQueryMapping.size()]);
            }
        }
        JDFNotificationDef[] defs = new JDFNotificationDef[supportedQueryTypes.length + 1];
        JDFJMF dummyJmf = _factory.createJMF();
        // Events
        JDFResponse dummyResponse = dummyJmf.appendResponse();
        dummyResponse.setType("Events");
        String classes = "Event Information Warning Error Fatal";
        defs[0] = dummyResponse.appendNotificationDef();
        defs[0].setAttribute("Classes", classes);
        defs[0].setSignalType("Notification");
        // Other query types
        for (int i = 0; i < supportedQueryTypes.length; i++) {
            defs[i + 1] = dummyResponse.appendNotificationDef();
            defs[i + 1].setSignalType(supportedQueryTypes[i]);
        }
        log.debug("Returning " + defs.length + " NotificationDefs.");
        return defs;
    }
}

