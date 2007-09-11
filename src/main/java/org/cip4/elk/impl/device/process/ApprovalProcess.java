package org.cip4.elk.impl.device.process;

import java.util.List;
import java.util.Random;

import org.cip4.elk.device.DeviceConfig;
import org.cip4.elk.impl.util.Repository;
import org.cip4.elk.impl.util.URLAccessTool;
import org.cip4.elk.jmf.OutgoingJMFDispatcher;
import org.cip4.elk.queue.Queue;
import org.cip4.jdflib.auto.JDFAutoNotification.EnumClass;
import org.cip4.jdflib.core.ElementName;
import org.cip4.jdflib.core.JDFComment;
import org.cip4.jdflib.core.KElement;
import org.cip4.jdflib.core.JDFElement.EnumNodeStatus;
import org.cip4.jdflib.datatypes.VJDFAttributeMap;
import org.cip4.jdflib.node.JDFNode;
import org.cip4.jdflib.pool.JDFAuditPool;
import org.cip4.jdflib.resource.JDFNotification;
import org.cip4.jdflib.resource.JDFResource;
import org.cip4.jdflib.resource.process.JDFApprovalParams;
import org.cip4.jdflib.resource.process.JDFApprovalPerson;
import org.cip4.jdflib.resource.process.JDFApprovalSuccess;
import org.cip4.jdflib.util.JDFDate;

/**
 * A device that implements the Approval process.
 * 
 * @see <a
 *      href="http://www.cip4.org/documents/jdf_specifications/JDF1.2.pdf">JDF
 *      Specification Release 1.2, 6.2.1 Approval </a>
 * @author Claes Buckwalter (clabu@itn.liu.se)
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: ApprovalProcess.java,v 1.11 2006/12/03 21:22:53 buckwalter Exp $
 */
public class ApprovalProcess extends BaseProcess {

    public static final String[] PROCESS_TYPES = {"Approval"};

    public ApprovalProcess(DeviceConfig config, Queue queue,
            URLAccessTool fileUtil, OutgoingJMFDispatcher dispatcher,
            Repository repository) {
        super(config, queue, fileUtil, dispatcher, repository);
        setProcessTypes(PROCESS_TYPES);
    }
    
    /**
     * Executes the specified JDF process node
     * 
     * @param jdf the JDF process node to execute
     */
    protected void executeNode(JDFNode jdf) {
        JDFDate startTime = new JDFDate(); // XXX why is this XXX?
        JDFAuditPool auditPool = jdf.getCreateAuditPool();        
        auditPool.addModified(_config.getID(), null);
        // JDFModified mod = auditPool.addModified(_config.getID(), null);
        // XXX Do I need to add something to the mod?
        _state.setJdf(jdf);
        // Setup phase
        jdf.setStatus(EnumNodeStatus.Setup);        
        int setUpTime = 2;
        log.debug(getProcessId() + " is setting up for " + setUpTime
                + " seconds");
        sleepAWhile(setUpTime);
        
        JDFDate endSetUpTime = new JDFDate();
        jdf.setStatus(EnumNodeStatus.InProgress);
        int inProgressTime = 8;
        log.debug(getProcessId() + " is InProgress for " + inProgressTime
                + " seconds");
        sleepAWhile(inProgressTime);

        // TODO Verify that the process and is correctly specified
        // TODO Verify that all input resources have status "Available" or
        // "Draft" so that the process can start executing
        // TODO Send JMF Signal messages for each processing step

        List inputResources = getInputResourcesToApprove(jdf);
        List outputResources = getOutputResourcesToApprove(jdf);
        // Copy the contents of each input resource to an output resource
        // TODO Verify that inputResources.size() == outputResources.size()
        JDFResource resIn;
        KElement[] children;
        JDFResource resOut;
        int randomStatus;
        Random random = new Random();
        boolean allApproved = true;
        int inputResSize = inputResources.size();
        int outputResSize = outputResources.size();

        if (inputResSize != outputResSize) {
            String msg = "The ApprovalProcess demands that the number of Input"
                    + " Resources are the same as the number of Output"
                    + " Resources (in the ResourceLinkPool). There are "
                    + inputResSize + " Input Resources and " + outputResSize
                    + " output Resources. The status is "
                    + "set to Aborted and the Node is not further executed.";
            log.error(msg);
            JDFNotification notification = auditPool.addNotification(
                EnumClass.Error, _config.getID() + ", " + getProcessId(),
                new VJDFAttributeMap());
            JDFComment comment = notification.appendComment();
            comment.appendText(msg);
            jdf.setStatus(EnumNodeStatus.Aborted);
            jdf.setStatusDetails("Could not execute Node, incorrectly "
                    + "specified Resources, see Notification.");
            // TODO Should an Elk event be sent in this case?
            return;
        }

        for (int i = 0, imax = inputResources.size(); i < imax; i++) {
            resIn = (JDFResource) inputResources.get(i);
            children = resIn.getChildElementArray();
            resOut = (JDFResource) outputResources.get(i);
            for (int j = 0; j < children.length; j++) {
                resOut.copyElement(children[j], null);
            }
            // Random status, either "Available" or "Rejected"
            randomStatus = random.nextInt(2);
            if (randomStatus == 0) {
                resOut.setStatus(JDFResource.EnumResStatus.Available);
            } else {
                allApproved = false;
                resOut.setAttribute("Status", "Rejected"); //XXX There is no
                // JDFResource.EnumResStatus.Rejected
            }
        }

        JDFApprovalParams approvalParams = getApprovalParams(jdf);
        JDFApprovalSuccess approvalSuccess = getApprovalSuccess(jdf);
        List approvalPersons = getApprovalPersons(approvalParams);
        // Copy all Contact elements to the ApprovalSuccess element
        for (int i = 0, imax = approvalPersons.size(); i < imax; i++) {
            JDFApprovalPerson person = (JDFApprovalPerson) approvalPersons
                    .get(i);
            approvalSuccess.refElement(person.getContact());
        }
        // Set status of ApprovalSuccess
        if (allApproved) {
            approvalSuccess.setStatus(JDFResource.EnumResStatus.Available);
        } else {
            approvalSuccess.setStatus(JDFResource.EnumResStatus.Unavailable);
        }

        addAudits(jdf,startTime,endSetUpTime,new JDFDate(),null);
        // Update the JDF node's state
        jdf.setStatus(EnumNodeStatus.Completed);
    }

