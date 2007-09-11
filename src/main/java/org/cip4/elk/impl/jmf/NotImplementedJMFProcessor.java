/*
 * Created on Aug 28, 2004
 */
package org.cip4.elk.impl.jmf;

import org.apache.log4j.Logger;
import org.cip4.elk.jmf.JMFProcessor;
import org.cip4.jdflib.core.JDFComment;
import org.cip4.jdflib.jmf.JDFMessage;
import org.cip4.jdflib.jmf.JDFMessageService;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.resource.JDFNotification;

/**
 * A <code>JMFProcessor</code> that always returns a <em>return code</em> with value <code>5</code>, 
 * <em>Query/command not implemented</em>.
 * 
 * @see <a href="http://www.cip4.org/documents/jdf_specifications/JDF1.2.pdf">JDF Specification Release 1.2, Appendix I: Supported Error Codes in JMF and Notification elements</a> 
 * @author clabu
 */
public class NotImplementedJMFProcessor implements JMFProcessor
{
    private static Logger log = Logger.getLogger(NotImplementedJMFProcessor.class);
    
    /**
     * Always returns a <em>return code</em> with value <code>5</code>, 
     * <em>Query/command not implemented</em>.
     * @see org.cip4.elk.jmf.JMFProcessor#processJMF(com.heidelberg.JDFLib.jmf.JDFMessage, com.heidelberg.JDFLib.jmf.JDFResponse)
     */
    public int processJMF(JDFMessage input, JDFResponse output) {
        int returnCode = 5;
        
        log.debug("Incoming message: " + input.toString());
        
        output.setReturnCode(returnCode);
        // Create Notification
        JDFNotification notification = output.appendNotification();
        notification.setClass(JDFNotification.EnumClass.Error);      
        notification.setType("Error"); // TODO: Does type need to be set?
        JDFComment comment = notification.appendComment();
        comment.appendText(input.getType() + " is not implemented.");       
        // TODO: Add NotificationDetails of type Error
        
        log.debug("Outgoing response: " + notification.toString());
       
        return returnCode;
    }
    
    /**
     * Return an empty array since this processor does not process any messages.
     */
    public JDFMessageService[] getMessageServices() {
        return new JDFMessageService[0];
    }
    
}
