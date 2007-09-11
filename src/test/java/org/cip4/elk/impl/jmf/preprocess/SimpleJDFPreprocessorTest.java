/*
 * Created on 2005-apr-21 
 */
package org.cip4.elk.impl.jmf.preprocess;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.cip4.elk.ElkTestCase;
import org.cip4.elk.JDFElementFactory;
import org.cip4.elk.device.DeviceConfig;
import org.cip4.elk.impl.device.SimpleDeviceConfig;
import org.cip4.elk.impl.device.process.ApprovalProcess;
import org.cip4.elk.impl.device.process.BaseProcess;
import org.cip4.elk.impl.jmf.KnownDevicesJMFProcessor;
import org.cip4.elk.impl.jmf.SubscribingIncomingJMFDispatcher;
import org.cip4.elk.impl.jmf.SyncHttpOutgoingJMFDispatcher;
import org.cip4.elk.impl.queue.MemoryQueue;
import org.cip4.elk.impl.util.FileRepository;
import org.cip4.elk.impl.util.Repository;
import org.cip4.elk.impl.util.URLAccessTool;
import org.cip4.elk.jmf.IncomingJMFDispatcher;
import org.cip4.elk.jmf.JMFProcessor;
import org.cip4.elk.jmf.OutgoingJMFDispatcher;
import org.cip4.elk.queue.Queue;
import org.cip4.jdflib.core.ElementName;
import org.cip4.jdflib.jmf.JDFCommand;
import org.cip4.jdflib.jmf.JDFJMF;
import org.cip4.jdflib.jmf.JDFQueueSubmissionParams;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.util.UrlUtil;

/** 
 * 
 * @author Ola Stering (olst6875@student.uu.se) *
 */
public class SimpleJDFPreprocessorTest extends ElkTestCase {

    static final String JDF_FILE = "Elk_Approval.jdf";

    SimpleJDFPreprocessor _jdfpre;

    URLAccessTool _accessTool;

    DeviceConfig _deviceConfig;

    JDFElementFactory _factory;

    Queue mq;

    BaseProcess p;

    /*
     * @see TestCase#setUp()
     */
    public void setUp() throws Exception {
        super.setUp();
        _accessTool = new URLAccessTool();
        // Load configuration
        File deviceFile = new File("src/test/" + _testDataPath + "ApprovalDevice.xml");
        assertTrue("Tests that file exists: " + deviceFile, deviceFile.exists());
        _deviceConfig = new SimpleDeviceConfig(UrlUtil.fileToUrl(deviceFile, true), _accessTool);
        assertNotNull(_deviceConfig.getDeviceConfig());        
        
        // File repository
        Map contentTypes = new HashMap();
        contentTypes.put("text/xml", "xml");
        Repository r = new FileRepository(contentTypes);
        // JMF dispatchers
        OutgoingJMFDispatcher o = new SyncHttpOutgoingJMFDispatcher(_deviceConfig);
        mq = new MemoryQueue(_deviceConfig, 50, _accessTool);
        IncomingJMFDispatcher i = new SubscribingIncomingJMFDispatcher();
        i.setConfig(_deviceConfig);
        // Process
        p = new ApprovalProcess(_deviceConfig, mq, _accessTool, o, r);
        // JMF processors
        JMFProcessor knownDevices = new KnownDevicesJMFProcessor(p);        
        i.registerProcessor("KnownDevices", knownDevices);
        _factory = JDFElementFactory.getInstance();
        // Preprocessor
        //_jdfpre = new SimpleJDFPreprocessor(mq, _accessTool, o, i, _deviceConfig, p, r);
        _jdfpre = new SimpleJDFPreprocessor(mq, o, i, _deviceConfig, r);
        _jdfpre.setValidation(true);
        
        assertNotNull(p.getDeviceInfo(true));
    }

    public void testSimpleJDFPreprocessor() {

    }

    public void testAppendNotification() {
    }

