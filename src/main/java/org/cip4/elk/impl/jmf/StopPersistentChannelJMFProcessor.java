package org.cip4.elk.impl.jmf;

import org.cip4.elk.jmf.SubscriptionManager;
import org.cip4.jdflib.jmf.JDFMessage;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.jmf.JDFStopPersChParams;

/**
 * A processor for <em>StopPersistentChannel</em> commands.
 * 
 * @see <a
 *      href="http://www.cip4.org/documents/jdf_specifications/JDF1.2.pdf">JDF
 *      Specification Release 1.2, 5.5.1.6 StopPersistentChannel </a>
 * @author Claes Buckwalter (clabu@itn.liu.se)
 * @version $Id: StopPersistentChannelJMFProcessor.java,v 1.4 2005/05/10 16:56:47 ola.stering Exp $
 */
public class StopPersistentChannelJMFProcessor extends AbstractJMFProcessor {

    private SubscriptionManager _subscriptionManager;
    public static final String MESSAGE_TYPE = "StopPersistentChannel";

    /**
     * Creates a new instance that uses the specified subscription manager for
     * unregistering subscriptions, persistent channels.
     * 
     * @param subManager the manager for unregistering subscriptions
     */
    public StopPersistentChannelJMFProcessor(SubscriptionManager subManager) {
        super();
        setMessageType(MESSAGE_TYPE);
        setCommandProcessor(true);
        _subscriptionManager = subManager;
    }

    /**
     * Processes <em>StopPersistentChannel</em> commands.
     * 
     * @see org.cip4.elk.impl.jmf.AbstractJMFProcessor#processMessage(JDFMessage,
     *      JDFResponse)
     */
    public int processMessage(JDFMessage input, JDFResponse output) {
        int returnCode = 0;
        String msg;

        JDFStopPersChParams stopParams = input.getStopPersChParams(0);

        if (stopParams != null) {
            returnCode = _subscriptionManager
                    .unregisterSubscription(stopParams);

            if (returnCode == 0) {
                msg = "Unsubscription with " + stopParams + " successful.";
            } else {
                msg = "No unsubscription has been made with " + stopParams;
            }
            log.debug(msg);
        } else {
            msg = "StopPersistentChannel/@StopPersChParams was"
                    + " null but it is required";
            log.error(msg);
            returnCode = 7; // Insufficient Parameters
        }

        output.appendComment().appendText(msg);
        output.setReturnCode(returnCode);
        return returnCode;
    }

}
