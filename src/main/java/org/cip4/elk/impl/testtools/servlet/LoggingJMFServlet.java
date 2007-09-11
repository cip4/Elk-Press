package org.cip4.elk.impl.testtools.servlet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cip4.elk.jmf.servlet.JMFServlet;

/**
 * This servlet that logs received JMF messages, JDF documents and MIME messages
 * to a directory. Useful if you want to test a JMF client. The directory to log
 * JMF messages to is specified either as a <em>context init parameter</em> or
 * a <em>servlet init parameter</em>. The parameter's name is
 * <code>log.dir</code> and its value should be an absolute path or a path
 * relative to the web application's directory. If the init parameter is
 * unspecified then the servlet context's temporary directory will be used (
 * <code>ServletContext.getAttribute("javax.servlet.context.tempdir")</code>).
 * <p>
 * Each logged file will have a filename that uses the pattern:
 * <code>{remote address}_{current time in millis}</code>.{jmf|jdf|mime|dat}. The
 * file suffix <code>dat</code> is used for files of unknown content-type.
 * </p>
 * @todo Validation of JDF, JMF, MIME
 * @todo Return a valid JMF response.
 * 
 * @author Claes Buckwalter (clabu@itn.liu.se)
 * @version $Id: LoggingJMFServlet.java,v 1.3 2005/09/11 18:42:45 buckwalter Exp $
 */
public class LoggingJMFServlet extends JMFServlet {

    public static final String SERVLET_NAME = "Elk Logging JMF Servlet";

    public static final String INIT_PARAM_LOG_DIR = "log.dir";

    protected File _logDir = null;

    public void init() throws ServletException {
        super.init();
        // Get context init parameters
        String logDirPath = getServletContext()
                .getInitParameter(INIT_PARAM_LOG_DIR);
        // Get servlet init parameters
        Enumeration e = getInitParameterNames();
        while (e.hasMoreElements()) {
            String paramName = (String) e.nextElement();
            if (paramName.equals(INIT_PARAM_LOG_DIR)) {
                logDirPath = getInitParameter(paramName);
            } else {
                String msg = "Unknown servlet init parameter: " + paramName
                        + " = " + getInitParameter(paramName);
                log.warn(msg);
                log(msg);
            }
        }
        configureLogDir(logDirPath);
    }

    public String getServletName() {
        return SERVLET_NAME;
    }

    /**
     * Configures the directory where incoming JMF messages and JDF files are
     * logged.
     * 
     * @param logDirPath
     *            the path to the directory to log JMF messages
     */
    private void configureLogDir(String logDirPath) {
        // Try provided value
        File logDir = null;
        if (logDirPath != null && logDirPath.length() != 0) {
            logDir = new File(logDirPath);
            if (!logDir.exists() || !logDir.canWrite()) {
                logDir = null;
                log.warn("The configured logging directory does not exist or"
                        + "was incorrect in some other way.");
            }
        } else {
            log
                    .warn("No logging directory was configured. Default will be used.");
            logDir = (File) getServletContext().getAttribute(
                    "javax.servlet.context.tempdir");
            if (logDir == null) {
                logDir = new File(System.getProperty("java.io.tmpdir"));
            }
        }

        log.info("Incoming JMF/JDF will be logged to the following logging "
                + "directory: " + logDir.getAbsolutePath());
        _logDir = logDir;
    }

    /**
     * Handles JDF.
     * 
     * @param req
     * @param res
     */
    public void processJDF(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        writeRequestBodyToLogDir(req, res, generateFileName(req)
                + JDF_EXTENSION);
    }

    /**
     * Handles JMF.
     * 
     * @param req
     * @param res
     */
    public void processJMF(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        writeRequestBodyToLogDir(req, res, generateFileName(req)
                + JMF_EXTENSION);
    }

