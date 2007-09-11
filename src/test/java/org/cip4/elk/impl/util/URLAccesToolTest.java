/*
 * Created on Sep 10, 2004
 */
package org.cip4.elk.impl.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Random;

import org.apache.commons.io.CopyUtils;
import org.apache.commons.io.IOUtils;
import org.cip4.elk.ElkTestCase;

/**
 * @author Claes Buckwalter (clabu@itn.liu.se)
 */
public class URLAccesToolTest extends ElkTestCase {

    private byte[] _randomBytes;
    private File _tempFile;
    
    /**
     * Creates a temp file with random content
     */
    public void setUp() throws Exception {
        super.setUp();
        _tempFile = File.createTempFile("URLAccesToolTest", "txt");
        _tempFile.deleteOnExit();      
        FileOutputStream out = new FileOutputStream(_tempFile);        
        _randomBytes = new byte[32];
        new Random().nextBytes(_randomBytes);
        CopyUtils.copy(_randomBytes, out);        
        out.close();
    }
    
    /**
     * Tests that HTTP proxy host can be set/get
     */
    public void testProxyHost() {
        URLAccessTool ff = new URLAccessTool();
        String host = "proxy";
        ff.setProxyHost(host);
        assertEquals(host, ff.getProxyHost());
    }
    
    /**
     * Test that proxy port can be set/get
     */
    public void testProxyPort() {
        URLAccessTool ff = new URLAccessTool();
        String port = "8080";
        ff.setProxyPort(port);
        assertEquals(port, ff.getProxyPort());
    }

    /**
     * Tests that a URL can be used to resolve a file and the file cab be read.
     * @throws Exception
     */
    public void testGetUrlAsInputStream_file() throws Exception {
        URLAccessTool fu = new URLAccessTool();
        InputStream urlStream = fu.getURLAsInputStream(_tempFile.toURI().toURL());
        InputStream fileStream = new FileInputStream(_tempFile);
        assertTrue(IOUtils.contentEquals(urlStream, fileStream));
        urlStream.close();
        fileStream.close();
    }
    
    /**
     * Tests that relative file URLs can be resolved against a base URL.
     * @throws Exception
     */
    public void testBaseUrl() throws Exception {
        // Use parent directory of temp file as base URL
        URL baseUrl = _tempFile.getParentFile().toURI().toURL();
        URLAccessTool fu = new URLAccessTool(baseUrl.toString());
//        assertEquals(baseUrl.toExternalForm(), fu.getBaseUrl().toExternalForm());
        // Resolve temp file relative to base URL
        String tempFileName = _tempFile.getName();
        InputStream fuStream = fu.getURLAsInputStream(tempFileName);
        // Compare files
        InputStream fileStream = new FileInputStream(_tempFile);
        assertTrue(IOUtils.contentEquals(fuStream, fileStream));         
        IOUtils.closeQuietly(fuStream);
        IOUtils.closeQuietly(fileStream);
    }
    
    /**
     * Tests that a URL can be used to resolve a file and the file can be written.
     * @throws Exception
     */
    public void testGetUrlAsOutputStream_file() throws Exception {
        URLAccessTool fu = new URLAccessTool();
        // Creates a new temp file and writes the random bytes to it
        File tempFile2 = new File(_tempFile.getAbsolutePath() + "2");
        URL tempFile2Url = tempFile2.toURI().toURL();
        OutputStream fuOutStream = fu.getURLAsOutputStream(tempFile2Url);
        CopyUtils.copy(_randomBytes, fuOutStream);
        IOUtils.closeQuietly(fuOutStream);
        // Compare the new file with the
        InputStream fuStream = fu.getURLAsInputStream(tempFile2Url);
        InputStream fileStream = new FileInputStream(_tempFile);
        assertTrue(IOUtils.contentEquals(fuStream, fileStream));
        IOUtils.closeQuietly(fuStream);
        IOUtils.closeQuietly(fileStream);
    }
    
    
//    public void testWebdav() throws Exception {
//        System.getProperties().put("http.proxyHost", "proxy");
//        System.getProperties().put("http.proxyPort", "8080");
//        
//        String url = "webdav://guest:guest@cog.itn.liu.se:8080/webdav/clabu/webapps/elk/output/test.jdf";
//        URLAccessTool fu = new URLAccessTool();
//        fu.setProxyHost("proxy");
//        fu.setProxyPort("8080");
//        OutputStream out = fu.getURLAsOutputStream(url);
//        CopyUtils.copy("This is a test: "  + System.currentTimeMillis(), out);
//    }

    
    
}
