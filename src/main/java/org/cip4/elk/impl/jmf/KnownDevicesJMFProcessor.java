/*
 * Created on 2005-mar-05
 */

package org.cip4.elk.impl.jmf;

import org.cip4.elk.JDFElementFactory;
import org.cip4.elk.device.DeviceConfig;
import org.cip4.elk.device.process.Process;
import org.cip4.elk.impl.jmf.util.BaseICSDeviceFilter;
import org.cip4.elk.jmf.util.DeviceFilter;
import org.cip4.jdflib.core.ElementName;
import org.cip4.jdflib.jmf.JDFDeviceInfo;
import org.cip4.jdflib.jmf.JDFMessage;
import org.cip4.jdflib.jmf.JDFQuery;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.resource.JDFDeviceList;

/**
 * <p>
 * A <code>JMFprocessor</code> that handles the <em>KnowDevices</em> query.
 * </p>
 * <p>
 * The <em>Response</em> for the <em>KnownDevices</em> Query will be Base
 * ICS 1.0 conformant. The <code>KnownDevicesJMFProcessor</code> will always
 * return required fields of the device in the <em>Response</em> message. If
 * the incoming device is not Base ICS 1.0 conformant, the processor will add
 * required attributes at best effort.
 * </p>
 * 
 * @see <a
 *      href="http://www.cip4.org/document_archive/documents/ICS-Base-1.0.pdf">ICS-Base-1.0
 *      Specification, 5.4.1 KnownDevices </a>
 * @see <a
 *      href="http://www.cip4.org/documents/jdf_specifications/JDF1.2.pdf">JDF
 *      Specification Release 1.2, 5.5.1.2 KnownDevices </a>
 * 
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: KnownDevicesJMFProcessor.java,v 1.6 2005/11/11 10:16:55 ola.stering Exp $
 */
public class KnownDevicesJMFProcessor extends AbstractJMFProcessor {

    private Process _deviceProcess; // XXX
    private DeviceFilter _devicefilter;
    private final static String MESSAGE_TYPE = "KnownDevices";

    /**
     * Creates an instance of the KnownDevicesJMFProcessor.
     * 
     * @deprecated use {@link #KnownDevicesJMFProcessor(Process)} instead.
     * 
     * @param deviceProcess the <code>Process</code> that is part of the
     *            <em>Device</em> this <code>JMFProcessor</code> belongs to.
     * @param config the configuration of the <em>Device</em> this
     *            <code>JMFProcessor</code> belongs to.
     */
    public KnownDevicesJMFProcessor(Process deviceProcess, DeviceConfig config) {
        super();
        _deviceProcess = deviceProcess;
        _devicefilter = new BaseICSDeviceFilter();
        setMessageType(MESSAGE_TYPE);
        setQueryProcessor(true);
        log.debug("KnownDevicesJMFProcessor created");
    }

    /**
     * Creates an instance of the KnownDevicesJMFProcessor.
     * 
     * @param deviceProcess the <code>Process</code> that is part of the
     *            <em>Device</em> this <code>JMFProcessor</code> belongs to.
     */
    public KnownDevicesJMFProcessor(Process deviceProcess) {
        super();
        _deviceProcess = deviceProcess;
        _devicefilter = new BaseICSDeviceFilter();
        setMessageType(MESSAGE_TYPE);
        setQueryProcessor(true);
        log.debug("KnownDevicesJMFProcessor created.");
    }

    /**
     * Processes <em>KnownDevices</em> queries.
     * 
     * @param input the input <em>Query</em>.
     * @param output the <em>Response</em> message.
     * @return the return code: 0 Success, else Error code.
     */
    public int processMessage(JDFMessage input, JDFResponse output) {
        return processKnownDevices((JDFQuery) input, output);
    }

    /**
     * Processes <em>KnownDevices</em> queries.
     * 
     * @param input The <em>Query</em>.
     * @param output the <em>Response</em> message.
     * @return the JMF return code: 0 = Success
     */
    private int processKnownDevices(JDFQuery input, JDFResponse output) {

        int returnCode = 0;

        JDFDeviceInfo deviceinfo = _deviceProcess.getDeviceInfo(true);

        JDFDeviceList list = (JDFDeviceList) JDFElementFactory.getInstance()
                .createJDFElement(ElementName.DEVICELIST);
        list.copyElement(deviceinfo, null); // add deviceInfo to list

        list = _devicefilter.filterDeviceList(list, input.getDeviceFilter(0));

        output.copyElement(list, null);
        output.setType(getMessageType());
        output.setReturnCode(returnCode);
        return returnCode;
    }
}
