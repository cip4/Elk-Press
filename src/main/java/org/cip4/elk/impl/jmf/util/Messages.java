/*
 * Created on 2005-apr-22
 */
package org.cip4.elk.impl.jmf.util;

import org.apache.log4j.Logger;
import org.cip4.elk.ElkEvent;
import org.cip4.elk.JDFElementFactory;
import org.cip4.jdflib.core.ElementName;
import org.cip4.jdflib.core.JDFComment;
import org.cip4.jdflib.core.KElement;
import org.cip4.jdflib.jmf.JDFAcknowledge;
import org.cip4.jdflib.jmf.JDFCommand;
import org.cip4.jdflib.jmf.JDFJMF;
import org.cip4.jdflib.jmf.JDFMessage;
import org.cip4.jdflib.jmf.JDFQuery;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.jmf.JDFSignal;
import org.cip4.jdflib.jmf.JDFSubscription;
import org.cip4.jdflib.resource.JDFNotification;

/**
 * This is a utility class for constructing different kind of messages. The
 * class can not be instantiated and consists only of static methods.
 * 
 * The class is thread safe.
 * 
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: Messages.java,v 1.9 2006/08/30 15:46:02 buckwalter Exp $
 */
public class Messages {

    private static JDFElementFactory _factory = JDFElementFactory.getInstance();
    private static Logger log = Logger.getLogger(Messages.class.getName());
    private static final String XSI_ATTRIBUTE_NAME = "xsi:type";
    private static final String XSI_NAMESPACE_URI = "http://www.w3.org/2001/XMLSchema-instance";
    private static volatile int msgNumber = 1;

    /** Private constructor to prevent instantiation */
    private Messages() {
    }

    /**
     * Builds a JMF Signal based on the original query and the event that
     * triggered this signal.
     * 
     * @todo Add <em>Signal/NotificationDetails</em> elements
     * @todo Add <em>Signal/Trigger</em> elements
     * @param originalQuery the subscription's original query
     * @param event the event the generated this signal
     * @return a JMF Signal
     */
    public static JDFSignal buildSignal(JDFQuery originalQuery, ElkEvent event) {

        JDFSignal signal = createSignal(originalQuery.getID(), originalQuery
                .getType());

        if (event != null) { // No notification element for non-events

            JDFNotification notification = signal.appendNotification(); // Signal/Notification
            notification.setClass(event.getEventClass()); // Signal/Notification/@Class
            // TODO Set Type
            // TODO Attach concrete NotificationDetails element
            notification.appendComment().appendText(event.getDescription()); // Signal/Notification/Comment
            notification.appendComment().appendText(event.toString());
            signal.appendComment().appendText("Event-based signal");
        } else {
            signal.appendComment().appendText("Time-based signal");
        }
        // JMF/Signal/Trigger
        // TODO Add triggers not Base ICS

        return signal;
    }

    /**
     * Creates a <em>Signal</em> with required fields according to Base ICS.
     * Sets:
     * <ul>
     * <li>Signal/@ID</li>
     * <li>Signal/@RefID</li>
     * <li>Signal/@Type</li>
     * <li>Signal/@xsi:type</li>
     * </ul>
     * 
     * @param refID the ID of the Query/Command that this message is a response
     *            to
     * @param type the type of the this signal (same as the Query/Command that
     *            this is signal is generated for)
     * 
     * @return the generated <em>Signal</em>
     */
    public static JDFSignal createSignal(String refID, String type) {
        JDFSignal signal = (JDFSignal) _factory
                .createJDFElement(ElementName.SIGNAL);

        signal.setID("S" + refID);
        signal.setrefID(refID);
        signal.setType(type);
        signal.setAttributeNS(XSI_NAMESPACE_URI, XSI_ATTRIBUTE_NAME, "Signal"
                + type);

        log.debug("Created Signal message with id " + signal.getID()
                + " and type " + type);

        return signal;

    }

    /**
     * Appends a <em>Notification</em> element to a <em>Response</em>
     * element. If there already exists a <em>Notification</em> a
     * <em>Comment</em> containing the specified message is added to the
     * <em>Notification</em>.
     * 
     * @param response
     *            the response to append the notification to
     * @param notClass
     *            the class of the notification
     * @param returnCode
     *            the return code
     * @param msg
     *            a message that will be appended as a comment to the
     *            notification
     */
    public static void appendNotification(JDFResponse response,
            JDFNotification.EnumClass notClass, int returnCode, String msg) {
        response.setReturnCode(returnCode);
        final JDFNotification notification; 
        if (response.getNotification() == null) {
            notification = response.appendNotification();
        } else {
            notification = response.getNotification();            
        }
        notification.setClass(notClass);
        final JDFComment comment = notification.appendComment();
        comment.appendText(msg);
    }

