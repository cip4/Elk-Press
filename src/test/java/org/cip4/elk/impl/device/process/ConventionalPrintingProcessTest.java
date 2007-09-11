/*
 * Created on Jun 22, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cip4.elk.impl.device.process;

import java.util.List;

import org.cip4.elk.ElkTestCase;
import org.cip4.elk.JDFElementFactory;
import org.cip4.elk.device.DeviceConfig;
import org.cip4.elk.device.process.ProcessAmountListener;
import org.cip4.elk.device.process.ProcessQueueEntryEventListener;
import org.cip4.elk.device.process.ProcessStatusListener;
import org.cip4.elk.impl.queue.MemoryQueue;
import org.cip4.elk.impl.util.PreFlightJDF;
import org.cip4.elk.impl.util.URLAccessTool;
import org.cip4.elk.jmf.SubscriptionManager;
import org.cip4.elk.queue.Queue;
import org.cip4.elk.queue.QueueStatusListener;
import org.cip4.elk.util.JDFUtil;
import org.cip4.jdflib.auto.JDFAutoNotification.EnumClass;
import org.cip4.jdflib.core.JDFPartAmount;
import org.cip4.jdflib.core.JDFResourceLink;
import org.cip4.jdflib.core.JDFElement.EnumNodeStatus;
import org.cip4.jdflib.datatypes.JDFAttributeMap;
import org.cip4.jdflib.datatypes.VJDFAttributeMap;
import org.cip4.jdflib.node.JDFNode;
import org.cip4.jdflib.pool.JDFAmountPool;
import org.cip4.jdflib.resource.JDFNotification;
import org.cip4.jdflib.resource.JDFResource;
import org.cip4.jdflib.resource.process.JDFComponent;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: ConventionalPrintingProcessTest.java,v 1.7 2006/11/29 15:22:04 buckwalter Exp $
 */
public class ConventionalPrintingProcessTest extends ElkTestCase {

    private static String _jdfExecutable;
    private static URLAccessTool _fileUtil;
    private static BeanFactory _factory;
    private ConventionalPrintingProcess _process;
    private SubscriptionManager _subManager;

