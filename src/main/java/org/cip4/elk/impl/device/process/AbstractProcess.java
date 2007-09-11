/*
 * Created on Aug 31, 2004
 */
package org.cip4.elk.impl.device.process;

import org.cip4.elk.device.process.Process;
import org.cip4.elk.device.process.ProcessAmountEvent;
import org.cip4.elk.device.process.ProcessAmountListener;
import org.cip4.elk.device.process.ProcessStatusEvent;
import org.cip4.elk.device.process.ProcessStatusListener;
import org.cip4.elk.device.process.ProcessQueueEntryEvent;
import org.cip4.elk.device.process.ProcessQueueEntryEventListener;

/**
 * This abstract class implements registering and unregistering of event
 * listeners for the Process. It also has methods to fire events to the
 * registered listeners.
 * 
 * Currently there are three different kinds of events that the Process
 * may fire:
 * <ul>
 * <li>{@link org.cip4.elk.device.process.ProcessAmountEvent}s which should be
 * fired when the Process produces an Amount-change.</li>
 * <li>{@link org.cip4.elk.device.process.ProcessStatusEvent}s which should be
 * fired when the Process' Status changes.</li>
 * <li>{@link org.cip4.elk.device.process.ProcessQueueEntryEvent}s which
 * should be fired when the Process modifies a QueueEntry (for example changes
 * its Status).</li>
 * <ul>
 * 
 * @author Claes Buckwalter (clabu@itn.liu.se)
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: AbstractProcess.java,v 1.7 2006/11/06 18:32:08 buckwalter Exp $
 */
public abstract class AbstractProcess implements Process {

    protected ProcessStatusListenerNotifier _statusNotifier;
    protected ProcessQueueEntryListenerNotifier _queueEntryStatusNotifier;
    protected ProcessAmountListenerNotifier _amountNotifier;

    public AbstractProcess() {
        //nop
    }

    // ProcessAmount events handling ///////////////////////////////
    /**
     * Adds a listener that listens for ProcessAmount events from this Process.
     * 
     * @param listener the listener to be added.
     */
    public void addProcessAmountListener(ProcessAmountListener listener) {
        if (_amountNotifier == null) {
            _amountNotifier = new ProcessAmountListenerNotifier();
        }
        _amountNotifier.addListener(listener);
    }

    /**
     * Removes an AmountListener.
     * 
     * @param listener the object that has listened to this Process.
     */
    public void removeProcessAmountListener(ProcessAmountListener listener) {
        if (_amountNotifier != null) {
            _amountNotifier.removeListener(listener);
        }
    }

    /**
     * Sends the ProcessAmountEvent to all listeners.
     * 
     * @param event the event being fired.
     */
    protected void fireEvent(ProcessAmountEvent event) {
        if (_amountNotifier != null) {
            _amountNotifier.fireEvent(event);
        }
    }

    // ProcessQueueEntry events handling ////////////////////////////
    /**
     * Adds a listener that listens for QueueEntry events from this Process.
     * 
     * @see org.cip4.elk.impl.device.process.ProcessQueueEntryListenerNotifier#addListener(ProcessQueueEntryEventListener)
     */
    public void addQueueEntryEventListener(
            ProcessQueueEntryEventListener listener) {
        if (_queueEntryStatusNotifier == null) {
            _queueEntryStatusNotifier = new ProcessQueueEntryListenerNotifier();
        }
        _queueEntryStatusNotifier.addListener(listener);
    }

    /**
     * Removes a listener that listened for QueueEntry events from this Process.
     * 
     * @see ProcessQueueEntryListenerNotifier#removeListener(ProcessQueueEntryEventListener)
     */
    public void removeQueueEntryEventListener(
            ProcessQueueEntryEventListener listener) {
        if (_queueEntryStatusNotifier != null) {
            _queueEntryStatusNotifier.removeListener(listener);
        }
    }

    /**
     * Sends the QueueEntry event to all listeners.
     * 
     * @param event the event to fire
     */
    protected void fireEvent(ProcessQueueEntryEvent event) {
        if (_queueEntryStatusNotifier != null) {
            _queueEntryStatusNotifier.fireEvent(event);
        }
    }

    // ProcessStatus events handling ////////////////////////////////
    /**
     * Adds a listener that listens for events from this Process.
     * 
     * @see org.cip4.elk.device.process.Process#addProcessStatusListener(ProcessStatusListener)
     */
    public void addProcessStatusListener(ProcessStatusListener listener) {
        if (_statusNotifier == null) {
            _statusNotifier = new ProcessStatusListenerNotifier();
        }
        _statusNotifier.addListener(listener);
    }

    /**
     * Removes a listener that listened for events from this Process.
     * 
     * @see org.cip4.elk.device.process.Process#removeProcessStatusListener(ProcessStatusListener)
     */
    public void removeProcessStatusListener(ProcessStatusListener listener) {
        if (_statusNotifier != null) {
            _statusNotifier.removeListener(listener);
        }
    }

    /**
     * Sends the ProcessStatusEvent to all listeners.
     * 
     * @param event the event to fire
     */
    protected void fireEvent(ProcessStatusEvent event) {
        if (_statusNotifier != null) {
            _statusNotifier.fireEvent(event);
        }
    }
}
