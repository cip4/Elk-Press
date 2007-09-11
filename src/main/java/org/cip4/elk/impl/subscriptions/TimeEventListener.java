/*
 * Created on 2005-apr-29
 */
package org.cip4.elk.impl.subscriptions;

import java.util.EventListener;

/**
 * A listener that listens for TimeEvents.
 * 
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: TimeEventListener.java,v 1.1 2005/05/27 19:27:33 ola.stering Exp $
 */
public interface TimeEventListener extends EventListener {
    
    public void timeTriggered(TimeEvent event);   

}
