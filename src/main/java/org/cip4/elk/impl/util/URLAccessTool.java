package org.cip4.elk.impl.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import org.apache.commons.io.CopyUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.VFS;
import org.apache.commons.vfs.impl.DefaultFileSystemManager;
import org.apache.commons.vfs.util.DelegatingFileSystemOptionsBuilder;
import org.apache.log4j.Logger;
import org.cip4.jdflib.core.JDFParser;
import org.cip4.jdflib.jmf.JDFJMF;
import org.cip4.jdflib.node.JDFNode;

/**
 * A file utility for accessing file systems. File are addressed using URLs. 
 * This class uses 
 * <a href="http://jakarta.apache.org/commons/sandbox/vfs">Jakarta Commmons VFS</a>.
 * See The support URL schemes are those supported by Jakarta Commons VFS, among
 * others:
 * <ul>
 *  <li>file</li>
 *  <li>http</li>
 *  <li>ftp</li>
 *  <li>webdav</li>
 * </ul>
 * See the <a href="http://jakarta.apache.org/commons/sandbox/vfs">Jakarta Commmons VFS</a>
 * web site for a complete list.
 * 
 * @author Claes Buckwalter (clabu@itn.liu.se)
 */
public class URLAccessTool {
    private FileSystemManager _fsManager;
    private FileSystemOptions _fsOptions;
    private DelegatingFileSystemOptionsBuilder _fsOptionsBuilder;
    private String _httpProxyHost;
    private String _httpProxyPort;
    private static Logger log;
    
    /**
     * Creates a new file utility for accessing file systems.
     */
    public URLAccessTool() {
        this(null);
    }
    
    /**
     * Creates a new file utility for accessing file systems. All URLs that are
     * relative will be resolved against the specified base URL.
     * @param baseUrl the base URL to resolve relative URLs against
     */
    public URLAccessTool(String baseUrl) {
        log = Logger.getLogger(this.getClass().getName());
        try {
            _fsManager = VFS.getManager();
            // Configures base URL
            setBaseUrl(baseUrl);            
            _fsOptionsBuilder = new DelegatingFileSystemOptionsBuilder(_fsManager);
            _fsOptions = new FileSystemOptions();
        } catch (FileSystemException fse) {
            log.error("Could not instantiate FileFetcher because the file system could not be configured: " + fse, fse);
            throw new IllegalArgumentException("Could not instantiate FileFetcher because the file system could not be configured: " + fse);
        }
    }

    /**
     * Gets the base URL used to resolve relative paths.
     * @return
     */
    public URL getBaseUrl() {
        URL baseUrl = null;
        if (_fsManager instanceof DefaultFileSystemManager) {
            try {
                baseUrl = ((DefaultFileSystemManager)_fsManager).getBaseFile().getURL(); 
            } catch (FileSystemException fse) {
                log.error("Could not get base URL for file system: " + fse);
            }
        }
        return baseUrl;
    }
    
    /**
     * Sets the base URL used to resolve relative paths.
     * @param baseUrl
     */
    public void setBaseUrl(String baseUrl) {
        try {
            if (baseUrl != null && _fsManager instanceof DefaultFileSystemManager) {
                FileObject baseFile = _fsManager.resolveFile(baseUrl);
                ((DefaultFileSystemManager)_fsManager).setBaseFile(baseFile);
            }
        } catch (FileSystemException fse) {
            log.error("Could not set base URL '" + baseUrl + 
                    "' for file system: " + fse);
        }
    }

    public synchronized void setProxyHost(String proxyHost) {
        try {
            _fsOptionsBuilder.setConfigString(_fsOptions, "http", "proxyHost", proxyHost);
            _httpProxyHost = proxyHost;
        } catch (FileSystemException fse) {
            log.error("Could not set proxy host to '" + proxyHost + "': " + proxyHost, fse);
        } 
    }
    
    public synchronized String getProxyHost() {
        return _httpProxyHost;
    }
    
    public synchronized  void setProxyPort(String proxyPort) {        
        try {
            _fsOptionsBuilder.setConfigString(_fsOptions, "http", "proxyPort", proxyPort);
            _httpProxyPort = proxyPort;
        } catch (FileSystemException fse) {
            log.error("Could not set proxy port to '" + proxyPort + "': " + proxyPort, fse);
        }        
    }
    
    public synchronized String getProxyPort() {
        return _httpProxyPort;
    }
    
    /**
     * Gets an InputStream from the resource that the specified URL points to.
     * @param url   the URL to get the as an input stream
     * @return an InputStream from the URL; null if the InputStream could not be created 
     */
    public InputStream getURLAsInputStream(URL url) {
        InputStream in = null;
        try {
            in = getURLInputStream(url.toString()); 
        } catch(FileSystemException fse) {
            log.error("Could not get input stream for URL '" + url + "': " + fse, fse);
        }
        return in;
    }
        
