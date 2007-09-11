/*
 * Created on 2005-apr-20
 */
package org.cip4.elk.impl.jmf.preprocess;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cip4.elk.JDFElementFactory;
import org.cip4.elk.device.DeviceConfig;
import org.cip4.elk.device.process.Process;
import org.cip4.elk.impl.jmf.util.Messages;
import org.cip4.elk.impl.util.Repository;
import org.cip4.elk.jmf.IncomingJMFDispatcher;
import org.cip4.elk.jmf.OutgoingJMFDispatcher;
import org.cip4.elk.queue.Queue;
import org.cip4.jdflib.auto.JDFAutoAcknowledge.EnumAcknowledgeType;
import org.cip4.jdflib.auto.JDFAutoNotification.EnumClass;
import org.cip4.jdflib.auto.JDFAutoQueueEntry.EnumQueueEntryStatus;
import org.cip4.jdflib.core.ElementName;
import org.cip4.jdflib.core.JDFNodeInfo;
import org.cip4.jdflib.core.JDFParser;
import org.cip4.jdflib.core.VString;
import org.cip4.jdflib.jmf.JDFAcknowledge;
import org.cip4.jdflib.jmf.JDFCommand;
import org.cip4.jdflib.jmf.JDFJMF;
import org.cip4.jdflib.jmf.JDFQueue;
import org.cip4.jdflib.jmf.JDFQueueEntry;
import org.cip4.jdflib.jmf.JDFQueueSubmissionParams;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.node.JDFNode;
import org.cip4.jdflib.resource.JDFNotification;
import org.cip4.jdflib.resource.devicecapability.JDFDeviceCap;
import org.cip4.jdflib.util.UrlUtil;

/**
 * <p>
 * Implements a Simple JDFPreprocessor.
 * </p>
 * <p>
 * The method {@link #preProcessJDF(JDFCommand command)} is the essential method
 * of this class. This preprocessor will send <em>Acknowledge</em> messages to
 * the URL given in the <em>AcknowledgeURL</em>.
 * 
 * {@link org.cip4.elk.impl.jmf.preprocess.SimpleJDFPreprocessor#preProcessJDF(org.cip4.jdflib.jmf.JDFCommand,boolean)}
 * for details.
 * <p>
 * Implemented features:
 * <ul>
 * <li>Checking incoming parameters</li>
 * <li>downloads the JDF</li>
 * <li>saves the JDF in a {@link org.cip4.elk.impl.util.Repository}</li>
 * <li>Enqueues the JDF</li>
 * <li>Validates the JDF (optional)</li>
 * <li>Checking so that the JDF contains at least one (JDF) Process node of
 * correct type</li>
 * <li>Makes at least one (JDF) Process Node is executable (@link
 * org.cip4.elk.impl.util.PreFlightJDF#isExecutableAndAvailbleResources(JDFNode,
 * JDFResponse)}) </li>
 * </ul>
 * </p>
 * 
 * @author Ola Stering (olst6875@student.uu.se)
 * @author Claes Buckwalter (clabu@itn.liu.se)
 * @version $Id: SimpleJDFPreprocessor.java,v 1.18 2006/11/20 10:19:25 buckwalter Exp $
 */
public class SimpleJDFPreprocessor implements JDFPreprocessor {

    /** This fields indicates whether the JDF Node should be validated or not */
    private boolean validation = false;
    protected static Logger log;
    protected Queue _queue;
    protected OutgoingJMFDispatcher _outgoingJMFDispatcher;
    protected DeviceConfig _config;
    protected Process _process;
    protected IncomingJMFDispatcher _incomingJMFDispatcher;
    protected Repository _repository;

    /**
     * Creates a SimpleJDFPreprocessor.
     * 
     * @param queue
     * @param outgoingJMFDispatcher
     * @param config
     * @param p
     */
    public SimpleJDFPreprocessor(Queue queue,
            OutgoingJMFDispatcher outgoingJMFDispatcher,
            IncomingJMFDispatcher incomingJMFDispatcher, DeviceConfig config,
            Repository repository) {
        log = Logger.getLogger(this.getClass().getName());
        _queue = queue;
        _outgoingJMFDispatcher = outgoingJMFDispatcher;
        _config = config;
        _incomingJMFDispatcher = incomingJMFDispatcher;
        _repository = repository;
        log.debug("Instance of " + this.getClass().getName() + " created");
    }

