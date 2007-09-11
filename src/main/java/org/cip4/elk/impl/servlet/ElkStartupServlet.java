package org.cip4.elk.impl.servlet;

import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cip4.elk.device.DeviceConfig;
import org.cip4.elk.device.process.Process;
import org.cip4.elk.device.process.ProcessAmountListener;
import org.cip4.elk.device.process.ProcessQueueEntryEventListener;
import org.cip4.elk.device.process.ProcessStatusListener;
import org.cip4.elk.impl.device.process.AbstractProcess;
import org.cip4.elk.impl.device.process.BaseProcess;
import org.cip4.elk.impl.spring.ElkSpringConfiguration;
import org.cip4.elk.impl.util.FileRepository;
import org.cip4.elk.impl.util.URLAccessTool;
import org.cip4.elk.impl.util.security.ElkAuthenticationHandler;
import org.cip4.elk.jmf.SubscriptionManager;
import org.cip4.elk.lifecycle.Lifecycle;
import org.cip4.elk.queue.Queue;
import org.cip4.elk.queue.QueueStatusListener;
import org.cip4.jdflib.core.JDFParser;
import org.cip4.jdflib.resource.JDFDevice;
import org.cip4.jdflib.util.UrlUtil;
import org.springframework.beans.factory.BeanFactory;

/**
 * This servlet's sole purpose is to configure Elk reference implementation.
 * This servlet does not process any HTTP requests. The following configuration
 * is performed:
 * <ul>
 * <li>Loads Spring configuration that instantiates objects and injects
 * dependencies</li>
 * <li>Adds listeners to the {@link org.cip4.elk.queue.Queue Queue}</li>
 * <li>Adds listeners to the
 * {@link org.cip4.elk.device.process.Process Process}</li>
 * </ul>
 * 
 * @author Claes Buckwalter (clabu@itn.liu.se)
 * @version $Id: ElkStartupServlet.java,v 1.18 2006/12/04 23:37:59 buckwalter Exp $
 */
public class ElkStartupServlet extends HttpServlet {

    private static final long serialVersionUID = -1587606271210782128L;

    public static final String SERVLET_NAME = "Elk Startup Servlet";

    public static final String OUTPUT_DIR = "/output/";

    public static final String CONFIG_DIR = "config/";
    
    private static Log log = LogFactory.getLog(ElkStartupServlet.class);

    private BeanFactory _beanFactory = null;

    public void init() throws ServletException {
        super.init();
        log.debug("Initializing " + getServletName() + "...");
        _beanFactory = ElkSpringConfiguration.getBeanFactory();
        buildDeviceConfiguration();
        configureEventListeners();
        log.debug("Initialized " + getServletName() + ".");
    }

    /**
     * Destroys the servlet and shuts down the Mule manager.
     */
    public void destroy() {
        log.debug("Destroying " + getServletName() + " ...");

        // Stops process        
        BaseProcess process = (BaseProcess) _beanFactory
                .getBean("process");
        process.stop();        
        // Stops subscription manager
        SubscriptionManager subManager = (SubscriptionManager) _beanFactory
                .getBean("subscriptionManager");        
        if (subManager instanceof Lifecycle) {
            ((Lifecycle) subManager).destroy();
        }
        // Stops queue
        Queue queue = (Queue) _beanFactory.getBean("queue");
        if (queue instanceof Lifecycle) {
            ((Lifecycle) queue).destroy();
        }
        // Stops file repository
        FileRepository fileRepo = (FileRepository) _beanFactory.getBean("fileRepository");
        fileRepo.destroy();
                
        ElkAuthenticationHandler authHandler = (ElkAuthenticationHandler) _beanFactory.getBean("authHandler");
        authHandler.destroy();
                
        super.destroy();
        log.debug(getServletName() + " destroyed.");
    }

    public String getServletName() {
        return SERVLET_NAME;
    }

    /**
     * Configures event listeners. Gets the beans created by Spring and hooks up
     * the queue and process listeners since Spring can not do this.
     */
    private void configureEventListeners() {
        // Configure queue listeners
        log.debug("Configuring event listeners...");
        Queue queue = (Queue) _beanFactory.getBean("queue");
        SubscriptionManager subManager = (SubscriptionManager) _beanFactory
                .getBean("subscriptionManager");
        queue.addQueueStatusListener((QueueStatusListener) subManager);
        // Configure process listeners
        Process process = (Process) _beanFactory.getBean("process");
        process.addProcessStatusListener((ProcessStatusListener) queue);
        process.addProcessStatusListener((ProcessStatusListener) subManager);
        process.addQueueEntryEventListener((ProcessQueueEntryEventListener) queue);
        if (process instanceof AbstractProcess) {
            ((AbstractProcess)process).addProcessAmountListener((ProcessAmountListener)subManager);
        }
        log.debug("Configured event listeners.");
    }

    private void buildDeviceConfiguration() {
        // Set base URL
        URLAccessTool urlTool = (URLAccessTool) _beanFactory
                .getBean("fileUtil");
        try {
            urlTool.setBaseUrl(getServletContext().getRealPath("/"));
//            urlTool.setBaseUrl(UrlUtil.fileToUrl(new File(getServletContext().getRealPath("/")), true));
//        } catch (MalformedURLException mue) {
        } catch (Exception mue) {
                       log.warn("Base URL could not be configured.", mue);
        }

        DeviceConfig config = (DeviceConfig) _beanFactory
                .getBean("deviceConfig");

        // Set configuration's Device element
        InputStream inStream = urlTool.getURLAsInputStream(CONFIG_DIR
                + "Device.xml");
        JDFDevice device = (JDFDevice) new JDFParser().parseStream(inStream)
                .getRoot();
        config.setDeviceConfig(device);

        // Configure directories
        if (config.getJDFTempURL() == null) {
            File tempDir = (File) getServletContext().getAttribute(
                "javax.servlet.context.tempdir");
            try {
               final String toUrl = UrlUtil.fileToUrl(tempDir, true);
                config.setJDFTempURL(toUrl);
                
                log.warn("Using default JDF temp directory: "+ toUrl);
            } catch (MalformedURLException mue) {
                log.error("Could not configure default JDF temp directory.", mue);
            }
        }
        if (config.getLocalJDFOutputURL() == null) {
            File localDir = new File(getServletContext()
                    .getRealPath(OUTPUT_DIR));
            localDir.mkdir();
            try {
               final String toUrl = UrlUtil.fileToUrl(localDir, true);
                config.setLocalJDFOutputURL(toUrl);
                log.warn("Using default local JDF directory: "+ localDir);
            } catch (MalformedURLException mue) {
                log.error("Could not configure default local JDF directory.", mue);
            }
        }
        // Configure public URL
        try {
        	if (device.getJDFOutputURL() != null) {
        		config.setJDFOutputURL(device.getJDFOutputURL());
        	} else {
	            String address = "http://" + InetAddress.getLocalHost().getHostAddress() +
	                ":8080" + "/elk" + OUTPUT_DIR;
	            config.setJDFOutputURL(address);
        	}
        } catch (Exception e) {
            log.warn("Could not dynamically configure public JDF output URL.", e);
        }
        // Configure FileRepository
        log.debug("Configuring FileRepository dynamically...");
        FileRepository repository = (FileRepository) _beanFactory.getBean("fileRepository");
        repository.setPrivateDirBaseURL(config.getJDFTempURL());
        repository.setPublicDirPublicBaseURL(config.getJDFOutputURL());
        repository.setPublicLocalBaseURL(config.getLocalJDFOutputURL());
    }
}
