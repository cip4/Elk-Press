/*
 * Created on 2005-mar-16
 *
 */
package org.cip4.elk.impl.jmf;

import org.cip4.elk.device.DeviceConfig;
import org.cip4.jdflib.core.ElementName;
//import org.cip4.jdflib.core.JDFConstants;
import org.cip4.jdflib.core.VString;
import org.cip4.jdflib.jmf.JDFMessage;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.jmf.JDFSubmissionMethods;

/**
 * Implements the SubmissionMehtods Query.
 * 
 * In the response message, only the <em>URLSchemes</em> attribute is
 * considered
 * 
 * The <em>HotFolder</em> attribute is ignored. Needs to be handled for Device
 * to be Base ICS.
 * 
 * @see <a
 *      href="http://www.cip4.org/document_archive/documents/ICS-Base-1.0.pdf">ICS-Base-1.0
 *      Specification, 5.4.5 SubmissionMethods </a>
 * @see <a
 *      href="http://www.cip4.org/documents/jdf_specifications/JDF1.2.pdf">JDF
 *      Specification Release 1.2, 5.6.4.8 SubmissionMethods </a>
 * 
 * @author Ola Stering, olst6875@student.uu.se
 * @version $Id: SubmissionMethodsJMFProcessor.java,v 1.3 2006/08/24 09:01:27 prosi Exp $
 */
public class SubmissionMethodsJMFProcessor extends AbstractJMFProcessor {

    private DeviceConfig _config;

    private final static String MESSAGE_TYPE = "SubmissionMethods";

    public SubmissionMethodsJMFProcessor(DeviceConfig config) {
        super();
        log.info("Created " + ElementName.SUBMISSIONMETHODS + "JMFProcessor");
        _config = config;
        setMessageType(MESSAGE_TYPE);
        setQueryProcessor(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cip4.elk.impl.jmf.AbstractJMFProcessor#processMessage(com.heidelberg.JDFLib.jmf.JDFMessage,
     *      com.heidelberg.JDFLib.jmf.JDFResponse)
     */
    public int processMessage(JDFMessage input, JDFResponse output) {
        JDFSubmissionMethods sm = output.appendSubmissionMethods();

        sm.setURLSchemes(new VString(_config.getURLSchemes(),null));
        int returnCode = 0;
        output.setType(getMessageType());
        output.setReturnCode(returnCode);
        return returnCode;

    }    

}
