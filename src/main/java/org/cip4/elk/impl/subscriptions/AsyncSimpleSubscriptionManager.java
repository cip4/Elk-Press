/*
 * Created on Sep 29, 2004
 */
package org.cip4.elk.impl.subscriptions;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cip4.elk.ElkEvent;
import org.cip4.elk.ElkEventListener;
import org.cip4.elk.device.DeviceConfig;
import org.cip4.elk.device.process.ProcessAmountEvent;
import org.cip4.elk.device.process.ProcessAmountListener;
import org.cip4.elk.device.process.ProcessStatusEvent;
import org.cip4.elk.device.process.ProcessStatusListener;
import org.cip4.elk.impl.jmf.util.Messages;
import org.cip4.elk.jmf.IncomingJMFDispatcher;
import org.cip4.elk.jmf.JMFProcessor;
import org.cip4.elk.jmf.OutgoingJMFDispatcher;
import org.cip4.elk.jmf.Subscription;
import org.cip4.elk.jmf.SubscriptionManager;
import org.cip4.elk.lifecycle.Lifecycle;
import org.cip4.elk.queue.QueueStatusEvent;
import org.cip4.elk.queue.QueueStatusListener;
import org.cip4.jdflib.core.JDFComment;
import org.cip4.jdflib.core.KElement;
import org.cip4.jdflib.jmf.JDFJMF;
import org.cip4.jdflib.jmf.JDFMessage;
import org.cip4.jdflib.jmf.JDFNotificationDef;
import org.cip4.jdflib.jmf.JDFQuery;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.jmf.JDFSignal;
import org.cip4.jdflib.jmf.JDFStopPersChParams;
import org.cip4.jdflib.jmf.JDFSubscription;
import org.cip4.jdflib.resource.process.JDFNotificationFilter;

import EDU.oswego.cs.dl.util.concurrent.Executor;
import EDU.oswego.cs.dl.util.concurrent.QueuedExecutor;

/**
 * <p>
 * A simple implementation of a subscription manager. It handles the
 * requirements of Base ICS level 3.
 * </p>
 * <p>
 * Explanation of constructor arguments:
 * </p>
 * <p>
 * The argument <code>eventQueryMapping</code> is a {@link java.util.Map}
 * which are classes that implements events that this class will listen to. For
 * example <code>QueueStatusEvents</code>,<code>ProcessStatusEvents</code>
 * and <code>ProcessAmountEvents</code>, and thus the SubscriptionManager
 * implement the corresponding interfaces. An example of a Mapping from a Spring
 * configuration file would look like this:
 * </p>
 * 
 * <pre>
 *      &lt;map&gt;
 *             &lt;entry key=&quot;org.cip4.elk.queue.QueueStatusEvent&quot;&gt;
 *                 &lt;value&gt;QueueStatus&lt;/value&gt;
 *             &lt;/entry&gt;
 *             &lt;entry key=&quot;org.cip4.elk.device.process.ProcessStatusEvent&quot;&gt;
 *                 &lt;value&gt;Status&lt;/value&gt;
 *             &lt;/entry&gt;                     
 *             &lt;entry key=&quot;org.cip4.elk.device.process.ProcessAmountEvent&quot;&gt;
 *                 &lt;value&gt;Amount&lt;/value&gt;
 *             &lt;/entry&gt;
 *      &lt;/map&gt;
 * </pre>
 * 
 * <p>
 * The argument <code>knownQueryTypes</code> is a {@link java.util.List}of
 * the <em>Queries</em> that can be subscribed. What this manager will do is
 * simply to remove the <em>Subscription</em> element of the subscribing
 * <em>Query</em> and resend it at the interval given in the
 * <em>Subscription</em> element of the <em>Query</em> that is being
 * registered. Therefore, if implementing new
 * {@link org.cip4.elk.jmf.JMFProcessor}s that handles <em>Query</em>
 * messages, the new message can simply be added to the list.
 * </p>
 * <p>
 * This implementation of the <code>SubscriptionManager</code> uses <a
 * href="http://mule.codehaus.org/">Mule </a> to accomplish asynchronous
 * messaging. For the manager to work asynchronously the
 * <code>EventForwarder</code> needs to be configured and listen to the
 * channel 'vm://subsChannel'.
 * </p>
 * <p>
 * This manager can also be used in synchronous mode by using the
 * {@link #setAsynchronous(boolean)}method and set it to <code>false</code>
 * and the the EventFowarder does not need to be configured (it won't be used).
 * This may however in some cases cause Elk to deadlock.
 * </p>
 * This class is Thread safe if its SubscriptionContainer is Thread safe.
 * 
 * @see <a
 *      href="http://www.cip4.org/documents/jdf_specifications/JDF1.2.pdf">JDF
 *      Specification Release 1.2, 5.2.1.3 Signal </a>
 * @see <a
 *      href="http://www.cip4.org/documents/jdf_specifications/JDF1.2.pdf">JDF
 *      Specification Release 1.2, 5.2.2.3 Persistent Channels </a>
 * @author Claes Buckwalter (clabu@itn.liu.se)
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: AsyncSimpleSubscriptionManager.java,v 1.9 2006/08/15 09:09:06 prosi Exp $
 */
