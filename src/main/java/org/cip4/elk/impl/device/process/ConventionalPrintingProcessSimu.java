package org.cip4.elk.impl.device.process;

import java.util.ArrayList;
import java.util.List;

import org.cip4.elk.JDFElementFactory;
import org.cip4.elk.device.DeviceConfig;
import org.cip4.elk.device.process.ProcessAmountEvent;
import org.cip4.elk.impl.device.process.simulation.Cleanup;
import org.cip4.elk.impl.device.process.simulation.ConfSimuHandler;
import org.cip4.elk.impl.device.process.simulation.DeviceStatusDetails;
import org.cip4.elk.impl.device.process.simulation.Down;
import org.cip4.elk.impl.device.process.simulation.Running;
import org.cip4.elk.impl.device.process.simulation.Setup;
import org.cip4.elk.impl.device.process.simulation.SimulationPhaseInterface;
import org.cip4.elk.impl.device.process.simulation.Stopped;
import org.cip4.elk.impl.util.Repository;
import org.cip4.elk.impl.util.URLAccessTool;
import org.cip4.elk.jmf.OutgoingJMFDispatcher;
import org.cip4.elk.queue.Queue;
import org.cip4.elk.util.JDFUtil;
import org.cip4.jdflib.auto.JDFAutoDeviceInfo.EnumDeviceStatus;
import org.cip4.jdflib.auto.JDFAutoNotification.EnumClass;
import org.cip4.jdflib.core.AttributeName;
import org.cip4.jdflib.core.ElementName;
import org.cip4.jdflib.core.JDFElement;
import org.cip4.jdflib.core.JDFParser;
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
import org.cip4.jdflib.pool.JDFResourcePool;
import org.cip4.jdflib.resource.JDFNotification;
import org.cip4.jdflib.resource.JDFPart;
import org.cip4.jdflib.resource.JDFResource;
import org.cip4.jdflib.resource.JDFResource.EnumResStatus;
import org.cip4.jdflib.resource.process.JDFComponent;
import org.cip4.jdflib.util.JDFDate;
import org.cip4.jdflib.util.JDFDuration;

/**
 * A simulation of a the JDF ConventionalPrinting process.
 * 
 * @version $Id$
 * @author Marco Kornrumpf (Marco.Kornrumpf@Bertelsmann.de)
 */
public class ConventionalPrintingProcessSimu extends BaseProcess {

	/** The type of this Process */
	public static final String[] PROCESS_TYPES = {"ConventionalPrinting"};

	private int _totalAmount = 0;

	// Device specific settings.
	private final String _counterUnit = "Sheets";

	/** If true device goodcopycounter is on */
	private boolean _wasteCounter = false;

	private int _totalProductionCounter = 0;

	private DeviceStatusDetails _statusDetails = null;

	private ArrayList _jobPhaseMap = null;

	private JDFJobPhase _actualJobPhase = null;

	private JDFDate _phaseStartTime = null;

	private JDFDate _startTime = null;

	private double _oldPhaseWaste = 0;

	private double _oldPhaseAmount = 0;

	private int _deviceSpeed = 0;
	
	private ArrayList simuPhases = null;

