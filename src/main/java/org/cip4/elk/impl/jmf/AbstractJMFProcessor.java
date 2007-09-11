/*
 * Created on Aug 28, 2004
 */
package org.cip4.elk.impl.jmf;

import org.apache.log4j.Logger;
import org.cip4.elk.JDFElementFactory;
import org.cip4.elk.jmf.JMFProcessor;
import org.cip4.jdflib.core.ElementName;
import org.cip4.jdflib.core.JDFComment;
import org.cip4.jdflib.jmf.JDFMessage;
import org.cip4.jdflib.jmf.JDFMessageService;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.resource.JDFNotification;

/**
 * An abstract <code>JMFProcessor</code> that provides error handling and a
 * convenience method for creating notifications. Extend this class add your
 * message processing to {@link processMesssage(JDFMessage, JDFResponse)}.
 * 
 * It is crucial to set the messageType using {@link setMessageType(messageType)} and
 * to set the message type families of the processor. setQueryProcessor(true) for a 
 * processor that handles Query messages. A JMFProcessor may handle multiple message 
 * families. 
 * 
 * @see <a
 *      href="http://www.cip4.org/documents/jdf_specifications/JDF1.2.pdf">JDF
 *      Specification Release 1.2, Appendix I: Supported Error Codes in JMF and
 *      Notification elements </a>
 * @author Claes Buckwalter (clabu@itn.liu.se)
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: AbstractJMFProcessor.java,v 1.6 2005/05/25 19:08:19 ola.stering Exp $
 */
public abstract class AbstractJMFProcessor implements JMFProcessor {
    protected Logger log;

    protected String _messageType = "";

    protected boolean _isQueryProcessor = false;

    protected boolean _isCommandProcessor = false;

    protected boolean _isSignalProcessor = false;

    protected boolean _isAcknowledgeProcessor = false;

    public AbstractJMFProcessor() {
        log = Logger.getLogger(this.getClass().getName());
    }

    /**
     * Always returns a <em>return code</em> with value <code>5</code>,
     * <em>Query/command not implemented</em>.
     * 
     * @see org.cip4.elk.jmf.JMFProcessor#processJMF(com.heidelberg.JDFLib.jmf.JDFMessage,
     *      com.heidelberg.JDFLib.jmf.JDFResponse)
     */
    public int processJMF(JDFMessage input, JDFResponse output) {
        int returnCode = 0;
        try {
            returnCode = processMessage(input, output);
        } catch (Exception e) {
            returnCode = 5;
            String msg = "Could not process JMF message. [Java Exception: " + e
                    + " ]";
            log.error(msg, e);
            e.printStackTrace();
            appendNotification(output, JDFNotification.EnumClass.Error,
                    returnCode, msg);
        }
        return returnCode;
    }

    /**
     * Creates a notification.
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
     * @see com.heidelberg.JDFLib.Auto.JDFAutoNotification
     */
    protected void appendNotification(JDFResponse response,
            JDFNotification.EnumClass notClass, int returnCode, String msg) {
        response.setReturnCode(returnCode);
        JDFNotification notification = response.appendNotification();
        notification.setClass(notClass);
        JDFComment comment = notification.appendComment();
        comment.appendText(msg);
    }

    /**
     * Sets the message type that is returned when
     * {@link getMessageServices() getMessageServices}is called.
     * 
     * @param messageType
     */
    public void setMessageType(String messageType) {
        _messageType = messageType;
    }

    /**
     * Returns the message type returned when
     * {@link getMessageServices() getMessageServices}is called.
     * 
     * @return the message type
     */
    public String getMessageType() {
        return _messageType;
    }

    public abstract int processMessage(JDFMessage input, JDFResponse output);

    /**
     * @return true if processor handles <em>Acknowledge</em> messages
     */
    public boolean isAcknowledgeProcessor() {
        return _isAcknowledgeProcessor;
    }

    /**
     * Sets whether the processor handles <em>Acknowledge</em> messages.
     * 
     * @param acknowledgeProcessor
     *            
     */
    public void setAcknowledgeProcessor(boolean acknowledgeProcessor) {
        _isAcknowledgeProcessor = acknowledgeProcessor;
    }

    /**
     * @return true if processor handles <em>Command</em> messages.
     */
    public boolean isCommandProcessor() {
        return _isCommandProcessor;
    }

    /**
     * Sets whether the processor handles <em>Command</em> messages.
     * @param commandProcessor
     *            
     */
    public void setCommandProcessor(boolean commandProcessor) {
        _isCommandProcessor = commandProcessor;
    }

    /**
     * @return true if processor handles <em>Query</em> messages.
     */
    public boolean isQueryProcessor() {
        return _isQueryProcessor;
    }

    /**
     * Sets whether the processor handles <em>Query</em> messages.
     * @param queryProcessor
     *            
     */
    public void setQueryProcessor(boolean queryProcessor) {        
        _isQueryProcessor = queryProcessor;
    }

    /**
     * @return true if processor handles <em>Signal</em> messages.
     */
    public boolean isSignalProcessor() {
        return _isSignalProcessor;
    }

    /**
     * Sets whether the processor handles <em>Signal</em> messages.
     * @param signalProcessor
     *            
     */
    public void setSignalProcessor(boolean signalProcessor) {
        _isSignalProcessor = signalProcessor;
    }

    /*
     * (non-Javadoc)
     * 
     * @todo Currently all queries are set to be <em>Persistent</em>
     * 
     * @see org.cip4.elk.jmf.JMFProcessor#getMessageServices()
     */
    public JDFMessageService[] getMessageServices() {
        JDFMessageService s1 = (JDFMessageService) JDFElementFactory
                .getInstance().createJDFElement(ElementName.MESSAGESERVICE);

        s1.setType(getMessageType());

        if (isQueryProcessor()) {
            s1.setQuery(true);
            s1.setPersistent(true); // XXX All queries can be subscribed
        }
        if (isCommandProcessor()) {
            s1.setCommand(true);            
        }
        if (isSignalProcessor()) {
            s1.setSignal(true);                        
        }
        if (isAcknowledgeProcessor()) {
            s1.setAcknowledge(true);            
        }
               
        JDFMessageService[] s = {s1};      
        
        return s;
    }
}
