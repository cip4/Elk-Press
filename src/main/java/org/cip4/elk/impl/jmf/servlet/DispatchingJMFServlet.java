/*
 * Created on Sep 12, 2004
 */
package org.cip4.elk.impl.jmf.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;

import javax.mail.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.CopyUtils;
import org.apache.commons.io.IOUtils;
import org.cip4.elk.Config;
import org.cip4.elk.JDFElementFactory;
import org.cip4.elk.impl.mime.MimeReader;
import org.cip4.elk.impl.spring.ElkSpringConfiguration;
import org.cip4.elk.jmf.IncomingJMFDispatcher;
import org.cip4.elk.jmf.servlet.JMFServlet;
import org.cip4.jdflib.core.JDFComment;
import org.cip4.jdflib.core.JDFParser;
import org.cip4.jdflib.jmf.JDFJMF;
import org.cip4.jdflib.jmf.JDFMessage;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.resource.JDFNotification;
import org.cip4.jdflib.util.UrlUtil;

/**
 * This servlet receives JMF messages, parses them, and forwards them to an
 * {@link org.cip4.elk.jmf.IncomingJMFDispatcher IncomingJMFDispatcher}. The
 * <code>IncomingJMFDispatcher</code> this servlet uses is configured using <a
 * href="http://www.springframework.com">Spring </a>, see
 * {@link org.cip4.elk.impl.spring.ElkSpringConfiguration}.
 * 
 * @author Claes Buckwalter (clabu@itn.liu.se)
 * @version $Id: DispatchingJMFServlet.java,v 1.8 2006/08/30 07:55:06 buckwalter Exp $
 */
public class DispatchingJMFServlet extends JMFServlet {

    public static final String SERVLET_NAME = "Elk Dispatching JMF Servlet";

    public static final String INIT_PARAM_MIME_OUTPUT_DIR = "mime.output.dir";

    private Session _mailSession;

    private MimeReader _mimeReader;

    private File _mimeOutputDir;

    private Config _config;

    private IncomingJMFDispatcher _jmfDispatcher;

    /**
     * Initializes this servlet. References to the Elk components used by this servlet
     * are obtained using <a href="http://www.springframework.org">Spring's </a>.
     * <p>
     * The directory where received MIME package contents are stored is determined 
     * in the following order:
     * <ul>
     * <li>javax.servlet.context.tempdir</li>
     * <li>{@link org.cip4.elk.Config#getJDFTempURL()}</li>
     * <li>Context init parameter: {@link DispatchingJMFServlet#INIT_PARAM_MIME_OUTPUT_DIR}</li>
     * <li>Servlet init parameter: {@link DispatchingJMFServlet#INIT_PARAM_MIME_OUTPUT_DIR}</li>
     * </ul>
     * </p>
     */ 
    public void init() throws ServletException {
        super.init();
        // Configuration using Spring
        _config = (Config) ElkSpringConfiguration.getBeanFactory().getBean(
            "deviceConfig");
        _jmfDispatcher = (IncomingJMFDispatcher) ElkSpringConfiguration
                .getBeanFactory().getBean("incomingDispatcher");

        // Configure default output dir
        File outputDir = (File) getServletContext().getAttribute("javax.servlet.context.tempdir");
        // Override default with device config ouput dir
        String outputDirUrl = _config.getJDFTempURL();
        if (outputDirUrl != null) {
            try {
                outputDir = new File(new URI(outputDirUrl));
            } catch (Exception use) {
                log.warn("Ignoring output dir URL: " + outputDirUrl, use);
                outputDir = (File) getServletContext().getAttribute("javax.servlet.context.tempdir");
            }
        }        
        // Override device config with context init parameter
        String outputDirPath = getServletContext().getInitParameter(
            INIT_PARAM_MIME_OUTPUT_DIR);
        // Override context init parameter with servlet init parameter
        Enumeration e = getInitParameterNames();
        while (e.hasMoreElements()) {
            String paramName = (String) e.nextElement();
            if (paramName.equals(INIT_PARAM_MIME_OUTPUT_DIR)) {
                outputDirPath = getInitParameter(paramName);
            } else {
                String msg = "Unknown servlet init parameter: " + paramName
                        + " = " + getInitParameter(paramName);
                log.warn(msg);
                log(msg);
            }
        }
        if (outputDirPath != null) {
            outputDir = new File(outputDirPath);
        }
        configureMime(outputDir);
    }

