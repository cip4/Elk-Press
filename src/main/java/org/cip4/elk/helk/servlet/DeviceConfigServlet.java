package org.cip4.elk.helk.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cip4.elk.JDFElementFactory;
import org.cip4.elk.device.DeviceConfig;
import org.cip4.elk.impl.jmf.preprocess.JDFPreprocessor;
import org.cip4.elk.impl.jmf.preprocess.SimpleJDFPreprocessor;
import org.cip4.elk.impl.spring.ElkSpringConfiguration;
import org.cip4.elk.jmf.SubscriptionManager;
import org.cip4.jdflib.jmf.JDFStopPersChParams;
import org.springframework.beans.factory.BeanFactory;

/**
 * Servlet implementation class for Servlet: QueueServlet
 * 
 */
public class DeviceConfigServlet extends javax.servlet.http.HttpServlet
        implements javax.servlet.Servlet {

    public static final String SERVLET_NAME = "Device Configuration Servlet";
    
    private static final long serialVersionUID = -2248152145974243656L;
    
    private static Log log = LogFactory.getLog(DeviceConfigServlet.class);
    
    protected BeanFactory _factory;

    public DeviceConfigServlet() {
        super();
    }

    public void init() throws ServletException {
        super.init();
        log.info("Initializing " + getServletName() + "...");
        initBeanFactory();
        log.info("Initialized " + getServletName() + ".");
    }
    
    /**
     * Inititializes the Spring BeanFactory.
     */
    private void initBeanFactory() {
        _factory = ElkSpringConfiguration.getBeanFactory();
    }

    public String getServletName() {
        return SERVLET_NAME;
    }
    
    public String getServletInfo() {
        return getServletName(); // TODO Replace this with version info
    }
    
    /*
     * (non-Java-doc)
     * 
     * @see javax.servlet.http.HttpServlet#doGet(HttpServletRequest request,
     *      HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        String command = request.getParameter("cmd");

        if (command == null || command.length() == 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (command.equals("showConfig")) {
            showConfig(request, response);
        } else if (command.equals("showSubscriptions")) {
            showSubscriptions(request, response);
        } else if (command.equals("deleteSubscription")) {
            deleteSubscription(request, response);
        } else if (command.equals("setValidation")) {
            setValidation(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Unkonwn command.");
        }
    }

    /*
     * (non-Java-doc)
     * 
     * @see javax.servlet.http.HttpServlet#doPost(HttpServletRequest request,
     *      HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
    }
    
    /**
     * Shows the device's configuration.
     * @param req
     * @param res
     * @throws ServletException
     * @throws IOException
     */
    private void showConfig(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {      
        DeviceConfig config = (DeviceConfig) _factory.getBean("deviceConfig");
        req.setAttribute("config", config);
        JDFPreprocessor preprocessor  = (JDFPreprocessor) _factory.getBean("preProcessor");
        req.setAttribute("preprocessor", preprocessor);        
        req.getRequestDispatcher("/config/showConfig.jsp").forward(req, res);
    }
    
    /**
     * Shows all subscriptions registered with the device.
     * @param req
     * @param res
     * @throws ServletException
     * @throws IOException
     */
    private void showSubscriptions(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        SubscriptionManager subManager = (SubscriptionManager) _factory.getBean("subscriptionManager");
        req.setAttribute("subscriptions", subManager);
        req.getRequestDispatcher("/config/showSubscriptions.jsp").forward(req, res);        
    }
    
    /**
     * Deletes the specified subscription.
     * @param req
     * @param res
     * @throws ServletException
     * @throws IOException
     */
    private void deleteSubscription(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        SubscriptionManager subManager = (SubscriptionManager) _factory.getBean("subscriptionManager");
        
        String channelID = req.getParameter("channel");
        if (channelID == null || channelID.length() == 0) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Could not delete subscription. 'channel' parameter was not specified.");
            return;
        }
        String url = req.getParameter("url");
        if (url == null || url.length() == 0) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Could not delete subscription. 'url' parameter was not specified.");
            return;
        }
        // Build StopPersChParams
        JDFStopPersChParams stopParams = 
            (JDFStopPersChParams)JDFElementFactory.getInstance().createJDFElement("StopPersChParams");        
        stopParams.setChannelID(channelID);
        stopParams.setURL(url);
        // Unregisters subscription
        log.debug("Unregistering subscription " + channelID + ", " + url + "...");
        boolean deleted = (subManager.unregisterSubscription(stopParams) == 0);
        log.debug("Unregistered subscription: " + deleted);
        // Displays the remaining subscriptions
        req.setAttribute("subscriptions", subManager);
        req.getRequestDispatcher("/config/showSubscriptions.jsp").forward(req, res);        
    }
    
    /**
     * Enables/disables validation of JDF before they are added to the device's queue.
     * 
     * @param req
     * @param res
     * @throws ServletException
     * @throws IOException
     */
    private void setValidation(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException {
        String validation = req.getParameter("validation");
        if (validation == null || !(validation.equals("true") || validation.equals("false"))) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Could not configure validation. 'validation' paramater must have value 'true' or 'false'.");
            return;
        }
        boolean validationSwitch = new Boolean(validation).booleanValue();
        JDFPreprocessor preprocessor = (JDFPreprocessor) _factory.getBean("preProcessor");
        if (preprocessor instanceof SimpleJDFPreprocessor) {
            ((SimpleJDFPreprocessor) preprocessor).setValidation(validationSwitch);
        }
        req.getRequestDispatcher("/config?cmd=showConfig").forward(req, res);
    }
    
}