    /**
     * The type of process that this preprocessor is checking for in the JDF
     * Node.
     * 
     * @return the process type; null if no type
     */
    public String getProcessType() {        
        final JDFDeviceCap deviceCap = _config.getDeviceConfig().getDeviceCap(0);
        if (deviceCap != null) {
            final VString types = deviceCap.getTypes(); 
            if (types != null) {
                return (String) types.get(0);
            }
        }
        return null;
    }

    /**
     * Submits a <em>QueueEntry</em> and sets <em>QueueEntry</em> Status to
     * 'Setup'. Queue entries will not be submitted if the queue has status
     * <em>Closed</em>,<em>Full</em> or <em>Blocked</em>.
     * 
     * @param submissionParams the <em>QueueSumissionParams</em>, may not be
     *            <code>null</code>
     * @param response the response message, a <em>Notification</em> will be
     *            appended to the response if the submission failed, otherwise
     *            the submitted <em>QueueEntry</em> will be appended
     * @return 0 on success, 112 otherwise (according to specification)
     * @throws NullPointerException if submissionParams is <code>null</code>
     *             or if response is <code>null</code>
     */
    private int enqueueJDF(final JDFQueueSubmissionParams submissionParams,
            final JDFResponse response) {
        JDFQueueEntry queueEntry = null;
        int returnCode = 0;
        synchronized (_queue) {
            // must set the status of the queue entry to setUp
            // until the JDF Node is valid and ready to be processed.
            queueEntry = _queue.addQueueEntry(submissionParams);
            if (queueEntry != null) {
                log.info("Submitted queue entry with id "
                        + queueEntry.getQueueEntryID()
                        + " and its status set to 'Setup'");
                // The QueueEntryStatus is set to held during its setUp phase
                // so that the process can not execute it.
                queueEntry.setQueueEntryStatus(EnumQueueEntryStatus.Held);
                _queue.putQueueEntry(queueEntry);
                response.copyElement(queueEntry, null);
            } else {
                final JDFQueue.EnumQueueStatus status = _queue.getQueueStatus();
                returnCode = 112;
                final String msg = "Job rejected. The queue is " + status.getName()
                        + ".";
                Messages.appendNotification(response,
                    JDFNotification.EnumClass.Warning, returnCode, msg);
                log.info(msg);
            }
        }
        return returnCode;
    }

    /**
     * Sets whether the preprocessor should validate its nodes or not.
     * 
     * @param validate
     */
    public void setValidation(boolean validate) {
        validation = validate;
    }

    public boolean getValidation() {
        return validation;
    }
    
    /**
     * Validates a JDF node using CheckJDF. Currently only CheckJDF proper
     * validation is used.
     * 
     * @param jdf the <em>JDFNode</em> to be validated
     * @param response the response message, a <em>Notification</em> will be
     *            appended to the response if validation fails, otherwise the
     *            response will be unmodified
     * @return 0 if the node validated; 4 if the JDF node did not validate; 2 if
     *         an error occurred
     */
    public int validateJDFNode(JDFNode jdf, JDFResponse response) {
        int returnCode = 0;
        String jdfID = jdf.getID();        

        boolean validated = false;
        String reportUrl = null;
        try {
            File schemaFile = null; // TODO: Add path to schema to activate
            // schema validation
            File reportFile = File.createTempFile(
                "SimpleJDFPreprocessor_report", ".xml");
            JDFJMF knownDevicesQuery = (JDFJMF) JDFElementFactory.getInstance()
                    .createJDFElement("SimpleJDFPreprocessor_KnownDevices");
            log.debug("Retrieving device capabilities by submitting a "
                    + "KnownDevices JMF query internally...");
            JDFJMF knownDevicesResponse = _incomingJMFDispatcher
                    .dispatchJMF(knownDevicesQuery);
            log.debug("Validating JDF node (" + jdfID + ") using CheckJDF...");
            validated = CheckJDFWrapper.validate(jdf, schemaFile, knownDevicesResponse,
                reportFile);            
            // Publish the report
            String tempReportUrl = UrlUtil.fileToUrl(reportFile, true);
            reportUrl = _repository.addPublicFile(tempReportUrl);
            log.debug("Copied CheckJDF validation report from " + tempReportUrl
                    + " to " + reportUrl);
        } catch (Exception ioe) {
            String msg = "An error occurred while validating JDF (" + jdfID
                    + "): " + ioe;
            log.error(msg, ioe);
            ioe.printStackTrace();
            returnCode = 2;
            Messages.appendNotification(response, EnumClass.Error, returnCode,
                msg);
            return returnCode;
        }

        if (validated) {
            String msg = "JDF node (" + jdfID + ") passed validation. For more"
                    + " information, see the CheckJDF XML report: " + reportUrl;
            log.info(msg);
            Messages.appendNotification(response, EnumClass.Information,
                returnCode, msg);
        } else {
            String msg = "JDF node (" + jdfID + ") does not pass validation. "
                    + "For more information, see the CheckJDF XML report: "
                    + reportUrl;
            log.debug(msg);
            returnCode = 4; // XML validation error
            Messages.appendNotification(response, EnumClass.Warning,
                returnCode, msg);
        }
        return returnCode;
    }