    public String getServletName() {
        return SERVLET_NAME + " for " + _config.getID();
    }

    /**
     * HTTP GET is not implemented.
     */
    public void doGet(HttpServletRequest req,  HttpServletResponse res) throws IOException, ServletException {
        req.getRequestDispatcher("/index.jsp").forward(req, res);
    }
    
    
    
    /**
     * Configures a MIME reader and the directory where contents of MIME files
     * are written.
     * 
     * @param mimeOutputDir
     */
    private void configureMime(File mimeOutputDir) {
        _mimeOutputDir = mimeOutputDir;
        _mailSession = Session.getDefaultInstance(new Properties());
        _mimeReader = new MimeReader();
        log.debug("Configured MIME output directory: " + _mimeOutputDir);
    }

    /**
     * Processes JMF.
     * 
     * @param req
     * @param resp
     * @throws IOException
     */
    public void processJMF(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        // Process JMF
        JDFJMF jmfIn = null;
        JDFJMF jmfOut = null;
        jmfIn = parseJMF(req.getInputStream());
        if (log.isDebugEnabled()) {
            log.debug("Received JMF: " + jmfIn);
        }
        jmfOut = _jmfDispatcher.dispatchJMF(jmfIn);
        if (jmfOut != null) {
            // Set content-type
            resp
                    .setContentType(JMFServlet.JMF_CONTENT_TYPE
                            + "; charset=UTF-8");
            // Send JMF Response
            jmfOut.getOwnerDocument_KElement().write2Stream(
                resp.getOutputStream(), 0);
        } else {
            // Send HTTP response code
            resp.setStatus(HttpServletResponse.SC_OK);
        }
    }

    /**
     * Processes MIME packages. Only MIME packages containing a JMF
     * message are processed. MIME packages without a JMF message will result in a 
     * <em>HTTP 400 Bad Request</em> error message being sent back to the client.
     * 
     * @param req
     * @param res
     */
    public void processMime(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        String userInfo = req.getHeader("User-Agent") + " @ "
                + req.getRemoteHost() + " (" + req.getRemoteAddr() + ")";
        log.info("Receiving MIME package from " + userInfo + "...");
        // Write MIME packet to disk
        File outputDir = null;
        File mimeFile = null;
        OutputStream outStream = null;
        try {
            //TODO Convert file URL to absolute path
            outputDir = new File(_mimeOutputDir, generateFileName(req));
            outputDir.mkdirs();
            mimeFile = new File(outputDir, "package.mime"); //XXX .mjd or .mjm
            outStream = new FileOutputStream(mimeFile);
            CopyUtils.copy(req.getInputStream(), outStream);
            log.debug("Wrote MIME package from " + userInfo + " to: "
                    + mimeFile.getAbsolutePath());
        } catch (Exception e) {
            String err = "Could not write MIME package to disk: " + e;
            log.error(err, e);
            e.printStackTrace();
            sendJMFResponse(res, 6, err);
            return;
        } finally {
            IOUtils.closeQuietly(outStream);
        }
        // Read MIME from disk and extract its contents
        String outputDirUrl = null;
        InputStream mimeStream = null;
        JDFJMF jmfIn = null;
        try {
            outputDirUrl = UrlUtil.fileToUrl(outputDir, true);
            mimeStream = new FileInputStream(mimeFile);
            String[] fileUrls = _mimeReader.extractMimePackage(mimeStream,
                outputDirUrl);
            if (log.isDebugEnabled()) {
                log.debug("Extracted contents of MIME package from " + userInfo
                        + " to: " + Arrays.asList(fileUrls));
            }

            // Extract JMF from MIME
            // TODO Add support for implicit JDF submission, i.e. no
            // SubmitQueueEntry command
            jmfIn = getJMFFromMimeContents(fileUrls);
            if (jmfIn == null) {
                String msg = "The MIME package did not contain a JMF message.";
                log.error(msg);
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
                return;
            }
        } catch (Exception e) {
            String err = "Could not extract contents from MIME package to '"
                    + outputDirUrl + "': " + e;
            log.error(err, e);
            e.printStackTrace();
            sendJMFResponse(res, 6, err);
            return;
        } finally {
            IOUtils.closeQuietly(mimeStream);
        }          

        log.debug("Dispatching JMF from MIME package...");
        
        JDFJMF jmfOut = null;
        jmfOut = _jmfDispatcher.dispatchJMF(jmfIn);
        
        log.debug("Response from dispatching JMF from MIME package: " + jmfOut);
        if (jmfOut != null) {
            // Set content-type
            res.setContentType(JMFServlet.JMF_CONTENT_TYPE
                    + "; charset=UTF-8");
            // Send JMF Response
            jmfOut.getOwnerDocument_KElement().write2Stream(
                res.getOutputStream(), 0);
        } else {
            log.error("Could not dispatch JMF message in MIME package.");
            // Send HTTP response code                
            res.setStatus(HttpServletResponse.SC_OK);                
        }
    }

