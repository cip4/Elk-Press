/*
 * Created on Sep 23, 2004
 */
package org.cip4.elk.impl.queue;

import java.util.ArrayList;
import java.util.List;

import org.cip4.elk.queue.QueueStatusEvent;
import org.cip4.elk.queue.QueueStatusListener;

/**
 * @author Claes Buckwalter (clabu@itn.liu.se)
 */
public class QueueStatusListenerNotifier {

    protected List _listeners;
    
    public QueueStatusListenerNotifier() {
        _listeners = new ArrayList();
    }
    
    public void addListener(QueueStatusListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        _listeners.add(listener);
    }
    
    public void removeListener(QueueStatusListener listener) {
        _listeners.remove(listener);
    }
    
    public void removeAllListeners() {
        _listeners.clear();
    }
    
    /**
     * Fires an event to all registered listeners.
     * @param event the event to fire
     * @throws IllegalArgumentException if the event is null
     */
    public void fireEvent(QueueStatusEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        if (_listeners.size() == 0) {
            return;
        }
        QueueStatusListener listeners[] = new QueueStatusListener[0];
        synchronized (_listeners) {
            listeners = (QueueStatusListener[])_listeners.toArray(new QueueStatusListener[_listeners.size()]); 
        }        
        for(int i=0; i<listeners.length; i++) {
            listeners[i].queueStatusChanged(event);
        }
    }    
}
