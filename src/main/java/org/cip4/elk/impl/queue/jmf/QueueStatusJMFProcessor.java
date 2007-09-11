/*
 * Created on Sep 19, 2004
 */
package org.cip4.elk.impl.queue.jmf;

import org.cip4.elk.Config;
import org.cip4.elk.impl.jmf.AbstractJMFProcessor;
import org.cip4.elk.queue.Queue;

import org.cip4.jdflib.jmf.JDFMessage;
import org.cip4.jdflib.jmf.JDFQuery;
import org.cip4.jdflib.jmf.JDFQueue;
import org.cip4.jdflib.jmf.JDFResponse;

/**
 * A processor for QueueStatus queries.
 * 
 * @author Claes Buckwalter (clabu@itn.liu.se)
 * @version $Id: QueueStatusJMFProcessor.java,v 1.4 2005/09/10 02:28:38 ola.stering Exp $
 */
public class QueueStatusJMFProcessor extends AbstractJMFProcessor {

	// private Config _config;
	private Queue _queue;
    private static final String MESSAGE_TYPE = "QueueStatus";

	public QueueStatusJMFProcessor(Config config, Queue queue) {
		super();
        // _config = config; TODO Not used, should it be here?
		_queue = queue;		
		setMessageType(MESSAGE_TYPE);
		setQueryProcessor(true);
	}

	/**
	 * Processes JMF messages.
	 */
	public int processMessage(JDFMessage input, JDFResponse output) {
		return processQueueStatus((JDFQuery) input, output);
	}

	/**
	 * Processes QueueStatus queries.
	 * 
	 * @param query
	 * @param response
	 * @return
	 */
	private int processQueueStatus(JDFQuery query, JDFResponse response) {
		int returnCode = 0;
		// Checks if queue is full
		JDFQueue q = _queue.getQueue(query.getQueueFilter(0));
		response.copyElement(q, null);
		response.setReturnCode(returnCode);
		return returnCode;
	}

}
