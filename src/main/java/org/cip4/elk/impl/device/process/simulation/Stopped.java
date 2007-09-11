package org.cip4.elk.impl.device.process.simulation;

import org.cip4.jdflib.auto.JDFAutoDeviceInfo;
import org.cip4.jdflib.auto.JDFAutoDeviceInfo.EnumDeviceStatus;
/**
 * Represents a Stopped simulation phase
 * 
 * @author Marco.Kornrumpf@Bertelsmann.de
 *
 */
public class Stopped implements SimulationPhaseInterface {

	private static final JDFAutoDeviceInfo.EnumDeviceStatus SIMULATION_PHASE = JDFAutoDeviceInfo.EnumDeviceStatus.Stopped;

	private int _wasteVariance = 0;

	private String _jmfComment = "No comment defined";

	private DeviceStatusDetails _statusDetails = null;

	private long _phaseLength = 0;

	/**
	 * Generates a Stopped phase
	 * 
	 * @param phaseLength
	 *            in millis
	 * @param jmfComment
	 */
	public Stopped(long phaseLength, String jmfComment) {
		_phaseLength = phaseLength;
		_jmfComment = jmfComment;
	}

	/**
	 * 
	 * @return
	 */
	public DeviceStatusDetails getStatusDetails() {
		return _statusDetails;
	}

	/**
	 * @see org.cip4.elk.simlation.SimulationPhaseInterface#setStatusDetails(org.cip4.elk.simlation.DeviceStatusDetails)
	 */
	public void setStatusDetails(DeviceStatusDetails details) {
		_statusDetails = details;
	}

	/**
	 * 
	 * @return
	 */
	public boolean hasStatusDetails() {
		if (_statusDetails != null)
			return true;

		return false;
	}

	/**
	 * @see org.cip4.elk.simlation.SimulationPhaseInterface#getSimulationPhase()
	 */
	public EnumDeviceStatus getSimulationPhase() {

		return SIMULATION_PHASE;
	}

	/**
	 * @see org.cip4.elk.simlation.SimulationPhaseInterface#producesWaste()
	 */
	public boolean producesWaste() {

		return false;
	}

	/**
	 * @see org.cip4.elk.simlation.SimulationPhaseInterface#producesGood()
	 */
	public boolean producesGood() {

		return false;
	}

	/**
	 * @see org.cip4.elk.simlation.SimulationPhaseInterface#getJmfComment()
	 */
	public String getJmfComment() {

		return _jmfComment;
	}

	/**
	 * @see org.cip4.elk.simlation.SimulationPhaseInterface#getWasteAmount()
	 */
	public int getWasteAmount() {

		return 0;
	}

	/**
	 * @see org.cip4.elk.simlation.SimulationPhaseInterface#getDeviceSpeed()
	 */
	public int getDeviceSpeed() {

		return 0;
	}

	/**
	 * @see org.cip4.elk.simlation.SimulationPhaseInterface#getPhaseLength()
	 */
	public long getPhaseLength() {

		return _phaseLength;
	}

	/**
	 * @see org.cip4.elk.simlation.SimulationPhaseInterface#getGoodAmount()
	 */
	public int getGoodAmount() {

		return 0;
	}

	/**
	 * @see org.cip4.elk.simlation.SimulationPhaseInterface#getGoodVariance()
	 */
	public int getGoodVariance() {

		return 0;
	}

	/**
	 * @see org.cip4.elk.simlation.SimulationPhaseInterface#getWasteVariance()
	 */
	public int getWasteVariance() {

		return _wasteVariance;
	}

}