public class AsyncSimpleSubscriptionManager implements SubscriptionManager,
        ElkEventListener, QueueStatusListener, ProcessStatusListener,
        ProcessAmountListener, TimeEventListener, Lifecycle {

    public final String EVENTS_TYPE = JDFMessage.EnumType.Events.getName();
    private transient Logger log;
    private SubscriptionContainer _subscriptions;
    private BaseICSSubscriber _subscriber;
    private BaseICSUnsubscriber _unsubscriber;
    private Map _eventQueryMapping;
    private Vector _knownQueryTypes;
    private boolean _async = true;
    private Executor _executor = null;

    private OutgoingJMFDispatcher _outgoingDispatcher;
    private IncomingJMFDispatcher _incomingDispatcher;
    private DeviceConfig _deviceConfig;

    /**
     * Creates a <code>SubscriptionManager</code> that per default delivers
     * broadcasts <em>Signals</em> to subscribers asynchronously.
     * 
     * @param outgoingDispatcher
     * @param incomingDispatcher
     * @param deviceConfig
     */
    public AsyncSimpleSubscriptionManager(
            OutgoingJMFDispatcher outgoingDispatcher, DeviceConfig deviceConfig) {
        this(outgoingDispatcher, deviceConfig, new HashMap(), null);
    }

    /**
     * Creates a <code>SubscriptionManager</code> that per default delivers
     * broadcasts <em>Signals</em> to subscribers asynchronously.
     * 
     * @param outgoingDispatcher
     * @param deviceConfig
     * @param eventQueryMapping
     * @param knownQueryTypes
     */
    public AsyncSimpleSubscriptionManager(
            OutgoingJMFDispatcher outgoingDispatcher,
            DeviceConfig deviceConfig, Map eventQueryMapping,
            String[] knownQueryTypes) {
        super();
        log = Logger.getLogger(this.getClass().getName());
        _subscriptions = new SubscriptionContainer();
        if (knownQueryTypes != null) {
            _knownQueryTypes = new Vector(Arrays.asList(knownQueryTypes));
        } else {
            _knownQueryTypes = new Vector();
        }
        _outgoingDispatcher = outgoingDispatcher;
        _deviceConfig = deviceConfig;
        _eventQueryMapping = eventQueryMapping;
        _subscriber = new BaseICSSubscriber(_deviceConfig.getID(), this);
        _unsubscriber = new BaseICSUnsubscriber(_deviceConfig.getID());
        _executor = new QueuedExecutor();
        log.debug("Created subscription manager with knownQueries "
                + _knownQueryTypes + " and known " + "Events "
                + _eventQueryMapping.values());

    }

    public Collection getSubscriptions() {
        return _subscriptions.getSubscriptions();
    }    
    
    public void setIncomingDispatcher(IncomingJMFDispatcher incomingDispatcher) {
        _incomingDispatcher = incomingDispatcher;
    }

    /**
     * Sets whether the subscription manager should act asynchronously.
     * 
     * @param asynchronous The mode to be set (<code>true</code> for
     *            asynchronous, <code>false</code> for synchronous)
     */
    public void setAsynchronous(boolean asynchronous) {
        _async = asynchronous;
    }

    /**
     * Returns <code>true</code> if this manager is Asynchronous,
     * <code>false</code> otherwise.
     * 
     * @return <code>true</code> if this manager is Asynchronous,
     *         <code>false</code> otherwise.
     */
    public boolean isAsynchronous() {
        return _async;
    }

    /**
     * Returns <code>true</code> if this subscription manager handles
     * subscriptions of <em>Query/@Type</em>, false otherwise. The known
     * queries can be set using the {@link #setKnownQueryTypes(String[])}
     * method.
     * 
     * @param query The incoming query.
     * @return <code>true</code> if this subscription manager handles
     *         subscriptions of <em>Query/@Type</em>, false otherwise.
     */
    public boolean isKnownQuery(JDFQuery query) {
        return _knownQueryTypes.contains(query.getType());
    }

    /**
     * Sets which <em>Query/@Type</em> queries that can be subscribed, and
     * clears any previous values.
     * 
     * @param queryTypes The list of Strings that represents the query types
     *            that can be subscribed for this subscription manager.
     */
    public synchronized void setKnownQueryTypes(String[] queryTypes) {
        _knownQueryTypes.clear();
        if (queryTypes != null) {
            _knownQueryTypes.addAll(Arrays.asList(queryTypes));
            log.debug("Queries that can be subscribed has been set to "
                    + _knownQueryTypes);
        } else {
            log.warn("Tried to set known Queries for this subscription"
                    + " manager but parameter was null, known Queries are now"
                    + " [] and known Event Queries are "
                    + _eventQueryMapping.values());
        }

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
     * @throws IllegalArgumentException if the map's keys are not
     *             <code>String</code> s that are the fully qualified names of
     *             subclasses of <code>ElkEvent</code> and if the map's values
     *             are not <code>String</code>s. No restriction is placed on
     *             the latter since custom JMF query types must be supported.
     * @see org.cip4.jdflib.jmf.JDFMessage.EnumType
     */
    public synchronized void setEventQueryMapping(Map eventQueryMapping) {
        // TODO Validate and make defensive copy
        if (eventQueryMapping == null) {
            throw new IllegalArgumentException(
                    "eventQueryMapping must not be null");
        }
        _eventQueryMapping = eventQueryMapping;
    }

    /**
     * Tests if the subscriptions to the specified query are supported. For a
     * query to be supported it be registered using (@link
     * #setKnownQueryTypes(String[])) or must have been registered using
     * {@link #setEventQueryMapping(Map)}.
     * 
     * @param query the query to test
     * @return <code>true</code> if the query is supported; <code>false</code>
     *         otherwise
     * @see #setEventQueryMapping(Map)
     */
    private boolean isQuerySupported(JDFQuery query) {
        String queryType = query.getType();

        if (_eventQueryMapping.containsValue(queryType) || isKnownQuery(query)) {
            return true;
        } else {
            log.warn("Queries of type " + queryType
                    + " are not supported. Allowed types are "
                    + _eventQueryMapping.values() + " and " + _knownQueryTypes);
            return false;
        }
    }

    /**
     * Test if a <code>ElkEvent</code> has been mapped to a query type. This
     * means that each time the specified <code>ElkEvent</code> is received by
     * this subscription manager a JMF Signal should be sent to all subscribers
     * of the query that the event is mapped to.
     * 
     * @param event
     * @return <code>true</code> if the event is mapped, <code>false</code>
     *         otherwise
     */
    //private boolean isEventSupported(ElkEvent event) {
    //    return _eventQueryMapping.containsKey(event.getClass().getName());
    //}

    /**
     * Registers a subscription.
     * 
     * {@link BaseICSSubscriber#subscribe(SubscriptionContainer, JDFQuery)} for
     * details.
     * 
     * @param query the original query containing the Subscription element.
     * @return <code>true</code> if subscription was successful,
     *         <code>false</code> otherwise
     */
    public boolean registerSubscription(JDFQuery query) {
        log.debug("About to register subscription: " + query);
        boolean subscribed = false;

        if (isQuerySupported(query)
                && _subscriber.subscribe(_subscriptions, query) == 0) {
            subscribed = true;
            log.debug("Subscription registered successfully");

        } else {
            subscribed = false;
            log.debug("Subscription not registered: " + query.getID());
        }

        return subscribed;
    }

    /**
     * Unregisters a subscription.
     * 
     * @see org.cip4.elk.impl.subscriptions.BaseICSUnsubscriber#unsubscribe(SubscriptionContainer,
     *      JDFStopPersChParams)
     * @see #registerSubscription(JDFQuery)
     * @see org.cip4.elk.jmf.SubscriptionManager#unregisterSubscription(JDFStopPersChParams)
     * @return 0 on success, otherwise error code according to spec.
     * @throws IllegalArgumentException if stopParams ==<code>null</code>
     * 
     */
    public int unregisterSubscription(JDFStopPersChParams stopParams) {
        log.debug("About to unregister subscription with: " + stopParams);
        int returnCode = _unsubscriber.unsubscribe(_subscriptions, stopParams);
        return returnCode;
    }

    /**
     * Handles events. Delegates to {@link #syncBroadcastEvent(ElkEvent)}.
     * 
     * @see #syncBroadcastEvent(ElkEvent)
     */
    public void eventGenerated(ElkEvent event) {
        log.debug("Received event: " + event);
        broadcastEvent(event);
    }

    /**
     * Handles queue status events. Broadcasts JMF <em>Signals</em> to all
     * subscribers who have subscribed to <em>QueueStatus</em> queries.
     */
    public void queueStatusChanged(QueueStatusEvent queueEvent) {
        log.debug("Received queue status event: " + queueEvent);
        broadcastEvent(queueEvent);
    }

    /**
     * Handles process status events. Broadcasts JMF Signals to all subscribers
     * who have subscribed to <em>Status</em> queries.
     */
    public void processStatusChanged(ProcessStatusEvent processEvent) {
        log.debug("Received process status event: " + processEvent);
        broadcastEvent(processEvent);
    }

    /**
     * Handles process Amount events. Broadcasts JMF Signals to all subscribers
     * who have subscribed had Subscription/@RepeatStep attribute set.
     * 
     * NOTE: Not all processes have an Amount, this event may be unsupported.
     * 
     * @see org.cip4.elk.device.process.ProcessAmountListener#processAmountChanged(org.cip4.elk.device.process.ProcessAmountEvent)
     */
    public void processAmountChanged(ProcessAmountEvent processAmountEvent) {
        log.debug("Received process amount event: " + processAmountEvent + "");
        broadcastEvent(processAmountEvent);
    }

    /**
     * Broadcasts the event to all subscribers whose subscriptions match the
     * event. Events are delivered to subscribers synchronously or
     * asynchronously, depending on how this <code>SubscriptionManager</code>
     * has been configured.
     * 
     * @param event the <code>ElkEvent</code> to broadcast
     * @see AsyncSimpleSubscriptionManager#isAsynchronous()
     * @see AsyncSimpleSubscriptionManager#setAsynchronous(boolean)
     * @see AsyncSimpleSubscriptionManager#syncBroadcastEvent(ElkEvent)
     * @see AsyncSimpleSubscriptionManager#asyncBroadcastEvent(ElkEvent)
     */
    public void broadcastEvent(ElkEvent event) {
        if (_async) {
            log.debug("Broadcasting event (" + event.getEventClass().getName()
                    + ") asynchronously...");
            asyncBroadcastEvent(event);
        } else {
            log.debug("Broadcasting event (" + event.getEventClass().getName()
                    + ") synchronously...");
            syncBroadcastEvent(event);
            log.debug("Done broadcasted event ("
                    + event.getEventClass().getName() + ") synchronously.");
        }
    }

    /**
     * Broadcasts an event asynchronously. A single thread broadcast all events
     * so events will be delivered to subscribers in the order that they are
     * broadcasted.
     * 
     * @param event
     */
    public void asyncBroadcastEvent(final ElkEvent event) {

        try {
            _executor.execute(new Runnable() {
                public void run() {
                    syncBroadcastEvent(event);
                    log.debug("Done broadcasting event ("
                            + event.getEventClass().getName()
                            + ") asynchronously.");
                }
            });
            log.debug("Dispatched event " + event.getEventClass().getName()
                    + ") for asynchronous broadcasting.");
        } catch (InterruptedException ie) {
            log
                    .error("Could not broadcast event ("
                            + event.getEventClass().getName()
                            + ") asynchronously.", ie);
        }
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
     * @param event the event to broadcast
     * @see #isEventsSubscriptionAndMatchesEvent(JDFQuery, ElkEvent)
     * @see #doesEventMatchSubscription(ElkEvent, JDFQuery)
     */
    public void syncBroadcastEvent(ElkEvent event) {
        log.debug("Broadcasting signals to subscribers...");
        // For each subscription
        Collection c = _subscriptions.getSubscriptions();
        for (Iterator it = c.iterator(); it.hasNext();) {
            Subscription subElement = (Subscription) it.next();
            JDFQuery query = subElement.getQuery();

            if (isEventsSubscriptionAndMatchesEvent(query, event)) {
                // Events subscription
                log.debug("A Query[@Type='Events'] subscription is sent");
                sendEventsSignal(event, query);
            } else if (event instanceof ProcessAmountEvent
                    && subElement.isRepeatStepSubscription()) {
                // Special case if the Event is an Amount event.
                int amount = ((ProcessAmountEvent) event).getAmount();
                int repeatStep = subElement.getRepeatStep();
                if (amount % repeatStep == 0) {
                    log.debug("Sending a RepeatStep signal");
                    sendQuerySignal(event, query);
                    // TODO Testing.
                } else {
                    // TODO Maybe remove this debugging comment
                    // Can be A LOT of debug comments.
                    log.debug("Did not send event: Amount (" + amount
                            + ") is not divisible by RepeatStep(" + repeatStep
                            + ").");
                }
            } else if (doesEventMatchSubscription(event, query)) {
                // Query subscription which is not an Amount event.
                sendQuerySignal(event, query);
                log.debug("Sending an Events subscription");
            } else {
                log.debug("No subScriptions registered for events of type "
                        + event + ".");
            }

        }
    }

    /**
     * Sends an <em>Events</em> signal to the subscriber specified in the
     * original query.
     * 
     * @param event the triggering event
     * @param query the original query that initiated the subscription
     * @return <code>true</code> if a signal was sent; <code>false</code>
     *         otherwise
     */
    private boolean sendEventsSignal(ElkEvent event, JDFQuery query) {
        log.debug("About to send an Events Signal whose "
                + "original query was: " + query);

        JDFSignal signal = Messages.buildSignal(query, event);
        JDFJMF jmf = Messages.createJMFMessage(signal, _deviceConfig.getID());
        String url = query.getSubscription().getURL();
        log.debug("Sending Signal to \"" + url + "\" for JMF with id \""
                + jmf.getID() + "\"");
        _outgoingDispatcher.dispatchSignal(jmf, url);
        return true;
    }

    /**
     * Sends a signal to the subscriber specified in the original query. The
     * signal will contain the response of the query.
     * 
     * @param event the triggering event
     * @param query the original query that initiated the subscription
     * @return <code>true</code> if a signal was sent; <code>false</code>
     *         otherwise
     */
    private boolean sendQuerySignal(ElkEvent event, JDFQuery query) {
        boolean signalSent = false;

        JDFSignal signal = Messages.buildSignal(query, event);
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

        JDFResponse dummyResponse = Messages.createJMFMessage(
            Messages.createResponse("dummy", queryType), "dummySenderID")
                .getResponse();

        // Send dummies to JMFProcessor
        JMFProcessor processor = _incomingDispatcher.getProcessor(queryType);

        int returnCode = processor.processJMF(dummyQuery, dummyResponse);
        String url = query.getSubscription().getURL();
        if (returnCode == 0) { // Query okay
            signal.copyElement(dummyResponse.getChildElementArray()[0], null);
            // TODO
            // Vsignal.copyElement((KElement)dummyResponse.getChildElementVector(JDFConstants.WILDCARD,
            // JDFConstants.NONAMESPACE, null, false, 0, false).elementAt(0),
            // null);
            JDFJMF jmf = Messages.createJMFMessage(signal, _deviceConfig
                    .getID());
            log.debug("Sending signal to '" + url + "': " + jmf);
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
    private synchronized boolean doesEventMatchSubscription(ElkEvent event,
            JDFQuery query) {
        boolean match = false;
        Object queryType = _eventQueryMapping.get(event.getClass().getName());
        if (queryType != null && queryType.equals(query.getType())) {
            match = true;
        } else {
            // log.debug("Event '" + event.getClass().getName() + "' "
            // + "does not match subscription of query type: "
            // + query.getType());
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
                    log.warn("Ignored Events subscription because the "
                            + "NotificationFilter parameters are not "
                            + "supported yet. The original query was: "
                            + subscribedQuery);
                }
            }
        }
        return match;
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
        // Events
        // NOTE: this is an awkward way to do it, but there is trouble with
        // the owner document if you only create a dummy Query.
        JDFResponse dummyResponse = Messages.createResponse("dummy", "Events");
        JDFJMF dummyJmf = Messages.createJMFMessage(dummyResponse,
            "dummySenderID");
        dummyResponse = dummyJmf.getResponse();

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

    /**
     * Handles a time-triggered event. The event contains its original query
     * where the Subscription/@TimeRepeat element was set. This method re-sends
     * the query and sends the response as a <em>Signal</em> to the
     * <em>Subscription/@URL</em>.
     * 
     * @see org.cip4.elk.impl.subscriptions.TimeEventListener#timeTriggered(TimeEvent)
     * @param event The event that was triggered
     */
    public void timeTriggered(TimeEvent event) {
        log.debug("Received time-triggered event");
        JDFQuery query = event.getQuery();
        sendQuerySignal(null, query);
    }

    /**
     * Initializes this subscription manager.
     * 
     * @see org.cip4.elk.lifecycle.Lifecycle
     */
    public void init() {
    }

    /**
     * Destroys this subscription manager. Cleans up all subscriptions and
     * prevents any new events on the queue from being broadcasted.
     * 
     * @see org.cip4.elk.lifecycle.Lifecycle
     */
    public void destroy() {
        log.debug("Destroying subscription manager...");
        _subscriptions.cleanUp();
        if (_executor instanceof QueuedExecutor) {
            ((QueuedExecutor) _executor).shutdownAfterProcessingCurrentTask();
        }
        log.debug("Destroyed subscription manager.");
    }
}
