/*
 * Created on 2005-maj-18 
 */
package org.cip4.elk.impl.jmf;

import org.cip4.elk.ElkTestCase;
import org.cip4.elk.device.DeviceConfig;
import org.cip4.elk.impl.device.SimpleDeviceConfig;
import org.cip4.elk.impl.device.process.ApprovalProcess;
import org.cip4.elk.impl.jmf.util.Messages;
import org.cip4.jdflib.auto.JDFAutoDeviceFilter.EnumDeviceDetails;
import org.cip4.jdflib.core.ElementName;
import org.cip4.jdflib.core.JDFConstants;
import org.cip4.jdflib.core.JDFDoc;
import org.cip4.jdflib.core.VElement;
import org.cip4.jdflib.datatypes.JDFAttributeMap;
import org.cip4.jdflib.jmf.JDFDeviceFilter;
import org.cip4.jdflib.jmf.JDFDeviceInfo;
import org.cip4.jdflib.jmf.JDFJMF;
import org.cip4.jdflib.jmf.JDFQuery;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.resource.JDFDevice;

/**
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: KnownDevicesJMFProcessorTest.java,v 1.3 2006/11/29 15:22:20 buckwalter Exp $
 */
public class KnownDevicesJMFProcessorTest extends ElkTestCase {

    public void testProcessMessage() {
        log.info("Testing KnownDevicesJMFProcessor...");
        String filename = _testDataPath + "KnownDevices.jmf";
        String theDeviceXMLfile = _testDataPath + "Device_Elk_ConventionalPrinting.xml";
        JDFJMF jmf = null;
        JDFDevice device = null;
        try {
            jmf = (JDFJMF) getResourceAsJDF(filename);
            device = (JDFDevice) getResourceAsJDF(theDeviceXMLfile);
        } catch (Exception e1) {
            log.error("Do you have the '" + filename + "' and '"
                    + theDeviceXMLfile + "'in the classpath, If yes, "
                    + "is it a parseable JMF message?", e1);
            assertTrue(false);
            return;
        }

        DeviceConfig dc = new SimpleDeviceConfig();
        dc.setDeviceConfig(device);
        ApprovalProcess p = new ApprovalProcess(dc, null, null, null, null);
        KnownDevicesJMFProcessor pr = new KnownDevicesJMFProcessor(p, dc);
        JDFQuery msgIn = jmf.getQuery();
        log.debug("The query that is being tested: " + msgIn);
        JDFResponse msgOut = Messages.createResponse(msgIn.getID(),
            "KnownDevices");
        pr.processMessage(msgIn, msgOut);
        log
                .info("When the Brief filter is applied no Device elements are returned in the Response, check that between the lines of **** (manually)");
        log.info("The output message ********************************");
        log.info(msgOut);
        log.info("End of message ************************************");
        VElement v = msgOut.getChildrenByTagName(ElementName.DEVICE,
        JDFConstants.NONAMESPACE, new JDFAttributeMap(), true, true, 0);
        assertTrue(v.size() == 0);
        JDFDeviceFilter f = msgIn.getDeviceFilter(0);
        assertNotNull(f);
        f.setDeviceDetails(EnumDeviceDetails.Details);
        msgOut = Messages.createResponse(msgIn.getID(), "KnownDevices");
        pr.processMessage(msgIn, msgOut);
        log.info("When the Details filter is applied, Device elements are returned in the Response.");
        log.info("The output message ********************************");
        log.info(msgOut);
        log.info("End of message ************************************");

        JDFDeviceInfo deviceinfo = (JDFDeviceInfo) new JDFDoc(ElementName.JDF)
                .getRoot().appendElement(ElementName.DEVICEINFO,
                    JDFConstants.NONAMESPACE);
        JDFDevice d = (JDFDevice) new JDFDoc(ElementName.JDF).getRoot()
                .appendElement(ElementName.DEVICE, JDFConstants.NONAMESPACE);

        log.debug("TESTING NAMESPACE CONFIGURATION");
        log.debug("d's namespace: " + d.getNamespaceURI());
        log.debug("trying to set new name space with "
                + "d.addNameSpace('xmlns','http://atestnamespaseURI')");
        if (d.addNameSpace("xmlns", "http://aTestNamespaceURI")) {
            log.debug("The new namespace was set.");
        }
        log.debug("d.getNamespaceURI(): " + d.getNamespaceURI());
        log.debug("So this did not work.");
        d
                .setAttribute("xmlns", "http://aTestURI.com",
                    JDFConstants.NONAMESPACE);
        log.debug("The device: " + d);
        deviceinfo.copyElement(device, null); // add device to deviceInfo
    }

}
