/*
 * Created on 2005-mar-09
 */

package org.cip4.elk.impl.jmf.util;

import org.apache.log4j.Logger;
import org.cip4.elk.jmf.util.DeviceFilter;
import org.cip4.jdflib.auto.JDFAutoDeviceFilter.EnumDeviceDetails;
import org.cip4.jdflib.core.ElementName;
import org.cip4.jdflib.core.JDFComment;
import org.cip4.jdflib.core.VElement;
import org.cip4.jdflib.jmf.JDFDeviceFilter;
import org.cip4.jdflib.jmf.JDFDeviceInfo;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.resource.JDFDevice;
import org.cip4.jdflib.resource.JDFDeviceList;

/**
 * Handles a <em>DeviceFilter</em> according to the Base ICS level 3.
 * 
 * @see <a
 *      href="http://www.cip4.org/documents/jdf_specifications/JDF1.2.pdf">JDF
 *      Specification Release 1.2, Table 5-23 DeviceFilter </a>
 * @see <a
 *      href="http://www.cip4.org/document_archive/documents/ICS-Base-1.0.pdf">ICS-Base-1.0
 *      Specification, Table 29 DeviceFilter </a>
 * 
 * @author Ola Stering, (olst6875@student.uu.se)
 * @version $Id: BaseICSDeviceFilter.java,v 1.6 2006/08/30 07:55:06 buckwalter Exp $
 */
public class BaseICSDeviceFilter implements DeviceFilter {

	private static Logger log = Logger.getLogger(BaseICSDeviceFilter.class
			.getName());

	/**
	 * <p>
	 * Filters the contents of the <em>DeviceInfo/Device</em> element
	 * according to the value of <em>DeviceDetails</em>. Implemented levels
	 * of detail (LoD) for <em>DeviceDetails</em> are: (Summary, for details
	 * see JDF 1.2 Specification and ICS-Base-1.0 Specification)
	 * </p>
	 * <ul>
	 * <li>None - returns the device except child elements, same as brief</li>
	 * <li>Brief - Provide all available device information except for the
	 * <em>Device</em> elements.</li>
	 * <li>Details - Provide maximum available device information excluding
	 * device capability descriptions. Includes <em>Device</em> elements which
	 * represent details of the <em>Device</em>.</li>
	 * </ul>
	 * <p>
	 * Non-implemented filters will leave <em>DeviceList/DeviceInfo/Device</em>
	 * elements unchanged.
	 * </p>
	 * NOTE: The None filter is not allowed for a producer/manager according to
	 * Base ICS.
	 * 
	 * @param deviceInfo
	 *            the element containing the <em>Device</em> to filter.
	 * @param enumDeviceDetails
	 *            The level of device detail.
	 * @throws NullPointerException
	 *             if enumDeviceDetails is <code>null</code>.
	 */
	public void applyDetails(JDFDeviceInfo deviceInfo, String enumDeviceDetails) {

		if (enumDeviceDetails == null) {
			NullPointerException e = new NullPointerException(
					"EnumDeviceDetails can not be null");
			log.error(e);
			throw e;
		}

		JDFDevice device = deviceInfo.getDevice();

		if (device == null) {
			log.debug("The DeviceInfo element did not contain a Device, "
					+ "no filtering done. ");
			return;
		}

		// Creates a defensive copy of the device
		JDFDevice retDev = (JDFDevice) device.cloneNode(true);

		String jmfSenderID = retDev.getJMFSenderID();
		if (jmfSenderID == null || jmfSenderID.equals("")) {
			jmfSenderID = "Not configured";
			log.debug("No Device/@JMFSenderID configured: " + device
					+ " NOT CONFORMANT WITH BASE ICS");
		}

		retDev.setJMFSenderID(jmfSenderID);

		String jmfUrl = retDev.getJMFURL();
		if (jmfUrl == null || jmfUrl.equals("")) {
			jmfUrl = "Not configured";
			log.debug("No Device/@JMFURL configured: " + device
					+ " NOT CONFORMANT WITH BASE ICS");
		}

		retDev.setJMFURL(jmfUrl);

		// See table 5-23 p. 145 JDF 1.2 or table 5-58 p. 163 JDF 1.2
		if (enumDeviceDetails.equals(EnumDeviceDetails.None.toString())) {
			deviceInfo
					.removeChild(ElementName.DEVICE, null, 0);
			String msg = "A Producer/Manager MUST NOT write the "
					+ "DeviceFilter/@DeviceDetails='None' according to the"
					+ "Base ICS. The JDF 1.2 Specification is incorrectly"
					+ " specified for the 'None' value and will be "
					+ "corrected for version 1.4.";
			log.debug("No filter applied, defaults to None, "
					+ "NOT CONFORMANT WITH BASE ICS.");
			JDFComment c = deviceInfo.appendComment();
			c.appendText(msg);

		} else if (enumDeviceDetails.equals(EnumDeviceDetails.Brief.getName())) {
			deviceInfo
					.removeChild(ElementName.DEVICE, null, 0);
			log.debug("Brief filter applied, All attributes, Device element"
					+ " removed.");

		} else if (enumDeviceDetails
				.equals(EnumDeviceDetails.Details.getName())) {
			String nameSpaceURI = device.getNamespaceURI();
			retDev.removeChildren(ElementName.DEVICECAP, nameSpaceURI, null);
			deviceInfo.replaceChild(retDev, device);
			
			log.debug("Detailed filter applied");

		} else {
			log.debug("Not implemented filter: " + enumDeviceDetails);
		}

	}