    /**
     * Downloads, enqueues, and preprocesses the JDF instance referenced in the
     * incoming <em>SubmitQueueEntry</em> Command.
     * <p>
     * The method will send <em>Acknowledge</em> messages in case the
     * <em>Command/@AcknowledgeURL</em> is given. This applies to any outcome
     * of the result of the preprocessing. The method will always
     * <em>return</em> a complete <em>Response</em>, this is to "inform"
     * the caller of the result in case no <em>AcknowledgeURL</em> was given.
     * </p>
     * <p>
     * If an error occurs at any step in the preprocessing no further processing
     * will be carried out and a <em>Response</em> (and possibly
     * <em>Acknowledge</em>) message will be sent with a corresponding return
     * code (which is != 0).
     * </p>
     * <p>
     * The method performs the following actions in the specified order:
     * <ol>
     * <li> Ensures that the <em>Command</em> is a valid
     * <em>SumbmitQueueEntry Command</em>. Checks that:
     * <ul>
     * <li>Command/@Type="SubmitQueueEntry"</li>
     * <li>SubmitQueueEntry/QueueSubmissionParams exists</li>
     * <li>QueueSubmissionParams/@URL is not ""</li>
     * </ul>
     * </li>
     * <li> Fetches and parses the JDF instance referenced at
     * Command/QueueSubmissionParams/@URL and adds it to a
     * <code>Repository</code>. The <code>Repository</code> returns a URL
     * to its copy of the JDF instance and
     * <em>Command/QueueSubmissionParams/@URL</em> is updated with this URL.
     * </li>
     * <li> Validates ({@link #validateJDFNode(JDFNode, JDFResponse)}) the JDF
     * (optional). Use {@link #setValidation(boolean)} <code>true</code> for
     * validation, <code>false</code> otherwise.
     * <li> Enqueues the JDF instance adding a new queue entry to the
     * <code>Queue</code>. The queue entry is assigned status <em>Setup</em>.
     * A queue entry will not be added if the queue has status <em>Closed</em>,
     * <em>Full</em>, or <em>Blocked</em>. </li>
     * <li> Checks so that the JDF contains any JDF nodes that match the
     * device's capabilities, see {@link DeviceConfig#getProcessableNodes(JDFNode)}.</li>
     * <li> Register any subscriptions that exist in
     * <em>JDF/NodeInfo/JMF/Query/Subscription</em>. </li>
     * <li> Updates the <em>QueueEntry</em> setting its status to
     * <em>Waiting</em>. </li>
     * </ul>
     * </p>
     * 
     * @throws NullPointerException if command equals <code>null</code>
     * @see org.cip4.elk.impl.jmf.preprocess.JDFPreprocessor#preProcessJDF(org.cip4.jdflib.jmf.JDFCommand,
     *      boolean)
     */
    public JDFResponse preProcessJDF(final JDFCommand command)
            throws NullPointerException {
        log.debug("Preprocessing SubmitQueueEntry...");
        int returnCode = 0;
        String msg = null;

        if (command == null) {
            msg = "Command may not be null";
            log.error(msg);
            throw new NullPointerException(msg);
        }

        final JDFResponse response = Messages.createResponse(command.getID(), command
                .getType());
        final String ackURL = command.getAcknowledgeURL();

        // Check incoming parameters
        returnCode = checkIncomingParameters(command, response);
        if (returnCode != 0) { // The incoming parameters contained an error
            completeMessages(returnCode, response, ackURL);
            return response;
        }

        // Get submission parameters
        final JDFQueueSubmissionParams submissionParams = command
                .getQueueSubmissionParams(0);

        // Fetch JDF file and store it in the repository
        String jdfUrl = submissionParams.getURL();
        JDFNode jdf = null;
        try {
            // Add the JDF file to the repository
            jdfUrl = _repository.addPrivateFile(jdfUrl);
            // Parse the JDF file
            jdf = new JDFParser().parseStream(_repository.getFile(jdfUrl))
                    .getJDFRoot();
        } catch (Exception e) {
            msg = "Unable to download and parse JDF at " + jdfUrl + ": " + e;
            log.warn(msg);
            returnCode = 3; // XML parser error
            Messages.appendNotification(response, EnumClass.Error, returnCode,
                msg);
            completeMessages(returnCode, response, ackURL);
            // Deletes the JDF file from the repository
            _repository.removeFile(jdfUrl);
            return response;
        }

        // Update submission params with the repository URL
        submissionParams.setURL(jdfUrl);

        // Check if JDF contains any nodes that match the device's capabilities
        final List compatibleNodes = _config.getProcessableNodes(jdf);
        if (compatibleNodes.size() == 0) {
            _repository.removeFile(jdfUrl);
            final String logMsg = "The job was rejected because none of the its nodes" +
                    "matched this device's capabilities.";
            returnCode = 101; // Invalid parameter
            Messages.appendNotification(response, EnumClass.Warning,
                returnCode, logMsg);            
            log.warn(logMsg);
            return response;
        }
                
        // Enqueue JDF
        returnCode = enqueueJDF(submissionParams, response);
        if (returnCode != 0) { // The queue was in an invalid state
            _repository.removeFile(jdfUrl);
            completeMessages(returnCode, response, ackURL);
            return response;
        }
        // Get the queued queue entry
        final JDFQueueEntry qe = response.getQueueEntry(0);

        // If validation is enabled, validate JDF
        if (validation) {            
            returnCode = validateJDFNode(jdf, response);            
            if (returnCode != 0) {
                _queue.removeQueueEntry(qe.getQueueEntryID());
                _repository.removeFile(jdfUrl);
                response.removeChild(ElementName.QUEUEENTRY, null, 0);
                log.debug("Removed queue entry with id " + qe.getQueueEntryID());
                completeMessages(returnCode, response, ackURL);
                return response;
            }
        }

        // Dummy preprocess for a while.
        int processTime = 5; // seconds.
        if (!(ackURL == null) && !(ackURL.equals(""))) {
            JDFAcknowledge ackMsg = Messages.createAcknowledge(response);
            ackMsg.setReturnCode(returnCode);
            Vector acknowledgeTypes = new Vector();
            acknowledgeTypes.add(EnumAcknowledgeType.Applied);
            ackMsg.setAcknowledgeType(acknowledgeTypes);
            msg = "Approximating preprocessing to about " + processTime
                    + " seconds.";
            ackMsg.appendComment().appendText(msg);
            if (log.isDebugEnabled()) {
                log.debug("About to dispatch " + ackMsg + " to URL: " + ackURL);
            }
            _outgoingJMFDispatcher.dispatchSignal(Messages.createJMFMessage(
                ackMsg, _config.getID()), ackURL);
        }

        try {
            Thread.sleep(processTime * 1000);
        } catch (InterruptedException e1) {
            // don't bother, just pretend waiting anyway...
        }

        // Look for JDF/NodeInfo/JMF/Subscription.
        initSubscriptions(jdf);

        /*
         * // Persist update JDF instance try {
         * jdf.getOwnerDocument_KElement().write2Stream(_repository.updateFile(jdfUrl),
         * 2); } catch(IOException ioe) { log.error("Could not write updated JDF
         * file to repository.", ioe); }
         */
        // Update Queue
        synchronized (_queue) {
            qe.setQueueEntryStatus(EnumQueueEntryStatus.Waiting); // Should
            // save JDF
            qe.setJobID(jdf.getJobID(true));
            if (!jdf.getJobPartID(false).equals("")) {
                qe.setJobPartID(jdf.getJobPartID(false));
            } else {
                log.warn("No JobPartID was set for the JDF, the JobPartID"
                        + " is set to the same as JobID='" + jdf.getJobID(true)
                        + "'.");
                qe.setJobPartID(jdf.getJobID(true));
            }
            log.debug("Putting QueueEntry " + qe + " back into the Queue");
            _queue.putQueueEntry(qe);
            final JDFQueue q = _queue.getQueue(command.getQueueFilter(0));
            response.copyElement(q, null);
            response.setReturnCode(returnCode);
        }

        // The queue entry is already attached to response
        completeMessages(returnCode, response, ackURL);
        return response;
    }

