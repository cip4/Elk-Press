/*
 * Created on Sep 30, 2004
 */
package org.cip4.elk.impl.subscriptions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cip4.elk.ElkEvent;
import org.cip4.elk.ElkTestCase;
import org.cip4.elk.JDFElementFactory;
import org.cip4.elk.device.DeviceConfig;
import org.cip4.elk.device.process.ProcessStatusEvent;
import org.cip4.elk.impl.device.SimpleDeviceConfig;
import org.cip4.elk.impl.device.process.ApprovalProcess;
import org.cip4.elk.impl.jmf.AbstractIncomingJMFDispatcher;
import org.cip4.elk.jmf.JMFProcessor;
import org.cip4.elk.jmf.OutgoingJMFDispatcher;
import org.cip4.jdflib.core.ElementName;
import org.cip4.jdflib.jmf.JDFDeviceInfo;
import org.cip4.jdflib.jmf.JDFJMF;
import org.cip4.jdflib.jmf.JDFMessage;
import org.cip4.jdflib.jmf.JDFMessageService;
import org.cip4.jdflib.jmf.JDFQuery;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.jmf.JDFStopPersChParams;
import org.cip4.jdflib.node.JDFNode;
import org.cip4.jdflib.resource.JDFDevice;

/**
 * @author Claes Buckwalter (clabu@itn.liu.se)
 */
public class AsyncSimpleSubscriptionManagerTest extends ElkTestCase {

    DummyOutgoingJMFDispatcher _outDisp;
    DummyIncomingJMFDispatcher _inDisp;
    private DeviceConfig _device;

    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Convenience method for loading JMF Query.
     */
    private JDFQuery getQuery(String resPath) throws Exception {
        JDFJMF jmf = (JDFJMF) getResourceAsJDF(resPath);
        return jmf.getQuery();
    }

    /**
     * Convenience method for create subscription manager.
     */
    private AsyncSimpleSubscriptionManager getSubscriptionManager() {
        _outDisp = new DummyOutgoingJMFDispatcher();
        _inDisp = new DummyIncomingJMFDispatcher();
        _device = new SimpleDeviceConfig();
        JDFDevice device = (JDFDevice) JDFElementFactory.getInstance()
        .createJDFElement(ElementName.DEVICE);
        device.setDeviceID("Test Device");
        _device.setDeviceConfig(device);
        AsyncSimpleSubscriptionManager sm = new AsyncSimpleSubscriptionManager(
                _outDisp, _device);
        sm.setIncomingDispatcher(_inDisp);
        //      Setting the async mode to false to avoid having to start Mule
        sm.setAsynchronous(false);
        Map eventQueryMapping = new HashMap();
        eventQueryMapping.put("org.cip4.elk.device.process.ProcessStatusEvent",
            "Status");
        eventQueryMapping.put("org.cip4.elk.device.dummy.DummyEvent", "Dummy");
        eventQueryMapping
                .put("org.cip4.elk.device.dummy.DummyEvent2", "Dummy2");
        sm.setEventQueryMapping(eventQueryMapping);
        return sm;

    }

    /**
     * Tests that the notifications match the defined supported subscriptions
     * 
     * @throws Exception
     */
    public void testGetNotifications() throws Exception {
        AsyncSimpleSubscriptionManager sm = getSubscriptionManager();
        Map eventQueryMapping = new HashMap();
        eventQueryMapping.put("org.cip4.elk.device.process.ProcessStatusEvent",
            "Status");
        eventQueryMapping.put("org.cip4.elk.device.dummy.DummyEvent", "Dummy");
        eventQueryMapping
                .put("org.cip4.elk.device.dummy.DummyEvent2", "Dummy2");
        List defs = Arrays.asList(sm.getNotificationDefs(null));
        log.debug(defs);
        assertTrue(defs.size() == eventQueryMapping.size() + 1); // The
                                                                 // registered
                                                                 // mapping,
                                                                 // plus Events
    }

