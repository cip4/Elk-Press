/*
 * Created on Sep 20, 2004
 */
package org.cip4.elk.impl.device.process;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.cip4.elk.ElkEvent;
import org.cip4.elk.JDFElementFactory;
import org.cip4.elk.device.DeviceConfig;
import org.cip4.elk.device.process.Process;
import org.cip4.elk.device.process.ProcessQueueEntryEvent;
import org.cip4.elk.device.process.ProcessStatusEvent;
import org.cip4.elk.impl.jmf.util.Messages;
import org.cip4.elk.impl.util.Repository;
import org.cip4.elk.impl.util.URLAccessTool;
import org.cip4.elk.jmf.IncomingJMFDispatcher;
import org.cip4.elk.jmf.JMFProcessor;
import org.cip4.elk.jmf.OutgoingJMFDispatcher;
import org.cip4.elk.lifecycle.Lifecycle;
import org.cip4.elk.queue.Queue;
import org.cip4.elk.util.JDFUtil;
import org.cip4.jdflib.core.ElementName;
import org.cip4.jdflib.core.JDFComment;
import org.cip4.jdflib.core.JDFElement;
import org.cip4.jdflib.core.JDFNodeInfo;
import org.cip4.jdflib.core.JDFParser;
import org.cip4.jdflib.core.JDFResourceLink;
import org.cip4.jdflib.core.KElement;
import org.cip4.jdflib.core.VString;
import org.cip4.jdflib.core.XMLDoc;
import org.cip4.jdflib.core.JDFElement.EnumNodeStatus;
import org.cip4.jdflib.datatypes.JDFAttributeMap;
import org.cip4.jdflib.datatypes.VJDFAttributeMap;
import org.cip4.jdflib.jmf.JDFCommand;
import org.cip4.jdflib.jmf.JDFDeviceInfo;
import org.cip4.jdflib.jmf.JDFJMF;
import org.cip4.jdflib.jmf.JDFJobPhase;
import org.cip4.jdflib.jmf.JDFMessage;
import org.cip4.jdflib.jmf.JDFQuery;
import org.cip4.jdflib.jmf.JDFQueue;
import org.cip4.jdflib.jmf.JDFQueueEntry;
import org.cip4.jdflib.jmf.JDFQueueSubmissionParams;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.jmf.JDFReturnQueueEntryParams;
import org.cip4.jdflib.jmf.JDFSignal;
import org.cip4.jdflib.jmf.JDFStopPersChParams;
import org.cip4.jdflib.jmf.JDFSubscription;
import org.cip4.jdflib.jmf.JDFMessage.EnumType;
import org.cip4.jdflib.node.JDFNode;
import org.cip4.jdflib.pool.JDFAuditPool;
import org.cip4.jdflib.resource.JDFDevice;
import org.cip4.jdflib.resource.JDFPhaseTime;
import org.cip4.jdflib.resource.JDFProcessRun;
import org.cip4.jdflib.resource.JDFResource;
import org.cip4.jdflib.resource.process.JDFComponent;
import org.cip4.jdflib.util.JDFDate;

/**
 * This class contains base functionality for the Elk's reference processes.
 * 
 * @author Claes Buckwalter (clabu@itn.liu.se)
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: BaseProcess.java,v 1.24 2006/12/03 21:21:32 buckwalter Exp $
 */
