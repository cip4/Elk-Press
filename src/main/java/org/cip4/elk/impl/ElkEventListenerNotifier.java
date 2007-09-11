package org.cip4.elk.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.cip4.elk.ElkEvent;
import org.cip4.elk.ElkEventListener;

/**
 * Listeners are registered with this class that handles firing of events to all
 * listeners.
 * @author Claes Buckwalter (clabu@itn.liu.se)
 */
public class ElkEventListenerNotifier implements Serializable {
    protected List _listeners;
    
    public ElkEventListenerNotifier() {
        _listeners = new ArrayList();
    }
    
    public void addListener(ElkEventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        _listeners.add(listener);
    }
    
    public void removeListener(ElkEventListener listener) {
        _listeners.remove(listener);
    }
    
    public void removeAllListeners() {
        _listeners.clear();
    }
 
    /**
     * Fires an event to all registered listeners.
     * @param event the event to fire
     */
    public void fireEvent(ElkEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        if (_listeners.size() == 0) {
            return;
        }
        ElkEventListener listeners[] = new ElkEventListener[0];
        synchronized (_listeners) {
            listeners = (ElkEventListener[])_listeners.toArray(new ElkEventListener[_listeners.size()]); 
        }        
        for(int i=0; i<listeners.length; i++) {
            listeners[i].eventGenerated(event);
        }
    }
}
