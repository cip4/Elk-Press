/*
 * Created on Jun 9, 2005 
 */
package org.cip4.elk.impl.device.process;

import java.util.List;

import org.cip4.elk.device.DeviceConfig;
import org.cip4.elk.device.process.ProcessAmountEvent;
import org.cip4.elk.impl.util.Repository;
import org.cip4.elk.impl.util.URLAccessTool;
import org.cip4.elk.jmf.OutgoingJMFDispatcher;
import org.cip4.elk.queue.Queue;
import org.cip4.jdflib.auto.JDFAutoNotification.EnumClass;
import org.cip4.jdflib.core.AttributeName;
import org.cip4.jdflib.core.JDFElement;
import org.cip4.jdflib.core.JDFPartAmount;
import org.cip4.jdflib.core.JDFResourceLink;
import org.cip4.jdflib.core.KElement;
import org.cip4.jdflib.core.JDFElement.EnumNodeStatus;
import org.cip4.jdflib.datatypes.JDFAttributeMap;
import org.cip4.jdflib.datatypes.VJDFAttributeMap;
import org.cip4.jdflib.jmf.JDFDeviceInfo;
import org.cip4.jdflib.jmf.JDFJobPhase;
import org.cip4.jdflib.node.JDFNode;
import org.cip4.jdflib.pool.JDFAmountPool;
import org.cip4.jdflib.resource.JDFNotification;
import org.cip4.jdflib.resource.JDFResource;
import org.cip4.jdflib.resource.JDFResource.EnumResStatus;
import org.cip4.jdflib.resource.process.JDFComponent;
import org.cip4.jdflib.util.JDFDate;

/**
 * A Device that implements the <em>ConventionalPrinting Process</em>.
 * 
 * NOTE: If the process pagesPerMinute is not set it defaults to 1000 pages/min.
 * 
 * @see <a
 *      href="http://www.cip4.org/document_archive/documents/ICS-Base-1.0.pdf">ICS-Base-1.0
 *      Specification, 5.4.1 KnownDevices </a>
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: ConventionalPrintingProcess.java,v 1.10 2006/12/03 21:22:53 buckwalter Exp $
 */
public class ConventionalPrintingProcess extends BaseProcess {

    /** The type of this Process */
    public static final String[] PROCESS_TYPES = {"ConventionalPrinting"};

    private int _totalAmount = 0; // The total amount for each process node.
    // Reset in class State for each Node.
    private double _wastePercentage = 1.0;
    private int _wasteFrequency = (int) (100.0 / _wastePercentage);

    /** The time in seconds that this Process takes to set up. */
    protected int _setUpTime = 2;
    /** The time in seconds that this Process takes to be in progress. */
    protected int _inProgressTime = 8;

    /** The number of pages per minute this Process is able to Print. */
    protected int _pagesPerMinute = 1000;
    
    // Device specific settings.
    private String _counterUnit = "Sheets";
    private volatile int _totalProductionCounter = 0; 
    
    private int _waitingTimeMillis = 1;
    static final VJDFAttributeMap _emptyVJDFAttributeMap = new VJDFAttributeMap();

    /**
     * Creates a ConventionalPrinting Process.
     * 
     * @param config this Device's configuration.
     * @param queue this Device's queue.
     * @param fileUtil this Device's utility for accessing the file system.
     * @param dispatcher this Device's outgoing JMF Dispatcher.
     * @param repository this Device's repository.
     */
    public ConventionalPrintingProcess(DeviceConfig config, Queue queue,
            URLAccessTool fileUtil, OutgoingJMFDispatcher dispatcher,
            Repository repository) {
        super(config, queue, fileUtil, dispatcher, repository);
        setProcessTypes(PROCESS_TYPES);
        _waitingTimeMillis = 60000 / _pagesPerMinute;
    }

