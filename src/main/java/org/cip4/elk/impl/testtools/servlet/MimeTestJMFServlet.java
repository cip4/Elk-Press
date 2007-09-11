/*
 * Created on Sep 12, 2004
 */
package org.cip4.elk.impl.testtools.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Properties;

import javax.mail.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.CopyUtils;
import org.apache.commons.io.IOUtils;
import org.cip4.elk.JDFElementFactory;
import org.cip4.elk.impl.mime.MimeReader;
import org.cip4.elk.jmf.servlet.JMFServlet;
import org.cip4.jdflib.core.JDFComment;
import org.cip4.jdflib.core.JDFParser;
import org.cip4.jdflib.jmf.JDFJMF;
import org.cip4.jdflib.jmf.JDFMessage;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.resource.JDFNotification;
import org.cip4.jdflib.util.UrlUtil;

/**
 * This servlet parses MIME packages containing JMF and/or JDF and extracts the
 * contents to the output directory specified its superclass
 * {@link org.cip4.elk.impl.testtools.servlet.LoggingJMFServlet LoggingJMFServlet}.
 * 
 * @author Claes Buckwalter (clabu@itn.liu.se)
 */
public class MimeTestJMFServlet extends LoggingJMFServlet {

    public static final String SERVLET_NAME = "Elk Mime JMF Servlet";

    private Session _mailSession;

    private MimeReader _mimeReader;

    private JDFElementFactory _factory;

    public void init() throws ServletException {
        super.init();
        log.info("Received MIME packages will be logged to: "
                + _logDir.getAbsolutePath());
        _factory = JDFElementFactory.getInstance();
        configureMime();
    }

    public String getServletName() {
        return SERVLET_NAME;
    }

    private void configureMime() {
        _mailSession = Session.getDefaultInstance(new Properties());
        _mimeReader = new MimeReader();
    }

    /**
     * Handles MIME packages.
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
            outputDir = new File(_logDir, generateFileName(req));
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
        try {
            outputDirUrl = UrlUtil.fileToUrl(outputDir, true);
            mimeStream = new FileInputStream(mimeFile);
            String[] fileUrls = _mimeReader.extractMimePackage(mimeStream,
                    outputDirUrl);
            if (log.isDebugEnabled()) {
                log.debug("Extracted contents of MIME package from " + userInfo
                        + " to: " + Arrays.asList(fileUrls));
            }
            //getJMFFromMimeContents(fileUrls);
            sendJMFResponse(res, fileUrls);
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
    }

    /**
     * Sends a JMF Response message over the specified HTTP response. A comment
     * containing the file URLs will be built and inserted at
     * <code>JMF/Response/Notification/Comment</code>.
     * 
     * @param res
     *            the HTTP response
     * @param returnCode
     *            the JMF return code (JMF/Response/@ReturnCode
     * @param fileUrls
     *            the file URLs used to build a comment
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
     * @param res
     *            the HTTP response
     * @param returnCode
     *            the JMF return code (JMF/Response/@ReturnCode
     * @param comment
     *            the message that will be inserted into the Notification
     *            (JMF/Response/Notification/Comment)
     * @throws IOException
     */
    protected void sendJMFResponse(HttpServletResponse res, int returnCode,
            String comment) throws IOException {
        // Build JMF Response
        JDFJMF jmf = _factory.createJMF(); // JMF
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
     * Returns the refID of the SubmitQueueEntry or ReturnQueueEntry JMF message
     * in the MIME package.
     * @param contentFileUrls
     * @return
     */
    private String getJMFrefID(String[] contentFileUrls) {
        String refID = null;
        JDFJMF jmf = getJMFFromMimeContents(contentFileUrls);
        if (jmf != null) {
            refID = jmf.getMessage(0).getrefID();
        }
        return refID;            
    }
    
    /**
     * Returns the JMF message that was in the MIME package. Goes through the list
     * of files that were in the MIME package and parses the first file found that
     * has a <code>.jmf</code> suffix. 
     * @param contentFileUrls
     */
    private JDFJMF getJMFFromMimeContents(String[] contentFileUrls) {
        for(int i=0; i<contentFileUrls.length; i++) {
            if (contentFileUrls[i].endsWith(JMF_EXTENSION)) {
                return (new JDFParser().parseFile(contentFileUrls[i]).getJMFRoot());
            }
        }
        return null;        
    }

}
