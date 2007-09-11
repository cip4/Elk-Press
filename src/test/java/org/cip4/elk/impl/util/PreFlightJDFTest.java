/**
 * 2005-sep-09
 */
package org.cip4.elk.impl.util;

import java.util.List;

import org.cip4.elk.ElkTestCase;
import org.cip4.elk.device.DeviceConfig;
import org.cip4.elk.device.process.ProcessAmountListener;
import org.cip4.elk.device.process.ProcessQueueEntryEventListener;
import org.cip4.elk.device.process.ProcessStatusListener;
import org.cip4.elk.impl.device.process.ConventionalPrintingProcess;
import org.cip4.elk.impl.queue.MemoryQueue;
import org.cip4.elk.jmf.SubscriptionManager;
import org.cip4.elk.queue.Queue;
import org.cip4.elk.queue.QueueStatusListener;
import org.cip4.jdflib.core.JDFElement;
import org.cip4.jdflib.node.JDFNode;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
/**
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: PreFlightJDFTest.java,v 1.3 2006/01/19 10:03:23 buckwalter Exp $
 */
public class PreFlightJDFTest extends ElkTestCase {
   
    private ConventionalPrintingProcess _process;
    private static URLAccessTool _fileUtil;
    private static BeanFactory _factory;    
    private SubscriptionManager _subManager;
    private PreFlightJDF _preFlightJDF;
    
    public void setUp() throws Exception {
        super.setUp();
        _preFlightJDF = new PreFlightJDF();
        _fileUtil = new URLAccessTool(getResourceAsURL(".").toString());    
        _factory = (BeanFactory) new ClassPathXmlApplicationContext(
                _testDataPath + "elk-spring-config.xml");

        // Configure queue listeners
        log.debug("Configuring event listeners...");
        Queue queue = (Queue) _factory.getBean("queue");
        _subManager = (SubscriptionManager) _factory
                .getBean("subscriptionManager");
        queue.addQueueStatusListener((QueueStatusListener) _subManager);
        // Configure _process listeners
        _process = createProcess();
        _process.addProcessStatusListener((ProcessStatusListener) queue);
        _process.addProcessStatusListener((ProcessStatusListener) _subManager);
        _process.addProcessAmountListener((ProcessAmountListener) _subManager);
        _process
                .addQueueEntryEventListener((ProcessQueueEntryEventListener) queue);
        log.debug("Configured event listeners.");
    }
    
    public ConventionalPrintingProcess createProcess() {
        DeviceConfig config = (DeviceConfig) _factory.getBean("deviceConfig");
        Queue queue = new MemoryQueue(config, 10, _fileUtil);
        return new ConventionalPrintingProcess(config, queue, _fileUtil, null,
                null);
    }
    /*
     * Test method for 'org.cip4.elk.impl.util.PreFlightJDF.isExecutableAndAvailbleResources(JDFNode, JDFResponse)'
     */
    public void testIsExecutableAndAvailbleResources() {
        JDFNode jdf1 = null;
        try {            
            jdf1 = (JDFNode) getResourceAsJDF(_jdfFilesPath
                    + "echo2.jdf");
        } catch (Exception e) {
            log.error("Unable to load jdfs...");
            assertTrue(false);
        }    
        jdf1.setActivation(JDFNode.Activation_Held);
        JDFNode jdf3 = (JDFNode) jdf1.cloneNode(true);
        _preFlightJDF.isExecutableAndAvailbleResources(jdf3,null);
        log.debug(jdf1.getAuditPool());
        log.debug(jdf3.getAuditPool());
        testGetProcessNodes();

    }
    
    public void testGetProcessNodes(){
        JDFNode jdf1 = null;
        try {            
            jdf1 = (JDFNode) getResourceAsJDF(_jdfFilesPath
                    + "echo2.jdf");
        } catch (Exception e) {
            log.error("Unable to load jdfs...");
            assertTrue(false);
        }    
        
        List l = _preFlightJDF.getProcessNodes(jdf1,"ConventionalPrinting",JDFElement.EnumNodeStatus.Aborted);
        List l2 = _preFlightJDF.getProcessNodes(jdf1,"ConventionalPrinting",JDFElement.EnumNodeStatus.Completed);
        assertTrue(l.size() == 4);
        assertTrue(l2.size() == 2);
    }

}