    /**
     * Sets the wastePercentage this <em>ConventionalPrinting Process</em>
     * produces.
     * 
     * Default is 1 % waste.
     * 
     * @param wastePercentage The percentage of waste, must be >= 0.0 and <=
     *            100.0.
     * @throws IllegalArgumentException if wastePercentage < 0.0 or > 100.0.
     */
    public void setWastePercentage(double wastePercentage) {
        if (wastePercentage < 0.0 || wastePercentage > 100.0) {
            String msg = "wastePercenatage must be given in percentage (>= 0.0 and <= 100.0), not: "
                    + wastePercentage;
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
        _wastePercentage = wastePercentage;
        _wasteFrequency = (int) (100.0 / _wastePercentage);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cip4.elk.impl.device.process.BaseProcess#executeNode(org.cip4.jdflib.node.JDFNode)
     */
    protected void executeNode(JDFNode jdf) {
        log.debug("About to execute node for '" + getProcessId() + "'.");
        JDFAttributeMap emptyAttributeMap = new JDFAttributeMap();
        JDFDate startTime = new JDFDate();

        // Add Modified Audit
        jdf.getCreateAuditPool().addModified(_config.getID(), null);
        _state.setJdf(jdf);

        // Setup phase ...
        jdf.setStatus(EnumNodeStatus.Setup);
        log.debug("Setting up Device...");
        sleepAWhile(_setUpTime);
        JDFDate endSetUpTime = new JDFDate();

        // InProgress phase ...
        jdf.setStatus(EnumNodeStatus.InProgress);
        // TODO Verify that the process and is correctly specified
        // TODO Send JMF Signal messages for each processing step

        JDFResourceLink resLinkAmountOut = getOutputResourceLink(jdf);
        JDFResourceLink resLinkAmountIn = getAmountResource("Input", jdf);

        if (resLinkAmountOut == null) {
            String msg = "The Elk could not execute the JDFNode. No Output Resource is given.";
            log.error(msg);
            JDFNotification n = jdf.getCreateAuditPool().addNotification(
                EnumClass.Error, this.getClass().getName(),
                new VJDFAttributeMap());
            n.appendComment().appendText(msg);
            jdf.setStatus(EnumNodeStatus.Aborted);
            // TODO generate events.
            return;
        }

        // Creates an AmountPool
        log.debug("Gets/Creates AmountPool for Output resource...");
        JDFAmountPool amountPool = resLinkAmountOut.getCreateAmountPool();
        JDFResource resAmountOut = (JDFResource) resLinkAmountOut.getTarget();

        // Creates the Condition='Good' and 'Waste' Parts if they don't exist.
        JDFComponent goodPart = getCreateConditionPart(resAmountOut, "Good");
        JDFComponent wastePart = getCreateConditionPart(resAmountOut, "Waste");

        // Adds the (output) AmountPool/PartAmount elements if they don't exist.
        JDFPartAmount partAmountGood = 
                amountPool.getCreatePartAmount(goodPart.getPartMap());
        JDFPartAmount partAmountWaste = 
                amountPool.getCreatePartAmount(wastePart.getPartMap());

        _totalAmount = (int) getTotalAmount(jdf, resLinkAmountOut, resLinkAmountIn,
            partAmountGood);

        int inAmount = 0;
        if (resLinkAmountIn != null) {
            inAmount = (int) resLinkAmountIn.getAmount(emptyAttributeMap);
            log.info("Available amount " + (int) inAmount + ", desired amount "
                    + (int) _totalAmount);
            if (_totalAmount > inAmount && inAmount != 0) {
                // if the desired amount is more than the available amount
                _totalAmount = inAmount;
            }
        } else {
            log.debug("No input resource with amount attribute was given. "
                    + "Unlimited resources available.");
            log.info("Printing amount: " + (int) _totalAmount);
        }

        log.info("Estimated print time: "
                + Integer.toString(_waitingTimeMillis * _totalAmount / 1000)
                + " seconds.");

        if (resLinkAmountIn != null) {

        }
        int actualWaste = 0;
        int producedAmount = 0;

        // Produce until desired or available amount is reached.
        for (int i = 1; producedAmount <= _totalAmount - 1
                && (producedAmount <= inAmount || inAmount == 0); i++) {
            try {
                Thread.sleep(_waitingTimeMillis);
            } catch (InterruptedException e) {
                log.error("Thread was interrupted in method"
                        + " executeNode(JDFNode), execution continued.", e);
            }
            if (i % _wasteFrequency == 0) {
                actualWaste++;
                log.debug("Produced waste, actualWate: " + actualWaste);
                partAmountWaste.setAttribute("ActualAmount", ""+actualWaste); ///XXX partAmountWaste.setActualAmount(actualWaste, emptyAttributeMap);
            } else {
                producedAmount++;
                if (i % 10 == 0) {
                    log.debug("ActualAmount=" + producedAmount + " Total=" + _totalAmount);
                }
                resLinkAmountOut.setActualAmount(producedAmount, emptyAttributeMap);                
                partAmountGood.setAttribute("ActualAmount", ""+producedAmount); //XXX partAmountGood.setActualAmount(producedAmount, emptyAttributeMap);
                _amountNotifier.fireEvent(new ProcessAmountEvent(
                        EnumClass.Event, producedAmount, this,
                        "Amount changed event"));
            }
            _totalProductionCounter++;
            
            if (resLinkAmountIn != null) {
                resLinkAmountIn.setActualAmount(actualWaste + producedAmount,
                    emptyAttributeMap);
            }
            resLinkAmountOut.setActualAmount(actualWaste + producedAmount,
                emptyAttributeMap);
        }

        List outRes = getOutputResources(jdf);
        for (int i = 0; i < outRes.size(); i++) {
            // Set all OutputResources to status available.
            setResourceStatus(((JDFResource) outRes.get(i)).getResourceRoot(),
                EnumResStatus.Available);
        }

        log.debug("Device is in progress for another " + _inProgressTime
                + " seconds.");
        sleepAWhile(_inProgressTime);
        // Add audits
        addAudits(jdf, startTime, endSetUpTime, new JDFDate(), null);
        // Update the JDF node's state
        jdf.setStatus(EnumNodeStatus.Completed);

    }

    public double getTotalAmount(JDFNode jdf, JDFResourceLink resAmountOut,
            JDFResourceLink resAmountIn, JDFPartAmount partAmountGood) {
        JDFAttributeMap emptyAttributeMap = new JDFAttributeMap();
        int outAmount = (int) resAmountOut.getAmount(emptyAttributeMap);
        int partAmountGoodAmount = 0;
        try {
            partAmountGoodAmount = Integer.parseInt(partAmountGood.getAttribute("Amount")); //(int) partAmountGood.getAmount(emptyAttributeMap);
        } catch(NumberFormatException nfe) {
            partAmountGoodAmount = 0;
        }
        log.debug("ComponentLink(output)/@Amount=" + outAmount
                + " PartAmount/@Amount=" + partAmountGoodAmount);

        if (partAmountGoodAmount > 0) {
            // There was a PartAmount/Part/@Condition="Good" defined.
            if (partAmountGoodAmount < outAmount) {
                log.info("The PartAmount/Part/@Condition='Good' with the "
                        + "amount " + partAmountGoodAmount
                        + " overrides the Component/@Amount (" + outAmount
                        + ").");
                outAmount = partAmountGoodAmount;
            } else {
                if (outAmount != 0) {
                    log.warn("The JDFNode is not correctly specified, the "
                            + "ResourceLink (output)/@Amount is less than "
                            + "ResourceLink (output)/PartAmount/@Amount, "
                            + "execution continued with the "
                            + "ResourceLink/@Amount='" + outAmount
                            + "', ignoring the PartAmount/@Amount='"
                            + partAmountGoodAmount + "'.");
                } else {
                    outAmount = partAmountGoodAmount;
                    log.debug("The JDFNode's ResourceLink (output)/@Amount"
                            + " is not given or <= 0. "
                            + "ResourceLink(output)/" + "PartAmount/@Amount='"
                            + outAmount + "' is used.");
                }
            }
        }

        return outAmount;
    }

    /**
     * Sets this <code>JDFResource</code> and all its children of type
     * <code>JDFResource</code> to the given <em>Status</em>. If the
     * incoming object is not of type <code>JDFResource</code>, no action is
     * taken.
     * 
     * @param resource The incoming Resource whose status to be set.
     * @param status The new status. Old status will be be overwritten.
     */
    private void setResourceStatus(Object resource, EnumResStatus status) {

        if (resource instanceof JDFResource) {
            KElement[] elems = ((JDFElement) resource).getChildElementArray();
            ((JDFResource) resource).setStatus(status);
            for (int i = 0; i < elems.length; i++) {
                setResourceStatus(elems[i], status);
            }
        }
    }

    /**
     * Gets the <em>OutputResourceLink</em> from the given jdf.
     * 
     * If the jdf contains more than ONE <em>OutputResourceLink</em> an error
     * is logged and the first <em>OutputResourceLink</em> is returned. If the
     * jdf contains no <em>OutputResourceLink</em>s <code>null</code> is
     * returned and an error is logged.
     * 
     * TODO Generate Notfication and events on error.
     * 
     * @param jdf the jdf whose <em>OutputResourceLink</em> is returned.
     * @return The OutputResourceLink for the given JDF, <code>null</code> if
     *         no output <em>ResourceLink</em>s were defined.
     * @throws NullPointerException if jdf is <code>null</code>.
     */
    public JDFResourceLink getOutputResourceLink(JDFNode jdf) {
        JDFAttributeMap m = new JDFAttributeMap();
        m.put("Usage", "Output");

        if (jdf == null) {
            String msg = "The incoming jdf node may not be null.";
            log.error(msg);
            throw new NullPointerException(msg);
        }
        List resources = jdf.getResourceLinks(m);

        if (resources.size() == 0) {
            log.error("No output resources were defined.");
            return null;
        } else if (resources.size() != 1) {
            log.error("The jdf is not correctly specified, only one "
                    + "Output Resource is allowed for process of type "
                    + getProcessType());
        }

        return (JDFResourceLink) resources.get(0);

    }

    /**
     * Returns the <em>ResourceLink</em> from the
     * <em>JDF/ResourceLinkPool</em> with the specified <em>Usage</em> and
     * its <em>Amount</em> attribute set. If multiple <em>ResourceLink</em>
     * s has the <em>Amount</em> attribute set the first (specified first in
     * the XML-file) <em>ResourceLink</em> is returned. If no <em>Amount</em>
     * attributes are set <code>null</code> is returned.
     * 
     * @param usage The <em>Usage</em> of the Resource, must be "Input" or
     *            "Output"
     * @param jdf the <code>JDFNode</code> to with the <em>Resource</em> s
     *            to check.
     * @return The <em>ResourceLink</em> with <em>Amount</em> attribute,
     *         <code>null</code> if no <em>Amount</em> attributes are set.
     * @throws IllegalArgumentException if usage is not equal to 'Input' or
     *             'Output'
     * @throws NullPointerException if usage or jdf is <code>null</code>.
     */
    public JDFResourceLink getAmountResource(String usage, JDFNode jdf) {
        JDFAttributeMap m = new JDFAttributeMap();
        m.put("Usage", usage);
        List resources = jdf.getResourceLinks(m);
        JDFResourceLink tmp = null;
        boolean hasAmount = false;

        if (usage.equals("Output") || usage.equals("Input")) {

            log.debug("Scanning " + resources.size() + " " + usage
                    + " resources for Amount attributes");
            for (int i = 0, imax = resources.size(); i < imax; i++) {
                JDFResourceLink res = (JDFResourceLink) resources.get(i);
                if (log.isDebugEnabled()) {
                    log.debug("Resource " + res);
                }

                if (res.hasAttribute(AttributeName.AMOUNT)) {
                    if (hasAmount) {
                        log.warn("This Process does not implement JDF Nodes"
                                + " with multiple Resources with Amount "
                                + "attributes. Only last Amount attribute "
                                + "will be considered.");
                    } else {
                        tmp = res;
                        hasAmount = true;
                    }
                }
            }

        } else {
            String msg = "The usage parameter must be either 'Input' or 'Output', not '"
                    + usage + "'.";
            log.error(msg);
            throw new IllegalArgumentException(msg);

        }
        return tmp;
    }
    
    /**
     * Sets additional values in the <em>DeviceInfo</em> element.
     * The following values that are set:
     * <ul>
     * <li>DeviceSpeed</li>
     * <li>CounterUnit</li>
     * <li>TotalProductionCounter</li>
     * </ul>
     * @see org.cip4.elk.device.process.Process#getDeviceInfo(boolean)
     */
    public JDFDeviceInfo getDeviceInfo(boolean includeJobPhase){
        JDFDeviceInfo deviceInfo = super.getDeviceInfo(true);
        deviceInfo.setSpeed(getDeviceSpeed());
        deviceInfo.setCounterUnit(_counterUnit);
        deviceInfo.setTotalProductionCounter(_totalProductionCounter);        
        return deviceInfo;
    }
    /*
     * (non-Javadoc)
     * 
     * @see org.cip4.elk.device.process.Process#getJobPhase()
     */
    public JDFJobPhase getJobPhase() {

        JDFJobPhase jobPhase = super.getJobPhase();

        if (jobPhase != null) {
            JDFNode jdf = _state.getJdf();
            synchronized (jdf) {
                JDFAttributeMap emptyAttributeMap = new JDFAttributeMap();
                double producedAmount = getOutputResourceLink(jdf)
                        .getActualAmount(emptyAttributeMap);
                jobPhase.setAmount(producedAmount);
                jobPhase.setTotalAmount(_totalAmount);
                jobPhase.setPercentCompleted(Math.min((int) ((double) producedAmount
                        / (double) _totalAmount * 100),100)); // XXX not a nice solution...
            }

        }
        return jobPhase;
    }

    /**
     * Sets the Device's speed in the unit sheets/hour.
     * @param speed
     */
    public void setSpeed(int speed) {
        _pagesPerMinute = speed / 60;        
    }
    /**
     * Returns the speed of the device in the unit sheets/hour.
     * @return the speed of the device in the unit sheets/hour.
     */
    public int getDeviceSpeed(){
        return _pagesPerMinute * 60;
    }
}