    /**
     * Returns the ApprovalParams input resource from the specified JDF process
     * node
     * 
     * @param jdf the JDF process node
     * @return a JDFApprovalParams element; null if there was no
     *         JDFApprovalResource element
     */
    protected JDFApprovalParams getApprovalParams(JDFNode jdf) {
        List ir = getInputResources(jdf);
        Object res;
        for (int i = 0, imax = ir.size(); i < imax; i++) {
            res = ir.get(i);
            if (res instanceof JDFApprovalParams) {
                return (JDFApprovalParams) res;
            }
        }
        return null;
    }

    /**
     * Returns the ApprovalSuccess output resource from the specified JDF
     * process node
     * 
     * @param jdf the JDF process node
     * @return a JDFApprovalSuccess element; null if there was no
     *         JDFApprovalSuccess element
     */
    protected JDFApprovalSuccess getApprovalSuccess(JDFNode jdf) {
        List or = getOutputResources(jdf);
        Object res;
        for (int i = 0, imax = or.size(); i < imax; i++) {
            res = or.get(i);
            if (res instanceof JDFApprovalSuccess) {
                return (JDFApprovalSuccess) res;
            }
        }
        return null;
    }

    /**
     * Returns a list of all ApprovalPerson elements referenced by the specified
     * ApprovalParams element. Referenced resources (using the rRef attribute),
     * if any, are followed and the referenced resource is returned.
     * 
     * @param approvalParams the ApprovalParams to get ApprovalPerson elements
     *            from
     * @return a List of JDFApprovalPerson elements; null if there were not
     *         JDFApprovalPerson elements
     */
    protected List getApprovalPersons(JDFApprovalParams approvalParams) {
        return approvalParams.getChildElementVector(ElementName.APPROVALPERSON,
            null, null, false, 0, true);
    }

    /**
     * Returns a list containing all the input resources to be approved by the
     * specified JDF Approval process
     * 
     * @param jdf the JDF process node of type "Approval"
     * @return a List of JDFResources to be approved
     */
    protected List getInputResourcesToApprove(JDFNode jdf) {
        List ir = getInputResources(jdf);
        Object res;
        for (int i = 0, imax = ir.size(); i < imax; i++) {
            // Remove the ApprovalParams resource and return all other input
            // resources
            res = ir.get(i);
            if (res instanceof JDFApprovalParams) {
                ir.remove(res);
            }
        }
        return ir;
    }

    /**
     * Returns an vector containing all the output resources to be approved by
     * the specified JDF Approval process
     * 
     * @param jdf the JDF process node of type "Approval"
     * @return a Vector of JDFResources to be approved
     */
    protected List getOutputResourcesToApprove(JDFNode jdf) {
        List or = getOutputResources(jdf);
        Object res;
        for (int i = 0, imax = or.size(); i < imax; i++) {
            // Remove the ApprovalSuccess resource and return all other output
            // resources
            res = or.get(i);
            if (res instanceof JDFApprovalSuccess) {
                or.remove(res);
            }
        }
        return or;
    }
}