	/**
	 * Added through the old one doesn't work
	 * 
	 * @param deviceInfo
	 * @param enumDeviceDetails
	 * @param response
	 *            The JDFResponse that will be send
	 */
	public void applyDetails(JDFDeviceInfo deviceInfo,
			String enumDeviceDetails, JDFResponse response) {

		if (enumDeviceDetails == null) {
			NullPointerException e = new NullPointerException(
					"EnumDeviceDetails can not be null");
			log.error(e);
			throw e;
		}

		JDFDevice device = deviceInfo.getDevice();

		if (device == null) {
			log.debug("The DeviceInfo element did not contain a Device, "
					+ "no filtering done. ");
			return;
		}

		// Creates a defensive copy of the device
		JDFDevice retDev = (JDFDevice) device.cloneNode(true);

		String jmfSenderID = retDev.getJMFSenderID();
		if (jmfSenderID == null || jmfSenderID.equals("")) {
			jmfSenderID = "Not configured";
			log.debug("No Device/@JMFSenderID configured: " + device
					+ " NOT CONFORMANT WITH BASE ICS");
		}

		retDev.setJMFSenderID(jmfSenderID);

		String jmfUrl = retDev.getJMFURL();
		if (jmfUrl == null || jmfUrl.equals("")) {
			jmfUrl = "Not configured";
			log.debug("No Device/@JMFURL configured: " + device
					+ " NOT CONFORMANT WITH BASE ICS");
		}

		retDev.setJMFURL(jmfUrl);

		// See table 5-23 p. 145 JDF 1.2 or table 5-58 p. 163 JDF 1.2
		if (enumDeviceDetails.equals(EnumDeviceDetails.None.toString())) {
			deviceInfo
					.removeChild(ElementName.DEVICE, null, 0);
			String msg = "A Producer/Manager MUST NOT write the "
					+ "DeviceFilter/@DeviceDetails='None' according to the"
					+ "Base ICS. The JDF 1.2 Specification is incorrectly"
					+ " specified for the 'None' value and will be "
					+ "corrected for version 1.4.";
			log.debug("No filter applied, defaults to None, "
					+ "NOT CONFORMANT WITH BASE ICS.");
			JDFComment c = deviceInfo.appendComment();
			c.appendText(msg);

		} else if (enumDeviceDetails.equals(EnumDeviceDetails.Brief.getName())) {
			deviceInfo
					.removeChild(ElementName.DEVICE, null, 0);
			log.debug("Brief filter applied, All attributes, Device element"
					+ " removed.");

		} else if (enumDeviceDetails
				.equals(EnumDeviceDetails.Details.getName())) {
			String nameSpaceURI = device.getNamespaceURI();
			retDev.removeChildren(ElementName.DEVICECAP, nameSpaceURI, null);
			JDFDeviceInfo reDeIn = (JDFDeviceInfo) response
					.getElement(ElementName.DEVICEINFO);
			JDFDeviceInfo old = (JDFDeviceInfo) response
					.getElement(ElementName.DEVICEINFO);

			deviceInfo.replaceChild(retDev, device);
			JDFDeviceInfo di = (JDFDeviceInfo) response
					.getElement(ElementName.DEVICEINFO);
			JDFDevice d = (JDFDevice) di.getElement(ElementName.DEVICE);
			d.removeChildren(ElementName.DEVICECAP, nameSpaceURI,null);

			log.debug("Detailed filter applied");
			log.debug(response.toString());

		} else {
			log.debug("Not implemented filter: " + enumDeviceDetails);
		}

	}

