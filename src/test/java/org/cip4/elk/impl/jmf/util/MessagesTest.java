/*
 * Created on 2005-apr-22
 */
package org.cip4.elk.impl.jmf.util;

import org.cip4.elk.ElkTestCase;
import org.cip4.jdflib.jmf.JDFAcknowledge;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.resource.JDFNotification;

/**
 * A simple test class to view the results of applying the methods from the
 * Messages class.
 * 
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: MessagesTest.java,v 1.4 2005/09/10 11:24:56 ola.stering Exp $
 */
public class MessagesTest extends ElkTestCase {

    JDFResponse r;
    JDFAcknowledge a;

    public void setUp() {
        JDFResponse r = Messages.createResponse("MusseMachine", "KnownDecices");
        JDFAcknowledge a = Messages.createAcknowledge(r);
        this.r = r;
        this.a = a;
        log.info("Testing Messages utils...");
    }

    public void testAppendNotification() {

        Messages.appendNotification(r, JDFNotification.EnumClass.Warning, 4,
            "HERE ARE SOME BUNS!");
        JDFAcknowledge a = Messages.createAcknowledge(r);
        assertTrue(a.getChildElementArray().length == r.getChildElementArray().length);
        // Make sure the response and the ack have the same length.
    }

    public void testCreateAcknowledge() {
        JDFAcknowledge a = Messages.createAcknowledge(r);

        // TEST CREATE RESPONSE AND CREATE ACKNOWLEDGE
        JDFResponse r = Messages.createResponse("testID", "testType");
        a = Messages.createAcknowledge(r);
        assertTrue(a.getChildElementArray().length == 0); // Check if the array is
        // of length 0 and not
        // null. No child elements
        // are appended
        assertTrue(a.getrefID() == r.getrefID());
    }

    public void testCreateResponse() {}

    public void testCreateJMFMessage() {
        // TEST CREATE JMF
        // JDFJMF j = (JDFJMF) Messages.createJMFMessage(a, "BoMachine");
    }

}