	/**
	 * Creates a ConventionalPrinting Process.
	 * 
	 * @param config
	 *            this Device's configuration.
	 * @param queue
	 *            this Device's queue.
	 * @param fileUtil
	 *            this Device's utility for accessing the file system.
	 * @param dispatcher
	 *            this Device's outgoing JMF Dispatcher.
	 * @param repository
	 *            this Device's repository.
	 */
	public ConventionalPrintingProcessSimu(DeviceConfig config, Queue queue,
			URLAccessTool fileUtil, OutgoingJMFDispatcher dispatcher,
			Repository repository) {
		super(config, queue, fileUtil, dispatcher, repository);
		setProcessTypes(PROCESS_TYPES);
		

	}

//	/**
//	 * Hardcoded Simulation Setup for executeNode
//	 * 
//	 * @return ArrayList with all Setup Phases
//	 */
//	private ArrayList generateSimuPhases() {
//
//		ArrayList simuPhases = new ArrayList();
//
//		Setup setup1 = new Setup(5, "Device warming up... Waste=0, Time=5");
//		setup1.setStatusDetails(DeviceStatusDetails.WARMINGUP);
//		setup1.setDeviceSpeed(0);
//		simuPhases.add(setup1);
//
//		Setup setup2 = new Setup(5,
//				"Format change for new media size... Waste=0, Time=5");
//		setup2.setDeviceSpeed(0);
//		setup2.setStatusDetails(DeviceStatusDetails.SIZECHANGE);
//		simuPhases.add(setup2);
//
//		Setup setup3 = new Setup(5,
//				"Formchange for the new job... Waste=0, Time=5");
//		setup3.setDeviceSpeed(0);
//		setup3.setStatusDetails(DeviceStatusDetails.FORMCHANGE);
//		simuPhases.add(setup3);
//
//		Setup setup4 = new Setup(5,
//				"General Setup before starting printing... Waste=0, Time=5");
//		setup4.setDeviceSpeed(0);
//		setup4.setStatusDetails(DeviceStatusDetails.WASTE);
//		simuPhases.add(setup4);

//		Running run1 = new Running("Running but producing waste... Waste=100");
//		run1.setStatusDetails(DeviceStatusDetails.WASTE);
//		run1.setDeviceSpeed(10000);
//		run1.setGoodVariance(0);
//		run1.setWasteVariance(1);
//		simuPhases.add(run1);

//		Running run2 = new Running("Running producing good... Good=200");
//		run2.setStatusDetails(DeviceStatusDetails.GOOD);
//		run2.setDeviceSpeed(20000);
//		run2.setGoodVariance(20);
//		run2.setWasteVariance(0);
//		simuPhases.add(run2);
//
//		Stopped stop1 = new Stopped(5,
//				"Customer Approval needed... Waste=0, Time=5");
//		stop1.setTimefactor(1);
//		stop1.setStatusDetails(DeviceStatusDetails.WAITFORAPPROVAL);
//		simuPhases.add(stop1);
//
//		Stopped stop2 = new Stopped(5,
//				"New resources as demanded by the Customer not available... Waste=0, Time=5");
//		stop2.setTimefactor(1);
//		stop2.setStatusDetails(DeviceStatusDetails.MISSRESOURCES);
//		simuPhases.add(stop2);
//
//		Setup setup5 = new Setup(5,
//				"General Setup before starting printing... Waste=0, Time=5");
//		setup5.setDeviceSpeed(0);
//		setup5.setStatusDetails(DeviceStatusDetails.WASTE);
//		simuPhases.add(setup5);
//
//		Running run3 = new Running("Running but producing waste... Waste=50");
//		run3.setStatusDetails(DeviceStatusDetails.WASTE);
//		run3.setDeviceSpeed(10000);
//		run3.setGoodVariance(0);
//		run3.setWasteVariance(5);
//		simuPhases.add(run3);
//
//		Running run4 = new Running("Running producing good... Good=200");
//		run4.setStatusDetails(DeviceStatusDetails.GOOD);
//		run4.setDeviceSpeed(20000);
//		run4.setGoodVariance(20);
//		run4.setWasteVariance(0);
//		simuPhases.add(run4);
//
//		Stopped stop3 = new Stopped(5,
//				"New resources as demanded by the Customer not available... Waste=0, Time=5");
//		stop3.setTimefactor(1);
//		stop3.setStatusDetails(DeviceStatusDetails.PAUSE);
//		simuPhases.add(stop3);
//
//		Setup setup6 = new Setup(5,
//				"General Setup before starting printing... Waste=0, Time=5");
//		setup6.setDeviceSpeed(0);
//		setup6.setStatusDetails(DeviceStatusDetails.WASTE);
//		simuPhases.add(setup6);
//
//		Running run5 = new Running("Running but producing waste... Waste=50");
//		run5.setStatusDetails(DeviceStatusDetails.WASTE);
//		run5.setDeviceSpeed(10000);
//		run5.setGoodVariance(0);
//		run5.setWasteVariance(5);
//		simuPhases.add(run5);
//
//		Setup setup7 = new Setup(5, "BlanketWash... Waste=0, Time=5");
//		setup7.setDeviceSpeed(0);
//		setup7.setStatusDetails(DeviceStatusDetails.BLANKETWASH);
//		simuPhases.add(setup7);
//
//		Running run6 = new Running("Running but producing waste... Waste=50");
//		run6.setStatusDetails(DeviceStatusDetails.WASTE);
//		run6.setDeviceSpeed(10000);
//		run6.setGoodVariance(0);
//		run6.setWasteVariance(5);
//		simuPhases.add(run5);
//
//		Stopped stop4 = new Stopped(5, "Generally Maintenance... Time=5");
//		stop4.setTimefactor(1);
//		stop4.setStatusDetails(DeviceStatusDetails.MAINTENANCE);
//		simuPhases.add(stop4);
//
//		Stopped stop5 = new Stopped(5, "BlanketChange ... Time=5");
//		stop5.setTimefactor(1);
//		stop5.setStatusDetails(DeviceStatusDetails.BLANKETCHANGE);
//		simuPhases.add(stop5);
//
//		Stopped stop6 = new Stopped(5,
//				"Sleeves in a Sheetfeed press? ... Time=5");
//		stop6.setTimefactor(1);
//		stop6.setStatusDetails(DeviceStatusDetails.SLEEVECHANGE);
//		simuPhases.add(stop6);
//
//		Setup setup8 = new Setup(5,
//				"General Setup before starting printing... Waste=0, Time=5");
//		setup8.setDeviceSpeed(0);
//		setup8.setStatusDetails(DeviceStatusDetails.WASTE);
//		simuPhases.add(setup8);
//
//		Running run7 = new Running("Running but producing waste... Waste=50");
//		run7.setStatusDetails(DeviceStatusDetails.WASTE);
//		run7.setDeviceSpeed(10000);
//		run7.setGoodVariance(0);
//		run7.setWasteVariance(5);
//		simuPhases.add(run7);
//
//		Running run8 = new Running("Running producing good... Good=50");
//		run8.setStatusDetails(DeviceStatusDetails.GOOD);
//		run8.setDeviceSpeed(20000);
//		run8.setGoodVariance(5);
//		run8.setWasteVariance(0);
//		simuPhases.add(run8);
//
//		Down down1 = new Down(5, "Technical breakdown... Time= 5");
//		down1.setStatusDetails(DeviceStatusDetails.BREAKDOWN);
//		simuPhases.add(down1);
//
//		Down down2 = new Down(5, "General repair... Time= 5");
//		down2.setStatusDetails(DeviceStatusDetails.REPAIR);
//		simuPhases.add(down2);
//
//		Setup setup9 = new Setup(5,
//				"General Setup before starting printing... Waste=0, Time=5");
//		setup9.setDeviceSpeed(0);
//		setup9.setStatusDetails(DeviceStatusDetails.WASTE);
//		simuPhases.add(setup9);
//
//		Running run9 = new Running("Running but producing waste... Waste=50");
//		run9.setStatusDetails(DeviceStatusDetails.WASTE);
//		run9.setDeviceSpeed(10000);
//		run9.setGoodVariance(0);
//		run9.setWasteVariance(5);
//		simuPhases.add(run9);
//
//		Running run10 = new Running("Running producing good... Good=200");
//		run10.setStatusDetails(DeviceStatusDetails.GOOD);
//		run10.setDeviceSpeed(20000);
//		run10.setGoodVariance(20);
//		run10.setWasteVariance(0);
//		simuPhases.add(run10);
//
//		Stopped stop7 = new Stopped(5, "General breakdown... Time=5");
//		stop7.setTimefactor(1);
//		stop7.setStatusDetails(DeviceStatusDetails.FAILURE);
//		simuPhases.add(stop7);
//
//		Stopped stop8 = new Stopped(5, "PaperJam in press... Time=5");
//		stop8.setTimefactor(1);
//		stop8.setStatusDetails(DeviceStatusDetails.PAPERJAM);
//		simuPhases.add(stop8);
//
//		Stopped stop9 = new Stopped(5,
//				"Cover open machine is stopped... Time=5");
//		stop9.setTimefactor(1);
//		stop9.setStatusDetails(DeviceStatusDetails.COVEROPEN);
//		simuPhases.add(stop9);
//
//		Stopped stop10 = new Stopped(5,
//				"Door ?!? open machine is stopped... Time=5");
//		stop10.setTimefactor(1);
//		stop10.setStatusDetails(DeviceStatusDetails.DOOROPEN);
//		simuPhases.add(stop10);
//
//		Setup setup10 = new Setup(5,
//				"General Setup before starting printing... Waste=0, Time=5");
//		setup10.setDeviceSpeed(0);
//		setup10.setStatusDetails(DeviceStatusDetails.WASTE);
//		simuPhases.add(setup10);
//
//		Running run11 = new Running("Running but producing waste... Waste=50");
//		run11.setStatusDetails(DeviceStatusDetails.WASTE);
//		run11.setDeviceSpeed(10000);
//		run11.setGoodVariance(0);
//		run11.setWasteVariance(5);
//		simuPhases.add(run11);
//
//		Running run12 = new Running(
//				"Running producing good... Good=Rest to be produced");
//		run12.setStatusDetails(DeviceStatusDetails.GOOD);
//		run12.setDeviceSpeed(20000);
//		run12.setGoodVariance(35);
//		run12.setWasteVariance(0);
//		simuPhases.add(run12);
//
//		Cleanup clean1 = new Cleanup(5, "General cleanup... Time=5");
//		clean1.setStatusDetails(DeviceStatusDetails.WASHUP);
//
//		simuPhases.add(clean1);
//
//		Cleanup clean2 = new Cleanup(5, "Cleaning plates... Time=5");
//		clean2.setStatusDetails(DeviceStatusDetails.PLATEWASH);
//
//		simuPhases.add(clean2);
//
//		Cleanup clean3 = new Cleanup(5,
//				"Cleaning counter pressure cylinder... Time=5");
//		clean3.setStatusDetails(DeviceStatusDetails.CYLINDERWASH);
//
//		simuPhases.add(clean3);
//
//		Cleanup clean4 = new Cleanup(5, "Cleaning damping roller... Time=5");
//		clean4.setStatusDetails(DeviceStatusDetails.DAMPINGROLLERWASH);
//
//		simuPhases.add(clean4);
//
//		Cleanup clean5 = new Cleanup(5, "Cleaning ink fountain... Time=5");
//		clean5.setStatusDetails(DeviceStatusDetails.CLEANINGINKFOUNTAIN);
//
//		simuPhases.add(clean5);
//
//		Cleanup clean6 = new Cleanup(5, "Cleaning ink roller... Time=5");
//		clean6.setStatusDetails(DeviceStatusDetails.INKROLLERWASH);
//
//		simuPhases.add(clean6);

//		return simuPhases;
//	}
		

