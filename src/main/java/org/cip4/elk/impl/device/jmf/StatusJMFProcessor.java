/*
 * Created on Sep 20, 2004
 */
package org.cip4.elk.impl.device.jmf;

import org.cip4.elk.device.DeviceConfig;
import org.cip4.elk.device.process.Process;
//import org.cip4.elk.impl.device.process.BaseProcess;
import org.cip4.elk.impl.jmf.AbstractJMFProcessor;
import org.cip4.elk.impl.jmf.util.BaseICSDeviceFilter;
import org.cip4.elk.queue.Queue;
import org.cip4.jdflib.auto.JDFAutoStatusQuParams.EnumDeviceDetails;
import org.cip4.jdflib.jmf.JDFDeviceInfo;
import org.cip4.jdflib.jmf.JDFMessage;
import org.cip4.jdflib.jmf.JDFQuery;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.jmf.JDFStatusQuParams;

/**
 * A JMF processor that handles the <em>Status</em> query.
 * 
 * @TODO return a QueueElement implement StatusQuParams/@QueueInfo.
 * 
 * @see <a
 *      href="http://www.cip4.org/documents/jdf_specifications/JDF1.2.pdf">JDF
 *      Specification Release 1.2, 5.5.2.8 Status </a>
 * @author Claes Buckwalter (clabu@itn.liu.se)
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: StatusJMFProcessor.java,v 1.8 2006/05/29 09:25:45 buckwalter Exp $
 */
public class StatusJMFProcessor extends AbstractJMFProcessor {
    private Process _device; // XXX
    private BaseICSDeviceFilter filter; // Could also be a DeviceFilter
    // private Queue _queue; // TODO Read it
    private static final String MESSAGE_TYPE = "Status";

    /**
     * Creates a StatusJMFProcessor.
     * 
     * @deprecated use {@link #StatusJMFProcessor(Process, Queue)} instead.
     * 
     * @param config this Processor's configuration.
     * @param device this Processor's process.
     * @param queue this Processor's queue.
     */
    public StatusJMFProcessor(DeviceConfig config, Process device, Queue queue) {
        super();
        _device = device; // XXX
        // _queue = queue; TODO Read it.
        setMessageType(MESSAGE_TYPE);
        setQueryProcessor(true);
        filter = new BaseICSDeviceFilter();
    }

    /**
     * Creates a StatusJMFProcessor.
     * 
     * @param device this Processor's process.
     * @param queue this Processor's queue.
     */
    public StatusJMFProcessor(Process device, Queue queue) {
        super();
        _device = device; // XXX
        // _queue = queue; TODO Read it.
        setMessageType(MESSAGE_TYPE);
        setQueryProcessor(true);
        filter = new BaseICSDeviceFilter();
    }

    /**
     * Processes a <em>Status</em> message. Implemented attributes of the
     * <em>StatusQuParams</em> is <em>DeviceDetails</em>. Other
     * <em>StatusQuParams</em> attributes/elements are ignored. Filters the
     * <em>Response/DeviceInfo</em> element according to
     * {@link BaseICSDeviceFilter#applyDetails(JDFDeviceInfo, String)}for the
     * <em>DeviceDetails</em>.
     * 
     * @see BaseICSDeviceFilter
     * @see org.cip4.elk.impl.jmf.AbstractJMFProcessor#processMessage(org.cip4.jdflib.jmf.JDFMessage,
     *      org.cip4.jdflib.jmf.JDFResponse)
     * @return appropriate return code, 0 for success.
     * @throws NullPointerException if Query/StatusQuParams is <code>null</code>.
     */
    public int processMessage(JDFMessage input, JDFResponse output) {
        return processStatus((JDFQuery) input, output);
    }

    /**
     * @see #processMessage(JDFMessage, JDFResponse)
     * @param query
     * @param response
     * @return
     */
    private int processStatus(JDFQuery query, JDFResponse response) {
        int returnCode = 0;
        JDFStatusQuParams sqp = query.getStatusQuParams(0);
        if (sqp == null) {
            String msg = "StatusQuParams may not be null.";
            log.error(msg);
            throw new NullPointerException(msg);
        }
        log.debug("Processing Status query: " + sqp);
        // TODO Parse the status queue parameters

        JDFDeviceInfo info = _device.getDeviceInfo(true);
        response.copyElement(info,null);
        
        EnumDeviceDetails edd = sqp.getDeviceDetails();
        if (edd == null) {
            // Default
            edd = EnumDeviceDetails.None;
        }
        filter.applyDetails(info, edd.getName());
        log.debug("DeviceInfo: " + info);
        return returnCode;
    }
}
