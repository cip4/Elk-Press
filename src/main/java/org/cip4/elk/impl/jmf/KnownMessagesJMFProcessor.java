package org.cip4.elk.impl.jmf;

import java.util.Iterator;
import java.util.Set;

import org.cip4.elk.jmf.IncomingJMFDispatcher;
import org.cip4.elk.jmf.JMFProcessor;
import org.cip4.jdflib.jmf.JDFKnownMsgQuParams;
import org.cip4.jdflib.jmf.JDFMessage;
import org.cip4.jdflib.jmf.JDFMessageService;
import org.cip4.jdflib.jmf.JDFQuery;
import org.cip4.jdflib.jmf.JDFResponse;

/**
 * A JMF processor that handles the KnownMessages query.
 * 
 * @see <a
 *      href="http://www.cip4.org/documents/jdf_specifications/JDF1.2.pdf">JDF
 *      Specification Release 1.2, 5.5.1.4 KnownMessage </a>
 * @author Claes Buckwalter (clabu@itn.liu.se)
 * @version $Id: KnownMessagesJMFProcessor.java,v 1.6 2005/09/04 19:07:07 ola.stering Exp $
 */
public class KnownMessagesJMFProcessor extends AbstractJMFProcessor {

    private IncomingJMFDispatcher _dispatcher;    
    public final String MESSAGE_TYPE = "KnownMessages";

    /**
     * Default constructor.
     */
    public KnownMessagesJMFProcessor() {
        this(null);
    }

    public KnownMessagesJMFProcessor(IncomingJMFDispatcher dispatcher) {
        super();
        _dispatcher = dispatcher;        
        setMessageType(MESSAGE_TYPE);
        setQueryProcessor(true);
    }

    /**
     * Sets the incoming request dispatcher used for retrieving all known
     * message services.
     * 
     * @param dispatcher
     */
    public void setIncomingJMFDispatcher(IncomingJMFDispatcher dispatcher) {
        _dispatcher = dispatcher;
    }

    /**
     * Processes KnownMessages queries. Return code <code>0</code> for success
     * Return code <code>5</code>, query/command not implemented will be
     * returned for all other input message types.
     * 
     * Limitations of the KnownMessages query: KnownMsgQuParams/@Exact not read,
     * defaults to "false"
     * 
     * @see org.cip4.elk.jmf.JMFProcessor#getMessageServices()
     */
    public int processMessage(JDFMessage input, JDFResponse output) {
        return processKnownMessages((JDFQuery) input, output);
    }

    /**
     * Processes KnownMessages queries.
     * 
     * @todo Add error handling. Clone <code>output</code> so that we still
     *       have the original if something were to fail halfway through
     *       appending <em>MessageService</em> elements.
     * @see #processMessage(JDFMessage, JDFResponse)
     * @param input the query
     * @param output the response template
     * @return the JMF return code
     */
    private int processKnownMessages(JDFQuery input, JDFResponse output) {
        // Gets MessageService for all registered processors
        Set messageTypes = _dispatcher.getMessageTypes();
        
        int returnCode = 0;

        JDFKnownMsgQuParams parameters = input.getKnownMsgQuParams(0);
        boolean command = true; // default values
        boolean queries = true;
        boolean signals = true;
        boolean persistent = false;

        if (parameters != null) {
            command = parameters.getListCommands();
            queries = parameters.getListQueries();
            signals = parameters.getListSignals();
            persistent = parameters.getPersistent();
            // The Exact parameter ignored
            log.debug("KnownMessages parameters specified: commands=" + command
                    + " queries=" + queries + " signals=" + signals
                    + " persistent=" + persistent);
        }

        JMFProcessor processor;
        for (Iterator it = messageTypes.iterator(); it.hasNext();) {
            processor = _dispatcher.getProcessor((String) it.next());            
            
            try {
                JDFMessageService[] services = processor.getMessageServices();
                for (int i = 0; i < services.length; i++) {

                    if (!((services[i].isCommand() && !command)
                            || (services[i].isQuery() && !queries)
                            || (!services[i].isQuery() && persistent) || (services[i]
                            .getSignal() && !signals))) {

                        output.copyElement(services[i], null);
                    }
                }
            } catch (Exception e) {
                String msg = "Error in JMFProcessor " + processor + ".";
                log.error(msg, e);
                output.setErrorText(msg);
                returnCode = 2;
            }
        }
        output.setReturnCode(returnCode);
        return returnCode;
    }

}
