/**
 * Created on Mar 22, 2006, 3:36:09 PM
 * org.cip4.elk.impl.util.security.ElkRFAJMFProcessor.java
 * Project Name: Elk
 */
package org.cip4.elk.impl.util.security;

import java.util.Iterator;
import java.util.Vector;

import org.apache.commons.logging.LogFactory;
import org.cip4.elk.impl.jmf.AsyncHttpOutgoingJMFDispatcher;
import org.cip4.elk.util.security.AbstractRFAJMFProcessor;
import org.cip4.elk.util.security.RemoteHost;
import org.cip4.elk.util.security.TrustEntry;
import org.cip4.jdflib.core.ElementName;
import org.cip4.jdflib.core.JDFDoc;
import org.cip4.jdflib.jmf.JDFJDFController;
import org.cip4.jdflib.jmf.JDFJMF;
import org.cip4.jdflib.jmf.JDFQuery;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.jmf.JDFSignal;

/**
 * TODO JAVADOC
 *
 * @author Markus Nyman, (markus@myman.se)
 * 
 */
public class ElkRFAJMFProcessor extends AbstractRFAJMFProcessor {

	/**
	 * The underlying dispatcher that sends JDFJMF messages
	 */
	private AsyncHttpOutgoingJMFDispatcher _outgoingDispatcher;
	
	
	/**
	 * Constructor. Does nothing but initializes a Log4J Logger
	 *
	 */
	public ElkRFAJMFProcessor() {
		log = LogFactory.getLog(this.getClass().getName());
	}

	
	/**
	 * Getter for the underlying dispatcer
	 */
	public AsyncHttpOutgoingJMFDispatcher getOutgoingJMFDispatcher() {
		return _outgoingDispatcher;
	}
	
	
	/**
	 * Setter for the underlying HTTP dispatcher
	 * @param outgoingDispatcher
	 */
	public void setOutgoingJMFDispatcher(AsyncHttpOutgoingJMFDispatcher outgoingDispatcher) {
		_outgoingDispatcher = outgoingDispatcher;
	}
	
	
	/**
	 * TODO this should catch SSL exceptions when that is allowed in
	 * the underlyind HttpDispatcher
	 * 
	 * sends a JDFJMF message to a target URL
	 * 
	 */
	public synchronized JDFJMF dispatchJDF(JDFJMF output, String targetUrl) {
		log.debug("Dispatching RFA message to " + targetUrl);
		JDFResponse resp = _outgoingDispatcher.dispatchJMF(output, targetUrl);
		if (resp == null) {
			log.debug("Error dispatching JDFJMF. No response received.");
			return null;
		}
		JDFJMF jmfResponse = resp.getJMFRoot();
		log.debug("RESPONSE JDFJMF: \n" + jmfResponse.toString());
		return jmfResponse;
		
	}
	
	
	/**
	 * overriddes the default behaviour since the trust entries must be
	 * initialized when a RFA request is received. If this is not overridden
	 * the Query URL of the TrustEntry is set in the Properties file
	 */
	public void processClientInitiateCmd(String senderID,
			String certificate, JDFResponse response, RemoteHost hostInfo) {

		// the query url is present for elk
		TrustEntry trustEntry = getAuthenticationHandler().getClientTrustEntry(senderID);
		if (trustEntry != null) {
			trustEntry.setRemoteHostInfo(hostInfo);
			trustEntry.setRemoteCertificate(certificate);
			super.processClientInitiateCmd(senderID, certificate, response, hostInfo);
		} else {
			response.setReturnCode(NO_AUTH_REQUEST_IN_PROCESS);
		}
	}
	
	
	/**
	 * overriddes the default behaviour since the trust entries must be
	 * initialized when a RFA request is received. If this is not overridden
	 * the Query URL of the TrustEntry is set in the Properties file
	 */
	public void processServerInitiateCmd(String senderID,
			String certificate, JDFResponse response, RemoteHost hostInfo) {
		// FIXME The query url must be present at this stage
		TrustEntry trustEntry = getAuthenticationHandler().getServerTrustEntry(senderID);
		if (trustEntry != null) {
			trustEntry.setRemoteHostInfo(hostInfo);
			trustEntry.setRemoteCertificate(certificate);
			super.processServerInitiateCmd(senderID, certificate, response, hostInfo);
		} else {
			response.setReturnCode(NO_AUTH_REQUEST_IN_PROCESS);
		}
	}
	
	public boolean registerDevice(String nameServerUrl, String myID, String myURL) {
    	
		// register at the server
		{
		JDFJMF signal = createKnownControllersSignal(myID, myURL);

		JDFJMF signalResp = dispatchJDF(signal, nameServerUrl);
		if (signalResp == null)  
			return false;
		
		
		JDFResponse response = signalResp.getResponse();
        if (response == null || response.getReturnCode() != 0)
        	return false;
        else log.info("Registered at name server: " + nameServerUrl);
		}



		// query known controllers

		JDFJMF query = createKnownControllersQuery(myID);
		JDFJMF queryResp = dispatchJDF(query, nameServerUrl);
		if (queryResp == null)
			return false;

		JDFResponse response = queryResp.getResponse();
		if (response == null || response.getReturnCode() != 0)
			return false;
		else {

			Vector controllers = response.getChildElementVector("JDFController", null, null, false, 0);
			for (Iterator it = controllers.iterator(); it.hasNext();) {
				JDFJDFController con = (JDFJDFController) it.next();
				// TODO ARE ControllerID CASE-SENSITIVE OR NOT?
				if (!con.getControllerID().equals(myID)) {
					_authHandler.addAllowedTrustRelation(con.getControllerID(), con.getURL());
					log.debug("Added controller: " + con.getControllerID() + "@" + con.getURL());
				}
			}
			return true;

		}

		
	}

	private JDFJMF createKnownControllersSignal(String id, String url) {
		JDFJMF jmf = (JDFJMF) new JDFDoc(ElementName.JMF).getRoot();
		jmf.addNameSpace("", "http://www.CIP4.org/JDFSchema_1_1");
		jmf.setSenderID(id);
		JDFSignal signal = jmf.appendSignal();
		signal.setType("KnownControllers");
		JDFJDFController controller = signal.appendJDFController();
		controller.setControllerID(id);
		controller.setURL(url);
		return jmf;
	}

	private JDFJMF createKnownControllersQuery(String id) {
		JDFJMF jmf = (JDFJMF) new JDFDoc(ElementName.JMF).getRoot();
		jmf.addNameSpace("", "http://www.CIP4.org/JDFSchema_1_1");
		jmf.setSenderID(id);
		JDFQuery query = jmf.appendQuery();
		query.setType("KnownControllers");
		return jmf;

	}




}
