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
import org.cip4.jdflib.jmf.JDFResponse;

/**
 * A processor for CloseQueue commands. All this processor does is changes the
 * queue's status to <em>Closed</em>.
 * 
 * @author Claes Buckwalter (clabu@itn.liu.se)
 * @version $Id: HoldQueueJMFProcessor.java,v 1.4 2005/09/10 02:28:38 ola.stering Exp $
 */
public class HoldQueueJMFProcessor extends AbstractJMFProcessor {

	// private Config _config;

	private Queue _queue;

	private static final String MESSAGE_TYPE = "HoldQueue";

	public HoldQueueJMFProcessor(Config config, Queue queue) {
		super();
		// _config = config; TODO Not used, should it be here?
		_queue = queue;
		setMessageType(MESSAGE_TYPE);
		setCommandProcessor(true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cip4.elk.impl.jmf.AbstractJMFProcessor#processMessage(com.heidelberg.JDFLib.jmf.JDFMessage,
	 *      com.heidelberg.JDFLib.jmf.JDFResponse)
	 */
	public int processMessage(JDFMessage input, JDFResponse output) {
		return processHoldQueue((JDFCommand) input, output);
	}

	public int processHoldQueue(JDFCommand command, JDFResponse response) {
		int returnCode = 0;
		// Changes the queue's status to closed
		_queue.holdQueue();

		// Returns a filtered queue
		JDFQueue q = _queue.getQueue(command.getQueueFilter(0));
		response.copyElement(q, null);
		response.setReturnCode(returnCode);
		return returnCode;
	}
}
