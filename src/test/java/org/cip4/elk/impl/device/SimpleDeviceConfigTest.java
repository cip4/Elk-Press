/*
 * Created on Sep 10, 2004
 */
package org.cip4.elk.impl.device;

import java.net.URI;
import java.net.URISyntaxException;

import org.cip4.elk.ElkTestCase;
import org.cip4.elk.device.DeviceConfig;
import org.cip4.elk.impl.util.URLAccessTool;
import org.cip4.jdflib.core.JDFParser;
import org.cip4.jdflib.node.JDFNode;
import org.cip4.jdflib.resource.JDFDevice;

/**
 * @author Claes Buckwalter (clabu@itn.liu.se)
 */
public class SimpleDeviceConfigTest extends ElkTestCase {

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testSetDeviceConfig() {
        JDFDevice device2 = (JDFDevice) new JDFParser().parseStream(
            getResourceAsStream("data/DeviceExcpected.xml")).getRoot();
        assertNotNull(device2);
        DeviceConfig config = new SimpleDeviceConfig();
        config.setDeviceConfig(device2);
    }

    public void testLoadDeviceConfigFromFile() throws URISyntaxException {
        URLAccessTool fileUtil = new URLAccessTool(getResourceAsURL("data").toExternalForm());
        
        assertEquals(new URI(getResourceAsURL("data").toExternalForm()), new URI(fileUtil.getBaseUrl().toExternalForm()));
        String url = "Device_Elk_ConventionalPrinting.xml"; //DeviceWithoutNamespace.xml"; // data/Device.xml
        DeviceConfig config = new SimpleDeviceConfig(url, fileUtil);
        JDFDevice device1 = config.getDeviceConfig();
        assertNotNull(device1);
    }
    
    /**
     * Tests that Process nodes are matched
     * @throws URISyntaxException
     */
    public void testGetProcessableNodes() throws URISyntaxException {        
        final URLAccessTool fileUtil = new URLAccessTool(getResourceAsURL("data").toExternalForm());        
        assertEquals(new URI(getResourceAsURL("data").toExternalForm()), new URI(fileUtil.getBaseUrl().toExternalForm()));
        
        final String url = "Device_Elk_ConventionalPrinting.xml";        
        final DeviceConfig config = new SimpleDeviceConfig(url, fileUtil);

        final JDFNode jdf = (JDFNode) new JDFParser().parseStream(getResourceAsStream("data/jdf/Elk_ConventionalPrinting.jdf")).getRoot();
        assertEquals(1, config.getProcessableNodes(jdf).size());
        final JDFNode jdf2 = (JDFNode) new JDFParser().parseStream(getResourceAsStream("data/jdf/Elk_Approval.jdf")).getRoot();        
        assertEquals(0, config.getProcessableNodes(jdf2).size());
        final JDFNode jdf3 = (JDFNode) new JDFParser().parseStream(getResourceAsStream("data/jdf/Elk_Optimus.jdf")).getRoot();        
        assertEquals(5, config.getProcessableNodes(jdf3).size());
    }
    
    /**
     * Tests that CombinedProcess nodes are matched
     * @throws URISyntaxException
     */
    public void testGetProcessableNodesCombined() throws URISyntaxException {
        final URLAccessTool fileUtil = new URLAccessTool(getResourceAsURL("data").toExternalForm());        
        assertEquals(new URI(getResourceAsURL("data").toExternalForm()), new URI(fileUtil.getBaseUrl().toExternalForm()));
        
        final String url = "Device_Elk_DigitalPrinting_Combined.xml";        
        final DeviceConfig config = new SimpleDeviceConfig(url, fileUtil);
        
        final JDFNode jdf = (JDFNode) new JDFParser().parseStream(getResourceAsStream("data/jdf/Elk_ConventionalPrinting.jdf")).getRoot();
        assertEquals(0, config.getProcessableNodes(jdf).size());
    }
    
    public void testGetProcessableNodesUndefined() throws URISyntaxException {
        final URLAccessTool fileUtil = new URLAccessTool(getResourceAsURL("data").toExternalForm());        
        assertEquals(new URI(getResourceAsURL("data").toExternalForm()), new URI(fileUtil.getBaseUrl().toExternalForm()));
        
        final String url = "Device_Elk_DigitalPrinting_Undefined.xml";        
        final DeviceConfig config = new SimpleDeviceConfig(url, fileUtil);
        
        final JDFNode jdf = (JDFNode) new JDFParser().parseStream(getResourceAsStream("data/jdf/Elk_Optimus.jdf")).getRoot();
        assertEquals(24, config.getProcessableNodes(jdf).size());
    }
}
