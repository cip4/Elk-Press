/*
 * Created on 2005-apr-20
 */
package org.cip4.elk.impl.jmf.preprocess;

import org.cip4.jdflib.jmf.JDFCommand;
import org.cip4.jdflib.jmf.JDFResponse;

/**
 * An interface for a JDFPreprocessor. The intended use for classes implementing
 * this interface is to preprocess the JDF-file related to a
 * <em>SubmitQueueEntry</em> Command. The returned <em>Response</em> message
 * of an implementing class should be the resulting response <em>after</em> 
 * preprocessing.
 * 
 * TODO Better documentation
 * 
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: JDFPreprocessor.java,v 1.4 2006/11/17 15:36:35 buckwalter Exp $
 */
public interface JDFPreprocessor {

    /**
     * Preprocesses the JDF file referenced in the incoming
     * <em>SubmitQueueEntry</em> command. It is up to the implementor if this
     * method sends any Acknowledge messages during the preprocessing. The
     * <em>Response</em> message should be complete, including return code
     * (indicating if the preprocessing was successful). 
     * 
     * @param command
     *            The incoming <em>SubmitQueueEntry</em> command.
     * @return the <em>Response</em> message after preprocessing.
     */
    public JDFResponse preProcessJDF(JDFCommand command);
}