    public void testSetEventQueryMapping_null() {
        try {
            AsyncSimpleSubscriptionManager sm = getSubscriptionManager();
            sm.setEventQueryMapping(null);
            assertFalse(false);
        } catch (IllegalArgumentException iae) {
            assertTrue(true);
        }
    }

    /**
     * Tests that an Events subscription gets events that match its classes.
     * 
     * @throws Exception
     */
    public void testBroadcastEvent_NoEventQueryMapping() throws Exception {
        AsyncSimpleSubscriptionManager sm = getSubscriptionManager();
        sm.setEventQueryMapping(new HashMap());
        String[] knownQueries = { "Events" };
        sm.setKnownQueryTypes(knownQueries);
        JDFQuery query = getQuery(_testDataPath + "Events.jmf");
        assertTrue(sm.registerSubscription(query));
        // Set expected URL from the query URL
        _outDisp.expectedUrl = query.getSubscription().getURL();
        sm.eventGenerated(new ElkEvent(ElkEvent.EVENT, this, "Broadcast me"));
    }

    /**
     * Tests that a Status query subscription does not get broadcast when there
     * is no ProcessStatusEvent-to-Status mapping.
     * 
     * @throws Exception
     */
    public void testBroadcastStatusEvent_NoEventQueryMapping() throws Exception {
        AsyncSimpleSubscriptionManager sm = getSubscriptionManager();
        sm.setEventQueryMapping(new HashMap());
        log.debug("Testing Status subscription...");
        JDFQuery query = getQuery(_testDataPath + "Status_Subscription.jmf");
        assertFalse(sm.registerSubscription(query));

        DummyStatusJMFProcessor processor = new DummyStatusJMFProcessor();
        _inDisp.registerProcessor("Status", processor);
        // Set expected URL from the query URL
        _outDisp.expectedUrl = "foo";
        ElkEvent event = new ProcessStatusEvent(ElkEvent.EVENT,
                JDFDeviceInfo.EnumDeviceStatus.Running, new ApprovalProcess(
                        null, null, null, null,null), "Broadcast me");
        sm.eventGenerated(event);
        assertFalse(processor.visitedProcessJMF);
    }

    /**
     * Tests that an Events subscription gets events that match its classes.
     * 
     * @throws Exception
     */
    public void testBroadcastEvent() throws Exception {
        AsyncSimpleSubscriptionManager sm = getSubscriptionManager();
        String[] knownQueries = { "Events" };
        sm.setKnownQueryTypes(knownQueries);
        JDFQuery query = getQuery(_testDataPath + "Events.jmf");
        assertTrue(sm.registerSubscription(query));
        // Set expected URL from the query URL
        _outDisp.expectedUrl = query.getSubscription().getURL();
        sm.eventGenerated(new ElkEvent(ElkEvent.EVENT, this, "Broadcast me"));
    }

    /**
     * Tests that an Events subscription does not get events that do not match
     * its classes.
     * 
     * @throws Exception
     */
    public void testDoNotBroadcastEvent() throws Exception {
        AsyncSimpleSubscriptionManager sm = getSubscriptionManager();
        String[] knownQueries = { "Events" };
        sm.setKnownQueryTypes(knownQueries);
        JDFQuery query = getQuery(_testDataPath + "Events.jmf");
        assertTrue(sm.registerSubscription(query));
        // Set expected URL from the query URL
        _outDisp.expectedUrl = "foo";
        sm.eventGenerated(new ElkEvent(ElkEvent.INFORMATION, this,
                "Do not broadcast me"));
    }

