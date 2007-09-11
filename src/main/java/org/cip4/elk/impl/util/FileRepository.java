/*
 * Created on May 19, 2005
 */
package org.cip4.elk.impl.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import org.cip4.elk.Config;
import org.cip4.elk.lifecycle.Lifecycle;

/**
 * An implementation of <code>Repository</code> that stores files in a file
 * system.
 * 
 * TODO Implement caching
 * 
 * @author Claes Buckwalter (clabu@itn.liu.se)
 * @version $Id: FileRepository.java,v 1.2 2005/06/10 16:21:18 buckwalter Exp $
 */
public class FileRepository implements Repository, Lifecycle {

    protected static final String DEFAULT_FILE_EXTENSION = "data";

    protected static Logger log = Logger.getLogger(FileRepository.class
            .getName());

    private String _privateDirBaseUrl = null;
    private String _publicDirLocalBaseUrl = null;
    private String _publicDirPublicBaseUrl = null;
    private URLAccessTool _urlTool = null;
    private Map _contentTypeExtensionMap = new HashMap();
    private Map _extensionContentTypeMap = new HashMap();

    /**
     * Creates a new repository without any content-type mappings and base URL
     * configuration. See {@link FileRepository#FileRepository(Map)}for
     * documentation regarding base URLs.
     * 
     * @throws IOException
     */
    public FileRepository() throws IOException {
        this(new HashMap());
    }

    /**
     * Creates a new repository without configuring base URLs. All base URLs
     * will the value returned by
     * <code>System.getProperty("java.io.tmpdir")</code>. Base URLs can be
     * configured manually using the setters
     * {@link FileRepository#setPrivateDirBaseURL(String)},
     * {@link FileRepository#setPublicLocalBaseURL(String)}, and
     * {@link FileRepository#setPublicDirPublicBaseURL(String)}.
     * 
     * @param contentTypeMapping A mapping of content types to file extensions.
     *            Content types (e.g. <code>text/plain</code>) are keys; file
     *            extensions (e.g. <code>txt</code>) are values.
     * @throws IOException
     */
    public FileRepository(Map contentTypeMapping) throws IOException {
        this(contentTypeMapping,
                "file:" + System.getProperty("java.io.tmpdir"), "file:"

                + System.getProperty("java.io.tmpdir"), "file:"
                        + System.getProperty("java.io.tmpdir"));
        log.debug("Used system property 'java.io.tmpdir' for base URLs.");
    }

    /**
     * Creates a new repository that configures its base URLs using a
     * <code>Config</code>.
     * 
     * @param contentTypeMapping A mapping of content types to file extensions.
     *            Content types (e.g. <code>text/plain</code>) are keys; file
     *            extensions (e.g. <code>txt</code>) are values.
     * @param config the <code>Config</code> used for configuring base URLs
     * @throws IOException
     */
    public FileRepository(Map contentTypeMapping, Config config)
            throws IOException {
        this(contentTypeMapping, config.getJDFTempURL(), config
                .getLocalJDFOutputURL(), config.getJDFOutputURL());
    }

    /**
     * Creates a new repository.
     * 
     * @param contentTypeMapping A mapping of content types to file extensions.
     *            Content types (e.g. <code>text/plain</code>) are keys; file
     *            extensions (e.g. <code>txt</code>) are values.
     * @param privateDirBaseUrl the base URL to where private files are stored
     * @param publicDirLocalBaseUrl the local base URL to where files that are
     *            public are stored
     * @param publicDirPublicBaseUrl The public base URL to where files that are
     *            public are stored. This URL is in some implementation specific
     *            way mapped to the <code>publicLocalBaseUrl</code>.
     */
    public FileRepository(Map contentTypeMapping, String privateDirBaseUrl,
            String publicDirLocalBaseUrl, String publicDirPublicBaseUrl)
            throws IOException {
        setContentTypeMapping(contentTypeMapping);
        setPrivateDirBaseURL(privateDirBaseUrl);
        setPublicLocalBaseURL(publicDirLocalBaseUrl);
        setPublicDirPublicBaseURL(publicDirPublicBaseUrl);
        initFileSystem();
    }

    /**
     * 
     * @param contentTypeExtensionMap A mapping of content types to file
     *            extensions. Content types (e.g. <code>text/plain</code>)
     *            are keys; file extensions (e.g. <code>txt</code>) are
     *            values.
     */
    private void setContentTypeMapping(Map contentTypeExtensionMap) {
        // Create two way mapping of content types and file extensions
        Map extensionContentTypeMap = new Hashtable();
        for (Iterator i = contentTypeExtensionMap.keySet().iterator(); i
                .hasNext();) {
            Object key = i.next();
            extensionContentTypeMap.put(contentTypeExtensionMap.get(key), key);
        }
        // Set maps
        synchronized (_contentTypeExtensionMap) {
            synchronized (_extensionContentTypeMap) {
                _contentTypeExtensionMap = contentTypeExtensionMap;
                _extensionContentTypeMap = extensionContentTypeMap;
            }
        }
    }

