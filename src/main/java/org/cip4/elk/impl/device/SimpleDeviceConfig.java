/*
 * Created on Sep 10, 2004
 */
package org.cip4.elk.impl.device;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.cip4.elk.DefaultConfig;
import org.cip4.elk.device.DeviceConfig;
import org.cip4.elk.impl.util.URLAccessTool;
import org.cip4.jdflib.core.ElementName;
import org.cip4.jdflib.core.JDFParser;
import org.cip4.jdflib.node.JDFNode;
import org.cip4.jdflib.resource.JDFDevice;
import org.cip4.jdflib.resource.devicecapability.JDFDeviceCap;

/**
 * An object that represents the configuration of a JDF <em>Device</em>.
 * 
 * NOTE: The supported url schemes are hardwired. The Elk Device currently
 * supports these schemes: file, ftp, http, https
 * 
 * @todo modify so that the url schemes are not hard wired (i.e. through
 *       configuration)
 * 
 * @author Claes Buckwalter (clabu@itn.liu.se)
 * @version $Id: SimpleDeviceConfig.java,v 1.14 2006/08/30 15:44:21 buckwalter
 *          Exp $
 */
public class SimpleDeviceConfig extends DefaultConfig implements DeviceConfig {

    protected Logger log;

    protected JDFDevice _jdfDevice;

    protected String _deviceConfigUrl;

    protected URLAccessTool _urlAccessTool;

    protected String urlSchemes = "file ftp http https";

    /**
     * Instantiates a new <code>DeviceConfig</code>. Its configuration must
     * be set using the setter {@link #setDeviceConfig(JDFDevice)}.
     */
    public SimpleDeviceConfig() {
        this(null, null);
    }

    /**
     * Instantiates a new <code>DeviceConfig</code> that loads its
     * configuration from the file pointed to by the specified URL.
     * 
     * The device configuration file pointed to by the URL must be an XML file
     * with a <code>Device</code> resource element as the root element. The
     * <code>Device</code> element and it's child elements represent the
     * device's configuration. If the URL is not absolute it will be interpreted
     * as relative to the base URL of the <code>URLAccessTool</code>, see
     * {@link URLAccessTool#getBaseUrl()}.
     * </p>
     * 
     * @param deviceConfigUrl
     *            the URL to the Device XML element
     * @param urlAccessTool
     *            the <code>URLAccessTool</code> used to access the URL
     */
    public SimpleDeviceConfig(String deviceConfigUrl,
            URLAccessTool urlAccessTool) {
        super();
        log = Logger.getLogger(this.getClass());
        _urlAccessTool = urlAccessTool;
        setDeviceConfigURL(deviceConfigUrl);
    }

    /**
     * Sets the URL that points the file containing the device's configuration.
     * The file must be an XML file with a <code>Device</code> resource
     * element as the root element. The <code>Device</code> element and it's
     * child elements represent the device's configuration.
     * <p>
     * If the URL is not absolute it will be interpreted as relative to the base
     * URL of the <code>URLAccessTool</code>, see
     * {@link URLAccessTool#getBaseUrl()}.
     * </p>
     * 
     * @see org.cip4.elk.impl.util.URLAccessTool
     * @param deviceConfigUrl
     */
    public void setDeviceConfigURL(String deviceConfigUrl) {
        _deviceConfigUrl = deviceConfigUrl;
    }

    /*
     * @see org.cip4.elk.device.DeviceConfig#getDeviceConfig()
     */
    public synchronized JDFDevice getDeviceConfig() {
        if (_jdfDevice == null) {
            if (_urlAccessTool != null && _deviceConfigUrl != null) {
                // XXX JDFDevice d = loadDeviceConfiguration(_deviceConfigUrl);
                // XXX _jdfDevice = (JDFDevice) convert2DOMLevel2(d);
                _jdfDevice = loadDeviceConfiguration(_deviceConfigUrl);
            }
        }
        return _jdfDevice;
    }

    /**
     * List of supported schemes for retrieving JDF files.
     * 
     * @see org.cip4.elk.device.DeviceConfig#getURLSchemes()
     */
    public String getURLSchemes() {

        return urlSchemes;
    }

    /**
     * Returns the JMFURL of the Device.
     * 
     * @return the JMFURL of the Device.
     * @see org.cip4.elk.device.DeviceConfig#getJMFURL()
     * @throws NullPointerExeception
     *             if the <em>JDFDevice</em> of this configuration is
     *             <code>null</code>
     */
    public String getJMFURL() {
        JDFDevice d = getDeviceConfig();
        if (d == null) {
            throw new NullPointerException(
                    "The JDFDevice of this SimpleDeviceConfig is null.");
        }
        return d.getJMFURL();
    }

    /*
     * @see org.cip4.elk.device.DeviceConfig#setDeviceConfig(JDFDevice)
     */
    public synchronized void setDeviceConfig(final JDFDevice deviceConfig) {
        _jdfDevice = deviceConfig;
        super.setID(_jdfDevice.getDeviceID());
    }

    /**
     * Set the Device/@JMFSenderID, Device/@JMFSenderID of this SimpleDevice.
     * For this device's configuration the DeviceID and the JMFSenderID will
     * always be the same.
     * 
     * To get the these values use getID
     * 
     * @see org.cip4.elk.impl.device.DeviceConfig#getID()
     * 
     * @param id
     *            the new id for the Device
     * @throws NullPointerException
     *             if id == null
     */
    public void setID(String id) {

        if (id == null)
            throw new NullPointerException("parameter 'id' can not be null");

        _jdfDevice.setJMFSenderID(id);
        _jdfDevice.setDeviceID(id);
        super.setID(id);
    }