    /**
     * Handles MIME packages.
     * 
     * @param req
     * @param res
     */
    public void processMime(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        writeRequestBodyToLogDir(req, res, generateFileName(req) + ".mime");
        //TODO Use different extensions for JDF and JMF MIME
    }

    /**
     * Handles MIME packages.
     * 
     * @param req
     * @param res
     */
    public void processOther(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        writeRequestBodyToLogDir(req, res, generateFileName(req) + ".dat");
    }

    /**
     * Writes the request to the specified file. If the file already exists an
     * <code>IOException</code> is thrown and the file will not be
     * overwritten.
     * 
     * @param req
     * @param res
     * @param outputFile
     * @throws IOException
     *             if the file already exists
     */
    protected void writeRequestBodyToLogDir(HttpServletRequest req,
            HttpServletResponse res, String outputFile) throws IOException {
        String reqInfo = "Request from " + req.getHeader("User-Agent") + " @ "
                + req.getRemoteHost() + " (" + req.getRemoteAddr() + ")";

        File file = new File(_logDir, outputFile);
        if (file.exists()) {
            if (log.isInfoEnabled()) {
                log.info(reqInfo + " will not be logged to file. Output file '"
                        + file.getAbsolutePath()
                        + "' already exist and will not be overwritten.");
            }
            logEntireRequest(req);
        } else {
            OutputStream out = new FileOutputStream(new File(_logDir,
                    outputFile));
            copy(req.getInputStream(), out);
            out.close();
            if (file.exists()) {
                log
                        .info(reqInfo + " logged to file: "
                                + file.getAbsolutePath());
            }
        }
        res.setStatus(HttpServletResponse.SC_OK);
    }

    /**
     * Logs the request headers and the body to the logger if the body is not a
     * MIME package.
     * 
     * @param req
     * @throws IOException
     */
    protected void logEntireRequest(HttpServletRequest req) throws IOException {
        if (log.isDebugEnabled()
                && req.getContentType().indexOf(MIME_CONTENT_TYPE) == -1) {
            Enumeration e = req.getHeaderNames();
            StringBuffer msg = new StringBuffer("Request from ");
            msg.append(req.getHeader("User-Agent")).append(" @ ");
            msg.append(req.getRemoteHost()).append(" (");
            msg.append(req.getRemoteAddr()).append("). ");
            msg.append(" Request headers:\n");
            while (e.hasMoreElements()) {
                String headerName = (String) e.nextElement();
                msg.append("   ").append(headerName).append(": ");
                msg.append(req.getHeader(headerName)).append("\n");
            }
            msg.append(" Request body:\n");
            msg.append(convertToString(req.getInputStream()));
            log.debug(msg);
        }
    }

    /**
     * Converts an <code>InputStream</code> to a <code>String</code>. This
     * code was extracted from <code>IOUtils</code> and <code>CopyUtils</code>
     * of <a href="http://jakarta.apache.org/commons/io/">Jakarta Commons IO
     * </a>.
     * 
     * @param inputStream
     *            the <code>InputStream</code> to convert
     * @return the contents of the stream as a <code>String</code>
     * @throws IOException
     *             In case of an I/O problem
     */
    protected static String convertToString(InputStream inputStream)
            throws IOException {
        Reader input = new InputStreamReader(inputStream);
        StringWriter output = new StringWriter();
        char[] buffer = new char[4096];
        int count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return output.toString();
    }

    /**
     * This code was extracted from <code>CopyUtils</code> of <a
     * href="http://jakarta.apache.org/commons/io/">Jakarta Commons IO </a>.
     * Copy bytes from an <code>InputStream</code> to an
     * <code>OutputStream</code>.
     * 
     * @param input
     *            the <code>InputStream</code> to read from
     * @param output
     *            the <code>OutputStream</code> to write to
     * @return the number of bytes copied
     * @throws IOException
     *             In case of an I/O problem
     */
    private static int copy(InputStream input, OutputStream output)
            throws IOException {
        byte[] buffer = new byte[4096];
        int count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
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
}
