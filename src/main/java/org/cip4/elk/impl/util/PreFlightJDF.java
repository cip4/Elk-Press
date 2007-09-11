/**
 * 2005-sep-09
 */
package org.cip4.elk.impl.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cip4.elk.JDFElementFactory;
import org.cip4.jdflib.auto.JDFAutoNotification.EnumClass;
import org.cip4.jdflib.core.ElementName;
//import org.cip4.jdflib.core.JDFConstants;
import org.cip4.jdflib.core.JDFElement;
import org.cip4.jdflib.core.JDFResourceLink;
import org.cip4.jdflib.core.JDFElement.EnumNodeStatus;
import org.cip4.jdflib.datatypes.JDFAttributeMap;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.node.JDFNode;
import org.cip4.jdflib.resource.JDFNotification;

/**
 * A class that mainly consists of utility methods to check if a JDFNode is
 * executable or not.
 * 
 * NOTE: The class may not return correctly with JDFLib-J build 23, because of a
 * probable bug. Although a patched build 23 will be downloadable on the Elk web
 * site shortly.
 * 
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: PreFlightJDF.java,v 1.4 2006/08/15 09:09:16 prosi Exp $
 * @deprecated - functionality moved to JDFPreprocessor, use it instead
 */
public class PreFlightJDF {

    private static Logger log = Logger.getLogger(PreFlightJDF.class.getName());

    /**
     * Checks if the <code>JDFNode</code> is ready to be executed. The method
     * checks the following:
     * <ul>
     * <li><em>JDF/Status</em> (also checks <em>JDF/StatusPool</em>) is '<em>Waiting</em>'.
     * </li>
     * <li><code>JDFNode</code>'s <em>Resources</em> are
     * <em>Available</em></li>
     * <li><em>JDF/@Activation</em> is Active </li>
     * </ul>
     * A <em>Notification</em> will be appended to the <em>Response</em>
     * (alternatively to the <em>JDF/AutditPool</em>) if any of these are
     * requirements above are not fulfilled.
     * 
     * @param jdf the jdf to be checked.
     * @param response The <em>Response</em> to which the error <em>Notification</em>
     *            will be appended. Use <code>null</code> if the error
     *            <em>Notification</em> should be appended to
     *            <em>JDF/AuditPool</em> instead.
     * @return <code>true</code> if the JDF ready to execute,
     *         <code>false</code> otherwise.
     */
    public boolean isExecutableAndAvailbleResources(JDFNode jdf,
            JDFResponse response) {
        String msg = null;
        boolean isExecutable = true;

        JDFNotification notification = (JDFNotification) JDFElementFactory
                .getInstance().createJDFElement(ElementName.NOTIFICATION);
        notification.setClass(EnumClass.Error); // Required attribute
        notification.setAuthor(this.getClass().getName());
        notification.setTimeStamp(null);

        String activationStr = jdf.getActivation(false).getName();
        if (activationStr.length() != 0
                && !activationStr.equals(JDFNode.EnumActivation.Active.getName())) {
            msg = "The Activation of the JDFNode must be 'Active' or not "
                    + "given in order to execute. The Activation was '"
                    + jdf.getActivation(true).getName() + "'. ";
            log.info(msg);
            notification.appendComment().appendText(msg);
            isExecutable = false;
        }

        EnumNodeStatus status = jdf.getPartStatus(null);
        if (!status.equals(EnumNodeStatus.Waiting)) {
            msg = "The Status of the must be JDFNode 'Waiting' in order to"
                    + " execute. The Status was '" + jdf.getStatus().getName()
                    + "'. ";
            log.info(msg);
            notification.appendComment().appendText(msg);
            isExecutable = false;
        }

        if (false) { //XXXif (!jdf.isExecutable(emptyAttributeMap, false)) {
            msg = "The JDFNode with id '" + jdf.getID()
                    + "' is NOT executable due to 'Unavailable' resources. ";
            List l = getNonExecutableResources(jdf);
            msg += "The resources that are not executable are " + l;
            log.info(msg);
            notification.appendComment().appendText(msg);
            isExecutable = false;
        }

        if (!isExecutable) {
            if (response == null) {
                msg = "The Status of the JDFNode was set to 'Aborted'.";
                notification.appendComment().appendText(msg);
                jdf.setStatus(EnumNodeStatus.Aborted);
                jdf.getCreateAuditPool().copyElement(notification, null);
            } else {
                response.copyElement(notification, null);
            }
        }
        return isExecutable;
    }

    /**
     * Returns a list of non-executable Input <em>Resource</em>s for the
     * specified <code>JDFNode</code>, empty list if all Input
     * <em>Resource</em>s are executable.
     * 
     * TODO Add support for Notification messages in the method.
     * 
     * @param jdf The JDF Node which <em>Resource</em>s are being checked.
     * @return a list of non-executable <em>Resource</em>s for the specified
     *         <code>JDFNode</code>, empty list if all <em>Resource</em>s
     *         are executable.
     * @throws NullPointerException if jdf is <code>null</code>.
     */
    private List getNonExecutableResources(JDFNode jdf) {
        JDFAttributeMap m = new JDFAttributeMap();
        List l = new Vector();
        m.put("Usage", "Input");
        List inputResources = jdf.getResourceLinks(m);

        for (int i = 0, imax = inputResources.size(); i < imax; i++) {
            JDFResourceLink res = (JDFResourceLink) inputResources.get(i);
            if (res.isExecutable(new JDFAttributeMap(), false)) {
                log.debug("ResourceLink " + (i + 1)
                        + " allows the node to execute");
            } else {
                l.add(res);
                if (log.isDebugEnabled()) {
                    log.debug("ResourceLink " + (i + 1)
                            + " disallows the node to execute");
                    log.debug("Resource " + res);
                }
            }
        }
        return l;
    }

    /**
     * Returns a list of all JDF process nodes of the specified process
     * <em>Type</em> with the specified <em>Status</em>.
     * 
     * @param jdf the JDFNode to get child Process nodes from.
     * @param processType the Type of process node to look for.
     * @param status the status the node must have <code>null</code> if no
     *            restriction should be placed on node <em>Status</em>
     * @return a List of JDFNode elements representing process nodes of the
     *         specified <em>Type</em> (i.e. Approval)
     */
    public List getProcessNodes(JDFNode jdf, String processType,
            JDFElement.EnumNodeStatus status) {
        log.debug("Searching for '" + processType + "' in JDF '" + jdf.getID()
                + "'...");
        List processNodes = new ArrayList();
        if (jdf.getAttribute("Type").equals(processType)) {
            // The JDF was a leaf node.
            if (status == null || jdf.getStatus().equals(status)) {
                processNodes.add(jdf);
            }
        } else {
            JDFAttributeMap attr = new JDFAttributeMap();
            attr.put("Type", processType);
            if (status != null) {
                attr.put("Status", status.getName());
            }
            processNodes = jdf.getChildrenByTagName(ElementName.JDF,
                null, attr, false, true, 0);
        }
        return processNodes;
    }
}
