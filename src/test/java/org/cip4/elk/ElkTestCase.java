/*
 * Created on Sep 15, 2004
 */
package org.cip4.elk;

import java.io.InputStream;
import java.net.URL;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.cip4.jdflib.core.JDFElement;
import org.cip4.jdflib.core.JDFParser;

/**
 * @author Claes Buckwalter (clabu@itn.liu.se)
 * @version $Id: ElkTestCase.java,v 1.5 2006/08/24 11:56:02 buckwalter Exp $
 */
public abstract class ElkTestCase extends TestCase {
    public static String _testDataPath = "data/";
    public static String _jdfFilesPath = "data/jdf/";

    /**
     * Logger for logging messages
     */
    protected static Logger log = Logger.getLogger(ElkTestCase.class.getName());

    /**
     * Configures logger
     */
    public void setUp() throws Exception {
        log = Logger.getLogger(this.getClass().getName());
    }

    /**
     * Gets a URL to a resource in the classpath
     * 
     * @param resourcePath
     * @return the resources URL
     */
    public URL getResourceAsURL(String resourcePath) {
        return this.getClass().getClassLoader().getResource(resourcePath);
    }

    /**
     * Gets an input stream from a resource in the classpath
     * 
     * @param resourcePath
     * @return the resources input stream
     */
    public InputStream getResourceAsStream(String resourcePath)
            throws IllegalArgumentException {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(
            resourcePath);
        if (is == null) {
            String msg = "The resource " + resourcePath
                    + " is not in the class path.";
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        return is;
    }

    public JDFElement getResourceAsJDF(String resourcePath) throws Exception {
        InputStream stream = getResourceAsStream(resourcePath);
        if (stream == null) {
            log.error("The resource " + resourcePath
                    + " is not in the class path");
            throw new IllegalArgumentException("The resource " + resourcePath
                    + " is not in the class path");
        }
        return (JDFElement) new JDFParser().parseStream(stream).getRoot();
    }

    public JDFElement createJDFElement(String elementName) {
        return JDFElementFactory.getInstance().createJDFElement(elementName);
    }
}
