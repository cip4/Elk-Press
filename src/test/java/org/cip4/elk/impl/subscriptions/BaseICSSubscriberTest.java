/*
 * Created on 2005-apr-27
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cip4.elk.impl.subscriptions;

import org.cip4.elk.ElkTestCase;
import org.cip4.elk.JDFElementFactory;
import org.cip4.jdflib.core.ElementName;
import org.cip4.jdflib.jmf.JDFQuery;
import org.cip4.jdflib.jmf.JDFSubscription;

/**
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: BaseICSSubscriberTest.java,v 1.2 2006/08/24 11:56:02 buckwalter Exp $
 */
public class BaseICSSubscriberTest extends ElkTestCase {

    /*
     * @see TestCase#setUp()
     */
    public void setUp() throws Exception {
        super.setUp();        
    }

    public final void testSubscribe() {
        JDFQuery query = (JDFQuery) JDFElementFactory.getInstance().createJDFElement(ElementName.QUERY);
        JDFQuery q2 = (JDFQuery) query.cloneNode(true);
        

        assertTrue(query.getOwnerDocument() == q2.getOwnerDocument());
        assertNull(q2.getSubscription());
                
        JDFSubscription sub = q2.appendSubscription();        
        int repeatStep = sub.getRepeatStep();
        log.debug("Repeat step defaults to: " + repeatStep);
        log.debug("Needs more testing.");
        log.debug("Finished testing BaseICSSubscriber");
        //TODO Implement subscribe().
    }

}