    /**
     * Returns a JDF <em>Device</em> resource element that is loaded from the
     * specified URL.
     * 
     * @param deviceConfigUrl
     *            a URL to the <em>Device</em> element to return
     * @return the <code>JDFDevice</code> object loaded from the specified URL
     * @throws IOException
     *             if an IO problem occurs
     */
    private JDFDevice loadDeviceConfiguration(String deviceConfigUrl) {
        InputStream inStream = null;
        try {
            inStream = _urlAccessTool.getURLAsInputStream(deviceConfigUrl);
            return (JDFDevice) new JDFParser().parseStream(inStream).getRoot();
        } finally {
            IOUtils.closeQuietly(inStream);
        }
    }

    /*
     * @see org.cip4.elk.device.DeviceConfig#getProcessNodes(org.cip4.jdflib.node.JDFNode)
     */
    public List getProcessableNodes(JDFNode jdf) {
        final JDFDevice dev = getDeviceConfig();
        List matchingNodes = new ArrayList();                
        if (dev == null) {
            // No Device configuration; return all JDF nodes as matches
            matchingNodes = jdf.getvJDFNode(null, null, false);
            log.warn("The device configuration does not contain a Device element. All ("
                    + matchingNodes.size()
                    + ") JDF nodes will be accepted by the device.");
        } else {
            final List deviceCaps = dev.getChildElementVector(
                    ElementName.DEVICECAP, null, null, true, 99999, true);
            if (deviceCaps == null || deviceCaps.size() == 0) {
                // No DeviceCap elements; return all JDF nodes as matches
                matchingNodes = jdf.getvJDFNode(null, null, false);
                log.warn("The device configuration does not contain a Device/DeviceCap element. All ("
                        + matchingNodes.size()
                        + ") JDF nodes will be accepted by the device.");
            } else {
                matchingNodes = getProcessableNodes(jdf, deviceCaps);
            }
        }
        log.debug(matchingNodes.size() + " JDF nodes matched the device's capabilities.");
        return matchingNodes;
    }

    /**
     * Returns all JDF nodes that match the DeviceCap elements in the list.
     * 
     * @param jdf   the JDF instance to look for nodes in
     * @param deviceCaps    a list of JDFDeviceCap objects
     * @return  a list containing JDFNode objects that match thet DeviceCap elements
     * @todo Reimplement using JDFLib-J's device capabilities. It does not seem to work right now.
     */
    private List getProcessableNodes(JDFNode jdf, final List deviceCaps) {
        List matchingNodes = new ArrayList();
        for (Iterator i=deviceCaps.iterator(); i.hasNext(); ) {
            final JDFDeviceCap dc = (JDFDeviceCap) i.next();
            final String dcCombinedMethod = dc.getAttribute("CombinedMethod");
            final String dcTypeEx = dc.getTypeExpression(); // JDFLib automatically returns Types if TypeExpression is not defined in DeviceCap
            if (dcTypeEx == null || dcTypeEx.length() == 0) {
                // No DeviceCap/@Types or DeviceCap/@TypeExpression specified
                matchingNodes = jdf.getvJDFNode(null, null, false);
                log.warn("The device configuration does not contain a " +
                        "Device/DeviceCap/@TypeExpression or Device/DeviceCap/@Types attribute. " +
                        "All (" + matchingNodes.size() + ") JDF nodes will be accepted by the device.");
            } else {
                log.debug("Looking for JDF nodes with CombinedMethod='" + dcCombinedMethod + "' and Types that match regex '" + dcTypeEx + "'...");
                
                // Compare JDF nodes with DeviceCap
                final List nodes = jdf.getvJDFNode(null, null, false);
                for (Iterator j=nodes.iterator(); j.hasNext(); ) {
                    final JDFNode node = (JDFNode) j.next();
                    final String nodeType = node.getType();
                    final String nodeTypes = node.getAttribute("Types"); // nodes.getTypes() would need to be converted from VString to String
                    if (nodeType.matches(dcTypeEx)
                            && (dcCombinedMethod.length() == 0 || dcCombinedMethod.equals("None"))) {
                        // Process node
                        matchingNodes.add(node);
                        if (log.isDebugEnabled()) {
                            log.debug("Accepted process node: " + node.buildXPath() + "/@Type='" + nodeType + "'");
                        }
                    } else if (nodeType.equals("Combined")
                            && nodeTypes.matches(dcTypeEx)
                            && dcCombinedMethod.equals("Combined")) {
                        // Combined process node
                        matchingNodes.add(node);
                        if (log.isDebugEnabled()) {
                            log.debug("Accepted combined process node: " + node.buildXPath() + "/@Types='" + nodeTypes + "'");
                        }
                    } else if (nodeType.equals("ProcessGroup")
                            && nodeTypes.matches(dcTypeEx)
                            && dcCombinedMethod.equals("GrayBox")) {
                        // Gray box node
                        matchingNodes.add(node);
                        if (log.isDebugEnabled()) {
                            log.debug("Accepted gray box node: " + node.buildXPath() + "/@Types='" + nodeTypes + "'");
                        }
                    }
                }
//                        // Add matches for all JDF nodes that match the DeviceCap
//                        vMatchingNodes.appendUnique(dc.getMatchingTypeNodeVector(jdf));
//                        vMatchingNodes = dc.getExecutableJDF(jdf,
//                                EnumFitsValue.Allowed,
//                                EnumValidationLevel.RecursiveComplete);
            }
        }
        return matchingNodes;
    }
}