    /**
     * Dispatches any Queries (in the JDF) containing <em>Subscription</em>
     * elements to create a Persistent Channel.
     * 
     * TODO Make sure only Queries are dispatched.
     * 
     * @param jdf The JDFNode that may contain a
     *            <em>JDF/NodeInfo/JMF/Query/Subscription</em> element.
     * @throws NullPointerException if jdf is <code>null</code>.
     */
    public void initSubscriptions(final JDFNode jdf) {
        final JDFNodeInfo nodeInfo = jdf.getNodeInfo();
        if (nodeInfo != null) {
            final Vector v = nodeInfo.getChildElementVector(ElementName.JMF,
                null,null, true, 0, true);
            log.debug("Found " + v.size() + " JMF messages in JDF/NodeInfo.");

            for (Iterator it = v.iterator(); it.hasNext();) {
                final JDFJMF jmf = (JDFJMF) it.next();
                // TODO Should Check so that it is only
                // valid Queries with Subscription Elements in them...
                _incomingJMFDispatcher.dispatchJMF(jmf);
            }
        }
    }

    /**
     * A convenience method to complete the incoming <em>Response</em>
     * message, and to dispatch an <em>Acknowledge</em> message to the ackURL.
     * If the ackURL is not <code>null</code> or not "" the method copies the
     * children of the response to a new <em>Acknowledge</em> message. The
     * Acknowledge/@AcknowledgeType is set to 'Completed'.
     * 
     * For both the <em>Response</em> and the (possible) <em>Acknowledge</em>
     * message the
     * 
     * @returnCode is set.
     * 
     * NOTE: The incoming response will be modified.
     * 
     * @param returnCode The return code to be set
     * @param response the response message
     * @param ackURL The url to which the Acknowledge message will be sent
     * @return the response message with set return code.
     * @throws NullPointerException if response is <code>null</code>
     */
    private JDFResponse completeMessages(int returnCode, JDFResponse response,
            String ackURL) {

        response.setReturnCode(returnCode);
        if (!(ackURL == null) && !(ackURL.equals(""))) {
            JDFAcknowledge ackMsg = Messages.createAcknowledge(response);
            ackMsg.setReturnCode(returnCode);
            Vector acknowledgeTypes = new Vector();
            acknowledgeTypes.add(EnumAcknowledgeType.Completed);
            ackMsg.setAcknowledgeType(acknowledgeTypes);
            if (log.isDebugEnabled()) {
                log.debug("About to dispatch " + ackMsg + " to URL: " + ackURL);
            }
            _outgoingJMFDispatcher.dispatchSignal(Messages.createJMFMessage(
                ackMsg, _config.getID()), ackURL);
        }

        return response;
    }

