package org.cip4.elk.impl.device.process.simulation;

import org.cip4.jdflib.auto.JDFAutoDeviceInfo;
import org.cip4.jdflib.auto.JDFAutoDeviceInfo.EnumDeviceStatus;

/**
 * Represents a Running simulation phase
 * 
 * @author Marco.Kornrumpf@Bertelsmann.de
 * 
 */
public class Running implements SimulationPhaseInterface {
	private static final JDFAutoDeviceInfo.EnumDeviceStatus SIMULATION_PHASE = JDFAutoDeviceInfo.EnumDeviceStatus.Running;

	private int _wasteVariance = 0;

	private int _goodVariance = 0;

	private String _jmfComment = "No comment defined";

	private DeviceStatusDetails _statusDetails = null;

	private int _deviceSpeed = 0;

	private int _wasteAmount = 0;

	private int _goodAmount = 0;

	/**
	 * Generates a Running phase. No phase lenght is needed
	 * 
	 * @param jmfComment
	 */
	public Running(String jmfComment) {

		_jmfComment = jmfComment;
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
		if (_wasteVariance > 0)
			return true;
		return false;
	}

	/**
	 * @see org.cip4.elk.simlation.SimulationPhaseInterface#producesGood()
	 */
	public boolean producesGood() {
		if (_wasteVariance == 0)
			return true;
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

		return (_wasteAmount * _wasteVariance) / 100;
	}

	/**
	 * @see org.cip4.elk.simlation.SimulationPhaseInterface#getDeviceSpeed()
	 */
	public int getDeviceSpeed() {

		return _deviceSpeed;
	}

	/**
	 * @see org.cip4.elk.simlation.SimulationPhaseInterface#getPhaseLength()
	 */
	public long getPhaseLength() {

		double totalAmount = getGoodAmount() + getWasteAmount();

		return Math.round(totalAmount / _deviceSpeed) * 3600;

	}

	/**
	 * @see org.cip4.elk.simlation.SimulationPhaseInterface#getGoodAmount()
	 */
	public int getGoodAmount() {

		return (_goodAmount * _goodVariance) / 100;
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
	 * @see org.cip4.elk.simlation.SimulationPhaseInterface#getWasteVariance()
	 */
	public int getWasteVariance() {
		return _wasteVariance;
	}

	/**
	 * @see org.cip4.elk.simlation.SimulationPhaseInterface#setWasteVariance()
	 */
	public void setWasteVariance(int variance) {
		_wasteVariance = variance;
	}

	/**
	 * @see org.cip4.elk.simlation.SimulationPhaseInterface#getGoodVariance()
	 */
	public int getGoodVariance() {
		return _goodVariance;
	}

	/**
	 * 
	 * @param variance
	 */
	public void setGoodVariance(int variance) {
		_goodVariance = variance;
	}

	/**
	 * 
	 * @param speed
	 */
	public void setDeviceSpeed(int speed) {
		_deviceSpeed = speed;
	}

}
