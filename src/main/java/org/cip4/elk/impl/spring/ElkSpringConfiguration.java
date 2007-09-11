/*
 * Created on May 23, 2005
 */
package org.cip4.elk.impl.spring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.access.BeanFactoryLocator;
import org.springframework.beans.factory.access.BeanFactoryReference;
import org.springframework.beans.factory.access.SingletonBeanFactoryLocator;

/**
 * This factory class the configures a <a
 * href="http://www.springframework.com">Spring </a>
 * <code>SingletonBeanFactory</code> that should be used by the components in
 * the Elk reference implementation to obtain references to the beans defined in
 * the Elk reference implementation's Spring configuration file.
 * <p>
 * A reference to a Spring <code>SingletonBeanFactory</code> is retrieved
 * using a key. The key points to a <code>SingletonBeanFactory</code>
 * configured in the file <code>beanRefFactory.xml</code> located in the
 * classpath root. The <code>SingletonBeanFactory</code>'s configuration in
 * turn points to a Spring configuration file that is used to configure the
 * beans used by Elk. See <a
 * href="http://www.springframework.org/docs/api/org/springframework/beans/factory/access/SingletonBeanFactoryLocator.html">Spring's
 * Reference Documentation, section 3.16.1 </a> and <a
 * href="http://www.springframework.org/docs/api/org/springframework/beans/factory/access/SingletonBeanFactoryLocator.html">Spring's
 * API Documentation </a> for details.
 * </p>
 * <p>
 * If each Elk component was to create its own Spring <code>BeanFactory</code>
 * instance then each component would also have its own instances of all the
 * beans defined in the Spring configuration file. This means that there would
 * be several instances of a bean even though it was defined as being a
 * singleton in the Spring configuration file. Using a
 * <code>SingletonBeanFactory</code> solves this problem.
 * </p>
 * 
 * @see <a href="http://www.springframework.com">Spring </a>
 * @author Claes Buckwalter (clabu@itn.liu.se)
 * @version $Id: ElkSpringConfiguration.java,v 1.1 2005/05/30 13:43:50 buckwalter Exp $
 */
public class ElkSpringConfiguration {

    private static Log log = LogFactory.getLog(ElkSpringConfiguration.class);

    /**
     * The key to the singleton Spring factory used by this class and its
     * subclasses to get bean references.
     */
    public static final String SPRING_FACTORY_KEY = "org.cip4.elk.impl";

    private ElkSpringConfiguration() {
    }

    /**
     * Returns a reference to Elk Spring <code>BeanFactory</code>.
     * 
     * @return a BeanFacotry for obtaining references to Elk's components
     */
    public static BeanFactory getBeanFactory() {
        return getBeanFactory(SPRING_FACTORY_KEY);
    }

    /**
     * Returns a reference to the specified Spring <code>BeanFactory</code>.
     * Normally the default method {@link #getBeanFactory()}should be used
     * instead.
     * 
     * @param springFactoryKey the BeanFactory to get a reference to
     * @return a BeanFacotry for obtaining references to Elk's components
     */
    protected static BeanFactory getBeanFactory(String springFactoryKey) {
        // Get Spring factory
        log.debug("Using Spring SingletonBeanFactory with key: "
                + springFactoryKey);
        BeanFactoryLocator bfl = SingletonBeanFactoryLocator.getInstance();
        BeanFactoryReference bf = bfl.useBeanFactory(springFactoryKey);
        return bf.getFactory();
    }
}