    /**
     * Gets an InputStream from the resource that the specified URL points to.
     * @param url   a String that represents the URL to get as an input stream
     * @return an InputStream from the URL; null if the InputStream could not be created 
     */
    public InputStream getURLAsInputStream(String url) {
        try {
            return getURLInputStream(url);
        } catch(FileSystemException fse) {
            log.error("Could not get stream from URL '" + url + "': " + fse, fse);
            return null;
        }
    }

    private InputStream getURLInputStream(String url) throws FileSystemException {
        return _fsManager.resolveFile(url, _fsOptions).getContent().getInputStream();        
    }
    
    /**
     * Gets an OutputStream to the resource that the specified URL points to.
     * @param url   an URL to get as an input stream
     * @return an OutputStream to the URL; null if the OutputStream could not be created
     * @throws IOException if there was an IO error 
     */
    public OutputStream getURLAsOutputStream(URL url) throws IOException { 
        return getURLAsOutputStream(url.toString());
    }

    /**
     * Gets an OutputStream to the resource that the specified URL points to.
     * @param url   a String that represents the URL to get as an input stream
     * @return an OutputStream to the URL; null if the OutputStream could not be created 
     * @throws IOException if there was an IO error
     */
    public OutputStream getURLAsOutputStream(String url) throws IOException { 
        return _fsManager.resolveFile(url, _fsOptions).getContent().getOutputStream();
    }
    
    public JDFNode getURLAsJDF(URL url) {
        return getURLAsJDF(url.toString());
    }
    
    public JDFNode getURLAsJDF(String url) {
        InputStream inStream = null;
        try {
            inStream = getURLInputStream(url);
            JDFNode jdf = new JDFParser().parseStream(inStream).getJDFRoot();
            return jdf;
        } catch(IOException ioe) {
            log.error("Could not load JDF from URL '" + url + "': " + ioe, ioe);
            return null;
        } finally {
            IOUtils.closeQuietly(inStream);
        }    
    }
    
    public JDFJMF getURLAsJMF(URL url) {
        return getURLAsJMF(url.toString());
    }
    
    public JDFJMF getURLAsJMF(String url) {
        InputStream inStream = null;
        try {
            inStream = getURLInputStream(url);
            JDFJMF jmf = new JDFParser().parseStream(inStream).getJMFRoot();
            return jmf;
        } catch(IOException ioe) {
            log.error("Could not get load JMF from from URL '" + url + "': " + ioe, ioe);
            return null;
        } finally {
            IOUtils.closeQuietly(inStream);
        }    
    }
    
    
    /**
     * Writes the contents of a string to an URL.
     * Supported URL schemes are:
     * <ul>
     * <li>file</li>
     * <li>http</li>
     * <li>ftp</li>
     * <li>webdav</li>
     * </ul>
     * @param inputData the data to write to the URL
     * @param outputUrl the URL
     */
    public void writeToURL(String inputData, String outputUrl) {
        OutputStream outStream = null;
        try {
            outStream = getURLAsOutputStream(outputUrl.toString());
            CopyUtils.copy(inputData, outStream);
        } catch (IOException ioe) {
            log.error("Could write data to URL '" + outputUrl + "': " + ioe, ioe); 
        } finally {
            IOUtils.closeQuietly(outStream);
        }
    }
    
    /**
     * Writes the contents of an input stream to an URL.
     * @param inStream  the InputStream of data to write to the URL
     * @param outputUrl the URL to write to
     * @return true if the stream could be written; false otherwise
     */
    public boolean writeToURL(InputStream inStream, String outputUrl) {
        boolean success = false;
        OutputStream outStream = null;
        try {
            outStream = getURLAsOutputStream(outputUrl.toString());
            CopyUtils.copy(inStream, outStream);
            success = true;
        } catch (IOException ioe) {
            log.error("Could not write data to URL '" + outputUrl + "': " + ioe, ioe); 
        } finally {
            IOUtils.closeQuietly(outStream);
            IOUtils.closeQuietly(inStream);
        }
        return success;
    }

    /**
     * Copies the content of one URL to another URL. 
     * @param inputUrl
     * @param outputUrl
     */
    public void copyToURL(String inputUrl, String outputUrl) {
        OutputStream outStream = null;
        InputStream inStream = null;
        try {
            outStream = getURLAsOutputStream(outputUrl.toString());
            inStream = getURLInputStream(inputUrl.toString());
            CopyUtils.copy(inStream, outStream);
        } catch (IOException ioe) {
            log.error("Could not copy data from URL '" + inputUrl + "' to URL '" + outputUrl + "': " + ioe, ioe); 
        } finally {
            IOUtils.closeQuietly(outStream);
            IOUtils.closeQuietly(inStream);
        }  
    }
    
    /**
     * Deletes the resource locate at the specified URL.
     * @param url   the URL to the resource to delete
     * @return  true if deletion succeeded; false otherwise
     */
    public boolean deleteURL(String url) {
        boolean success = false;
        try {
            _fsManager.resolveFile(url, _fsOptions).delete();
            success = true;
        } catch (FileSystemException fse) {
            log.error("The resource located at URL '" + url + "' could not be deleted.");
        }
        return success;
    }
    
}
