/*
 * Created on Sep 12, 2004
 */
package org.cip4.elk.impl.testtools.servlet;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.cip4.elk.Config;
import org.cip4.elk.impl.spring.ElkSpringConfiguration;

/**
 * This servlet is used for testing JMF clients that send MIME packages. The
 * difference between this servlet an its superclass
 * {@link org.cip4.elk.impl.testtools.servlet.MimeTestJMFServlet MimeTestJMFServlet}
 * is that the MIME package contents are extracted to the local JDF output
 * directory provided by the <code>Config</code> object, see
 * {@link org.cip4.elk.Config#getLocalJDFOutputURL()}). After successfully
 * parsing a MIME package the JMF Response element
 * <em>JMF/Response/Notification/Comment</em> that is sent back to the client
 * will contain the public URLs (see
 * {@link org.cip4.elk.Config#getJDFOutputURL()}) that correspond to the local
 * JDF output directory. This makes it possible for third parties to access the
 * extracted files and verify that they are correct.
 * <p>
 * This servlet configures itself using <a
 * href="http://www.springframework.org">Spring</a>. A bean of class 
 * <code>Config</code> with ID <code>deviceConfig</code> must be in the 
 * Spring configuration. 
 * </p>
 * 
 * @see org.cip4.elk.Config
 * @see org.cip4.elk.Config#getLocalJDFOutputURL()
 * @see org.cip4.elk.Config#getJDFOutputURL()
 * @see org.cip4.elk.impl.spring.ElkSpringConfiguration
 * @see <a href="http://www.springframework.org">Spring</a>
 * 
 * @author Claes Buckwalter (clabu@itn.liu.se)
 */
public class ElkMimeTestJMFServlet extends MimeTestJMFServlet {

    public static final String SERVLET_NAME = "Elk MIME Test JMFServlet";

    private String _springFactoryKey;
    private Config _config;

    public void init() throws ServletException {
        super.init();        
        configureSpringBeanFactory();
    }

    private void configureSpringBeanFactory() {
        _config = (Config) ElkSpringConfiguration.getBeanFactory().getBean("deviceConfig");
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
        String localOutputUrl = _config.getLocalJDFOutputURL();
        String publicOutputUrl = _config.getJDFOutputURL();
        if (log.isDebugEnabled()) {
            log.debug("Mapping local output URLs to public output URLs...");
            log.debug(localOutputUrl + " -> " + publicOutputUrl);
            log.debug("Input: " + Arrays.asList(fileUrls));
        }
        // Regexp that compensates for varying number of slashes in file URLs
        String regexp = localOutputUrl.replaceFirst("file:(/+)", "file:(//|/)");
        for (int i = 0; i < fileUrls.length; i++) {
            fileUrls[i] = fileUrls[i].replaceAll(regexp, publicOutputUrl);
        }
        String msg = "Extracted " + fileUrls.length + " from MIME package: "
                + Arrays.asList(fileUrls);
        if (log.isDebugEnabled()) {
            log.debug("Output: " + Arrays.asList(fileUrls));
        }
        sendJMFResponse(res, 0, msg);
    }
}
