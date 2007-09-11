/**
 * Created on May 3, 2006, 3:27:28 PM
 * org.cip4.elk.impl.util.security.SubscribingIncomingRFAJMFDispatcher.java
 * Project Name: Elk
 */
package org.cip4.elk.impl.util.security;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.cip4.elk.JDFElementFactory;
import org.cip4.elk.impl.jmf.AbstractIncomingJMFDispatcher;
import org.cip4.elk.impl.jmf.NotImplementedJMFProcessor;
import org.cip4.elk.impl.jmf.RequestForAuthenticationJMFProcessor;
import org.cip4.elk.impl.jmf.SubscribingIncomingJMFDispatcher;
import org.cip4.elk.jmf.JMFProcessor;
import org.cip4.elk.jmf.SubscriptionManager;
import org.cip4.elk.util.security.RemoteHost;
import org.cip4.jdflib.jmf.JDFJMF;
import org.cip4.jdflib.jmf.JDFMessage;
import org.cip4.jdflib.jmf.JDFQuery;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.jmf.JDFSignal;
import org.cip4.jdflib.jmf.JDFSubscription;

/**
 * TODO JAVADOC
 *
 * @author Markus Nyman, (markus@myman.se)
 * 
 */
public class SubscribingIncomingRFAJMFDispatcher  extends
        AbstractIncomingJMFDispatcher {

    protected JMFProcessor _notImpJMFProcessor;
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
    public SubscribingIncomingRFAJMFDispatcher() {
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
    public SubscribingIncomingRFAJMFDispatcher(
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
     * The same as above but with the remote request included.
     * These are changes to allow host name verification in the RFA
     * messages
     */
    public JDFJMF dispatchJMF(JDFJMF jmfIn, HttpServletRequest req) {
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
            
            // Check if RFA, and add remote host info to the processor
            if (msgType.equals("RequestForAuthentication")) {
            RemoteHost hostinfo= new RemoteHost(req);
            	((RequestForAuthenticationJMFProcessor) processor).processMessage(msgIn, msgOut, hostinfo);
            }
            else processor.processJMF(msgIn, msgOut);
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
                //JDFSubscription subscription = query.getSubscription(0);
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
