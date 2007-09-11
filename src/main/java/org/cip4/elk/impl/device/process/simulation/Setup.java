package org.cip4.elk.impl.device.process.simulation;

import org.cip4.jdflib.auto.JDFAutoDeviceInfo;
import org.cip4.jdflib.auto.JDFAutoDeviceInfo.EnumDeviceStatus;
/**
 * Represents a Setup simulation phase
 * 
 * @author Marco.Kornrumpf@Bertelsmann.de
 *
 */
public class Setup implements SimulationPhaseInterface {

	private JDFAutoDeviceInfo.EnumDeviceStatus SIMULATION_PHASE = JDFAutoDeviceInfo.EnumDeviceStatus.Setup;

	private String _jmfComment = "No comment defined";

	private DeviceStatusDetails _statusDetails = null;

	private int _deviceSpeed = 0;

	private int _wasteAmount = 0;

	private int _wasteVariance = 0;

	private int _goodVariance = 0;

	private long _phaseLength = 0;

	/**
	 * Generates a Setup phase
	 * 
	 * @param phaseLength
	 *            in millis
	 * @param jmfComment
	 */
	public Setup(long phaseLength, String jmfComment) {
		_phaseLength = phaseLength;
		_jmfComment = jmfComment;

	}

	/**
	 * @see org.cip4.elk.simlation.SimulationPhaseInterface#getStatusDetails()
	 */
	public DeviceStatusDetails getStatusDetails() {
		return _statusDetails;
	}

	/**
	 * @see org.cip4.elk.simlation.SimulationPhaseInterface#getSimulationPhase()
	 */
	public EnumDeviceStatus getSimulationPhase() {
		return SIMULATION_PHASE;
	}

	/**
	 * @see org.cip4.elk.simlation.SimulationPhaseInterface#getWasteAmount()
	 */
	public int getWasteAmount() {
		return _wasteAmount;
	}

	/**
	 * @see org.cip4.elk.simlation.SimulationPhaseInterface#getJmfComment()
	 */
	public String getJmfComment() {
		return _jmfComment;
	}

	/**
	 * @see org.cip4.elk.simlation.SimulationPhaseInterface#set_jmfComment(java.lang.String)
	 */
	public void set_jmfComment(String jmfComment) {
		this._jmfComment = jmfComment;
	}

	/**
	 * @see org.cip4.elk.simlation.SimulationPhaseInterface#getDeviceSpeed()
	 */
	public int getDeviceSpeed() {
		return _deviceSpeed;
	}

	/**
	 * @see org.cip4.elk.simlation.SimulationPhaseInterface#getDeviceSpeed(java.lang.Integer)
	 */
	public void setDeviceSpeed(int deviceSpeed) {
		_deviceSpeed = deviceSpeed;
	}

	/**
	 * @see org.cip4.elk.simlation.SimulationPhaseInterface#producesWaste()
	 */
	public boolean producesWaste() {
		if (_wasteAmount > 0)
			return true;
		return false;
	}

	/**
	 * @see org.cip4.elk.simlation.SimulationPhaseInterface#getPhaseLength()
	 */
	public long getPhaseLength() {

		return _phaseLength;
	}

	/**
	 * @see org.cip4.elk.simlation.SimulationPhaseInterface#setStatusDetails(org.cip4.elk.simlation.DeviceStatusDetails)
	 */
	public void setStatusDetails(DeviceStatusDetails details) {
		_statusDetails = details;
	}

	/**
	 * @see org.cip4.elk.simlation.SimulationPhaseInterface#producesGood()
	 */
	public boolean producesGood() {

		return false;
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

		return _goodVariance;
	}

	/**
	 * @see org.cip4.elk.simlation.SimulationPhaseInterface#getWasteVariance()
	 */
	public int getWasteVariance() {

		return _wasteVariance;
	}

}