	/**
	 * <p>
	 * Filters the contents of the <em>Device</em> elements in a
	 * <em>DeviceList</em>.
	 * </p>
	 * <p>
	 * Any <em>DeviceFilter/Device</em> elements are ignored.
	 * </p>
	 * <p>
	 * If filter is <code>null</code> the default <em>DeviceDetails</em>
	 * value is Details. For implemented DeviceDetails see
	 * {@link #applyDetails(JDFDeviceInfo, String)}.
	 * </p>
	 * <p>
	 * NOTE: To be conformant with Base ICS the DeviceFilter MUST be included
	 * and the Producer/Manager is MUST NOT write the
	 * DeviceFilter/@DeviceDetails="None". The JDF 1.2 Specification is
	 * incorrectly specified for the "None" value and will be corrected for 1.4.
	 * </p>
	 * <p>
	 * NOTE: If the <em>DeviceList</em> or it its contents do not conform to
	 * the JDF 1.2 Specification there is no guarantee that the returned list do
	 * so. If a <em>DeviceList/DeviceInfo/Device</em> element do not contain
	 * the JMFURL or the JMFSenderID attributes these values will be set to "Not
	 * configured".
	 * </p>
	 * 
	 * @param deviceList
	 *            a list of <em>DeviceInfo</em> elements.
	 * @param filter
	 *            the filter applied to devices in deviceList.
	 * @throws NullPointerException
	 *             if deviceList is <code>null</code>.
	 * 
	 * @see #applyDetails(JDFDeviceInfo, String)
	 * @see org.cip4.elk.jmf.util.DeviceFilter#filterDeviceList(org.cip4.jdflib.resource.JDFDeviceList,
	 *      org.cip4.jdflib.jmf.JDFDeviceFilter)
	 * @throws NullPointerException
	 *             if deviceList is <code>null</code>.
	 */
	public JDFDeviceList filterDeviceList(JDFDeviceList deviceList,
			JDFDeviceFilter filter) {

		if (deviceList == null)
			throw new NullPointerException("deviceList can not be null");
		JDFDeviceList retDeviceList = (JDFDeviceList) deviceList
				.cloneNode(true);
		VElement deviceInfoElements = retDeviceList.getChildElementVector(ElementName.DEVICEINFO, null, null, true, 0, false);

		EnumDeviceDetails edd;

		if (filter == null) { // DEFAULT, equals the None filter
			String msg = "To be conformant with Base ICS the DeviceFilter"
					+ " MUST be included in a KnownDevices message.";
			log.debug("No filter applied, defaults to None, "
					+ "NOT CONFORMANT WITH BASE ICS.");
			JDFComment c = retDeviceList.appendComment();
			c.appendText(msg);
			edd = EnumDeviceDetails.None;
		} else {
            edd = filter.getDeviceDetails();
        }
        for (int i = 0, imax = deviceInfoElements.size(); i < imax; i++) {
            applyDetails((JDFDeviceInfo) deviceInfoElements.get(i), edd.getName());
        }
		return retDeviceList;
	}
}
