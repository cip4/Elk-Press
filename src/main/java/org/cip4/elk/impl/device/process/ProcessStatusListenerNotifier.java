/*
 * Created on Sep 23, 2004
 */
package org.cip4.elk.impl.device.process;

import java.util.ArrayList;
import java.util.List;

import org.cip4.elk.device.process.ProcessStatusEvent;
import org.cip4.elk.device.process.ProcessStatusListener;

/**
 * @author Claes Buckwalter (clabu@itn.liu.se)
 */
public class ProcessStatusListenerNotifier {

    protected List _listeners;
    
    public ProcessStatusListenerNotifier() {
        _listeners = new ArrayList();
    }
    
    public void addListener(ProcessStatusListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        _listeners.add(listener);
    }
    
    public void removeListener(ProcessStatusListener listener) {
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
    public void fireEvent(ProcessStatusEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        if (_listeners.size() == 0) {
            return;
        }
        ProcessStatusListener listeners[] = new ProcessStatusListener[0];
        synchronized (_listeners) {
            listeners = (ProcessStatusListener[])_listeners.toArray(new ProcessStatusListener[_listeners.size()]); 
        }        
        for(int i=0; i<listeners.length; i++) {
            listeners[i].processStatusChanged(event);
        }
    }    
    
}