    public void setUp() throws Exception {
        super.setUp();
        _fileUtil = new URLAccessTool(getResourceAsURL(".").toString());
        _jdfExecutable = _jdfFilesPath + "ConventionalPrintingGood.jdf";
        _factory = new ClassPathXmlApplicationContext(
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

    public void testExecuteNode() throws Exception {
        final JDFNode jdf = (JDFNode) getResourceAsJDF(_jdfExecutable);
        assertTrue(JDFUtil.processHandles(_process.getProcessType(), jdf, null) == 0);
        _process.setSpeed(1200000);
        _process.executeNode(jdf);
        assertTrue(JDFUtil.processHandles(_process.getProcessType(), jdf, null) != 0);        
    }

    /**
     * 
     */
    public void testGetSetPartAmounts() {
        
        log.info("Testing: testGetSetPartAmounts method...");
        JDFNode jdf0 = null, jdf1 = null;
        try {
            jdf0 = (JDFNode) getResourceAsJDF(_jdfFilesPath
                    + "ConventionalPrintingAmountPool.jdf");
            jdf1 = (JDFNode) getResourceAsJDF(_jdfFilesPath
                    + "ConventionalPrintingGood.jdf");
        } catch (Exception e) {
            log.error("Unable to load jdfs...");
            assertTrue(false);
        }

        JDFResourceLink resAmountOut = _process.getOutputResourceLink(jdf0);
        JDFResourceLink resAmountIn = _process.getAmountResource("Input", jdf0);
        JDFResourceLink resAmountOut2 = _process.getOutputResourceLink(jdf1);
        JDFResourceLink resAmountIn2 = _process
                .getAmountResource("Input", jdf1);

        log.debug("resAmountOut: " + resAmountOut);
        log.debug("resAmountOut: " + resAmountOut2);
        // Creates an AmountPool
        log.debug("Creating AmountPool...");
        JDFAmountPool amountPool = resAmountOut.getCreateAmountPool();
        JDFAmountPool amountPool2 = resAmountOut2.getCreateAmountPool();
        log.debug("resAmountOut: " + resAmountOut);
        log.debug("resAmountOut: " + resAmountOut2);
        JDFResource res = resAmountOut.getTarget();
        JDFResource res2 = resAmountOut2.getTarget();
        log.debug("TARGET res: " + res);
        log.debug("TARGET res2: " + res2);

        JDFComponent goodPart = _process.getCreateConditionPart(res, "Good");
        JDFComponent goodPart2 = _process.getCreateConditionPart(res2, "Good");
        log.debug("goodPart: " + goodPart);
        log.debug("goodPart2: " + goodPart2);
        log.info("getPartion Good returns null for resources with no Condition"
                + " good specified if the last argument is false.");
        assertNotNull(goodPart2);
        JDFPartAmount partAmountGood = amountPool.getCreatePartAmount(goodPart
                .getPartMap());
        JDFPartAmount partAmountGood2 = amountPool2
                .getCreatePartAmount(goodPart2.getPartMap());
        log.debug("partAmountGood when modified: " + partAmountGood);
        log.debug("partAmountGood2 when modified: " + partAmountGood2);
        log.debug(amountPool);
        log.debug(amountPool2);
        JDFAttributeMap emptyAttributeMap = new JDFAttributeMap();
        int inAmount = (int) resAmountIn.getAmount(emptyAttributeMap);
        int inAmount2 = (int) resAmountIn2.getAmount(emptyAttributeMap);
        int outAmount = (int) resAmountOut.getAmount(emptyAttributeMap);
        int outAmount2 = (int) resAmountOut2.getAmount(emptyAttributeMap);
        int partAmountGoodAmount = (int) partAmountGood.getAmount(null);
        int partAmountGoodAmount2 = (int) partAmountGood2.getAmount(null);
        log.debug("inAmount: " + inAmount + " outAmount=" + outAmount
                + " PartAmount/@Amount=" + partAmountGoodAmount);
        log.debug("inAmount2: " + inAmount2 + " outAmount2=" + outAmount2
                + " PartAmount2/@Amount=" + partAmountGoodAmount2);
        
    }

    /**
     * 
     */
    public void testGetPartStatusMethod() {
        log.info("Testing: the status method jdf.getPartStatus(new"
                + " JDFAttributeMap()) and StatusPool");
        JDFNode jdf0 = null, jdf1 = null;
        String jdfFile1 = "AncestorPoolTest.jdf";
        try {
            jdf0 = (JDFNode) getResourceAsJDF(_jdfFilesPath
                    + "AncestorPoolTest0.jdf");
            jdf1 = (JDFNode) getResourceAsJDF(_jdfFilesPath + jdfFile1);
        } catch (Exception e) {
            log.error("Unable to load jdfs...");
            assertTrue(false);
        }

        log.info("The JDFNode '" + _jdfFilesPath + jdfFile1
                + "' has status 'FailedTestRun' and will thus not"
                + " be executed.");
        assertFalse(jdf1.getPartStatus(new JDFAttributeMap()).equals(
            EnumNodeStatus.Waiting));

        log.info("The JDFNode has status 'Pool' and the StatuPool"
                + " has status 'Waiting'.");
        assertTrue(jdf0.getPartStatus(new JDFAttributeMap()).equals(
            EnumNodeStatus.Waiting));
    }

    public void testGetOutputResourceLink() {
        log.info("Testing: testGetOutputResourceLink()");
        JDFNode jdf = JDFElementFactory.getInstance().createJDF();
        List l = jdf.getResourceLinks();
        assertNotNull(l);
        log.info("Calling getResourceLinks() with no ResourceLinks"
                + " NEVER returns null");
        log.info("Calling a get(0) on an empty List throws"
                + " ArrayIndexOutOfBoundsException.");
    }
    

    public void testGetCreateConditionPart() {        
        JDFNode jdf1 = null;        
        String jdfFile2 = "ConventionalPrintingNoAmountPool.jdf";
        try {            
            jdf1 = (JDFNode) getResourceAsJDF(_jdfFilesPath + jdfFile2);
        } catch (Exception e) {
            log.error("Unable to load jdfs...");
            assertTrue(false);
        }
        
        PreFlightJDF p = new PreFlightJDF();
        List l = p.getProcessNodes(jdf1,"ConventionalPrinting",null);
        JDFNode jdf = (JDFNode) l.get(0);       
        
        JDFResourceLink resAmountOut = _process.getOutputResourceLink(jdf);
        JDFResourceLink resAmountIn = _process.getAmountResource("Input", jdf);

        if (resAmountOut == null || resAmountIn == null) {
            String msg = "The Elk could not execute the JDFNode. ";
            if (resAmountOut == null) {
                msg += "No Output Resource is given. ";
            }
            if (resAmountIn == null) {
                msg += "No Input Resource with Amount attribute. ";
            }
            log.error(msg);

            JDFNotification n = jdf.getCreateAuditPool().addNotification(EnumClass.Error,
                this.getClass().getName(), new VJDFAttributeMap());
            n.appendComment().appendText(msg);
            jdf.setStatus(EnumNodeStatus.Aborted);
            // TODO generate events.
            return;
        }

        /*
        // Creates an AmountPool
        log.info("Gets/Creates AmountPool for Output resource...");
        JDFAmountPool amountPool = resAmountOut.getCreateAmountPool();
        log.info("resAmountOut...: " + resAmountOut);
        log.info("AmountPool: " + amountPool);
        JDFResource res = ((JDFResource) resAmountOut.getTarget());

        // Creates the Condition='Good' and 'Waste' Parts if they don't exist.
        JDFComponent goodPart = _process.getCreateConditionPart(res, "Good");
        JDFComponent wastePart = _process.getCreateConditionPart(res, "Waste");
       
        // Adds the (output) AmountPool/PartAmount elements if they don't exist.
        JDFPartAmount partAmountGood = amountPool.getCreatePartAmount(goodPart
                .getPartMap());        
        log.info("partAmountGood: " + partAmountGood);
        log.info("amountPool: " + amountPool);
        log.info("goodPart: " + goodPart);      
        log.info("wastePart: " + wastePart);
        */
        
    }

}
