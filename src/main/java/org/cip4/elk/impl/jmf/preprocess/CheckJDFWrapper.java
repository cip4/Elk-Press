/*
 * Created on Jun 21, 2005
 */
package org.cip4.elk.impl.jmf.preprocess;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.cip4.jdflib.CheckJDF;
import org.cip4.jdflib.core.XMLDoc;
import org.cip4.jdflib.core.KElement.EnumValidationLevel;
import org.cip4.jdflib.jmf.JDFJMF;
import org.cip4.jdflib.node.JDFNode;
import org.jdom.Document;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

/**
 * A wrapper for easy validation with CheckJDF.
 * 
 * TODO Use caching to improve performance
 * 
 * @author Claes Buckwalter (clabu@itn.liu.se)
 */
public class CheckJDFWrapper {

    /**
     * Prevents instances from being created.
     */
    private CheckJDFWrapper() {
    }

    /**
     * Validates the JDF instance or JMF message read from the
     * <code>InputStream</code>
     * 
     * @param jdfIn
     *            the JDF instance or JMF message to parse
     * @param reportFile
     *            the absolute path to the file to write the validation report to 
     * @return true if the JDF was valid; false otherwise 
     * @throws IOException
     */
    public static boolean validate(InputStream jdfIn, File reportFile)
            throws IOException {
        // Write input stream to temp file
        File tempJdf = createTempFile(".jdf");
        try {
            FileOutputStream jdfOut = new FileOutputStream(tempJdf);
            IOUtils.copy(jdfIn, jdfOut);            
            return validate(tempJdf, new File("../testdata/schema/JDF.xsd"),
                    null, reportFile);

        } catch (IOException ioe) {
            System.err.println(ioe);
            return false;
        } finally {
            tempJdf.delete();
        }
    }

    /**
     * Validates a JDF instance or JMF message read from the specified
     * <code>String</code>
     * 
     * @param jdf
     *            the JDF instance or JMF message to parse
     * @return true if valid; false otherwise
     * @throws IOException
     */
    public static boolean validate(String jdf) throws IOException {
        File tempReport = createTempFile(".xml");
        try {
            return validate(jdf, tempReport);       
        } finally {
            tempReport.delete();
        }
    }
    
    /**
     * Validates a JDF instance or JMF message read from the specified
     * <code>String</code>
     * 
     * @param jdf       the JDF instance or JMF message to parse
     * @param report    a buffer to append the XML validation report to
     * @return true if valid; false otherwise
     */
    public static boolean validate(String jdf, final StringBuffer report) throws IOException {
        File tempReport = createTempFile(".xml");

        try {
            boolean result = validate(jdf, tempReport);            
            report.append(IOUtils.toString(new FileInputStream(tempReport)));
            return result; 
        } finally {
            tempReport.delete();
        }
    }    
    
    public static boolean validate(InputStream jdf, final StringBuffer report) throws IOException {
        File tempReport = createTempFile(".xml");
        try {
            boolean result = validate(jdf, tempReport);
            report.append(IOUtils.toString(new FileInputStream(tempReport)));
            return result; 
        } finally {
            tempReport.delete();
        }
    }
    
    public static boolean validate(File jdf, final StringBuffer report) throws IOException {
        File tempReport = createTempFile(".xml");
        try {
            boolean result = validate(jdf, tempReport);
            report.append(IOUtils.toString(new FileInputStream(tempReport)));
            return result; 
        } finally {
            tempReport.delete();
        }
    }
    
    /**
     * Validates the JDF instance or JMF message read from the
     * <code>String</code>
     * 
     * @param jdf
     *            the JDF instance or JMF message to parse
     * @param reportFile
     *            the absolute path to the file to write the validation report
     *            to
     * @throws IOException
     */
    public static boolean validate(String jdf, File reportFile)
            throws IOException {
        // Write input stream to temp file
        File tempJdf = createTempFile(".jdf");
        try {
            FileOutputStream jdfOut = new FileOutputStream(tempJdf);
            IOUtils.write(jdf, jdfOut);            
            return validate(tempJdf, new File("../testdata/schema/JDF.xsd"),
                    null, reportFile);
        } catch (IOException ioe) {
            System.err.println(ioe);
            return false;
        } finally {
            tempJdf.delete();
        }
    }

    /**
     * Validates the JDF instance or JMF message read from a file. A validation
     * report in XML format is written to a file. Both schema valid
     * 
     * @param jdfFile
     *            the JDF instance or JMF message to parse
     * @param reportFile
     *            the absolute path to the file to write the validation report
     *            to
     * @throws IOException
     */
    public static boolean validate(File jdfFile, File reportFile)
            throws IOException {
        return validate(jdfFile, new File("../testdata/schema/JDF.xsd"), null,
                reportFile);
    }

    /**
     * Validates the JDF instance or JMF message read from a file. A validation
     * report in XML format is written to a file.
     * 
     * @param jdf
     *            the JDF instance or JMF message to parse
     * @param schemaFile
     *            The schema file to use for schema validation. If
     *            <code>null</code> then schema validation is not performed.
     * @param devcap
     *            The device capabilities to use for validation. If
     *            <code>null</code> testing against device capabilities is not
     *            performed.
     * @param reportFile
     *            The file to write the validation XMl report to. If
     *            <code>null</code> no XML report is written.
     */
    public static boolean validate(JDFNode jdf, File schemaFile, JDFJMF devcap,
            File reportFile) throws IOException {
        if (jdf == null) {
            throw new IllegalArgumentException("JDFNode may not be null");
        }
        File jdfFile = createTempFile(".jdf");
        IOUtils.write(jdf.toXML(), new FileOutputStream(jdfFile));
        File devcapFile = null;
        if (devcap != null) {
            devcapFile = createTempFile(".xml");
            IOUtils.write(devcap.toXML(), new FileOutputStream(devcapFile));
        }
        return validate(jdfFile, schemaFile, devcapFile, reportFile);
    }

