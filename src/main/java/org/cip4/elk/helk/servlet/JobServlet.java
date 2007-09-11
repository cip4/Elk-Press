package org.cip4.elk.helk.servlet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.CopyUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cip4.elk.impl.util.URLAccessTool;
import org.cip4.jdflib.core.JDFConstants;
import org.cip4.jdflib.jmf.JDFQueueEntry;
import org.cip4.jdflib.jmf.JDFQueueSubmissionParams;
import org.cip4.jdflib.node.JDFNode;
import org.cip4.jdflib.pool.JDFAuditPool;
import org.cip4.jdflib.util.UrlUtil;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMResult;
import org.jdom.transform.JDOMSource;

/**
 * A servlet that handles the processing related to displaying JDF instances.
 * 
 * @author Claes Buckwalter (clabu@itn.liu.se)
 * @version $Id$
 */
public class JobServlet extends QueueServlet implements javax.servlet.Servlet {

    private static final long serialVersionUID = 1979377541276651386L;
    private static Log log = LogFactory.getLog(JobServlet.class);

    public static final String SERVLET_NAME = "Job Servlet";

    private File _jdf2html = null;

    private URLAccessTool _urlTool;

    public JobServlet() {
        super();
    }

    public String getServletName() {
        return SERVLET_NAME;
    }

    public void init() throws ServletException {
        super.init();
        log.info("Initializing " + SERVLET_NAME + "...");
        _urlTool = new URLAccessTool();
        initStyleSheet();
        log.info("Initialized " + SERVLET_NAME + ".");
    }

    /**
     * Uses the device's device capabilities to generate an XSL stylesheet for
     * transforming JDF instances into HTML for the device's job view user
     * interface.
     * 
     * @throws ServletException
     */
    private void initStyleSheet() throws ServletException {
        File baseDir = new File(getServletContext().getRealPath("/"));
        File deviceFile = new File(baseDir, "/config/Device.xml"); // TODO Get
        // device
        // capabilities
        // from
        // DeviceConfig
        // instead
        File device2xslFile = new File(baseDir,
                "/WEB-INF/xsl/DeviceCap2XSL.xsl");
        try {
            // Transform device capabilities into XSL stylesheet
            log.debug("Transforming device capabilities to XSL stylesheet...");
            log.debug("Input device capabilities: "
                    + deviceFile.getCanonicalPath());
            log.debug("Input XSL stylesheet: "
                    + device2xslFile.getCanonicalPath());
            JDOMResult jdf2html = transform(deviceFile.getCanonicalPath(),
                device2xslFile.getCanonicalPath());
            File jdf2htmlFile = File.createTempFile("JDF2HTML", ".xsl");
            jdf2htmlFile.deleteOnExit();
            log.debug("Output XSL stylesheet: "
                    + jdf2htmlFile.getCanonicalPath());
            // Writes XSL to temp
            OutputStream outStream = new FileOutputStream(jdf2htmlFile);
            XMLOutputter outputter = new XMLOutputter();
            outputter.output(jdf2html.getDocument(), outStream);
            outStream.close();
            log.debug("Wrote generated stylesheet to "
                    + jdf2htmlFile.getCanonicalPath());
            _jdf2html = jdf2htmlFile;
        } catch (IOException ioe) {
            String msg = "Could not create user interface template from device capabilities file.";
            log.error(msg, ioe);
            throw new ServletException(msg, ioe);
        }
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

        if (command.equals("showJob")) {
            showJob(request, response);
        } else if (command.equals("showJDF")){
            showJDF(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Unkonwn command.");
        }
    }
    
    /**
     * Display's the JDF instance for a queue entry ID
     * @param req
     * @param res
     * @throws ServletException
     * @throws IOException
     */
    private void showJDF(HttpServletRequest req, HttpServletResponse res)
    throws IOException {
        String qeID = req.getParameter("id");
        if (qeID == null || qeID.length() == 0) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "No Job ID was specified.");
            return;
        }
        String mimeType = req.getParameter("mime");
        if (mimeType == null || mimeType.length() == 0) {
            mimeType = "application/vnd.cip4-jdf+xml";
        }
        