    public void testEnqueueJDF() {
        /*
         * // TEST ENQUEUE JDFQueueSubmissionParams sp =
         * (JDFQueueSubmissionParams)
         * _factory.createJDFElement(ElementName.QUEUESUBMISSIONPARAMS);
         * sp.setURL(JDF_FILE); JDFResponse r =
         * Messages.createResponse("TestID","TestType"); JDFQueueEntry qe =
         * null; // _jdfpre.enqueueJDF(null,qe,r); may throw
         * NullPointerException, documented // _jdfpre.enqueueJDF(sp,qe,null);
         * may throw NullPointerException, documented
         * 
         * qe = (JDFQueueEntry)
         * _factory.createJDFElement(ElementName.QUEUEENTRY); // Method is now
         * private. //int returnCode = _jdfpre.enqueueJDF(sp,r);
         * 
         * //System.out.println("ReturnCode: " + returnCode);
         * //System.out.println(r); // Response modified. QueueEntry added //
         * r.getQueueEntry(0); XXX this does not work why? // THESE TEST DO NOT
         * WORK ANYMORE SINCE ENQUEUE IS PRIVATE
         * 
         * JDFElement e = r.getChildElement(1); // So index starts at 1
         * deprecated JDFQueueEntry q2 = (JDFQueueEntry) r.getChildElement(1);
         * assertNotNull(q2); JDFQueueEntry q3 = (JDFQueueEntry)
         * r.getElement(ElementName.QUEUEENTRY,JDFConstants.NONAMESPACE,0);
         * assertNotNull(q3); // The response should now contain a queue entry //
         * Why does this not work now?
         * 
         * //r.getElement_JDFElement(); //r.getElement() //r.getQueueEntry(1);
         * //System.out.println("NUMBER OF ELEMENTS: " +
         * r.getChildElements().length); //System.out.println("Q2: "+ q2);
         * //System.out.println("Q3: " + q3);
         * 
         * //assertTrue(qe.getStatus() == EnumNodeStatus.Setup);
         * //System.out.println(qe);
         *  
         */
    }

    public void testDownloadJdf() {

    }
    
    public void testJDFTemplate() {
        JDFJMF knownDevicesQuery = (JDFJMF) JDFElementFactory.getInstance().createJDFElement("SimpleJDFPreprocessor_KnownDevices");
        System.out.println(knownDevicesQuery);
    }
    

    public void testValidateJDFNode() {
    }

    public void testCheckResources() {
    }

    public void testProcessHandles() {
    }

    public JDFCommand createCommand(String type, JDFQueueSubmissionParams sp) {
        JDFCommand c = (JDFCommand) _factory
                .createJDFElement(ElementName.COMMAND);
        c.setID("M001");
        c.setType(type);

        if (sp != null) {
            c.copyElement(sp, null);
        }
        return c;
    }

    /**
     * NOTE: This test case was used to test the incoming parameters, now it is
     * declared private.
     */
    public void testCheckIncomingParameters() {
        /**
         * JDFResponse response = (JDFResponse)
         * _factory.createJDFElement(ElementName.RESPONSE);
         * 
         * assertTrue(_jdfpre.checkIncomingParameters(createCommand("KnownDevices",
         * null),response) == 7); // Must be SubmitQueueEntry
         * assertTrue(_jdfpre.checkIncomingParameters(createCommand("SubmitQueueEntry",
         * null),response) == 7); // Submission params must exist
         * 
         * JDFQueueSubmissionParams sp = (JDFQueueSubmissionParams)
         * _factory.createJDFElement(ElementName.QUEUESUBMISSIONPARAMS);
         * assertTrue(_jdfpre.checkIncomingParameters(createCommand("SubmitQueueEntry",
         * sp),response) == 7); // must have an URL
         * 
         * sp.setURL("file:C:\\program1\\jakarta-tomcat-5.5.9\\webapps\\samples\\Approval.jdf");
         * assertTrue(_jdfpre.checkIncomingParameters(createCommand("SubmitQueueEntry",
         * sp),response) == 0); // Should be OKAY
         * 
         * JDFResponse r = Messages.createResponse("MU","KorvTyp");
         * _jdfpre.checkIncomingParameters(createCommand("SubmitQueueEntry",
         * null),r);// This should return a notification error msg
         * assertTrue(r.getNotification(0) != null); // Test that a notification
         * was appendeded. //System.out.println(r.getNotification(0)); // For
         * visual manual //System.out.println("Aggregated response ");
         * //System.out.println(response); // NOTE: aggregated response
         * notifications...
         *  
         */
    }

    static volatile int correctCommands = 0;

    static volatile int correctReturnCodes = 0;

    static int msgs;

    
    public void testPreProcessJDF_incorrectSubmission() {

    }
    