	/**
	 * TODO Alter this text Creates the values for the following Amounts as
	 * specified in the JDFAmountPool <code>_totalAmount</code>,
	 * <code>_totalWasteAmount</code>, <code>_totalGoodAmount</code>. Also
	 * the partAmount Elements are created if not already specified.
	 * 
	 * @param jdf
	 *            The JDFNode that will be processed
     * @return  the amount produced
     * @todo Refactor... Claes 2006-11-16
	 */
	protected int getTotalAmount(JDFNode jdf) {
		jdf.getCreateAuditPool().addModified(_config.getID(), null);
		_state.setJdf(jdf);
		JDFAttributeMap emptyAttributeMap = new JDFAttributeMap();
		JDFResourceLink resAmountOut = getOutputResourceLink(jdf);

		if (resAmountOut == null) {
			String msg = "The Elk could not execute the JDFNode. No Output Resource is given.";
			log.error(msg);
			JDFNotification n = jdf.getCreateAuditPool().addNotification(
					EnumClass.Error, this.getClass().getName(),
					new VJDFAttributeMap());
			n.appendComment().appendText(msg);
			jdf.setStatus(EnumNodeStatus.Aborted);

			// TODO generate events.
			return _totalAmount;
		}
		resAmountOut.setActualAmount(0, emptyAttributeMap);
		// Creates an AmountPool if it is not specified
		createPartAmount(resAmountOut);

		final int partAmountGoodAmount = (int) resAmountOut
                .getAmount(new JDFAttributeMap("Condition", "Good"));
        final int partAmountWasteAmount = (int) resAmountOut
                .getAmount(new JDFAttributeMap("Condition", "Waste"));

		int outAmount = (int) resAmountOut.getAmount(new JDFAttributeMap());

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

		_totalAmount = outAmount;
        return _totalAmount;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cip4.elk.impl.device.process.BaseProcess#executeNode(org.cip4.jdflib.node.JDFNode)
	 */
	protected void executeNode(JDFNode jdf) {
		
		getTotalAmount(jdf);

		_jobPhaseMap = new ArrayList();
//		processSimuPhases(generateSimuPhases());
//		processSimuPhases(simuPhases);
		processSimuPhases(new ConfSimuHandler().getSimuPhases());
		
		printJobPhases();

	}

	private void printJobPhases() {
		if (log.isDebugEnabled()) {
			log.debug("Start Printing JobPhases...");
			for (int i = 0; i < _jobPhaseMap.size(); i++) {
				log.debug(((JDFJobPhase) _jobPhaseMap.get(i)).toString());
			}
		}

	}

	/**
	 * Sets this <code>JDFResource</code> and all its children of type
	 * <code>JDFResource</code> to the given <em>Status</em>. If the
	 * incoming object is not of type <code>JDFResource</code>, no action is
	 * taken.
	 * 
	 * @param resource
	 *            The incoming Resource whose status to be set.
	 * @param status
	 *            The new status. Old status will be be overwritten.
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
	 * @param jdf
	 *            the jdf whose <em>OutputResourceLink</em> is returned.
	 * @return The OutputResourceLink for the given JDF, <code>null</code> if
	 *         no output <em>ResourceLink</em>s were defined.
	 * @throws NullPointerException
	 *             if jdf is <code>null</code>.
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

	public void sendError(String msg, JDFNode jdf) {

		log.error(msg);
		JDFNotification n = jdf.getCreateAuditPool().addNotification(
				EnumClass.Error, this.getClass().getName(),
				new VJDFAttributeMap());
		n.appendComment().appendText(msg);
		jdf.setStatus(EnumNodeStatus.Aborted);
		// TODO generate events.

	}

	/**
	 * Returns the <em>ResourceLink</em> from the
	 * <em>JDF/ResourceLinkPool</em> with the specified <em>Usage</em> and
	 * its <em>Amount</em> attribute set. If multiple <em>ResourceLink</em>
	 * s has the <em>Amount</em> attribute set the first (specified first in
	 * the XML-file) <em>ResourceLink</em> is returned. If no <em>Amount</em>
	 * attributes are set <code>null</code> is returned.
	 * 
	 * @param usage
	 *            The <em>Usage</em> of the Resource, must be "Input" or
	 *            "Output"
	 * @param jdf
	 *            the <code>JDFNode</code> to with the <em>Resource</em> s
	 *            to check.
	 * @return The <em>ResourceLink</em> with <em>Amount</em> attribute,
	 *         <code>null</code> if no <em>Amount</em> attributes are set.
	 * @throws IllegalArgumentException
	 *             if usage is not equal to 'Input' or 'Output'
	 * @throws NullPointerException
	 *             if usage or jdf is <code>null</code>.
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
	 * Sets additional values in the <em>DeviceInfo</em> element. The
	 * following values that are set:
	 * <ul>
	 * <li>DeviceSpeed</li>
	 * <li>CounterUnit</li>
	 * <li>TotalProductionCounter</li>
	 * </ul>
	 * 
	 * @see org.cip4.elk.device.process.Process#getDeviceInfo(boolean)
	 */
	public JDFDeviceInfo getDeviceInfo(boolean includeJobPhase) {
		JDFDeviceInfo deviceInfo = super.getDeviceInfo(true);

		if (_statusDetails != null)
			deviceInfo.setStatusDetails(_statusDetails.getName());

		deviceInfo.setSpeed(getDeviceSpeed());
		deviceInfo.setCounterUnit(_counterUnit);
		deviceInfo.setTotalProductionCounter(_totalProductionCounter);
		deviceInfo.setDeviceStatus(_state.getState());

		return deviceInfo;
	}

	/**
	 * A PartAmount Element of a JDFNode can be called
	 * 
	 * @param resAmountOut
	 *            of the actual JDFNode
	 * @param ident
	 *            can be Good or Waste
	 * @return the PartAmount Element specified by the param ident
	 */
	private JDFPartAmount getPartAmountValue(JDFResourceLink resAmountOut,
			String ident) {
	    final JDFPartAmount partAmount;
        final JDFPart part = (JDFPart) resAmountOut.getAmountPool().getChildByTagName(
                ElementName.PART, null, 0, new JDFAttributeMap("Condition",
                        ident), false, false);        
        if (part != null) {
            partAmount = (JDFPartAmount) part.getParentNode();
        } else {
            partAmount = null;
        }
        return partAmount;
	}

	/**
	 * Initializes a new JobPhase
	 * 
	 */
	private synchronized void initNewJobPhase() {

		_phaseStartTime = new JDFDate();
		_state.setStatusEvent(false);

		// Checks if a JDF is loaded, only if JDF != null counter values will be
		// set
		if (_state.getJdf() != null)

		{
			JDFResourceLink resAmountOut = getOutputResourceLink(_state
					.getJdf());
			JDFPartAmount partAmountGood = getPartAmountValue(resAmountOut,
					"Good");
			JDFPartAmount partAmountWaste = getPartAmountValue(resAmountOut,
					"Waste");
			_oldPhaseWaste = Double.parseDouble(partAmountWaste
					.getAttribute("ActualAmount"));
			_oldPhaseAmount = Double.parseDouble(partAmountGood
					.getAttribute("ActualAmount"));
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cip4.elk.device.process.Process#getJobPhase()
	 */
	public JDFJobPhase getJobPhase() {

		if (_actualJobPhase == null || _state.isStateChanged()) {
			_actualJobPhase = super.getJobPhase();

			if ((_state.isStateChanged() && _state.isStatusEvent())
					|| _phaseStartTime == null) {
				initNewJobPhase();
				// _phaseStartTime = new JDFDate();
				// _state.setStatusEvent(false);
			}

		}

		if (_actualJobPhase != null && _state.getJdf() != null) {
			JDFNode jdf = _state.getJdf();
			synchronized (jdf) {

				JDFResourceLink resAmountOut = getOutputResourceLink(jdf);

				double producedAmount = 0;
				double producedWaste = 0;
				try {
					producedAmount = Double.parseDouble(getPartAmountValue(
							resAmountOut, "Good").getAttribute("ActualAmount"));
					producedWaste = Double
							.parseDouble(getPartAmountValue(resAmountOut,
									"Waste").getAttribute("ActualAmount"));
				} catch (NumberFormatException e) {
					log.info("ActualAmount values not available: " + e);
				}

				if (_state.getState().equals(EnumDeviceStatus.Setup)
						|| _state.getState().equals(EnumDeviceStatus.Cleanup)
						|| _state.getState().equals(EnumDeviceStatus.Stopped)
						|| _state.getState().equals(EnumDeviceStatus.Down)) {
					_actualJobPhase.setStatusDetails("Waste");

				}
				// Device in running state
				if (_state.getState().equals(EnumDeviceStatus.Running)) {
					_actualJobPhase.setStatus(EnumNodeStatus.InProgress);
					// Device goodcounter is off
					if (isWasteCounter()) {
						_actualJobPhase.setStatusDetails("Waste");

					}
					// Device goodcounter is on
					else {

						_actualJobPhase.setStatusDetails("Good");

					}
				}

				// Device in cleanup state
				// if (_state.getState().equals(EnumDeviceStatus.Cleanup)) {
				// _actualJobPhase.setStatusDetails("Waste");
				// }

				_actualJobPhase.setPhaseWaste(producedWaste - _oldPhaseWaste);
				_actualJobPhase
						.setPhaseAmount(producedAmount - _oldPhaseAmount);
				// Standard Elements of the JobPhase tag
				_actualJobPhase.setPercentCompleted(Math
						.min(
								((double) producedAmount
										/ (double) _totalAmount * 100), 100));

				// Start time should only be altered if a new JobPhase begins
				// like Setup to Running
				_actualJobPhase.setPhaseStartTime(_phaseStartTime);

				// Amount that will be produced when percenatage =100%
				_actualJobPhase.setTotalAmount(_totalAmount);

				// Amount produced during the whole phase, like Running to
				// Running if waste is also specified without waste

				_actualJobPhase.setAmount(producedAmount);

				// Amount of waste produced during the whole phase, like Running
				// to Running
				_actualJobPhase.setWaste(producedWaste);

				_actualJobPhase.setStartTime(_startTime);

				// Calculating RestTime after 10 percent of totalproduction is
				// already produced

				if (_startTime != null
						&& _actualJobPhase.getPercentCompleted() >= 10) {
					// If TotalAmount is already reached the RestTime is always
					// 0
					if (_actualJobPhase.getAmount() >= _actualJobPhase
							.getTotalAmount())
						_actualJobPhase.setRestTime(new JDFDuration(0));
					else {
						// The RestTime is calculated by:
						// ProducedAmount since JobStart (StartTime) =
						// amountStart
						// Time used until this point (ActualTime minus
						// StartTime) = actualTime
						// aStart : actualTime = units (sheets) produced for 1
						// time unit = unitTime
						// Remaining Amount to be produced to reach the
						// TotalProductionAmount rAmount
						// rAmount : unitTime = approximated time left until =
						// approxLeft
						// TotalProduction Limit is reached
						long actualTime = (new JDFDate().getTime().getTime() - _startTime
								.getTime().getTime()) / 1000;
						double rAmount = _actualJobPhase.getTotalAmount()
								- _actualJobPhase.getAmount();
						double amountStart = _actualJobPhase.getAmount();

						double unitTime = _actualJobPhase.getAmount()
								/ (actualTime);

						double approxLeft = rAmount / unitTime;
						_actualJobPhase.setRestTime(new JDFDuration(
								(int) approxLeft));

					}
					log.debug("Time left in seconds: "
							+ _actualJobPhase.getRestTime().getDuration());
				}

				// not a
				// nice
				// solution...
				// Just for controlling purpose
				if (log.isDebugEnabled()) {
					JDFJobPhase temp = (JDFJobPhase) JDFElementFactory
							.getInstance().createJDFElement(
									ElementName.JOBPHASE);
					_jobPhaseMap.add(temp.copyElement(_actualJobPhase, null));
				}

			}

		}

		return _actualJobPhase;
	}

	/**
	 * Returns the speed of the device in the unit sheets/hour.
	 * 
	 * @return the speed of the device in the unit sheets/hour.
	 */
	public double getDeviceSpeed() {
		return _deviceSpeed;
	}

	public long setWaitTime(int speed) {
		setDeviceSpeed(speed);
		// DeviceSpeed/ 60 minutes*60 seconds = Sheets per second
		// 1000/ Sheets per second = Sheets per millisecond
		return ((long) (1000 / (((float) speed) / 3600)));

	}

	/**
	 * Sets the Device-Speed. Does not have an effect on the processing speed.
	 * The Processing speed is calculated during
	 * <code>processRunningPhase</code>
	 * 
	 * @param speed
	 *            in pages per hour
	 */
	public void setDeviceSpeed(int speed) {

		_deviceSpeed = speed;

	}

	private void processSimuPhases(ArrayList phases) {
		_startTime = new JDFDate();
		try {
			// Just a test to keep the IDLE-State for 10 more seconds
			Thread.sleep(5 * 1000);
		} catch (InterruptedException e) {
			log.error("Sleeping interrupted in the processSimuPhases method: "
					+ e);

		}
		for (int i = 0; i < phases.size(); i++) {
//			checkRunningJob(); //(JP) check if still running.
			
			// (JP) info about simulation.
			log.info("simulation step: " 
						+ ((SimulationPhaseInterface)(phases.get(i))).getSimulationPhase().getName()
						+ "(" + ((SimulationPhaseInterface)(phases.get(i))).getPhaseLength() + "): "
						+ ((SimulationPhaseInterface)(phases.get(i))).getJmfComment());
			

			if (phases.get(i) instanceof Setup) {

				processSetupPhase((Setup) phases.get(i));
			}
			if (phases.get(i) instanceof Cleanup) {
				processCleanupPhase((Cleanup) phases.get(i));
			}
			if (phases.get(i) instanceof Running) {
				processRunningPhase((Running) phases.get(i));
			}
			if (phases.get(i) instanceof Down) {
				processDownPhase((Down) phases.get(i));
			}
			if (phases.get(i) instanceof Stopped) {
				processStoppedPhase((Stopped) phases.get(i));
			}
		}
		setDeviceStatusDetails(null);
		_state.getJdf().setStatus(EnumNodeStatus.Completed);
		// All SimuPhases are processed
		printJobPhases();

	}

	/**
	 * Creates the partAmount Elements for the Outputresource. All partAmount
	 * elements and the the <code>JDFResourceLink</code> element will be
	 * endorsed with an ActualAmount Attribute, which is set to 0 by default.
	 * 
	 * @param resAmountOut
	 *            is the resource that describes the output of the process
	 */
	private void createPartAmount(JDFResourceLink resAmountOut) {
		JDFResource res = ((JDFResource) resAmountOut.getTarget_JDFElement(
				resAmountOut.getrRef(), AttributeName.ID));
		JDFComponent goodPart = getCreateConditionPart(res, "Good");

		JDFComponent wastePart = getCreateConditionPart(res, "Waste");
		JDFAmountPool amountPool = resAmountOut.getCreateAmountPool();

		// Adds the (output) AmountPool/PartAmount elements if they don't
		// exist.
		JDFPartAmount partAmountGood = amountPool.getCreatePartAmount(goodPart
				.getPartMap());

		partAmountGood.setAttribute("ActualAmount", "0", null);

		JDFPartAmount partAmountWaste = amountPool
				.getCreatePartAmount(wastePart.getPartMap());
		partAmountWaste.setAttribute("ActualAmount", "0", null);
	}

	private void processRunningPhase(Running running) {

		log.debug("Starting a RunningPhase: " + running.getJmfComment());

		// Init all DeviceInfo- and JobPhase- Parameters
		setDeviceStatusDetails(running.getStatusDetails());
		long waitTime = setWaitTime(running.getDeviceSpeed());
		setWasteCounter(running.producesWaste());

		_state.getJdf().setStatus(EnumNodeStatus.InProgress);
		_state.setState(JDFDeviceInfo.EnumDeviceStatus.Running, running
				.getJmfComment());
		JDFAttributeMap emptyAttributeMap = new JDFAttributeMap();
		JDFResourceLink resAmountOut = getOutputResourceLink(_state.getJdf());

		JDFResourceLink resAmountIn = getAmountResource("Input", _state
				.getJdf());

		JDFPartAmount partAmountGood = getPartAmountValue(resAmountOut, "Good");
		JDFPartAmount partAmountWaste = getPartAmountValue(resAmountOut,
				"Waste");
		

		try {
			Thread.sleep(1 * 1000);
		} catch (InterruptedException e1) {

			log.error("Pre-Sleep interrupted in Running " + e1);
		}

		// Preparing the JDFNode Attributes
		// resAmountOut will be update after each processing step (Waste+Good)

		int phaseWasteAmount = 0;
		int phaseGoodAmount = 0;

		phaseWasteAmount = (_totalAmount * running.getWasteVariance()) / 100;
		phaseGoodAmount = (_totalAmount * running.getGoodVariance()) / 100;

		// Actual is the amout already produced (waste+good)
		int actualAmount = (int) resAmountOut
				.getActualAmount(new JDFAttributeMap());
		int oldGood = 0;
		int oldWaste = 0;
		try {
			oldGood = Integer.parseInt(partAmountGood
					.getAttribute("ActualAmount"));
			oldWaste = Integer.parseInt(partAmountWaste
					.getAttribute("ActualAmount"));
		} catch (NumberFormatException e) {
			log.info("No Actual Amounts available: " + e);
		}

		// Start Running the job
		int toBeProduced = 0;
		toBeProduced = phaseGoodAmount + phaseWasteAmount;
		int produced = 0;
		int actualWaste = 0;
		while (produced + actualWaste <= toBeProduced) {
			try {
				// Thread sleeps as long as it takes to produce 1 sheet
				Thread.sleep(waitTime);

			} catch (InterruptedException e) {
				log.error("Sleep interuppted during RunningPhase. " + e);
			}
			
//			checkRunningJob(); //(JP) check if still running.

			// Waste production
			if (actualWaste <= phaseWasteAmount && phaseWasteAmount != 0) {

				actualWaste++;
				partAmountWaste.setAttribute("ActualAmount", oldWaste
						+ actualWaste, null);
				resAmountOut.setActualAmount(actualAmount++, emptyAttributeMap);
				_totalProductionCounter++;
				if (log.isDebugEnabled() && actualWaste % 10 == 0)
					log.debug("Produced waste, actualWaste: " + actualWaste
							+ " :ActualAmount:" + actualAmount
							+ " :TotalProductionCounter: "
							+ _totalProductionCounter);
			}
			// Good production
			else {
				if (produced <= phaseGoodAmount && phaseGoodAmount != 0) {
					produced++;
					partAmountGood.setAttribute("ActualAmount", oldGood
							+ produced, null);
					resAmountOut.setActualAmount(actualAmount++,
							emptyAttributeMap);
					_totalProductionCounter++;
					if (log.isDebugEnabled() && produced % 10 == 0)
						log.debug("Produced good, produced: " + produced
								+ " :ActualAmount:" + actualAmount
								+ " :TotalProductionCounter: "
								+ _totalProductionCounter);
				}
			}

			_amountNotifier.fireEvent(new ProcessAmountEvent(EnumClass.Event,
					(int) actualAmount, this, "Amount changed event"));

			if (resAmountIn != null) {
				// Minus should be applied to simulate the consumption
				resAmountIn.setActualAmount(actualWaste + actualAmount,
						emptyAttributeMap);
			}

		}

		JDFDate endSetUpTime = new JDFDate();

		JDFResourceLink resLink = getOutputResourceLink(_state.getJdf());
		JDFResourcePool pool = _state.getJdf().getResourcePool();
		JDFResource resOut = pool.getResourceByID(resLink.getID());
		setResourceStatus(resOut.getResourceRoot(), EnumResStatus.Available);

		addAudits(_state.getJdf(), _startTime, endSetUpTime, new JDFDate(),_state.getJobPhase().getStatus());

		
		// Calls the jobPhaseHashMap
		printJobPhases();

	}

	private void processSetupPhase(Setup setupPhase) {
		log.debug("Starting SetupPhase: " + setupPhase.getJmfComment());

		// Init all DeviceInfo- and JobPhase- Parameters
		setDeviceStatusDetails(setupPhase.getStatusDetails());
		setDeviceSpeed(setupPhase.getDeviceSpeed());
		setWasteCounter(setupPhase.producesWaste());

		_state.getJdf().setStatus(EnumNodeStatus.Setup);
		_state.setState(JDFDeviceInfo.EnumDeviceStatus.Setup, setupPhase
				.getJmfComment());
		JDFAttributeMap emptyMap = new JDFAttributeMap();

		JDFResourceLink resAmountOut = getOutputResourceLink(_state.getJdf());

		JDFPartAmount partAmountWaste = getPartAmountValue(resAmountOut,
				"Waste");

		try {
			Thread.sleep(1 * 1000);

			if (setupPhase.producesWaste()) {
				setWasteCounter(true);
				double actualAmount = resAmountOut.getActualAmount(emptyMap);

				double oldWaste = Double.parseDouble(partAmountWaste
						.getAttribute("ActualAmount"));
				partAmountWaste.setAttribute("ActualAmount", setupPhase
						.getWasteAmount()
						+ oldWaste, null);

				resAmountOut.setActualAmount(actualAmount
						+ setupPhase.getWasteAmount(), emptyMap);
				_totalProductionCounter += setupPhase.getWasteAmount();

			}

			
			

			Thread.sleep(setupPhase.getPhaseLength() * 1000);
		} catch (InterruptedException e) {
			log
					.error("Sleeping has been interrupted, Setup not correctly processed.");
		}
		addAudits(_state.getJdf(), _startTime, new JDFDate(), new JDFDate(),_state.getJobPhase().getStatus());
	

	}

	private void processCleanupPhase(Cleanup clean) {
		log.debug("Starting SetupPhase: " + clean.getJmfComment());

		// Init all DeviceInfo- and JobPhase- Parameters
		setDeviceSpeed(clean.getDeviceSpeed());
		setDeviceStatusDetails(clean.getStatusDetails());

		_state.getJdf().setStatus(EnumNodeStatus.Cleanup);
		_state.setState(JDFDeviceInfo.EnumDeviceStatus.Cleanup, clean
				.getJmfComment());
		JDFAttributeMap emptyMap = new JDFAttributeMap();
		JDFResourceLink resAmountOut = getOutputResourceLink(_state.getJdf());

		JDFPartAmount partAmountWaste = getPartAmountValue(resAmountOut,
				"Waste");

		try {
			Thread.sleep(1 * 1000);

			if (clean.producesWaste()) {
				setWasteCounter(true);
				double actualAmount = resAmountOut.getActualAmount(emptyMap);

				double oldWaste = Double.parseDouble(partAmountWaste
						.getAttribute("ActualAmount"));
				partAmountWaste.setAttribute("ActualAmount", clean
						.getWasteAmount()
						+ oldWaste, null);

				resAmountOut.setActualAmount(actualAmount
						+ clean.getWasteAmount(), emptyMap);
				_totalProductionCounter += clean.getWasteAmount();

			}

			Thread.sleep(clean.getPhaseLength() * 1000);

		} catch (InterruptedException e) {
			log
					.error("Sleeping has been interrupted, Setup not correctly processed.");
		}
		addAudits(_state.getJdf(), _startTime, new JDFDate(), new JDFDate(),_state.getJobPhase().getStatus());

	}

	private void processStoppedPhase(Stopped stopped) {
		log.debug("Starting SetupPhase: " + stopped.getJmfComment());

		// Init all DeviceInfo- and JobPhase- Parameters
		setDeviceSpeed(stopped.getDeviceSpeed());
		setDeviceStatusDetails(stopped.getStatusDetails());

		_state.getJdf().setStatus(EnumNodeStatus.Stopped);
		_state.setState(JDFDeviceInfo.EnumDeviceStatus.Stopped, stopped
				.getJmfComment());

		// No waste production during down
		// JDFAttributeMap emptyMap = new JDFAttributeMap();
		// JDFResourceLink resAmountOut =
		// getOutputResourceLink(_state.getJdf());
		//
		// JDFPartAmount partAmountWaste = getPartAmountValue(resAmountOut,
		// "Waste");

		try {
			Thread.sleep(1 * 1000);

			if (stopped.producesWaste()) {
				setWasteCounter(true);
				// double actualAmount = resAmountOut.getActualAmount(emptyMap);
				//
				// double oldWaste = Double.parseDouble(partAmountWaste
				// .getAttribute("ActualAmount"));
				// partAmountWaste.setAttribute("ActualAmount", clean
				// .getWasteAmount()
				// + oldWaste, null);
				//
				// resAmountOut.setActualAmount(actualAmount
				// + clean.getWasteAmount(), emptyMap);
				// _totalProductionCounter += clean.getWasteAmount();

			}

			Thread.sleep(stopped.getPhaseLength() * 1000);

		} catch (InterruptedException e) {
			log
					.error("Sleeping has been interrupted, Down not correctly processed.");
		}
		addAudits(_state.getJdf(), _startTime, new JDFDate(), new JDFDate(),_state.getJobPhase().getStatus());

	}

	private void processDownPhase(Down down) {
		log.debug("Starting SetupPhase: " + down.getJmfComment());

		// Init all DeviceInfo- and JobPhase- Parameters
		setDeviceSpeed(down.getDeviceSpeed());
		setDeviceStatusDetails(down.getStatusDetails());

		_state.getJdf().setStatus(EnumNodeStatus.Stopped);
		_state.setState(JDFDeviceInfo.EnumDeviceStatus.Down, down
				.getJmfComment());

		// No waste production during down
		// JDFAttributeMap emptyMap = new JDFAttributeMap();
		// JDFResourceLink resAmountOut =
		// getOutputResourceLink(_state.getJdf());
		//
		// JDFPartAmount partAmountWaste = getPartAmountValue(resAmountOut,
		// "Waste");

		try {
			Thread.sleep(1 * 1000);

			if (down.producesWaste()) {
				setWasteCounter(true);

			}

			Thread.sleep(down.getPhaseLength() * 1000);

		} catch (InterruptedException e) {
			log
					.error("Sleeping has been interrupted, Down not correctly processed.");
		}
		addAudits(_state.getJdf(), _startTime, new JDFDate(), new JDFDate(),_state.getJobPhase().getStatus());

	}

	public void setDeviceStatusDetails(DeviceStatusDetails statusDetails) {

		_statusDetails = statusDetails;
	}

	private boolean isWasteCounter() {
		return _wasteCounter;
	}

	private void setWasteCounter(boolean wasteCounter) {

		_wasteCounter = wasteCounter;
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
        // SetUp has been moved to ConventionalPrintingProcessSimu
        // _state.setState(_state.SETUP, "Loading job into Device");
        // Download job
        JDFNode jdf = new JDFParser().parseStream(_repository.getFile(jdfUrl))
                .getJDFRoot();
        _state.setJdfUrl(jdfUrl.toString());
        // Execution phase for Device
        // _state.setState(_state.RUNNING);
        List processNodes = JDFUtil.getProcessNodes(getProcessType(), jdf, null);
        if (processNodes.size() == 0) {
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
}