    /**
     * Parses JMF from an InputStream
     * 
     * @param stream
     * @return
     */
    private JDFJMF parseJMF(InputStream stream) {
        return new JDFParser().parseStream(stream).getJMFRoot();
    }
    
    /**
     * Sends a JMF Response message over the specified HTTP response. A comment
     * containing the file URLs will be built and inserted at
     * <code>JMF/Response/Notification/Comment</code>.
     * 
     * @param res the HTTP response
     * @param returnCode the JMF return code (JMF/Response/@ReturnCode
     * @param fileUrls the file URLs used to build a comment
     * @throws IOException
     */
    protected void sendJMFResponse(HttpServletResponse res, String[] fileUrls)
            throws IOException {
        String msg = "Extracted " + fileUrls.length + " from MIME package: "
                + Arrays.asList(fileUrls);
        sendJMFResponse(res, 0, msg);
    }

    /**
     * Sends a JMF Response message over the specified HTTP response. The
     * specified comment will be inserted at
     * <code>JMF/Response/Notification/Comment</code>.
     * 
     * @param res the HTTP response
     * @param returnCode the JMF return code (JMF/Response/@ReturnCode
     * @param comment the message that will be inserted into the Notification
     *            (JMF/Response/Notification/Comment)
     * @throws IOException
     */
    protected void sendJMFResponse(HttpServletResponse res, int returnCode,
            String comment) throws IOException {
        // Build JMF Response
        JDFJMF jmf = JDFElementFactory.getInstance().createJMF(); // JMF
        jmf.init();
        JDFResponse response = jmf.appendResponse();
        response.init();
        response.setReturnCode(returnCode); // JMF/Response/@ReturnCode
        response.setType(JDFMessage.EnumType.Notification.getName()); // JMF/Response/@Type
        JDFNotification notification = response.appendNotification(); // JMF/Response/Notification
        if (returnCode == 0) {
            notification.setClass(JDFNotification.EnumClass.Information); // JMF/Response/Notification/@Class
        } else {
            notification.setClass(JDFNotification.EnumClass.Error); // JMF/Response/Notification/@Class
        }
        JDFComment jdfComment = notification.appendComment(); // JMF/Response/Notification/Comment
        jdfComment.appendText(comment);
        // Send response
        res.setContentType(JMFServlet.JMF_CONTENT_TYPE);
        jmf.getOwnerDocument_KElement().write2Stream(res.getOutputStream(), 0);
    }

    /**
     * Generates a random filename according to the following pattern:
     * <code>{remote address}_{current time in millis}</code>
     * 
     * @param req
     * @return a random filename
     */
    protected static String generateFileName(HttpServletRequest req) {
        return req.getRemoteAddr() + "_" + System.currentTimeMillis();
    }

    /**
     * Finds a <em>SubmitQueueEntry</em> JMF message from a MIME package. If
     * the package does not contain a SubmitQueueEntry JMF message then
     * <code>null</code> is returned.
     * 
     * @param fileUrls the URLs
     * @return
     * @throws IOException
     */
    protected JDFJMF getJMFFromMimeContents(String[] fileUrls)
            throws IOException, URISyntaxException {
        String jmfUrl = null;
        String jdfUrl = null;
        JDFJMF jmf = null;
        for (int i = 0; i < fileUrls.length; i++) {
            if (fileUrls[i].endsWith(".jmf")) {
                jmfUrl = fileUrls[i];
                break;
            } else if (fileUrls[i].endsWith(".jdf")) {
                jdfUrl = fileUrls[i];
            }
        }
        // Check if MIME contained JMF (SubmitQueueEntry)
        if (jmfUrl != null) {
            jmf = parseJMF(new FileInputStream(new File(new URI(jmfUrl)))); // XXX Convert URL to file path
        }
        if (log.isDebugEnabled()) {
            log.debug("Extracted JMF from MIME package: " + jmf);
        }
        return jmf;
    }

}
