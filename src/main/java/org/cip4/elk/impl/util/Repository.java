/*
 * Created on May 19, 2005
 */
package org.cip4.elk.impl.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A repository for storing and retrieving files using URLs. If a URL points to
 * a remote file the <code>Repository</code> handles the retrieval of the
 * file. A <code>Repository</code> may implement a caching mechanism that
 * caches local copies of remote files.
 * <p>
 * Files stored in a <code>Repository</code> can be made accessable from
 * outside of the repository using {@link #addPublicFile(String, InputStream)}.
 * When a public file is added to a <code>Repository</code> a URL is returned
 * that can be used to access the file without going through the
 * <code>Repository</code>; for example by a remote system that needs to
 * access a PDF file referenced in a JDF job ticket. It is up to the
 * <code>Repository</code> to in some implementation specific way make sure
 * that a file stored using {@link #addPublicFile(String, InputStream)}is
 * mapped to a publicly accessible URL. For example, a <code>Repository</code>
 * might store public files in a directory that is accessible through a web
 * server. The URL of the public file that the <code>Repository</code> returns
 * is the URL used to access the file through the web server.
 * </p>
 * 
 * @author Claes Buckwalter (clabu@itn.liu.se)
 * @version $Id: Repository.java,v 1.1 2005/06/08 14:31:50 buckwalter Exp $
 */
public interface Repository {
    
    public static final String CONTENT_TYPE_UNKNOWN = "unknown";

    /**
     * Gets an InputStream from the specified file. Used be components in Elk to
     * get the data of any file referenced by a URL in a JDF instance or JMF
     * message. The repository may fetch the file from the specified URL or
     * return a locally cached copy of the file.
     * <p>
     * This method does not automatically add the file to the repository, this
     * needs to be done manually by a call to
     * {@link #addPublicFile(String, InputStream)}or
     * {@link #addPrivateFile(String, InputStream)}.
     * </p>
     * <p>
     * TODO Should a call to this method automatically add the file to the
     * repository?
     * </p>
     * 
     * @param url the URL identifying the file to get
     * @return an InputStream from the file
     * @throws FileNotFoundException if the file does not exist
     * @throws IOException if the file could not be read
     */
    public InputStream getFile(String url) throws FileNotFoundException,
            IOException;

    /**
     * Adds a private file to the repository. A file that is private can only be
     * accessed through the repository.
     * 
     * @param contentType The file's content type, for example image/jpeg. If the content type is unknown, use {@link #CONTENT_TYPE_UNKNOWN}.
     * @param data the file's data
     * @return A URL that can be used to access the file through the repository.
     *         This URL does not necessarily map to a file in the local file
     *         system.
     */
    public String addPrivateFile(String contentType, InputStream data);

    /**
     * Adds the file located at the URL to the repository.
     * 
     * @param url the URL to the file to add
     * @return the private URL to the file in the repository
     * @throws IOException if the file could not be found
     */
    public String addPrivateFile(String url) throws IOException;

    /**
     * Adds a public file to the repository. A public file is accessible using
     * the returned URL without going through the repository. The URL can be
     * used in JDF instance and used by a remote system to access file.
     * 
     * @param contentType The file's content type, for example image/jpeg. If the content type is unknown, use {@link #CONTENT_TYPE_UNKNOWN}.
     * @param data the file's data
     * @return A public URL that can be used to access the file without going
     *         through the repository.
     */
    public String addPublicFile(String contentType, InputStream data);

    /**
     * Adds the file located at the URL to the repository.
     * 
     * @param url the URL to the file to add
     * @return the public URL to the file in the repository
     * @throws FileNotFoundException if the file could not be found
     */
    public String addPublicFile(String url) throws IOException;

    /**
     * Updates a file already available in the repository.
     * 
     * @param url the URL identifying the file to update
     * @param data the new file data
     * @return true if the file was updated; false if the file did not exist in
     *         the repository
     * @throws IOException if the the file could not be updated
     */
    public boolean updateFile(String url, InputStream data);

    /**
     * Updates a file already available in the repository.
     * 
     * @param url the URL identifying the file to update
     * @return an OutputStream to the file to update
     * @throws IOException if the the file could not be updated
     */    
    public OutputStream updateFile(String url);
    
    /**
     * Removes the file in the repository with the specified URL.
     * 
     * @param url the public or private URL of the file to remove
     * @return  true if the file was deleted; false otherwise
     */
    public boolean removeFile(String url);

}
