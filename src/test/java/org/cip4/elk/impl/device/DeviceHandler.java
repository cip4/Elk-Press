/*
 * Created on Sep 10, 2004
 */
package org.cip4.elk.impl.device;

import java.util.List;
import java.util.Vector;

import org.cip4.elk.ElkEvent;
import org.cip4.elk.device.process.ProcessStatusEvent;
import org.cip4.elk.device.process.ProcessStatusListener;
import org.cip4.jdflib.jmf.JDFDeviceInfo;

import EDU.oswego.cs.dl.util.concurrent.Latch;


/**
 * Helper class that logs events from the device.
 * 
 * @author Claes Buckwalter (clabu@itn.liu.se)
 */
public class DeviceHandler implements ProcessStatusListener {
    private Latch idleLatch;
    private List msgList;
    
    public DeviceHandler() {
        idleLatch = new Latch();
        msgList = new Vector(); 
    }
    
    public void processStatusChanged(ProcessStatusEvent event) {
        eventGenerated(event);
    }
    
    public void eventGenerated(ElkEvent event) {
        msgList.add(event);
        if (event instanceof ProcessStatusEvent) {           
            if(((ProcessStatusEvent)event).getProcessStatus().equals(JDFDeviceInfo.EnumDeviceStatus.Idle)) {
                idleLatch.release();
            }    
        }        
    }
    
    public int getReceviedMessageCount() {
        return msgList.size();
    }
    
    public List getReceivedMessages() {
        return msgList;
    }
    
    public void waitForDeviceToBecomeIdle(int waitTime) {
        try {
            idleLatch.attempt(waitTime);
        } catch(InterruptedException ie) {}
    }
}