    /**
     * Creates an <em>Acknowledge</em> message containing the same child
     * elements as the incoming <em>Response</em> message.
     * <ul>
     * <li>Acknowledge/@ID</li>
     * <li>Acknowledge/@RefID, same as the incoming <em>Response</em></li>
     * <li>Acknowledge/@Type, same as the incoming <em>Response</em></li>
     * <li>Acknowledge/@xsi:type, "Acknowledge" + the incoming type</li>
     * </ul>
     * 
     * @param response The <em>Response</em> message which child elements are
     *            copied to the <em>Acknowledge</em> message
     * @return a new <em>Acknowledge</em> message.
     */
    public static JDFAcknowledge createAcknowledge(JDFResponse response) {

        JDFAcknowledge ackMsg = (JDFAcknowledge) _factory
                .createJDFElement(ElementName.ACKNOWLEDGE);
        String refID = response.getrefID();
        String type = response.getType();
        ackMsg.setID("A" + refID);
        ackMsg.setrefID(refID);
        ackMsg.setType(type);
        ackMsg.setAttributeNS(XSI_NAMESPACE_URI, XSI_ATTRIBUTE_NAME,
            "Acknowledge" + type);
        KElement[] elements = response.getChildElementArray();

        for (int i = 0, size = elements.length; i < size; i++) {
            ackMsg.copyElement(elements[i], null);
        }

        log.debug("Created message with id " + ackMsg.getID() + "and type "
                + type);

        return ackMsg;
    }

    /**
     * Creates a <em>Response</em> with required fields according to Base ICS.
     * Sets:
     * <ul>
     * <li>Response/@ID</li>
     * <li>Response/@RefID</li>
     * <li>Response/@Type</li>
     * <li>Response/@xsi:type</li>
     * </ul>
     * 
     * @param refID the ID of the Query/Command that this message is a response
     *            to
     * @param type the type of the this response (same as the Query/Command that
     *            this is a response to)
     * 
     * @return the generated response
     */
    public static JDFResponse createResponse(String refID, String type) {
        JDFResponse response = (JDFResponse) _factory
                .createJDFElement(ElementName.RESPONSE);

        response.setID("R" + refID);
        response.setrefID(refID);
        response.setType(type);
        response.setAttributeNS(XSI_NAMESPACE_URI, XSI_ATTRIBUTE_NAME,
            "Response" + type);

        log.debug("Created Response message with id '" + response.getID()
                + "' and type " + type);

        return response;
    }

    /**
     * Creates a <em>Command</em> with required fields according to Base ICS.
     * The id of the command is generated by a "C" and then a number which
     * increases (Not neccessarly in sequnence and the number will start at 0 every
     * time Elk is restarted). Sets:
     * <ul>
     * <li>Command/@ID</li>
     * <li>Command/@Type</li>
     * <li>Command/@xsi:type</li>
     * </ul>
     * 
     * @param type the type of the this Command (i.e. "StopPersistentChannel")
     * @return the generated <em>Command</em>.
     */
    public static JDFCommand createCommand(String type) {
        JDFCommand command = (JDFCommand) _factory
                .createJDFElement(ElementName.COMMAND);
        int uniqueID = msgNumber++;
        command.setID("C" + uniqueID);
        command.setType(type);
        command.setAttributeNS(XSI_NAMESPACE_URI, XSI_ATTRIBUTE_NAME, "Command"
                + type);

        log.debug("Created Command message with id \"" + command.getID()
                + "\" and type \"" + type + "\"");
        return command;
    }

    /**
     * Creates a <em>JMF</em> message.
     * 
     * @param msg The message that is appended to the JMF message
     * @return A new JMF Message.
     */
    public static JDFJMF createJMFMessage(JDFMessage msg, String senderID) {
        JDFJMF jmfMsg = _factory.createJMF();
        if (senderID == null)
            throw new NullPointerException("The senderID may not be null");
        jmfMsg.copyElement(msg, null);
        jmfMsg.setSenderID(senderID);
        // Generate Unique ID
        if (log.isDebugEnabled()) {
            log.debug("Created JMF: " + jmfMsg);
        }
        return jmfMsg;
    }

    /**
     * <p>
     * Checks the Message for a correct Subscription parameters.
     * </p>
     * Checks that:
     * <ul>
     * <li><em>Query</em> exists</li>
     * <li>Incoming Message is of type <em>Query</em></li>
     * <li><em>Query/Subscription</em> exists</li>
     * <li><em>Subscription/@URL</em> is set and not equal to ''.</li>
     * </ul>
     * 
     * @param sourceLog
     * @param msg
     * @return 0 on success, 6 otherwise.`
     */
    public static int checkSubscriptionParameters(Logger sourceLog,
            JDFMessage msg) {

        if (msg == null) {
            sourceLog.debug("The incoming Query was null.");
            return 6; // insufficient parameters
        } else if (!(msg instanceof JDFQuery)) {
            sourceLog.debug("Message must belong to message Family Query, not "
                    + msg.getType());
            return 6;
        }

        JDFQuery incomingQuery = (JDFQuery) msg;
        JDFSubscription sub = incomingQuery.getSubscription();

        if (sub == null) {
            sourceLog.warn("Query/Subscriptions is not set for Query with id '"
                    + incomingQuery.getID() + "'");
            return 6;
        } else if (sub.getURL().equals("")) {
            sourceLog
                    .warn("Query/Subscription/@URL is not set for Query with id '"
                            + incomingQuery.getID() + "'");
            return 6;
        }
        return 0;

    }

}
