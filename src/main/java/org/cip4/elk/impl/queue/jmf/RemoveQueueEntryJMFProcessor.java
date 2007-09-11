/*
 * Created on Sep 19, 2004
 */
package org.cip4.elk.impl.queue.jmf;

import org.cip4.elk.Config;
import org.cip4.elk.impl.jmf.AbstractJMFProcessor;
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
 * @version $Id: RemoveQueueEntryJMFProcessor.java,v 1.5 2006/01/17 10:25:12 buckwalter Exp $
 */
public class RemoveQueueEntryJMFProcessor extends AbstractJMFProcessor {

	// private Config _config;
	private Queue _queue;
    private static final String MESSAGE_TYPE = "RemoveQueueEntry";

	public RemoveQueueEntryJMFProcessor(Config config, Queue queue) {
		super();
        // _config = config; TODO Not used, should it be here?
		_queue = queue;
		setMessageType(MESSAGE_TYPE);
		setCommandProcessor(true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cip4.elk.jmf.JMFProcessor#processJMF(com.heidelberg.JDFLib.jmf.JDFMessage,
	 *      com.heidelberg.JDFLib.jmf.JDFResponse)
	 */
	public int processMessage(JDFMessage input, JDFResponse output) {
		return processRemoveQueueEntry((JDFCommand) input, output);
	}

	/**
	 * Processes SubmitQueueEntry commands. Queue entries will not be submitted
	 * if the queue is full or has status <em>Closed</em>. If it is
	 * determined that the queue is full its status will be changed to
	 * <em>Full</em>.
	 * 
	 * @param command
	 * @param response
	 * @return
	 */
	private synchronized int processRemoveQueueEntry(JDFCommand command,
			JDFResponse response) {
		int returnCode = 0;
		String qeId = command.getQueueEntryDef(0).getQueueEntryID();
		JDFQueueEntry qe = _queue.getQueueEntry(qeId);
		if (qe == null) {
			// Queue entry does not exist
			returnCode = 105;
			String msg = "The queue entry '" + qeId
					+ "' could not be removed because it does not exist.";
			appendNotification(response, JDFNotification.EnumClass.Error,
					returnCode, msg);
			log.warn(msg);
		} else if (qe.getQueueEntryStatus().equals(
				JDFQueueEntry.EnumQueueEntryStatus.Running)) {
			// Queue entry is running
			returnCode = 106;
			String msg = "The queue entry '" + qeId
					+ "' could not be removed because it is running.";
			appendNotification(response, JDFNotification.EnumClass.Error,
					returnCode, msg);
			log.warn(msg);
		} else if (qe.getQueueEntryStatus().equals(
				JDFQueueEntry.EnumQueueEntryStatus.Suspended)) {
			// Queue entry is suspended
			returnCode = 106;
			String msg = "The queue entry '" + qeId
					+ "' could not be removed because it is suspended.";
			appendNotification(response, JDFNotification.EnumClass.Error,
					returnCode, msg);
			log.warn(msg);
		} else {
			// Queue entry can be removed
			returnCode = 0;
			_queue.removeQueueEntry(qeId);
			log.info("Removed the queue entry '" + qeId + "'.");
		}

		// Returns a filtered queue
		JDFQueue q = _queue.getQueue(command.getQueueFilter(0));
		response.copyElement(q, null);
		response.setReturnCode(returnCode);
		return returnCode;
	}

}
