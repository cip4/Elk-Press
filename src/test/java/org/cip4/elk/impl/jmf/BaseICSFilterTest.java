/*
 * Created on 2005-apr-13 
 */
package org.cip4.elk.impl.jmf;

import org.cip4.elk.ElkTestCase;
import org.cip4.elk.JDFElementFactory;
import org.cip4.elk.impl.jmf.util.BaseICSDeviceFilter;
import org.cip4.jdflib.auto.JDFAutoDeviceFilter.EnumDeviceDetails;
import org.cip4.jdflib.auto.JDFAutoDeviceInfo.EnumDeviceStatus;
import org.cip4.jdflib.core.AttributeName;
import org.cip4.jdflib.core.ElementName;
import org.cip4.jdflib.core.JDFDoc;
import org.cip4.jdflib.core.JDFParser;
import org.cip4.jdflib.core.KElement;
import org.cip4.jdflib.core.JDFElement.EnumNodeStatus;
import org.cip4.jdflib.jmf.JDFDeviceFilter;
import org.cip4.jdflib.jmf.JDFDeviceInfo;
import org.cip4.jdflib.resource.JDFDevice;
import org.cip4.jdflib.resource.JDFDeviceList;

/**
 * 
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: BaseICSFilterTest.java,v 1.6 2006/08/30 16:15:28 buckwalter Exp $
 */
public class BaseICSFilterTest extends ElkTestCase {

    private JDFElementFactory _factory;

    public void setUp() {
        _factory = JDFElementFactory.getInstance();

    }

    public JDFDevice createJDFDevice(String ID, String Class,
            EnumNodeStatus status, String deviceID) {
        //		Required attributes from the abstract resource element:
        //		ID
        //		Class
        //		Status
        //		And from the Device element:
        //		DeviceID
        JDFDevice d = (JDFDevice) _factory.createJDFElement(ElementName.DEVICE);
        d.setID(ID);
        d.setAttribute(AttributeName.CLASS, Class);
        d.setDeviceID(deviceID);
        d.setStatus(status);
        return d;

    }
    
    // Adds a device to a device list, also appending the DeviceInfo element
    public void addDeviceToJDFDeviceList(JDFDeviceList list, JDFDevice d,
            EnumDeviceStatus status) {
        JDFDeviceInfo deviceinfo = (JDFDeviceInfo) _factory
                .createJDFElement(ElementName.DEVICEINFO);

        deviceinfo.copyElement(d, null); // add device to deviceInfo
        deviceinfo.setDeviceStatus(status); // JDF 1.2
        list.copyElement(deviceinfo, null); // add deviceInfo to list
    }

    public JDFDeviceList getDeviceListFromFile(String file) {

        JDFDoc d = null;
        try {
            d = new JDFParser().parseFile(file);
        } catch (Exception e) {
            assertTrue(false);
            System.out.println("No such file, or could not parse file");
            e.printStackTrace();
        }
        JDFDeviceList deviceList = (JDFDeviceList) d.getRoot();
        assertNotNull(deviceList);
        return deviceList;

    }

    /**
     * Tests the list so that it contains required attributes for BaseICS. Does
     * not test if it is a valid list according to JDF 1.2.
     * 
     * @param list
     */
    private void testDeviceList(JDFDeviceList list) {
        final KElement deviceInfoElements[] = list.getChildElementArray();
        for (int i = 0; i < deviceInfoElements.length; i++) {
            final JDFDeviceInfo deviceInfo = (JDFDeviceInfo) deviceInfoElements[i];
            final JDFDevice device = deviceInfo.getDevice();
            assertTrue(deviceInfo.hasAttribute(AttributeName.DEVICESTATUS));
        }

    }

    public void testFilterDeviceList() {
        BaseICSDeviceFilter b = new BaseICSDeviceFilter();
        //		new JDFParser().parseStream(stream).getRoot();
        JDFDeviceFilter filter = (JDFDeviceFilter) _factory
                .createJDFElement(ElementName.DEVICEFILTER);

        filter.setDeviceDetails(EnumDeviceDetails.None);

        try {
            b.filterDeviceList(null, filter);
        } catch (Exception e) {
            assertTrue(true);
            log.info("Filter a devicelist that is null should"
                    + " cause an excpetion");
        }

        JDFDevice d1 = createJDFDevice("Elk", "Implementation",
                                       EnumNodeStatus.Ready, "Elk");
        JDFDevice d2 = createJDFDevice("Gosta", "Implementation",
                                       EnumNodeStatus.Completed, "Rolle");

        JDFDeviceList list = (JDFDeviceList) _factory
                .createJDFElement(ElementName.DEVICELIST);
        JDFDeviceList list2 = (JDFDeviceList) _factory
                .createJDFElement(ElementName.DEVICELIST);
        JDFDeviceList list3 = (JDFDeviceList) _factory
                .createJDFElement(ElementName.DEVICELIST);

        addDeviceToJDFDeviceList(list, d1, EnumDeviceStatus.Idle);
        addDeviceToJDFDeviceList(list, d2, EnumDeviceStatus.Stopped);

        list2 = (JDFDeviceList) list.cloneNode(true);

        // make sure that the filterDeviceList does not modify the incoming
        // list.
        // this test depends on the toString function but is valid for this
        // purpose
        assertEquals(list.toString(), list2.toString());
        list3 = b.filterDeviceList(list, filter);
        assertEquals(list.toString(), list2.toString());

        //System.out.println(list3);
        testDeviceList(list3);
        String testFile = "deviceList.xml";
        
        JDFDeviceList l4 = null;
        try {
            l4 = (JDFDeviceList) getResourceAsJDF(_testDataPath + testFile);
        } catch (Exception e1) {
            log.error("Check that the file is the class path: " + _testDataPath + testFile,e1);            
        }
        filter.setDeviceDetails(EnumDeviceDetails.Details);
        l4 = b.filterDeviceList(l4, filter);
        testDeviceList(list3);
        log.debug("Finished testing BaseICSFilter");

    }

}