    /**
     * Sets the local base URL to where files that are public are stored
     * 
     * @param publicDirLocalBaseUrl the local base URL to where files that are
     *            public are stored
     */
    public void setPublicLocalBaseURL(String publicDirLocalBaseUrl) {
        if (!publicDirLocalBaseUrl.endsWith("/")) {
            publicDirLocalBaseUrl += "/";
        }
        _publicDirLocalBaseUrl = publicDirLocalBaseUrl;
        log.info("Public repository local URL: " + _publicDirLocalBaseUrl);
    }

    /**
     * Sets the public base URL to where files that are public are stored. This
     * URL is in some implementation specific way external to this class mapped
     * to the <code>publicLocalBaseUrl</code> the local base URL to where
     * files that are public are stored
     * 
     * @param publicDirPublicBaseUrl the public base URL
     */
    public void setPublicDirPublicBaseURL(String publicDirPublicBaseUrl) {
        if (!publicDirPublicBaseUrl.endsWith("/")) {
            publicDirPublicBaseUrl += "/";
        }
        _publicDirPublicBaseUrl = publicDirPublicBaseUrl;
        log.info("Public repository public URL: " + _publicDirPublicBaseUrl);
    }

    /**
     * Sets the base URL to where private files are stored.
     * 
     * @param privateDirBaseUrl the base URL to where private files are stored
     */
    public void setPrivateDirBaseURL(String privateDirBaseUrl) {
        if (!privateDirBaseUrl.endsWith("/")) {
            privateDirBaseUrl += "/";
        }
        _privateDirBaseUrl = privateDirBaseUrl;
        log.info("Private repository URL: " + _privateDirBaseUrl);
    }

    /**
     * Initializes resources for accessing file systems using URLs.
     * 
     * @throws IOException
     */
    private void initFileSystem() throws IOException {
        _urlTool = new URLAccessTool();
    }

    /**
     * Adds the file located at the specified URL. This repository attempts to
     * determine the files content type automatically by examining the file's
     * filename extension.
     */
    public String addPrivateFile(String url) throws IOException {
        String fileExt = url.substring(url.lastIndexOf('.') + 1);
        String contentType = null;
        synchronized (_extensionContentTypeMap) {
            contentType = (String) _extensionContentTypeMap.get(fileExt);
        }
        return addPrivateFile(contentType, getFile(url));
    }

    public String addPrivateFile(String contentType, InputStream data) {
        return _privateDirBaseUrl
                + addFile(_privateDirBaseUrl, contentType, data);
    }

    /**
     * Adds the file located at the specified URL. This repository attempts to
     * determine the files content type automatically by examining the file's
     * filename extension.
     */
    public String addPublicFile(String url) throws IOException {
        String fileExt = url.substring(url.lastIndexOf('.') + 1);
        String contentType = null;
        synchronized (_extensionContentTypeMap) {
            contentType = (String) _extensionContentTypeMap.get(fileExt);
        }
        return addPublicFile(contentType, getFile(url));
    }

    public String addPublicFile(String contentType, InputStream data) {
        return _publicDirPublicBaseUrl
                + addFile(_publicDirLocalBaseUrl, contentType, data);
    }

    /**
     * Adds a file storing it at the location of the base URL.
     * 
     * @param baseUrl
     * @param contentType
     * @param data
     * @return the unique filename that was give to the added file (not the
     *         entire URL to the file)
     */
    private String addFile(String baseUrl, String contentType, InputStream data) {
        if (contentType == null) {
            contentType = CONTENT_TYPE_UNKNOWN;
        }
        String fileExtension = null;
        synchronized (_contentTypeExtensionMap) {
            fileExtension = (String) _contentTypeExtensionMap.get(contentType);
        }
        if (fileExtension == null) {
            fileExtension = DEFAULT_FILE_EXTENSION;
        }
        // Create unique filename
        String filename = RandomStringUtils.randomAlphanumeric(24) + "."
                + fileExtension;
        String url = baseUrl + filename;
        _urlTool.writeToURL(data, url);
        // TODO Handle if the file cannot be written
        return filename;
    }

    public InputStream getFile(String url) throws FileNotFoundException,
            IOException {
        // TODO Fetch the file and write it to cache
        // TODO Return a stream of the cached copy
        // TODO If it is a public file, use the local URL instead of the public
        // one
        return _urlTool.getURLAsInputStream(url);
    }

    public boolean updateFile(String url, InputStream data) {
        // TODO Update cached file or mark it as expired
        return _urlTool.writeToURL(data, url);
    }

    public OutputStream updateFile(String url) {
        OutputStream stream = null;
        try {
            stream = _urlTool.getURLAsOutputStream(url);
        } catch (IOException ioe) {
            log.error("Could not open OutputStream to file at URL: " + url);
        }
        return stream;
    }

    public boolean removeFile(String url) {
        return _urlTool.deleteURL(url);
    }

    //==================================================
    // Lifecycle interface
    //==================================================

    public void init() {
    }

    public void destroy() {
        // TODO Finish all active transactions
    }
}
