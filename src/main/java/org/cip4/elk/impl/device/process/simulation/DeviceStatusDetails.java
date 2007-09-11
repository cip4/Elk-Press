package org.cip4.elk.impl.device.process.simulation;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.enums.ValuedEnum;

/**
 * DeviceStatusDetails according to the JDF Specification 1.3 Appendix C page
 * 703 et seqq.
 * Until now there is no implementation in the JDFLib-J (Build26)
 * 
 * @author Marco.Kornrumpf@Bertelsmann.de
 * 
 */
public class DeviceStatusDetails extends ValuedEnum {
	private static final long serialVersionUID = 1L;

	private static int m_startValue = 0;

	private DeviceStatusDetails(String name) {
		super(name, m_startValue++);
	}

	public static DeviceStatusDetails getEnum(String enumName) {
		return (DeviceStatusDetails) getEnum(DeviceStatusDetails.class,
				enumName);
	}

	public static DeviceStatusDetails getEnum(int enumValue) {
		return (DeviceStatusDetails) getEnum(DeviceStatusDetails.class,
				enumValue);
	}

	public static Map getEnumMap() {
		return getEnumMap(DeviceStatusDetails.class);
	}

	public static List getEnumList() {
		return getEnumList(DeviceStatusDetails.class);
	}

	public static Iterator iterator() {
		return iterator(DeviceStatusDetails.class);
	}

	// StatusDetails for generic devices
	public static final DeviceStatusDetails WASTE = new DeviceStatusDetails(
			"Waste");

	public static final DeviceStatusDetails GOOD = new DeviceStatusDetails(
			"Good");

	// StatusDetails for a printing device
	public static final DeviceStatusDetails FORMCHANGE = new DeviceStatusDetails(
			"FormChange");

	public static final DeviceStatusDetails WASHUP = new DeviceStatusDetails(
			"WashUp");

	public static final DeviceStatusDetails SIZECHANGE = new DeviceStatusDetails(
			"SizeChange");

	public static final DeviceStatusDetails WARMINGUP = new DeviceStatusDetails(
			"WarmingUp");

	// StatusDetails for DeviceStatus = Down
	public static final DeviceStatusDetails BREAKDOWN = new DeviceStatusDetails(
			"BreakDown");

	public static final DeviceStatusDetails REPAIR = new DeviceStatusDetails(
			"Repair");

	// Following StatusDetails are subcategories of WashUp according to JDF
	// Spec 1.3 Appendix C Table C-2

	public static final DeviceStatusDetails BLANKETWASH = new DeviceStatusDetails(
			"BlanketWash");

	public static final DeviceStatusDetails CLEANINGINKFOUNTAIN = new DeviceStatusDetails(
			"CleaningInkFountain");

	public static final DeviceStatusDetails DAMPINGROLLERWASH = new DeviceStatusDetails(
			"DampingRollerWash");

	public static final DeviceStatusDetails CYLINDERWASH = new DeviceStatusDetails(
			"CylinderWash");

	public static final DeviceStatusDetails PLATEWASH = new DeviceStatusDetails(
			"PlateWash");

	public static final DeviceStatusDetails INKROLLERWASH = new DeviceStatusDetails(
			"InkRollerWash");

	// DeviceStatus = Stopped
	public static final DeviceStatusDetails PAUSE = new DeviceStatusDetails(
			"Pause");

	public static final DeviceStatusDetails WAITFORAPPROVAL = new DeviceStatusDetails(
			"WaitForApproval");

	public static final DeviceStatusDetails MISSRESOURCES = new DeviceStatusDetails(
			"MissResources");

	public static final DeviceStatusDetails MAINTENANCE = new DeviceStatusDetails(
			"Maintenance");

	public static final DeviceStatusDetails BLANKETCHANGE = new DeviceStatusDetails(
			"BlanketChange");

	public static final DeviceStatusDetails SLEEVECHANGE = new DeviceStatusDetails(
			"SleeveChange");

	public static final DeviceStatusDetails FAILURE = new DeviceStatusDetails(
			"Failure");

	public static final DeviceStatusDetails PAPERJAM = new DeviceStatusDetails(
			"PaperJam");

	public static final DeviceStatusDetails IDLING = new DeviceStatusDetails(
			"Idling");

	public static final DeviceStatusDetails COVEROPEN = new DeviceStatusDetails(
			"CoverOpen");

	public static final DeviceStatusDetails DOOROPEN = new DeviceStatusDetails(
			"DoorOpen");

}
