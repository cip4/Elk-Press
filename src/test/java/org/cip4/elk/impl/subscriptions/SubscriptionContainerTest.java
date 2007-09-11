/*
 * Created on 2005-apr-28
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cip4.elk.impl.subscriptions;
import java.util.Collection;

import org.cip4.elk.ElkTestCase;
import org.cip4.jdflib.core.ElementName;
import org.cip4.jdflib.jmf.JDFQuery;



/**
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: SubscriptionContainerTest.java,v 1.2 2005/11/08 15:59:33 buckwalter Exp $
 */
public class SubscriptionContainerTest extends ElkTestCase {

    SubscriptionContainer subs;
    /*
     * @see TestCase#setUp()
     */
    public void setUp() throws Exception {
        super.setUp();
        subs = new SubscriptionContainer();
    }

    public void testAddSubscription() {
        JDFQuery q = (JDFQuery) createJDFElement(ElementName.QUERY);
        subs.addSubscription(new SubscriptionImpl("dummID",q,"hcp://"));
        subs.addSubscription(new SubscriptionImpl("dummyID2",q,"GFD"));
        Collection c = subs.getSubscriptions();
        assertTrue(c.size() == 2);
        log.debug("Needs more testing.");
        log.debug("Finished testing Subscriptions.");
    }

    public void testRemoveSubscription() {
        //TODO Implement removeSubscription().
    }

    public void testRemoveSubscriptions() {
        //TODO Implement removeSubscriptions().
    }

    public void testGetSub() {
        //TODO Implement getSub().
    }

    public void testHasSubscriptionsAt() {
        //TODO Implement hasSubscriptionsAt().
    }

    public void testRemoveSubscriptionsOfType() {
        //TODO Implement removeSubscriptionsOfType().
    }

    public void testGetSubscriptions() throws InterruptedException {
        JDFQuery q = (JDFQuery) createJDFElement(ElementName.QUERY);
        q.setID("TestID");
        subs.addSubscription(new SubscriptionImpl("dummID",q,"hcp://"));
        subs.addSubscription(new SubscriptionImpl("dummyID2",q,"GFD"));
        Collection c = subs.getSubscriptions();
        assertTrue(c.size() == 2);
        subs.removeSubscription("hcp://",q.getID());
        Thread.sleep(1000);
        assertTrue(c.size() == 2);
        //Iterator it =  c.iterator();
        
        //it.remove();
        
        System.out.println(c);
        //assertTrue(c.size() == 2);
        log.debug("Needs more testing.");
        log.debug("Finished testing Subscriptions.");
        
        //TODO Implement getSubscriptions().
    }

}
