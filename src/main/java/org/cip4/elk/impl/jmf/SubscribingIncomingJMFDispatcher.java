package org.cip4.elk.impl.jmf;

import java.util.List;

import org.apache.log4j.Logger;
import org.cip4.elk.JDFElementFactory;
import org.cip4.elk.jmf.JMFProcessor;
import org.cip4.elk.jmf.SubscriptionManager;
import org.cip4.jdflib.jmf.JDFJMF;
import org.cip4.jdflib.jmf.JDFMessage;
import org.cip4.jdflib.jmf.JDFQuery;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.jmf.JDFSignal;
import org.cip4.jdflib.jmf.JDFSubscription;

/**
 * A dispatcher of incoming JMF that decomposes the incoming JMF message and
 * dispatches each <em>Query</em>/<em>Command</em>/<em>Signal</em> to
 * the corresponding JMF processor. The queries/commands are dispatched in
 * sequence, one after the other.
 * <p>
 * If a <em>Query</em> contains a <em>Subscription</em> element the
 * dispatcher attempts to register the subscription with the configured
 * <code>SubscriptionManager</code> before the message is dispatched.
 * </p>
 * 
 * @author Claes Buckwalter (clabu@itn.liu.se)
 * @version $Id: SubscribingIncomingJMFDispatcher.java,v 1.6 2006/08/15 08:52:38 prosi Exp $
 */
public class SubscribingIncomingJMFDispatcher extends
        AbstractIncomingJMFDispatcher {

    private JMFProcessor _notImpJMFProcessor;
    private JDFElementFactory _factory;
    protected SubscriptionManager _subscriptionManager;

    private final static String XSI_ATTRIBUTE_NAME = "xsi:type";
    private final static String XSI_NAMESPACE_URI = "http://www.w3.org/2001/XMLSchema-instance";

    private Logger log;

    /**
     * Creates a new instance that does not register subscriptions. To activate
     * subscriptions a <code>SubscriptionManager</code> must be set using the
     * constructor
     * {@link SubscribingIncomingJMFDispatcher#SubscribingIncomingJMFDispatcher(SubscriptionManager)}
     * or the modifier
     * {@link SubscribingIncomingJMFDispatcher#setSubscriptionManager(SubscriptionManager)}.
     * 
     * @see #SubscribingIncomingJMFDispatcher(SubscriptionManager)
     * @see #setSubscriptionManager(SubscriptionManager)
     */
    public SubscribingIncomingJMFDispatcher() {
        this(null);
    }

    /**
     * Creates a new instance of this class that uses the specified subscription
     * manager for registering subscriptions whenever a JMF message contains a
     * subscription.
     * 
     * @param subscriptionManager The <code>SubscriptionManager</code> to use
     *            for registering subscriptions. Use <code>null</code> if
     *            subscriptions should not be registered.
     */
    public SubscribingIncomingJMFDispatcher(
            SubscriptionManager subscriptionManager) {
        super();
        _subscriptionManager = subscriptionManager;
        _notImpJMFProcessor = new NotImplementedJMFProcessor();
        _factory = JDFElementFactory.getInstance();

        log = Logger.getLogger(this.getClass().getName());
    }

    /**
     * An implementation that dispatches JMF messages in sequence, one at a time
     * after each other.
     */
    public JDFJMF dispatchJMF(JDFJMF jmfIn) {
        // Build response JMF
        JDFJMF jmfOut = _factory.createJMF();
        jmfOut.setSenderID(_config.getID());

        List msgsIn = jmfIn.getMessageVector (null, null);
        for (int i = 0, imax = msgsIn.size(); i < imax; i++) {
            JDFMessage msgIn = (JDFMessage) msgsIn.get(i);
            String xsiType;

            JDFResponse msgOut = jmfOut.appendResponse();
            String msgID = msgIn.getID();
            String msgType = msgIn.getType();

            //XXX Signal
            if (msgIn instanceof JDFSignal) {

                handleSignal((JDFSignal) msgIn);
                if (imax == 1) {
                    // If the JMF only contained a Signal, return null
                    return null;
                } else {
                    // Process the rest of the messages
                    continue;
                }
            }//XXX

            msgOut.setID(generateResponseID(msgID));
            msgOut.setrefID(msgID);
            msgOut.setType(msgType);

            // set xsi:type.
            xsiType = "Response" + msgType;
            msgOut.setAttributeNS(XSI_NAMESPACE_URI, XSI_ATTRIBUTE_NAME,
                xsiType);

            // Dispatches message to processor
            registerSubscription(msgIn, msgOut);
            JMFProcessor processor = getProcessor(msgType);
            processor.processJMF(msgIn, msgOut);
        }
        return jmfOut;
    }

    /**
     * Sets the subscription manager.
     * 
     * @param subscriptionManager Sets the subscription manager. Use
     *            <code>null</code> to disable new subscriptions from being
     *            registered.
     *  
     */
    public void setSubscriptionManager(SubscriptionManager subscriptionManager) {
        _subscriptionManager = subscriptionManager;
    }

    /**
     * Returns the configured subscription mananger.
     * 
     * @return
     */
    public SubscriptionManager getSubscriptionManager() {
        return _subscriptionManager;
    }

    /**
     * Attempts to register a subscription if the input message is a
     * <em>Query</em> containing a <em>Subscription</em> element. Sets
     * <em>Response/@Subscribed</em> to <em>true</em> if a subscription was
     * registered; <em>false</em> otherwise.
     * 
     * @param input message that may be a Query with a Subscription
     * @param output response
     */
    protected void registerSubscription(JDFMessage input, JDFResponse output) {
        if (_subscriptionManager != null) {
            if (input instanceof JDFQuery) {
                JDFQuery query = (JDFQuery) input;
                JDFSubscription subscription = query.getSubscription();

                if (subscription != null) {
                    boolean subscribed = _subscriptionManager
                            .registerSubscription(query);
                    output.setSubscribed(subscribed);
                }
            }
        } else {
            log.warn("No subscriptionManager was set for this manager.");
        }
    }

    /**
     * @todo Implement support for receiving JMF <em>Signal</em>s.
     * @param signal a <code>JDFSignal</code> to process
     */
    protected void handleSignal(JDFSignal signal) {
        // TODO Handle Signal
    }

    /**
     * Generates a message ID for the response based on the message ID of the
     * command or query.
     * 
     * @param refID
     * @return
     */
    private String generateResponseID(String refID) {
        return "R" + refID;
    }
}
