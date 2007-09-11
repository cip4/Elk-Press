/*
 * Created on 2005-apr-29
 */
package org.cip4.elk.impl.subscriptions;

import java.util.ArrayList;
import java.util.List;

/**
 * A notifier that fires TimeEvents to all its listeners.
 * 
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: TimeEventListenerNotifier.java,v 1.1 2005/05/27 19:27:33 ola.stering Exp $
 */
public class TimeEventListenerNotifier {
    protected List _listeners;

    /**
     * Creates a TimeEventListenerNotifier.
     * 
     */
    public TimeEventListenerNotifier() {
        _listeners = new ArrayList();
    }

    /**
     * Adds a listener.
     * 
     * @param listener the listener to add.
     * @throws IllegalArgumentException if listener is <code>null</code>.
     */
    public void addListener(TimeEventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        _listeners.add(listener);
    }

    /**
     * Removes a listener.
     * 
     * @param listener the listener that is being removed.
     */
    public void removeListener(TimeEvent listener) {
        _listeners.remove(listener);
    }

    /**
     * Removes all listenters.
     */
    public void removeAllListeners() {
        _listeners.clear();
    }

    /**
     * Fires an event to all registered listeners.
     * 
     * @param event the event to fire
     * @throws IllegalArgumentException if the event is null
     */
    public void fireEvent(TimeEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        if (_listeners.size() == 0) {
            return;
        }
        TimeEventListener listeners[] = new TimeEventListener[0];
        synchronized (_listeners) {
            listeners = (TimeEventListener[]) _listeners
                    .toArray(new TimeEventListener[_listeners.size()]);
        }
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].timeTriggered(event);
        }
    }
}
