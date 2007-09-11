/*
 * Created on Sep 28, 2004
 */
package org.cip4.elk.impl.jmf.mime;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import org.cip4.elk.ElkTestCase;
import org.cip4.elk.impl.mime.MimePackageException;
import org.cip4.elk.impl.mime.MimeReader;
import org.cip4.jdflib.util.UrlUtil;

/**
 * @author Claes Buckwalter (clabu@itn.liu.se)
 * @version $Id$
 */
public class MimeReaderTest extends ElkTestCase {

    /*
     * @see ElkTestCase#setUp()
     */
    public void setUp() throws Exception {
        super.setUp();
    }


    /**
     * Extracts a JMF and a JDF from a MIME package.
     */
    public void testParseMime_JMF_JDF() throws IOException, MimePackageException, URISyntaxException {
        // Setup path to MIME package
        String mimeResource = _testDataPath + "MimeReaderTest/SubmitQueueEntry.jmf_Approval.jdf.mjm";        
        File mimeFile = new File(new URI(getResourceAsURL(mimeResource).toExternalForm()));
        log.debug("MIME file: " + mimeFile.getAbsolutePath());
        assertTrue(mimeFile.exists());                    
        // Setup output dir
        File outputDir = File.createTempFile("MimeReaderTest", "-output");
        assertTrue(outputDir.delete());
        assertTrue(outputDir.mkdir());
        outputDir.deleteOnExit();
        assertTrue(outputDir.exists());
        // Extract files form MIME package
        InputStream mimeStream = getResourceAsStream(mimeResource);
        MimeReader mimeReader = new MimeReader();
        String[] fileUrls = mimeReader.extractMimePackage(mimeStream,
            UrlUtil.fileToUrl(outputDir, true));
        log.debug("Extracted files: " + Arrays.asList(fileUrls).toString());
        assertTrue(fileUrls.length == 2);
        for (int i=0; i<fileUrls.length; i++) {
            File mimePart = new File(new URI(fileUrls[i]));
            assertTrue(mimePart.exists());
            mimePart.deleteOnExit();
        }
    }

    /**
     * Extracts a JDF plus 3 PDF files from a MIME package.
     */
    public void testParseMime_JDF_3xPDF() throws IOException, MimePackageException, URISyntaxException {
        // Setup path to MIME package
        String mimeResource = _testDataPath + "MimeReaderTest/Approval.jdf_file1.pdf_file2.pdf_file3.pdf.mjd";
        File mimeFile = new File(new URI(getResourceAsURL(mimeResource).toExternalForm()));
        log.debug("MIME file: " + mimeFile.getAbsolutePath());
        assertTrue(mimeFile.exists());
        // Setup output dir
        File outputDir = File.createTempFile("MimeReaderTest", "-output");
        assertTrue(outputDir.delete());
        assertTrue(outputDir.mkdir());
        outputDir.deleteOnExit();
        assertTrue(outputDir.exists());
        // Extract files from MIME package
        InputStream mimeStream = getResourceAsStream(mimeResource);
        MimeReader mimeReader = new MimeReader();
        String[] fileUrls = mimeReader.extractMimePackage(mimeStream,
            UrlUtil.fileToUrl(outputDir, true));
        log.debug("Extracted files: " + Arrays.asList(fileUrls).toString());
        assertTrue(fileUrls.length == 4);
        for (int i=0; i<fileUrls.length; i++) {
            File mimePart = new File(new URI(fileUrls[i]));
            assertTrue(mimePart.exists());
            mimePart.deleteOnExit();
        }        
    }

}
