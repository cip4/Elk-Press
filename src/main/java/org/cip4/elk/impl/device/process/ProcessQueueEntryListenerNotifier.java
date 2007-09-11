/*
 * Created on 2005-jun-06 
 */
package org.cip4.elk.impl.device.process;

import java.util.ArrayList;
import java.util.List;

import org.cip4.elk.device.process.ProcessQueueEntryEvent;
import org.cip4.elk.device.process.ProcessQueueEntryEventListener;

/**
 * A class for notifying {@link org.cip4.elk.device.process.ProcessQueueEntryEventListener}s of
 * {@link org.cip4.elk.device.process.ProcessQueueEntryEvent}s.
 * 
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: ProcessQueueEntryListenerNotifier.java,v 1.2 2005/08/31 20:21:40 ola.stering Exp $
 */
public class ProcessQueueEntryListenerNotifier {

    protected List _listeners;

    /**
     * Creates a ProcessQueueEntryListenerNotifier.
     */
    public ProcessQueueEntryListenerNotifier() {
        _listeners = new ArrayList();
    }

    /**
     * Adds a listener.
     * 
     * @param listener the listener to add.
     */
    public void addListener(ProcessQueueEntryEventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        _listeners.add(listener);
    }

    /**
     * Removes a listener.
     * 
     * @param listener the listener to be removed.
     */
    public void removeListener(ProcessQueueEntryEventListener listener) {
        _listeners.remove(listener);
    }

    /**
     * Removes all listeners.
     */
    public void removeAllListeners() {
        _listeners.clear();
    }

    /**
     * Fires an event to all registered listeners.
     * 
     * @param event the event to fire.
     * @throws IllegalArgumentException if the event is <code>null</code>.
     */
    public void fireEvent(ProcessQueueEntryEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        if (_listeners.size() == 0) {
            return;
        }
        ProcessQueueEntryEventListener listeners[] = new ProcessQueueEntryEventListener[0];
        synchronized (_listeners) {
            listeners = (ProcessQueueEntryEventListener[]) _listeners
                    .toArray(new ProcessQueueEntryEventListener[_listeners.size()]);
        }
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].queueEntryStatusChanged(event);
        }
    }
}
