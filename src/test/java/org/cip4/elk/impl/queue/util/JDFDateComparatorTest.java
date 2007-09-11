/*
 * Created on Sep 21, 2004
 */
package org.cip4.elk.impl.queue.util;

import org.cip4.elk.ElkTestCase;
import org.cip4.jdflib.util.JDFDate;

/**
 * @author Claes Buckwalter (clabu@itn.liu.se)
 */
public class JDFDateComparatorTest extends ElkTestCase {

    /*
     * @see ElkTestCase#setUp()
     */
    public void setUp() throws Exception {
        super.setUp();
    }
    
    public void testJDFDateIsEarlier() throws Exception {        
        JDFDate d1 = new JDFDate("2004-09-15T01:00:00+02:00");
        JDFDate d2 = new JDFDate("2004-09-15T01:00:00+01:00");
        assertTrue(d1.isEarlier(d2));
    }

    public void testJDFDateIsLater() throws Exception {        
        JDFDate d1 = new JDFDate("2004-09-15T01:00:00+02:00");
        JDFDate d2 = new JDFDate("2004-09-15T01:00:00+01:00");
        assertTrue(d2.isLater(d1));
    }

    public void testJDFDateIsEquals() throws Exception {        
        JDFDate d1;
        JDFDate d2;
        d1 = new JDFDate("2004-09-15T01:00:00+02:00");
        d2 = new JDFDate("2004-09-15T01:00:00+02:00");
        assertFalse(d1.isEarlier(d2));
        assertFalse(d1.isLater(d2));               
        assertTrue(d1.getTimeInMillis() == d2.getTimeInMillis());
        d1 = new JDFDate("2004-09-15T01:00:00+01:00");
        d2 = new JDFDate("2004-09-15T02:00:00+02:00");                
        assertFalse(d1.isEarlier(d2));
        assertFalse(d1.isLater(d2));
        assertTrue(d1.getTimeInMillis() == d2.getTimeInMillis());
    }

}