    /**
     * Ensures that the <em>Command</em> is a valid
     * <em>SumbmitQueueEntryCommand</em>. Checks that:
     * <ul>
     * <li>Command/@Type="SubmitQueueEntry"</li>
     * <li>SubmitQueueEntry/QueueSubmissionParams exists</li>
     * <li>QueueSubmissionParams/@URL is not ""</li>
     * <ul>
     * 
     * @param command The command that is being checked.
     * @param response If the <em>Command</em> did not contain the correct
     *            parameters a <em>Notification</em> will be appended to this
     *            response
     * @return 0 on success, 7 otherwise (invalid parameters)
     * 
     * @throws NullPointerException if command or response is <code>null</code>
     */
    protected int checkIncomingParameters(JDFCommand command, JDFResponse response) {
        int returnCode = 0;
        String msg = null;

        if (!command.getType().equals("SubmitQueueEntry")) {
            msg = "Command must be of type 'SubmitQueueEntry', not "
                    + command.getType() + ". No QueueEntry submitted";
            returnCode = 7;
        } else {

            JDFQueueSubmissionParams submissionParams = command
                    .getQueueSubmissionParams(0);

            if (submissionParams == null) {
                msg = "SubmitQueueEntry/QueueSubmissionParams is a required "
                        + "element, may not be null. No QueueEntry submitted";
                returnCode = 7;
            } else {

                String jdfUrl = submissionParams.getURL();

                if (jdfUrl == null || jdfUrl.equals("")) {
                    msg = "Could not get JDF Node because the "
                            + "QueueSubmissionParams/@URL was null. No QueueEntry submitted";
                    returnCode = 7;
                }
            }
        }

        if (returnCode != 0) {
            log.warn(msg);
            Messages.appendNotification(response, EnumClass.Warning,
                returnCode, msg);
        } else {
            log.debug("The 'SubmitQueueEntry' Command contained the correct"
                    + " parameters");
        }

        return returnCode;
    }
}