        // Gets the queue entry
        String jdfUrl = null;
        try {
            jdfUrl = getJDFURL(qeID);
        } catch (IllegalArgumentException iae) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "No queue entry was found with id=" + qeID);
            return;
        }
        // Sends JDF to browser
        res.setContentType(mimeType);
        InputStream in = _urlTool.getURLAsInputStream(jdfUrl);
        OutputStream out = res.getOutputStream();
        CopyUtils.copy(_urlTool.getURLAsInputStream(jdfUrl), res.getOutputStream());
        IOUtils.closeQuietly(in);
        IOUtils.closeQuietly(out);        
    }

    /**
     * Takes a queue entry ID, looks up the corresponding JDF, parses the JDF and
     * displays information from the JDF.
     * 
     * @param req
     * @param res
     * @throws ServletException
     * @throws IOException
     */
    private void showJob(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String qeID = req.getParameter("id");
        if (qeID == null || qeID.length() == 0) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "No Job ID was specified.");
            return;
        }
        // Gets the queue entry
        String jdfUrl = null;
        try {
            jdfUrl = getJDFURL(qeID);
        } catch (IllegalArgumentException iae) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "No queue entry was found with id=" + qeID);
            return;
        }
        /*
        JDFQueueEntry qe = _queue.getQueueEntry(qeID);
        if (qe == null) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "No queue entry was found with QueueEntryID " + qeID);
            return;
        }
        // Gets the queue entry's JDF URL
        JDFQueueSubmissionParams subParams = _queue
                .getQueueSubmissionParams(qeID);
        String jdfUrl = subParams.getURL();
        */
        
        // Gets JDF
        JDFNode jdf = _urlTool.getURLAsJDF(jdfUrl);
        if (jdf == null) {
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Could not read JDF from URL: " + jdfUrl);
            return;
        }
        
        // Stores JDF data in request
        JDFAuditPool auditPool = jdf.getAuditPool();
        if (auditPool != null) {
            req.setAttribute("audits", auditPool.getAudits(null, null));
        }        
        req.setAttribute("inputLinks", jdf.getResourceLinkPool().getInOutLinks(
            true, true, JDFConstants.WILDCARD, JDFConstants.WILDCARD));
        req.setAttribute("outputLinks", jdf.getResourceLinkPool()
                .getInOutLinks(false, true, JDFConstants.WILDCARD,
                    JDFConstants.WILDCARD));
        // Stores data for XSLT in request
        req.setAttribute("jdf", jdf);
        req.setAttribute("xsl", UrlUtil.fileToUrl(_jdf2html, true));
        req.getRequestDispatcher("/job/showJob.jsp").forward(req, res);
    }

    /**
     * Gets the JDF URL of a queue entry.
     * @param queueEntryID  a queue entry
     * @return the URL to the queue entry's JDF file
     * @throws IllegalArgumentException if no JDF URL was found for the specified queue entry
     */
    private String getJDFURL(String queueEntryID) throws IllegalArgumentException {
        JDFQueueEntry qe = _queue.getQueueEntry(queueEntryID);
        if (qe == null) {
            throw new IllegalArgumentException("No queue entry was found with QueueEntryID: " + queueEntryID);            
        }
        // Gets the queue entry's JDF URL
        JDFQueueSubmissionParams subParams = _queue.getQueueSubmissionParams(queueEntryID);
        return subParams.getURL();
    }
    
    
    /*
     * (non-Java-doc)
     * 
     * @see javax.servlet.http.HttpServlet#doPost(HttpServletRequest request,
     *      HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        // TODO Auto-generated method stub
    }

    /**
     * Transforms an XML document using the specified XSL stylesheet.
     * 
     * @param xmlPath the absolute path to the XML document to transform
     * @param xslPath the absolute path to the XSL stylesheet to use
     */
    private static JDOMResult transform(String xmlPath, String xslPath) {
        // Parses XML
        SAXBuilder builder = new SAXBuilder();
        builder.setValidation(false);
        builder.setIgnoringElementContentWhitespace(true);
        Document report = null;
        try {
            report = builder.build(new File(xmlPath));
        } catch (Exception e) {
            System.err.println("Could not parse XML file '" + xmlPath + "'.");
        }
        // Transforms XML
        JDOMResult out = new JDOMResult();
        try {
            Transformer transformer = TransformerFactory.newInstance()
                    .newTransformer(new StreamSource(xslPath));
            out = new JDOMResult();
            transformer.transform(new JDOMSource(report), out);
        } catch (Exception e) {
            System.err.println("Could not transform XML file '" + xmlPath
                    + "' using stylesheet '" + xslPath + "'.");
        }
        return out;
    }

}