    /**
     * TODO Refactor into two separate tests, one test for correct commands and one 
     * test for incorrect commands.
     * @throws NullPointerException
     * @throws URISyntaxException
     * @throws MalformedURLException
     */
    public void testPreProcessJDF_correctSubmission() 
        throws NullPointerException, URISyntaxException {
        JDFResponse response = (JDFResponse) _factory
                .createJDFElement(ElementName.RESPONSE);
        
        // Incorrect command
        log.debug("Submitting an incorrect command, and checks that a"
            + " Notification is attached to the response.");
        JDFCommand incorrectCommand = createCommand("SubmitQueueEntry", null);
        JDFResponse rr = _jdfpre.preProcessJDF(incorrectCommand);
        assertNotNull(rr.getNotification());
        log.debug("The Response looks like this: " + rr);

        // Correct command
        log.debug("Testing of a correct 'SubmitQueueEntry' command.");
        JDFQueueSubmissionParams sp = (JDFQueueSubmissionParams) _factory
                .createJDFElement(ElementName.QUEUESUBMISSIONPARAMS);
        String url = getResourceAsURL(_jdfFilesPath + JDF_FILE).toExternalForm();
        File jdfFile = new File(new URI(url));
        assertTrue(jdfFile.exists());
        log.info("The url where to get the JDF file: " + url);
        sp.setURL(url);        
        JDFCommand correctCommand = createCommand("SubmitQueueEntry", sp);
        // Test that the incoming parameters are correct
        assertTrue(_jdfpre.checkIncomingParameters(correctCommand, response) == 0); 
        
//        String ackURL = correctCommand.getAcknowledgeURL();
//        assertNotNull(ackURL); // The above method never returns null
        JDFResponse correctResponse = _jdfpre.preProcessJDF(correctCommand);
        log.info("Response: " + correctResponse);
        assertNotNull(correctResponse.getQueueEntry(0));
        assertNotNull(correctResponse.getQueue(0));
        
        Random r = new Random();
        int rInt;

        final JDFCommand c2 = correctCommand; // FILE
        final JDFCommand nc3 = incorrectCommand; // INCORRECT

        final int numberOfMessagesForEachTimer = 5;
        final int numberOfTimers = 5;
        final long delay = 1;
        Vector v = new Vector();
        log.debug("Scheduling " + numberOfTimers + " timers to preprocess "
                + numberOfMessagesForEachTimer + " commands each with an "
                + "interval of " + delay + " ms");

        for (int i = 0; i < numberOfTimers; i++) {
            rInt = r.nextInt(2);
            final JDFCommand c = (rInt == 1 ? c2 : nc3);
            String cm = " incorrect";
            if (rInt == 1) {
                correctCommands++;
                cm = " correct";
            }
            log.debug("Timer " + (i + 1) + " will send "
                    + numberOfMessagesForEachTimer + cm + " commands");

            Timer timer = new Timer();
            TimerTask t = new TimerTask() {
                int times;

                public void run() {
                    times++;
                    msgs++;
                    JDFResponse r = _jdfpre.preProcessJDF(c);
                    log.debug("Repsonse: " + r.toXML());
                    if (r.getReturnCode() == 0) {
                        correctReturnCodes++;
                    }
                    if (times == numberOfMessagesForEachTimer)
                        this.cancel();
                }
            };

            v.add(t);
            // Put some pressure on the method, with only one ms
            // waiting.
            timer.scheduleAtFixedRate(t, 0, delay);

        }

        try {
            long letTimersFinish = 45000;
            // Let the timers finish
            Thread.sleep(letTimersFinish);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        TimerTask t2 = (TimerTask) v.get(0);
        if (t2.cancel()) {
            log
                    .debug("The timers did not finish, most likely you need to increase the letTimersFinish value.");
        }

        // should be:
        System.out.println("MSGS SENT: " + msgs);
        System.out.println("CORRECT COMMANDS: " + correctCommands
                * numberOfMessagesForEachTimer);
        System.out.println("CORRECT RETURNCODES: " + correctReturnCodes);

        // Same number of sent CORRECT commands as correct returnCodes.
        // Remember that this also depends on the queue size.
        assertEquals(correctCommands * numberOfMessagesForEachTimer,
            correctReturnCodes);
        // NOTE: If this fails it can also be the reason that the "The timers
        // did not finish, most likely you need to increase the letTimersFinish
        // value (or decrease the processTime in SimpleJDFPreProcessor.
        // (5 seconds and it pass)

    }

}