    /**
     * Validates the JDF instance or JMF message read from a file. A validation
     * report in XML format is written to a file.
     * 
     * @param jdfFile
     *            the JDF instance or JMF message to parse
     * @param schemaFile
     *            The schema file to use for schema validation. If
     *            <code>null</code> then schema validation is not performed.
     * @param devcapFile
     *            The device capabilities file to use for validation. If
     *            <code>null</code> testing against device capabilities is not
     *            performed.
     * @param reportFile
     *            The file to write the validation XMl report to. If
     *            <code>null</code> no XML report is written.
     * @return true if JDF file passed validatio; false otherwise
     * @throws IOException
     */
    public static boolean validate(File jdfFile, File schemaFile,
            File devcapFile, File reportFile) throws IOException {
        // JDF
        if (jdfFile == null) {
            throw new IllegalArgumentException("The JDF File may not be null");
        }        
        // Configure
        CheckJDF checkJDF = new CheckJDF();
        checkJDF.setPrint(false);
        checkJDF.bQuiet = true;
        checkJDF.setIgnorePrivate(true);
        checkJDF.level = EnumValidationLevel.Complete;
        checkJDF.bTiming = false;
        checkJDF.bValidate = true;
        if (devcapFile != null) {
            checkJDF.devCapFile = devcapFile.getCanonicalPath();            
        }
        if (schemaFile != null) {
            checkJDF.setJDFSchemaLocation(schemaFile);
            //XXX checkJDF.setJDFSchemaLocation(schemaFile.toURI().toURL().toExternalForm());
        }
        // Validate
        XMLDoc reportDoc = checkJDF.processSingleFile(jdfFile.getCanonicalPath());
        // Write report to file
        // TODO Only write report file when necessary
        if (reportFile != null) {
            reportDoc.write2File(reportFile.getCanonicalPath(), 3, true);
        }        
        
        String schemaResult = reportDoc
                .getRoot()
                .getXPathAttribute(
                        "/CheckOutput/TestFile/SchemaValidationOutput/@ValidationResult",
                        "false");        
        String properResult = reportDoc.getRoot().getXPathAttribute(
                "/CheckOutput/TestFile/CheckJDFOutput/@IsValid", "true");        
        return (schemaResult.equals("Valid") || schemaResult.equals("NotPerformed")) && properResult.equals("true");
    }

    /**
     * Calls CheckJDF using the specified command line.
     * 
     * @param commandLineArgs
     * @deprecated CheckJDF's validation settings can now relatively easily be
     *             configured by setting public members instead of using the
     *             command line.
     * @see #validate(File, File, File, File)
     */
    public static void validateCommandLine(String[] commandLineArgs) {
        CheckJDF checker = new CheckJDF();
        checker.setPrint(false);
        checker.validate(commandLineArgs, null);
        checker = null;
    }

    /**
     * Creates a temp file with a random 16 character name, excluding a suffix.
     * 
     * @param suffix
     *            the file suffix of the temp file
     * @return the temp file
     * @throws IOException
     */
    private static File createTempFile(String suffix) throws IOException {
        // Write input stream to temp file
        String fileName = RandomStringUtils.randomAlphanumeric(16);
        return File.createTempFile(fileName, suffix);
    }

    /**
     * Looks at a CheckJDF XML report to see if the validated JDF was valid.
     * 
     * @param reportFile
     *            the CheckJDF XML report
     * @return <code>true</code> if the JDF was valid; <code>false</code>
     *         otherwise
     * @throws Exception
     * @deprecated Validation calls now return a boolean
     */
    public static boolean isValid(File reportFile) throws Exception {
        return isValid(new FileReader(reportFile));
    }

    /**
     * Looks at a CheckJDF XML report to see if the validated JDF was valid.
     * 
     * @param reportString
     *            a string containing the CheckJDF XML report
     * @return <code>true</code> if the JDF was valid; <code>false</code>
     *         otherwise
     * @throws Exception
     * @deprecated Validation calls now return a boolean 
     */
    public static boolean isValid(String reportString) throws Exception {
        return isValid(new StringReader(reportString));
    }

    /**
     * Looks at a CheckJDF XML report to see if the validated JDF was valid.
     * 
     * @param reportReader
     *            a Reader that reads an CheckJDF XML report
     * @return <code>true</code> if the JDF was valid; <code>false</code>
     *         otherwise
     * @throws Exception
     * @deprecated Validation calls now return a boolean
     */
    public static boolean isValid(Reader reportReader) throws Exception {
        // Parse report
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(reportReader);
        // Get report's namespace
        Namespace n = doc.getRootElement().getNamespace();
        // Check for validation failure
        // TODO Verify that this check sufficient
        XPath xp = XPath
                .newInstance("(count(//*[@IsValid='false'])=0) and (count(//jdf:SchemaValidationOutput[not(*)])=1)");
        xp.addNamespace("jdf", n.getURI());
        Boolean passed = (Boolean) xp.selectSingleNode(doc);
        return passed.booleanValue();
    }

}
