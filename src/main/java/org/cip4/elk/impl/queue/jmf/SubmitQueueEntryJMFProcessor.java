/*
 * Created on Sep 19, 2004
 */
package org.cip4.elk.impl.queue.jmf;

import org.cip4.elk.Config;
import org.cip4.elk.impl.jmf.AbstractJMFProcessor;
import org.cip4.elk.impl.util.URLAccessTool;
import org.cip4.elk.queue.Queue;
import org.cip4.jdflib.jmf.JDFCommand;
import org.cip4.jdflib.jmf.JDFMessage;
import org.cip4.jdflib.jmf.JDFQueue;
import org.cip4.jdflib.jmf.JDFQueueEntry;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.resource.JDFNotification;

/**
 * A processor that handles SubmitQueueEntry commands. Queue entries will not be 
 * submitted if the queue is full or if the queue's status is closed.
 * 
 * @author Claes Buckwalter (clabu@itn.liu.se)
 * @version $Id: SubmitQueueEntryJMFProcessor.java,v 1.4 2005/09/10 02:28:38 ola.stering Exp $
 */
public class SubmitQueueEntryJMFProcessor extends AbstractJMFProcessor {

    // private Config _config;
    private Queue _queue;
    private static final String MESSAGE_TYPE = "SubmitQueueEntry";
    
    public SubmitQueueEntryJMFProcessor(Config config, Queue queue, URLAccessTool fileUtil) {
        super();
        // _config = config; TODO Not used, should it be here?
        _queue = queue;        
        setMessageType(MESSAGE_TYPE);
        setCommandProcessor(true);
    }
    
    /* (non-Javadoc)
     * @see org.cip4.elk.jmf.JMFProcessor#processJMF(com.heidelberg.JDFLib.jmf.JDFMessage, com.heidelberg.JDFLib.jmf.JDFResponse)
     */
    public int processMessage(JDFMessage input, JDFResponse output) {
        return processSubmitQueueEntry((JDFCommand)input, output);   
    }

    /**
     * Processes SubmitQueueEntry commands. Queue entries will not be submitted if
     * the queue is has status <em>Closed</em>, <em>Full</em>, <em>Held</em> or
     * <em>Blocked</em>.
     * @param command
     * @param response
     * @return return code
     */
    private synchronized int processSubmitQueueEntry(JDFCommand command, JDFResponse response) {
        int returnCode = 0;
        JDFQueueEntry qe = _queue.addQueueEntry(command.getQueueSubmissionParams(0));
        if (qe != null) {
            log.info("Submitted queue entry: " + qe);
        } else {
            JDFQueue.EnumQueueStatus status = _queue.getQueueStatus();        
            returnCode = 112;
            String msg = "Job rejected. The queue is " + status.getName() + ".";            
            appendNotification(response, JDFNotification.EnumClass.Warning, returnCode, msg);            
            log.info(msg);         
        }
        // Returns a filtered queue
        JDFQueue q = _queue.getQueue(command.getQueueFilter(0));
        response.copyElement(q, null);
        response.setReturnCode(returnCode);
        return returnCode;
    }    
    
}