public abstract class BaseProcess extends AbstractProcess implements Runnable,
        Lifecycle {

    protected static Logger log;

    protected DeviceConfig _config;

    protected ProcessState _state;

    protected Queue _queue;

    protected URLAccessTool _fileUtil;

    protected OutgoingJMFDispatcher _outgoingDispatcher;

    protected IncomingJMFDispatcher _incomingDispatcher;

    protected Thread _thread;
    
    protected String[] _processTypes;

    protected Repository _repository;

    protected JDFQueueEntry _runningQueueEntry;
    
    /**
     * @param config
     *            The configuration for this Device.
     * @param queue
     *            The queue for this Device.
     * @param fileUtil
     *            The URL access tool for this device.
     * @param dispatcher
     *            The outgoing dispatcher for this device.
     * @param repository
     *            The repository for this device.
     */
    public BaseProcess(DeviceConfig config, Queue queue,
            URLAccessTool fileUtil, OutgoingJMFDispatcher dispatcher,
            Repository repository) {
        super();
        log = Logger.getLogger(this.getClass().getName());
        _config = config;
        _queue = queue;
        _fileUtil = fileUtil;
        _outgoingDispatcher = dispatcher;
        _state = new ProcessState();        
        _repository = repository;
    }

    /**
     * initialize <this> based on deviceConfig
     * 
     * @param config
     *            the config file to extract the information from
     * @author prosirai
     * @deprecated This method is no longer used and is deprecated together with
     *             get/setProcessType. Instead, get/setProcessTypes should be
     *             used, which return the JDF process types that the device can
     *             execute. [Claes]
     */
    protected void initConfig(DeviceConfig config)
    {
        _config = config;
        if(_config!=null)
        {
            JDFDevice dev =_config.getDeviceConfig();
            if(dev!=null)
                setProcessType(dev.getDeviceType());
        }
    }

    public BaseProcess() {
        super();
        log = Logger.getLogger(this.getClass().getName());
    }

    public String toString() {
        return "Executable processes: " + Arrays.asList(getProcessTypes());
    }

    /**
     * Sets this Process' incoming dispatcher.
     * 
     * @param incomingDispatcher
     *            the dispatcher to set.
     * @throws IllegalArgumentException
     *             if incomingDispatcher is <code>null</code>.
     */
    public void setIncomingDispatcher(IncomingJMFDispatcher incomingDispatcher) {
        _incomingDispatcher = incomingDispatcher;
        if (incomingDispatcher == null) {
            String msg = "incomingDispatcher may not be null.";
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
        log.debug("IncomingDispatcher was set with class '"
                + incomingDispatcher.getClass().getName() + "'");
    }

    /**
     * Sets this process type. It is crucial to set the process type in an
     * implementing Process' constructor.
     * 
     * @param processType
     *            The type to be set.
     * @deprecated Use {@link BaseProcess#setProcessTypes(String[])} instead.
     */
    protected void setProcessType(final String processType) {
        if (processType == null) {
            throw new IllegalArgumentException("Process type must not be null.");
        }
        final String[] processTypes = {processType};        
        setProcessTypes(processTypes);
    }
    
    /**
     * Sets the JDF process types that this Process can execute.
     * @param processTypes
     */
    protected void setProcessTypes(final String[] processTypes) {
        if (processTypes == null || processTypes.length == 0) {
            throw new IllegalArgumentException("Process types must be an non-empty array.");
        }
        _processTypes = processTypes;
    }

    /**
     * Returns the first element in the array of process types, see {@link Process#getProcessTypes()}.
     * 
     * @see org.cip4.elk.device.process.Process#getProcessType()
     * @deprecated Use {@link #getProcessTypes()}
     */
    public String getProcessType() {
        return getProcessTypes()[0];
    }

    public String[] getProcessTypes() {
        return _processTypes;
    }
    
    protected void setProcessState(ProcessState state) {
        _state = state;
    }

    /**
     * Starts this device.
     */
    public void start() {
        log.info("Starting " + getProcessType() + " device...");
        if (_thread == null) {
            _thread = new Thread(this);
        }
        _thread.start();
    }

    public void stop() {
        log.info("Stopping " + getProcessType() + " device...");
        _state.setState(_state.STOPPED);
    }

    /**
     * The device's main loop. Waits if the queue has status <em>Blocked</em>
     * or if there is no runnable job in the queue.
     * 
     * @see java.lang.Runnable#run()
     */
    public void run() {
        while (!_state.getState().equals(_state.STOPPED)) {
            JDFQueueEntry qe = null;
            synchronized (_queue) {
                try {
                    // Wait if:
                    // - Queue is Blocked
                    // - There is a queue entry
                    while ((_queue.getQueueStatus().equals(
                            JDFQueue.EnumQueueStatus.Blocked)
                            || _queue.getQueueStatus().equals(
                                    JDFQueue.EnumQueueStatus.Held) || _queue
                            .getFirstRunnableQueueEntry() == null)) {
                        // Check if device has stopped
                        if (_state.getState().equals(_state.STOPPED)) {
                            log.debug("Stopped running " + getProcessType()
                                    + ".");
                            return;
                        }
                        // Sleep
                        _queue.wait(1000);
                    }
                } catch (InterruptedException ie) {
                    log.error("Interrupted while waiting for queue: " + ie, ie);
                    log.debug("Stopped running " + getProcessType() + ".");
                    return;
                }
                // Gets the job at the top of the queue
                qe = _queue.getFirstRunnableQueueEntry();
            }
            try {
                // Check if device has stopped
                if (_state.getState().equals(_state.STOPPED)) {
                    log.debug("Stopped running " + getProcessType() + ".");
                    return;
                }
                // Runs the queue entry
                runJob(qe, _queue.getQueueSubmissionParams(qe.getQueueEntryID()));
            } catch (Exception e) {
                log.error("An error occurred while running queue entry '"
                        + qe.getQueueEntryID() + "': " + e);
            }
        }
        log.debug("Stopped running " + getProcessType() + ".");
    }

    
    
    
    /**
     * Runs the job represented by the specified queue entry.
     * 
     * @param qe
     *            the job to run
     * @throws Exception
     * @deprecated Use {@link #runJob(JDFQueueEntry, JDFQueueSubmissionParams)}
     */
    public void runJob(JDFQueueEntry qe) throws Exception {
        runJob(qe, _queue.getQueueSubmissionParams(qe.getQueueEntryID()));
    }
    
    
    /**
     * Runs the job represented by the specified queue entry.
     *
     * @see org.cip4.elk.device.process.Process#runJob(org.cip4.jdflib.jmf.JDFQueueEntry, org.cip4.jdflib.jmf.JDFQueueSubmissionParams)
     */
    public JDFNode runJob(JDFQueueEntry qe, JDFQueueSubmissionParams subParams)
            throws Exception {        
        log.debug("Running queue entry '" + qe.getQueueEntryID() + "'...");
        // Check that the device is idle before running the job
        if (!_state.getState().equals(_state.IDLE)) {
            log.warn("Could not run queue entry '" + qe.getQueueEntryID()
                    + "' because the device is not idle.");
            return null;
        }
        _runningQueueEntry = qe;
        
        // Change the queue status
        qe.setStartTime(new JDFDate());
        qe.setQueueEntryStatus(JDFQueueEntry.EnumQueueEntryStatus.Running);
        fireEvent(new ProcessQueueEntryEvent(this, qe));

        JDFNode jdf = null;
        try {
            // Runs the job
            final String jdfUrl = subParams.getURL();
            jdf = runJob(jdfUrl);
            qe
                    .setQueueEntryStatus(JDFQueueEntry.EnumQueueEntryStatus.Completed);
            // Returns the job
            try {
                postProcessJob(qe, jdf);
            } catch (IOException ioe) {
                log.error("Could not post-process JDF '" + jdf.getJobID(true)
                        + "': " + ioe, ioe);
                ioe.printStackTrace();
            }
        } catch (Exception e) {
            qe.setQueueEntryStatus(JDFQueueEntry.EnumQueueEntryStatus.Aborted);
            log.error("An error occurred while process queue entry '"
                    + qe.getQueueEntryID() + "': " + e);
            e.printStackTrace();
            // TODO After an error, should the device become Idle?
        } finally {
            qe.setEndTime(new JDFDate());
            _runningQueueEntry = null;
            fireEvent(new ProcessQueueEntryEvent(this, qe));
            _state.setState(_state.IDLE);
        }
        log.debug("Finished running queue entry '" + qe.getQueueEntryID()
                + "'.");
        return jdf;
    }

    /**
     * Runs a job.
     * 
     * @param jdfUrl
     *            the URL to the job to run
     * @return the processed job
     */
    public JDFNode runJob(String jdfUrl) throws Exception {
        // Setup phase //
        _state.setState(_state.SETUP);
        // Download job
        JDFNode jdf = new JDFParser().parseStream(_repository.getFile(jdfUrl))
                .getJDFRoot();
        _state.setJdfUrl(jdfUrl.toString());
        // Execution phase for Device
        _state.setState(_state.RUNNING);
        List processNodes = JDFUtil.getProcessNodes(getProcessType(), jdf, null);
        if (processNodes==null || processNodes.size() == 0) {
            String err = "Could not execute process because there were no"
                    + " process nodes of type '" + getProcessType()
                    + "' to execute.";
            log.error(err);
            // TODO Handle if there were no process nodes to execute,
            // this should not happen since it should be done in Preprocesssor.
        } else {
            log.info("Found " + processNodes.size() + " nodes to execute.");
            for (int i = 0, imax = processNodes.size(); i < imax; i++) {
                JDFNode jdfNode = (JDFNode) processNodes.get(i);
                if (JDFUtil.isExecutableAndAvailbleResources(jdfNode,
                        null)) {
                    executeNode(jdfNode);
                }
            }
        }
        // Cleanup phase //
        _state.setState(_state.CLEANUP);
        // TODO Clean up
        log.debug("Finished running job: " + jdf);
        return jdf;
    }

    // TODO Read this
    /**
     * Adds PhaseTime audits and ProcessRun audits for the given jdf node.
     * Extended to the accept different status values for a node. The method
     * calculates phase time according to this scheme:
     * <ol>
     * <li>Setup - startTime to endSetUpTime</li>
     * <li>InProgress - endSetUpTime to endTime</li>
     * <li>Completed - endTime</li>
     * <li>ProcessRun - startTime to endTime</li>
     * <li>nodeStatus defines the actual Status of this node for the actual
     * phase</li>
     * </ol>
     * 
     * @param jdf
     * @param startTime
     * @param endSetUpTime
     * @param endTime
     * @param nodeStatus
     */
    public void addAudits(JDFNode jdf, JDFDate startTime, JDFDate endSetUpTime,
            JDFDate endTime, EnumNodeStatus nodeStatus) {
        JDFAuditPool auditPool = jdf.getAuditPool();
        JDFPhaseTime phaseTime = null;
        // Add SetUp Audit.
        if (nodeStatus != null)
            phaseTime = auditPool.addPhaseTime(nodeStatus, _config.getID(),
                    new VJDFAttributeMap());
        else
            phaseTime = auditPool.addPhaseTime(EnumNodeStatus.Setup, _config
                    .getID(), new VJDFAttributeMap());

        phaseTime.setStart(startTime);
        phaseTime.setEnd(endSetUpTime);

        // Add InProgress Audit.
        phaseTime = auditPool.addPhaseTime(EnumNodeStatus.InProgress, _config
                .getID(), new VJDFAttributeMap());
        phaseTime.setStart(endSetUpTime);
        phaseTime.setEnd(endTime);

        // Add process run Audit.
        JDFProcessRun prAudit = jdf.getAuditPool().addProcessRun(
                EnumNodeStatus.Completed, _config.getID(), null);
        prAudit.setStart(startTime);
        prAudit.setEnd(new JDFDate());
    }

    /**
     * Gets or creates a <em>Condition Part</em> element with the specified
     * <code>conditionValue</code> from the given <em>Resource</em> and
     * returns it. <br />
     * TODO Make this method more generic. <br />
     * 
     * @param resource
     *            the Resource to which the Part element will be added.
     * @param conditionValue
     *            the <em>Condition</em> value, the only allowed values are
     *            "Good" or "Waste".
     * @return the <em>Component</em> which has a <em>Part</em> element with
     *         <em>Condition</em> attribute set.
     * @throws IllegalArgumentException
     *             if <code>conditionValue</code> is anything else except
     *             "Good" or "Waste"
     */
    public JDFComponent getCreateConditionPart(JDFResource resource,
            String conditionValue) {

        if (!conditionValue.equals("Good") && !conditionValue.equals("Waste")) {
            String msg = "The conditionValue in addConditionParts must be "
                    + "'Good' or 'Waste', not " + conditionValue + ".";
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
        if (resource != null) {
            JDFResource leaf = (JDFResource) resource.getLeaves(false)
                    .elementAt(0);
            JDFAttributeMap m = leaf.getPartMap();
            if (m.containsKey("Condition")) {
                if (m.get("Condition").equals(conditionValue))
                    return (JDFComponent) leaf;
            }
            m.remove("Condition");
            leaf = resource.getPartition(m, null);
            // leaf = resource.getPartition(m, false);
            m
                    .put(JDFResource.EnumPartIDKey.Condition.getName(),
                            conditionValue);
            JDFComponent conditionPart = (JDFComponent) leaf
                    .getCreatePartition(m, null);
            log.info(conditionPart);
            return conditionPart;
        }
        return null;
    }

    /**
     * Returns a list of all resources that are input resources of the specified
     * JDF process node. Referenced resources (using the rRef attribute), if
     * any, are followed and the referenced resource is returned.
     * 
     * @param jdf
     *            the JDF process node
     * @return a List containing JDFResource elements that are input resources
     */
    protected List getInputResources(JDFNode jdf) {
        return getResources(jdf, "Input");
    }

    /**
     * Returns a list of all resources that are output resources of the
     * specified JDF process node. Referenced resources (using the rRef
     * attribute), if any, are followed and the referenced resource is returned.
     * 
     * @param jdf
     *            the JDF process node
     * @return a List containing JDFResource elements that are output resources
     */
    protected List getOutputResources(JDFNode jdf) {
        return getResources(jdf, "Output");
    }

    /**
     * Returns a list of all resources of the specified JDF process node that
     * are of the specified usage type. Usage type can either be "Input" or
     * "Output". Referenced resources (using the rRef attribute), if any, are
     * followed and the referenced resource is returned.
     * 
     * @param jdf
     *            the JDF process node to get resources from
     * @return a List containing JDFResource elements that are resources of the
     *         specified usage type
     */
    protected List getResources(JDFNode jdf, String usageType) {
        JDFAttributeMap attr = new JDFAttributeMap();
        attr.put("Usage", usageType);
        // TODO Refactor to use JDFNode.getLinkedResources(...)
        List resources = jdf.getResourceLinks(attr);
        JDFResourceLink rLink;
        for (int i = 0, imax = resources.size(); i < imax; i++) {
            // Replaces each ResourceLink the Resource it references
            rLink = (JDFResourceLink) resources.get(i);
            resources.set(i, rLink.getTarget());
        }
        return resources;
    }

    /**
     * Creates a JDFSignal.
     * 
     * @param deviceState
     *            the status of the device.
     * @param description
     *            a description of the event that triggered the signal
     * @return
     */
    protected JDFSignal createSignal(
            JDFDeviceInfo.EnumDeviceStatus deviceState, String description) {
        // TODO Load Signal from template
        JDFSignal sig = JDFElementFactory.getInstance().createJMF()
                .appendSignal();
        sig.setType(EnumType.Status.getName());
        sig.init();
        JDFDeviceInfo info = sig.appendDeviceInfo();
        info.setDeviceStatus(deviceState);

        // TODO CHECK out... Why is deviceState an argument???
        JDFJobPhase phase = getJobPhase();
        if (phase != null) {
            info.copyElement(phase, null);
        }
        sig.appendComment().appendText(description);
        return sig;
    }

    protected void fireProcessStatusEvent(
            JDFDeviceInfo.EnumDeviceStatus status, String message) {
        fireEvent(new ProcessStatusEvent(ProcessStatusEvent.EVENT, status,
                this, message));
    }

    /**
     * Executes the specified JDF process node.
     * 
     * @param processNode
     *            the process node to execute
     */
    protected abstract void executeNode(JDFNode processNode);

    /**
     * For details on which attributes are set see
     * {@link org.cip4.elk.impl.device.BaseProcess.ProcessState#getJobPhase()}
     * {@link org.cip4.elk.impl.device.BaseProcess.ProcessState#getJobPhase()}.
     * 
     * @see org.cip4.elk.device.process.Process#getJobPhase()
     */
    public JDFJobPhase getJobPhase() {
        return _state.getJobPhase();
    }

    /**
     * Return the DeviceInfo of the process. Sets the following:
     * <ul>
     * <li>
     * 
     * @DeviceStatus</li>
     *               <li>Device/@JMFURL</li>
     *               <li>JobPhase if it is available see
     *               {@link org.cip4.org.impl.device.BaseProcess#getJobPhase()}.</li>
     *               </ul>
     *               Override this method in the process node implementation to
     *               add additional values.
     * 
     * @see org.cip4.elk.device.process.Process#getDeviceInfo(boolean)
     */
    public JDFDeviceInfo getDeviceInfo(boolean includeJobPhase) {
        // Get the DEVICE, for Elk, it assumes ONLY ONE Device
        JDFDevice device = _config.getDeviceConfig();
        device.setJMFURL(_config.getJMFURL());
        JDFDeviceInfo deviceinfo = (JDFDeviceInfo) JDFElementFactory
                .getInstance().createJDFElement(ElementName.DEVICEINFO);
        deviceinfo.copyElement(device, null); // add device to deviceInfo
        deviceinfo.setDeviceStatus(getStatus()); // JDF 1.2

        if (includeJobPhase) {
            JDFJobPhase jobPhase = getJobPhase();
            if (jobPhase != null) {
                deviceinfo.copyElement(jobPhase, null);
            } else {
                log.debug("No JobPhase element was included, no jobs are being"
                        + " processed right now.");
            }
        }
        return deviceinfo;
    }

    /**
     * Writes the specified JDF node's owner document to the specified URL.
     * Supported URL schemes are those supported by
     * {@link org.cip4.elk.impl.util.URLAccessTool URLAccessTool}.
     * 
     * @param jdf
     *            the XML document to write
     * @param url
     *            the URL to write the JDF to
     * @throws IOException
     *             if an error occurred while writing the JDF document
     * @see org.cip4.elk.impl.util.URLAccessTool
     */
    protected void writeJDFToURL(JDFElement jdf, String url) throws IOException {
        writeJDFToURL(jdf.getOwnerDocument_KElement(), url);
    }

    /**
     * Writes the specified JDF node's owner document to the specified URL.
     * Supported URL schemes are those supported by
     * {@link org.cip4.elk.impl.util.URLAccessTool URLAccessTool}.
     * 
     * @param jdf
     *            the XML document to write
     * @param url
     *            the URL to write the JDF to
     * @throws IOException
     *             if an error occurred while writing the XML document
     * @see org.cip4.elk.impl.util.URLAccessTool
     */
    protected void writeJDFToURL(XMLDoc jdf, String url) throws IOException {
        log.debug("Writing JDF to URL: " + url);
        OutputStream out = _fileUtil.getURLAsOutputStream(url);
        jdf.write2Stream(out, 0, true);
        IOUtils.closeQuietly(out);
        log.debug("Wrote JDF to URL: " + url);
    }

    /**
     * Post-processes a finished job.
     * 
     * @param qe
     *            the job's queue entry
     * @param jdf
     *            the job's updated JDF
     * @throws IOException
     */
    protected void postProcessJob(JDFQueueEntry qe, JDFNode jdf)
            throws IOException {
        log.debug("Post-processing JDF...");
        // Build JDF file name
        String fileName = generateJDFFileName(jdf);
        // Write JDF file to local output directory
        String localUrl = generateUrl(_config.getLocalJDFOutputURL(), fileName);
        log.debug("Writing backup of JDF to local output URL '" + localUrl
                + "'...");
        writeJDFToURL(jdf, localUrl);
        // Cancel subscriptions
        if (_incomingDispatcher != null) {
            cancelJobSubscriptions(jdf);
        } else {
            log.debug("Can not cancel subscription because no "
                    + "incoming Dispatcher is configured for "
                    + "this Process.");
        }
        // Update queue with new JDF
        JDFQueueSubmissionParams submissionParams = _queue
                .getQueueSubmissionParams(qe.getQueueEntryID());
        submissionParams.setURL(localUrl);
        log.debug("QueueSubmissionParams: " + submissionParams);
        // Sends JDF or JMF over HTTP
        String returnURL = getJDFReturnURL(submissionParams, jdf);
        String returnJMF = submissionParams.getReturnJMF();
        log.info("Return URL: '" + returnURL + "'");
        log.info("Return JMF: '" + returnJMF + "'");
        if (returnJMF != null && returnJMF.length() != 0) {
            // QueueSubmissionParams/@ReturnJMF - send ReturnQueueEntry
            sendReturnQueueEntry(qe, jdf, fileName);
            // This may cause Exceptions. 2005-08-12
        } else if (returnURL.length() != 0) {
            // QueueSubmissionParams/@ReturnURL or JDF/NodeInfo/@TargetRoute -
            // send JDF file
            log.debug("Sending processed JDF to return URL...");
            _outgoingDispatcher.dispatchJDF(jdf, returnURL);
        }
    }

    /**
     * Returns the URL to which the JDF should be sent. URL of the JDF is
     * determined by:
     * <ol>
     * <li>QueueSubmissionParams/@ReturnURL</li>
     * <li>JDF/NodeInfo/@TargetRoute</li>
     * </ol>
     * If none of these are specified, the method returns a <code>String</code>
     * of length 0.
     * 
     * @param submissionParams
     *            the <em>QueueSubmissionParams</em> for this
     *            <em>SubmitQueueEntry</em> Command.
     * @param jdf
     *            the jdf which may contain the
     *            <em>JDF/NodeInfo/@TargetRoute</em>.
     * @return the JDFReturnURL as described above, String of length 0 if not
     *         specified.
     * @throws NullPointerException
     *             if jdf or submissionParams is <code>null</code>.
     */
    private String getJDFReturnURL(JDFQueueSubmissionParams submissionParams,
            JDFNode jdf) {
        if (submissionParams == null) {
            throw new NullPointerException("submissionParams may not be null.");
        } else if (jdf == null) {
            throw new NullPointerException("jdf may not be null.");
        }

        // String jdfReturnUrl = submissionParams.getReturnURL();
        // getReturnURL() returns null used getReturnJMF() instead
        String jdfReturnUrl = submissionParams.getReturnJMF();

        if (jdfReturnUrl.length() != 0) {
            log.debug("QueueSubmissionParams/@ReturnURL specified, JDF "
                    + "returned to '" + jdfReturnUrl + "'.");
        } else {
            JDFNodeInfo ni = jdf.getNodeInfo();
            if (ni != null) {
                String targetRouteUrl = ni.getTargetRoute();
                if (targetRouteUrl.length() != 0) {
                    jdfReturnUrl = targetRouteUrl;
                    log.debug("JDF/NodeInfo/@TargetRoute specified, JDF "
                            + "returned to '" + jdfReturnUrl + "'");
                }
            }
        }
        return jdfReturnUrl;
    }

    /**
     * Sends a <em>Signal</em> with <em>LastRepeat</em> set to
     * <code>true</code> and a <em>StopPeristentChannel</em> message for any
     * <em>Query/Subscription</em> elements in the <em>JDF/NodeInfo</em>.
     * 
     * @param jdf
     *            The JDF Node whose Subscriptions will be cancelled.
     */
    private void cancelJobSubscriptions(JDFNode jdf) {

        JDFNodeInfo nodeInfo = jdf.getNodeInfo();
        if (nodeInfo != null) {
            Vector v = nodeInfo.getChildElementVector(ElementName.JMF, null,
                    new JDFAttributeMap(), true, 0, true);
            log.debug("Found " + v.size() + " JMF messages in JDF/NodeInfo.");

            for (Iterator it = v.iterator(); it.hasNext();) {
                JDFJMF jmf = (JDFJMF) it.next();
                Vector v2 = jmf.getMessageVector (null, null);
                for (Iterator it2 = v2.iterator(); it2.hasNext();) {
                    JDFMessage m = (JDFMessage) it2.next();

                    if (Messages.checkSubscriptionParameters(log, m) == 0) {
                        JDFQuery q = (JDFQuery) m;
                        String channelID = q.getID();
                        String url = q.getSubscription().getURL();
                        sendStopPeristenChannelMessage(channelID, url);
                        log.debug("Sending last Signal for query with id '"
                                + channelID + "'.");
                        sendQuerySignal(null, q, true);
                    } else {
                        log.debug("Message in JDF/NodeInfo had an invalid "
                                + "Subscription message.");
                    }
                }
            }
        } else {
            log.debug("No Subscription needs to be cancelled, node with id '"
                    + jdf.getID() + "' does not contain a NodeInfo element.");
        }
    }

    /**
     * Sends a <em>StopPersistenChannel</em> message for the given channelID
     * and url.
     * 
     * @param channelID
     *            the channelID of the <em>PersistentChannel</em> to stop.
     * @param url
     *            the url of the <em>PersistentChannel</em> to stop.
     */
    private void sendStopPeristenChannelMessage(String channelID, String url) {
        log.debug("About to send a StopPersistentChannelMessage for channel '"
                + channelID + "' and url '" + url + "'.");
        JDFCommand cmd = Messages.createCommand("StopPersistentChannel");
        JDFStopPersChParams params = cmd.appendStopPersChParams();
        params.setChannelID(channelID);
        params.setURL(url);
        JDFJMF jmf = Messages.createJMFMessage(cmd, _config.getID());
        _incomingDispatcher.dispatchJMF(jmf);
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
    private boolean sendQuerySignal(ElkEvent event, JDFQuery query,
            boolean lastRepeat) {
        boolean signalSent = false;

        log.debug("Sending signal to subscriber whose original query was: "
                + query);

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
                .getResponse(0);

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
            if (lastRepeat) {
                signal.setLastRepeat(true);
            }
            JDFJMF jmf = Messages.createJMFMessage(signal, _config.getID());
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
     * Makes this Thread sleep for the given amount of seconds.
     * 
     * @param seconds
     *            The number of seconds the Thread shall sleep.
     * @throws InterruptedException
     *             if Thread is interrupted.
     */
    protected void sleepAWhile(int seconds) {
        try {
            log.debug("Sleeping...");

            Thread.sleep(seconds * 1000);
            log.debug("Slept.");
        } catch (InterruptedException ie) {
            log.error("The sleeping was interrupted.");
        }

    }

    /**
     * Sends a ReturnQueueEntry.
     * 
     * @param qe
     *            the queue entry to return
     * @param jdf
     *            the JDF to return
     * @param filename
     *            the name of the file in the local file system
     */
    private void sendReturnQueueEntry(JDFQueueEntry qe, JDFNode jdf,
            String filename) 
    {

        JDFQueueSubmissionParams submissionParams = _queue
                .getQueueSubmissionParams(qe.getQueueEntryID());
        String returnJmfUrl = submissionParams.getReturnJMF();
        log.debug("Sending ReturnQueueEntry JMF with processed JDF to "
                + "return URL '" + returnJmfUrl + "'...");
        JDFCommand command = Messages
                .createCommand(JDFMessage.EnumType.ReturnQueueEntry.getName());
        returnJmfUrl = submissionParams.getReturnJMF();

        JDFReturnQueueEntryParams returnParams = command
                .appendReturnQueueEntryParams();

        // Get nodes that are completed
        List completedNodes = JDFUtil.getProcessNodes(getProcessType(), jdf, JDFElement.EnumNodeStatus.Completed);
        if (completedNodes != null && completedNodes.size() > 0) {
            Vector completedIDs = new Vector();
            for (int i = 0, imax = completedNodes.size(); i < imax; i++) {
                completedIDs.add(((JDFNode) completedNodes.get(i)).getID());
            }
            returnParams.setCompleted(new VString(completedIDs));
        }
        // Get nodes that are aborted
        List abortedNodes = JDFUtil.getProcessNodes(getProcessType(), jdf, JDFElement.EnumNodeStatus.Aborted);
        if (abortedNodes != null && abortedNodes.size() > 0) {
            Vector abortedIDs = new Vector();
            for (int i = 0, imax = abortedNodes.size(); i < imax; i++) {
                abortedIDs.add(((JDFNode) abortedNodes.get(i)).getID());
            }
            returnParams.setAborted(new VString(abortedIDs));
        }
        // Set priority
        returnParams.setPriority(qe.getPriority());
        // Set remote URL
        String remoteUrl = generateUrl(_config.getJDFOutputURL(), filename);
        returnParams.setURL(remoteUrl);
        // Send JMF
        JDFJMF jmf = Messages.createJMFMessage(command, "Elk");
        _outgoingDispatcher.dispatchJMF(jmf, returnJmfUrl);
    }

    /**
     * Generates a unique URL for a file.
     * 
     * @param baseUrl
     *            the URL that the file name is based on
     * @param fileName
     *            the file name
     * @return a generated URL
     */
    private String generateUrl(String baseUrl, String fileName) {
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        return baseUrl + fileName;
    }

    /**
     * Generates a unique JDF file name. The generated file name is guaranteed
     * to be unique as long as this method is not called more than once per
     * millisecond for JDFs with the same JDF ID.
     */
    private String generateJDFFileName(JDFNode jdf) {
        return jdf.getID() + "_ELK" + System.currentTimeMillis() + ".jdf";
    }
    
    
    /**
     * @see org.cip4.elk.device.process.Process#getRunningQueueEntry()
     */
    public JDFQueueEntry getRunningQueueEntry() {
        return _runningQueueEntry;
    }

    // ******************************************************
    // Methods common to ALL Processes inherited from Process
    // Override these in case special behaviour is desired.
    /*
     * (non-Javadoc)
     * 
     * @see org.cip4.elk.device.process.Process#wakeUp()
     */
    public void wakeUp() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cip4.elk.device.process.Process#setDeviceConfig(org.cip4.elk.device.DeviceConfig)
     */
    public void setDeviceConfig(DeviceConfig config) {
        _config = config;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cip4.elk.device.process.Process#getDeviceConfig()
     */
    public DeviceConfig getDeviceConfig() {
        return _config;
    }

    /**
     * @see org.cip4.elk.device.process.Process#getProcessId()
     * @deprecated Use {@link DeviceConfig#getDeviceConfig()}.getDeviceID() instead.
     */
    public String getProcessId() {
        return getDeviceConfig().getDeviceConfig().getDeviceID();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cip4.elk.device.process.Process#getStatus()
     */
    public JDFDeviceInfo.EnumDeviceStatus getStatus() {
        return _state.getState();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cip4.elk.device.process.Process#abortJob()
     */
    public void abortJob() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cip4.elk.device.process.Process#testRunJob(java.lang.String)
     */
    public void testRunJob(String jdfUrl) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cip4.elk.device.process.Process#flushResources()
     */
    public void flushResources() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cip4.elk.device.process.Process#sleep()
     */
    public void sleep() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cip4.elk.device.DeviceProcess#init()
     */
    public void init() {
        _state.setState(_state.IDLE);
        start();
    }

    public void setUp() {
        init();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cip4.elk.device.DeviceProcess#destroy()
     */
    public void destroy() {
        _state.setState(_state.STOPPED);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cip4.elk.device.process.Process#suspendJob()
     */
    public void suspendJob() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cip4.elk.device.process.Process#resumeJob()
     */
    public void resumeJob() {
        // TODO Auto-generated method stub

    }

    // **************************************************************
    /**
     * A class that models the state of a device process.
     * 
     * @author Claes Buckwalter (clabu@itn.liu.se)
     */
    protected class ProcessState {
        public final JDFDeviceInfo.EnumDeviceStatus UNKNOWN = JDFDeviceInfo.EnumDeviceStatus.Unknown; // JDFConstants.UNKNOWN;

        public final JDFDeviceInfo.EnumDeviceStatus IDLE = JDFDeviceInfo.EnumDeviceStatus.Idle; // JDFConstants.IDLE;

        public final JDFDeviceInfo.EnumDeviceStatus DOWN = JDFDeviceInfo.EnumDeviceStatus.Down; // JDFConstants.DOWN;

        public final JDFDeviceInfo.EnumDeviceStatus SETUP = JDFDeviceInfo.EnumDeviceStatus.Setup; // JDFConstants.SETUP;

        public final JDFDeviceInfo.EnumDeviceStatus RUNNING = JDFDeviceInfo.EnumDeviceStatus.Running; // JDFConstants.RUNNING;

        public final JDFDeviceInfo.EnumDeviceStatus CLEANUP = JDFDeviceInfo.EnumDeviceStatus.Cleanup; // JDFConstants.CLEANUP;

        public final JDFDeviceInfo.EnumDeviceStatus STOPPED = JDFDeviceInfo.EnumDeviceStatus.Stopped; // JDFConstants.STOPPED;

        private JDFNode jdf;

        private String jdfUrl;

        private JDFDeviceInfo.EnumDeviceStatus state;

        private boolean _stateChanged = false;

        private boolean running;

        public boolean _statusEvent = false;

        private Logger log;

        protected ProcessState() {
            log = Logger.getLogger(this.getClass().getName());
            state = UNKNOWN;
            jdf = null;
            jdfUrl = null;
            running = false;
        }

        protected void setRunning(boolean running) {
            this.running = running;
        }

        protected boolean isRunning() {
            return running;
        }

        protected void setJdf(JDFNode jdf) {
            this.jdf = jdf;
        }

        protected JDFNode getJdf() {
            return jdf;
        }

        protected void setJdfUrl(String _jdfUrl) {
            this.jdfUrl = _jdfUrl;
            jdf = null;
        }

        protected String getJdfUrl() {
            return jdfUrl;
        }

        /**
         * Default state change
         * 
         * @param state
         */
        protected synchronized void setState(
                JDFDeviceInfo.EnumDeviceStatus state) {
            // Determines if a new JobPhases begins
            if (state.equals(this.state))
                _stateChanged = false;
            else
                _stateChanged = true;

            JDFDeviceInfo.EnumDeviceStatus oldState = this.state;
            this.state = state;
            String msg = "Process status change: " + oldState.getName()
                    + " to " + this.state.getName();

            if (state.equals(IDLE)) {
                jdf = null;
                jdfUrl = null;
                running = false;
            }
            log.debug(msg);
            setStatusEvent(true);
            fireProcessStatusEvent(this.state, msg);

            // TODO Temp solution for time-based signals
            // _stateChanged=false;
        }

        /**
         * Extended state change, comments can be added and the same state can
         * be applied infinite times
         * 
         * @author Marco Kornrumpf
         * 
         * @param state
         * @param comment
         */
        protected synchronized void setState(
                JDFDeviceInfo.EnumDeviceStatus state, String comment) {

            // Determines if the state of the device has changed
            if (state.equals(this.state))
                _stateChanged = false;
            else
                _stateChanged = true;
            JDFDeviceInfo.EnumDeviceStatus oldState = this.state;
            this.state = state;
            String msg = "Process status change: " + oldState.getName()
                    + " to " + this.state.getName() + " :" + comment;

            if (state.equals(IDLE)) {
                jdf = null;
                jdfUrl = null;
                running = false;
            }
            if (log.isDebugEnabled())
                log.debug(msg);
            setStatusEvent(true);
            fireProcessStatusEvent(this.state, msg);

            // TODO Temp solution for time-based signals
            // _stateChanged=false;
        }

        protected JDFDeviceInfo.EnumDeviceStatus getState() {
            return state;
        }

        public String toString() {
            return "DeviceState[ State: " + state + ";  JDF URL: '" + jdfUrl
                    + "']";
        }

        /**
         * Returns the JobPhase for the current job. If no job is running
         * <code>null</code> is returned. The method will set the following
         * attributes:
         * <ul>
         * <li>JobPhase/@JobID</li>
         * <li>JobPhase/@JobPartID</li>
         * <li>JobPhase/@Status</li>
         * </ul>
         */
        protected JDFJobPhase getJobPhase() {
            if (jdf == null || jdfUrl == null) {
                return null;
            }
            JDFJobPhase phase = (JDFJobPhase) JDFElementFactory.getInstance()
                    .createJDFElement(ElementName.JOBPHASE);
            phase.setJobID(jdf.getJobID(true));
            phase.setJobPartID(jdf.getJobPartID(false));
            phase.setStatus(jdf.getStatus());
            return phase;
        }

        /**
         * Determines if a change of the DeviceStatus occurred if true a new
         * JobPhase has to be send
         * 
         * @return Returns the stateChanged.
         */
        protected synchronized boolean isStateChanged() {
            return _stateChanged;
        }

        /**
         * @return Returns the statusEvent.
         */
        protected synchronized boolean isStatusEvent() {
            return _statusEvent;
        }

        /**
         * @param statusEvent
         *            The statusEvent to set.
         */
        protected synchronized void setStatusEvent(boolean statusEvent) {
            _statusEvent = statusEvent;
        }
    }
}