    /**
     * Tests that a subscription can be unregistered using StopPersChParams.
     * 
     * @throws Exception
     */
    public void testUnregisterSubscription() throws Exception {
        AsyncSimpleSubscriptionManager sm = getSubscriptionManager();
        String[] knownQueries = { "Events" };
        sm.setKnownQueryTypes(knownQueries);
        JDFQuery query = getQuery(_testDataPath + "Events.jmf");
        // Register subscription
        assertTrue(sm.registerSubscription(query));
        // Unregister subscription
        JDFStopPersChParams stopParams = (JDFStopPersChParams) createJDFElement("StopPersChParams");
        stopParams.setURL(query.getSubscription().getURL());
        sm.unregisterSubscription(stopParams);
        // Set expected URL from the query URL
        _outDisp.expectedUrl = query.getSubscription().getURL();
        sm.eventGenerated(new ElkEvent(ElkEvent.EVENT, this,
                "No subscription should broadcast me"));
    }

    /**
     * Tests that a Status query subscription gets signals.
     * 
     * @throws Exception
     */
    public void testBroadcastStatusEvent() throws Exception {
        AsyncSimpleSubscriptionManager sm = getSubscriptionManager();
        log.debug("Testing Status subscription...");
        JDFQuery query = getQuery(_testDataPath + "Status_Subscription.jmf");
        assertTrue(sm.registerSubscription(query));

        DummyStatusJMFProcessor processor = new DummyStatusJMFProcessor();
        _inDisp.registerProcessor("Status", processor);
        // Set expected URL from the query URL
        _outDisp.expectedUrl = query.getSubscription().getURL();
        ElkEvent event = new ProcessStatusEvent(ElkEvent.EVENT,
                JDFDeviceInfo.EnumDeviceStatus.Running, new ApprovalProcess(
                        null, null, null, null,null), "Broadcast me");
        sm.eventGenerated(event);
        assertTrue(processor.visitedProcessJMF);
        log.debug("Finished testing AsyncSimpleSubcriptionManager");
    }

    public class DummyStatusJMFProcessor implements JMFProcessor {
        public boolean visitedProcessJMF = false;

        public int processJMF(JDFMessage input, JDFResponse response) {
            log.debug("Input query: " + input);
            log.debug("Input response: " + response);
            int returnCode = 0;
            //JDFQuery query = (JDFQuery) input;
            // JDFStatusQuParams sqp = query.getStatusQuParams(0);
            // TODO Parse the status queue parameters
            JDFDeviceInfo info = response.appendDeviceInfo();
            info.setDeviceStatus(JDFDeviceInfo.EnumDeviceStatus.Running);
            //info.copyElement(_config.getDeviceConfig(), null);
            //info.copyElement(_device.getJobPhase(), null);
            log.debug("Output query: " + input);
            log.debug("Output response: " + response);
            visitedProcessJMF = true;
            return returnCode;
        }

        public JDFMessageService[] getMessageServices() {
            JDFMessageService s1 = (JDFMessageService) JDFElementFactory
                    .getInstance().createJDFElement(ElementName.MESSAGESERVICE);
            s1.setType("Status");
            s1.setQuery(true);
            JDFMessageService[] services = { s1 };
            return services;
        }
    }

    private static class DummyIncomingJMFDispatcher extends
            AbstractIncomingJMFDispatcher {
        public JDFJMF dispatchJMF(JDFJMF jmfIn) {
            return null;
        }
    }

    public class DummyOutgoingJMFDispatcher implements OutgoingJMFDispatcher {
        public String expectedUrl = null;

        public JDFResponse dispatchJMF(JDFJMF jmf, String url) {
            log.debug("Received JMF to dispatch:\n" + url + "\n" + jmf);
            assertTrue(false);
            return null;
        }

        public void dispatchJDF(JDFNode jdf, String url) {
            log.debug("Received JDF to dispatch:\n" + url + "\n" + jdf);
            assertTrue(false);
        }

        public void dispatchSignal(JDFJMF jmf, String url) {
            log.debug("Received JMF Signal to dispatch:\n" + url + "\n" + jmf);
            assertEquals(expectedUrl, url);
        }
    }
}
