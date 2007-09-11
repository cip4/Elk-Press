package org.cip4.elk.impl.device.process.simulation;

import org.cip4.jdflib.auto.JDFAutoDeviceInfo.EnumDeviceStatus;

/**
 * All simulation phases are depending on this interface
 * 
 * @author Marco.Kornrumpf@Bertelsmann.de
 * 
 */
public interface SimulationPhaseInterface {

	/**
	 * Returns the simulation phase type as specified in
	 * {@link org.cip4.jdflib.auto.JDFAutoDeviceInfo.EnumDeviceStatus}
	 * 
	 * @return the type of the simulation phase
	 */
	public EnumDeviceStatus getSimulationPhase();

	/**
	 * Called to ensure if this simulation phase produces waste amount. Only the
	 * the phase type running can produce waste and good. Cleanup, Down, Stopped
	 * and Setup will always return true.
	 * 
	 * @return boolean
	 */
	public boolean producesWaste();

	/**
	 * Called to ensure if this simulation phase produces good amount. Only the
	 * the phase type running can produce waste and good. Cleanup, Down, Stopped
	 * and Setup will always return false.
	 * 
	 * @return boolean
	 */
	public boolean producesGood();

	/**
	 * Individual comment for JMF-Message fired during this phase
	 * 
	 * @return String
	 */
	public String getJmfComment();

	/**
	 * Return the individual waste amount for a simulation phase. Some phases
	 * can not produce waste, e.g. cleanup. Calculated from TotalAmount and
	 * WasteVariance
	 * 
	 * @return amount of waste as int
	 */
	public int getWasteAmount();

	/**
	 * Returns the individual speed of the device for this phase.
	 * 
	 * @return speed as int
	 */
	public int getDeviceSpeed();

	/**
	 * Returns the phase length in millis. The phase length is set in the
	 * constructor of a simulation phase. Only a running phase has no phase
	 * length
	 * 
	 * @return phase length in millis
	 */
	public long getPhaseLength();

	/**
	 * Percentage of good copys that will be produced during this phase. Depends
	 * on the TotalAmount of the entire job.
	 * 
	 * @return percentage as int
	 */
	public int getGoodVariance();

	/**
	 * 
	 * Percentage of waste copys that will be produced during this phase.
	 * Depends on the TotalAmount of the entire job.
	 * 
	 * @return percentage as int
	 */
	public int getWasteVariance();

	/**
	 * Return the individual good amount for a simulation phase. Some phases can
	 * not produce good, e.g. stopped. Calculated from TotalAmount and
	 * GoodVariance
	 * 
	 * @return amount of good copys as int
	 */
	public int getGoodAmount();

	/**
	 * Sets the specific StatusDetails for a device during the simulation phase.
	 * Values according the JDF Specification 1.3
	 * 
	 * @param details
	 * @link org.cip4.elk.simulation.DeviceStatusDetails
	 * 
	 */
	public void setStatusDetails(DeviceStatusDetails details);

}
