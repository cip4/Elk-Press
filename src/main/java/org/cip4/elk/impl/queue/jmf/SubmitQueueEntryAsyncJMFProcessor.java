package org.cip4.elk.impl.queue.jmf;

import org.cip4.elk.impl.jmf.AbstractJMFProcessor;
import org.cip4.elk.impl.jmf.preprocess.JDFPreprocessor;
import org.cip4.elk.impl.jmf.util.Messages;
import org.cip4.elk.queue.Queue;
import org.cip4.jdflib.core.KElement;
import org.cip4.jdflib.jmf.JDFCommand;
import org.cip4.jdflib.jmf.JDFMessage;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.resource.JDFNotification;

import EDU.oswego.cs.dl.util.concurrent.Executor;
import EDU.oswego.cs.dl.util.concurrent.ThreadedExecutor;

/**
 * A <code>JMProcessor</code> that handles <em>SubmitQueueEntry</em> commands, 
 * asynchronously or synchronously, and dispatches them to a <code>JDFPreprocessor</code>
 * for processing.
 * 
 * @author Ola Stering (olst6875@student.uu.se)
 * @author Claes Buckwalter (clabu@itn.liu.se)
 * @version $Id: SubmitQueueEntryAsyncJMFProcessor.java,v 1.7 2006/01/17 10:24:01 buckwalter Exp $
 */
public class SubmitQueueEntryAsyncJMFProcessor extends AbstractJMFProcessor {

    // private Queue _queue;
    private JDFPreprocessor _preprocessor;
    private static final String MESSAGE_TYPE = "SubmitQueueEntry";    
    private boolean _asynchronous = true;
    private Executor _executor;

    
    /**
     * Creates a new <code>JMFProcessor</code> for <em>SubmitQueueEntry</em> commands.
     * SubmitQueueEntry commands are preprocessed by the specified <code>JDFPreprocessor</code>
     * and added to the <code>Queue</code>. Commands are processed asynchronously.
     * @param queue the Queue to add submitted jobs/queue entries to
     * @param preprocessor  the JDFPreprocessor used to preprocess the submitted job
     */
    public SubmitQueueEntryAsyncJMFProcessor(Queue queue, JDFPreprocessor preprocessor) {
        this(queue, preprocessor, true);
    }
    
    /**
     * Creates a new <code>JMFProcessor</code> for <em>SubmitQueueEntry</em> commands.
     * SubmitQueueEntry commands are preprocessed by the specified <code>JDFPreprocessor</code>
     * and added to the <code>Queue</code>.
     * @param queue the Queue to add submitted jobs/queue entries to
     * @param preprocessor  the JDFPreprocessor used to preprocess the submitted job
     * @param asynchronous  true to do processing asynchronously; false to do processing synchronously 
     */
    public SubmitQueueEntryAsyncJMFProcessor(Queue queue, JDFPreprocessor preprocessor, boolean asynchronous) {
        super();
        // _queue = queue;
        _preprocessor = preprocessor;
        _executor = new ThreadedExecutor(); //TODO Replace with a PooledExecutor?
        setAsynchronous(asynchronous);
        setMessageType(MESSAGE_TYPE);
        setCommandProcessor(true);
        setAcknowledgeProcessor(true);
    }
    
    /**
     * Sets whether processing should be done synchronously or asynchronously. 
     * @param asyncMode true to process asynchronously; false to process synchronously
     */
    public void setAsynchronous(boolean asynchronous) {
        _asynchronous = asynchronous;
    }
    
    /**
     * Returns true i processing is done asynchronously.
     * @return true if processing is done asynchronously; false otherwise
     */
    public boolean isAsynchronous() {
        return _asynchronous;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cip4.elk.impl.jmf.AbstractJMFProcessor#processMessage(com.heidelberg.JDFLib.jmf.JDFMessage,
     *      com.heidelberg.JDFLib.jmf.JDFResponse)
     */
    public int processMessage(JDFMessage input, JDFResponse output) {
        return processSubmitQueueEntry((JDFCommand) input, output);
    }

    /**
     * Processes <em>SubmitQueueEntry</em> commands. If the
     * command/@AcknowledgeURL is set the command will be dispatched
     * asynchronously.
     * 
     * @param command
     * 			The incoming command
     * @param response
     * 	`	The resulting response
     * @return return code, 0 on success, errorCodes according to specification otherwise.
     * 
     * @throws NullPointerException
     *             if command or response is <code>null</code>
     */
    private int processSubmitQueueEntry(JDFCommand command,
            JDFResponse incomingResponse) {
        int returnCode = 0;

        String ackURL = command.getAcknowledgeURL();
        final JDFCommand commandSubmitQueueEntry = command;
        
        if (_asynchronous && ackURL != null && ackURL.length() > 0) {
            // Preprocess asynchronously
            log.debug("Processing SubmitQueueEntry command asynchronously...");
            try {
                _executor.execute(new Runnable() {
                    public void run() {
                        _preprocessor.preProcessJDF(commandSubmitQueueEntry);
                    }
                });
                incomingResponse.setAcknowledged(true);
                incomingResponse.setReturnCode(returnCode);
                incomingResponse.appendComment().appendText(
                    "The SubmitQueueEntry command was received and is being processed."
                        + " An Acknowledge message will be sent to URL: " + ackURL);
            } catch (InterruptedException ie) {
                String msg = "An error occurred while processing a SubmitQueueEntry" +
                        " command asynchronously. The job was not submitted." +
                        " SubmitQueueEntry command: "  + command;                
                log.error(msg, ie);
                returnCode = 1; // General error
                Messages.appendNotification(incomingResponse,
                    JDFNotification.EnumClass.Error, returnCode, msg);
            }
        } else {
            // Preprocess synchronously
            log.debug("Processing SubmitQueueEntry command synchronously...");
            JDFResponse newResponse = _preprocessor.preProcessJDF(commandSubmitQueueEntry);
            // Copy Response from preprocessor to original Response
            KElement[] elements = newResponse.getChildElementArray();
            for (int i = 0, size = elements.length; i < size; i++) {
                incomingResponse.copyElement(elements[i], null);
            }
            returnCode = newResponse.getReturnCode();
            incomingResponse.setReturnCode(returnCode);
            log.debug("Done processing SubmitQueueEntry command synchronously. Return code was: " + returnCode);
        }
        return returnCode;
    }
}
