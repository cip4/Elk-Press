/*
 * Created on Jun 22, 2005
 */
package org.cip4.elk.impl.device.process;

import java.util.ArrayList;
import java.util.List;

import org.cip4.elk.device.process.ProcessAmountEvent;
import org.cip4.elk.device.process.ProcessAmountListener;

/**
 *  A class for notifying {@link ProcessAmountListenerNotifier}s of
 * {@link org.cip4.elk.device.process.ProcessAmountEvent}s.
 * 
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: ProcessAmountListenerNotifier.java,v 1.1 2005/07/25 18:13:21 ola.stering Exp $
 */
public class ProcessAmountListenerNotifier {
    
    protected List _listeners;
    
    /**
     * Creates a ProcessAmountListenerNotifier.
     */
    public ProcessAmountListenerNotifier() {
        _listeners = new ArrayList();
    }
    
    /**
     * Adds a listener.
     * 
     * @param listener the listener to add.
     */
    public void addListener(ProcessAmountListener listener) {
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
    public void removeListener(ProcessAmountListener listener) {
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
     * @param event the event to fire.
     * @throws IllegalArgumentException if the event is <code>null</code>.
     */
    public void fireEvent(ProcessAmountEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        if (_listeners.size() == 0) {
            return;
        }
        ProcessAmountListener listeners[] = new ProcessAmountListener[0];
        synchronized (_listeners) {
            listeners = (ProcessAmountListener[])_listeners.toArray(new ProcessAmountListener[_listeners.size()]); 
        }        
        for(int i=0; i<listeners.length; i++) {
            listeners[i].processAmountChanged(event);
        }
    }    
    

}
