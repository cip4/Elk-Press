/**
 * Created on Feb 27, 2006, 11:24:26 AM
 * org.cip4.elk.impl.jmf.RequestForAuthenticationJMFProcessor.java
 * Project Name: Elk
 */
package org.cip4.elk.impl.jmf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cip4.elk.util.security.RFAJMFProcessor;
import org.cip4.elk.util.security.RemoteHost;
import org.cip4.jdflib.jmf.JDFMessage;
import org.cip4.jdflib.jmf.JDFResponse;



/**
 * TODO JAVADOC
 *
 * @author Markus Nyman, (markus@myman.se)
 * 
 */
public class RequestForAuthenticationJMFProcessor extends AbstractJMFProcessor {
	
	protected static Log log = LogFactory.getLog(RequestForAuthenticationJMFProcessor.class);
	
    public static final String MESSAGE_TYPE = "RequestForAuthentication";

    /**
     * RFAJMFProcessor to which all RFA messages are forwarded 
     */
    private static RFAJMFProcessor _rfaProcessor = null;
   
	/**
	 * The default constructor. RFAJMFProcessor must be set with 
	 * <code>setRFAJMFProcessor(RFAJMFProcessor)</code>
	 * in order to process RFA messages
	 */
	public RequestForAuthenticationJMFProcessor() {
		super();
        setMessageType(MESSAGE_TYPE);
		setCommandProcessor(true);
		setQueryProcessor(true);
		log.debug("RequestForAuthenticationJMFProcessor() created. " +
				"Initiate properly by calling setRFAJMFProcessor(RFAJMFProcessor)");
	}
	
	/**
	 * Setter for the required singleton <code>RFAJMFProcessor</code>
	 * This is set from the spring configuration files and from there only
	 * @param rfaProcessor an instance of <code>RFAJMFProcessor</code>
	 */
	public void setRFAJMFProcessor(RFAJMFProcessor rfaProcessor) {
		_rfaProcessor = rfaProcessor;
	}
	
	/**
	 * Getter for the singleton <code>RFAJMFProcessor</code>
	 * @return the <code>RFAJMFProcessor</code>
	 */
	public RFAJMFProcessor getRFAJMFProcessor() {
		return _rfaProcessor;
	}
	
	
	/**
	 * Processes an incoming RFA message. This method forwards the message to
	 * the <code>RFAJMFProcessor</code> configured.
	 * @param input The forwarded object is the JDFMessage.getJMFRoot() object
	 * @param output The forwarded object is the JDFResponse.getJMFRoot() object
	 * @return the return code of the processed JDFResponse output.
	 */
	public synchronized int processMessage(JDFMessage input, JDFResponse output) {
		if (_rfaProcessor != null) {
			// FIXME this must be processed in lower layers. host info is needed
			RemoteHost host = new RemoteHost("No host info");
			return _rfaProcessor.processJDF(input.getJMFRoot(), output.getJMFRoot(), host);
		}
		else {
			log.error("RFAJMFProcessor not initiated properly."
					+ " Make a call to setRFAJMFProcessor(RFAJMFProcessor).");
			return 2; // TODO get JDFJMF.INTERNAL_ERROR
		}
	}
	
	/**
	 * This method does the same as the one above, but includes the RemoteHost to the
	 * parameters to allow custom host name verification
	 * @param input inmessage
	 * @param output outmessage
	 * @param host remote host info
	 * @return response return code
	 */
	public synchronized int processMessage(JDFMessage input, JDFResponse output, RemoteHost host) {
		if (_rfaProcessor != null) {
			return _rfaProcessor.processJDF(input.getJMFRoot(), output.getJMFRoot(), host);
		}
		else {
			log.error("RFAJMFProcessor not initiated properly."
					+ " Make a call to setRFAJMFProcessor(RFAJMFProcessor).");
			return 2; // TODO get JDFJMF.INTERNAL_ERROR
		}
	}

}
