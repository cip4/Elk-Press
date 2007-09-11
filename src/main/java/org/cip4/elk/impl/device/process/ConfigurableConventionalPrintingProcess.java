/*
 * Created on 2005-jul-21
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cip4.elk.impl.device.process;

import org.cip4.elk.device.DeviceConfig;
import org.cip4.elk.impl.util.Repository;
import org.cip4.elk.impl.util.URLAccessTool;
import org.cip4.elk.jmf.OutgoingJMFDispatcher;
import org.cip4.elk.queue.Queue;

/**
 * A class to easily configure the ConeventionalPrinting <em>Process</em>.
 * 
 * 
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: ConfigurableConventionalPrintingProcess.java,v 1.2 2005/09/10 11:09:16 ola.stering Exp $
 */
public class ConfigurableConventionalPrintingProcess extends
        ConventionalPrintingProcess {

    /**
     * @see ConventionalPrintingProcess#ConventionalPrintingProcess(DeviceConfig, Queue, URLAccessTool, OutgoingJMFDispatcher, Repository)
     * @param config
     * @param queue
     * @param fileUtil
     * @param dispatcher
     * @param repository
     */
    ConfigurableConventionalPrintingProcess(DeviceConfig config, Queue queue,
            URLAccessTool fileUtil, OutgoingJMFDispatcher dispatcher,
            Repository repository) {
        super(config, queue, fileUtil, dispatcher, repository);
    }

    /**
     * Sets the <em>SetUp</em> time for this <em>Process</em>.
     * 
     * @param seconds The time this <em>Process</em> takes to SetUp (has
     *            <em>JDF/@Status="SetUp"</em>).
     * @throws IllegalArgumentException if seconds < 0.
     */
    public void setSetUpTime(int seconds) {
        if (seconds < 0) {
            String msg = "The setUpTime must be greater than 0, not: "
                    + seconds;
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        _setUpTime = seconds;
    }

    /**
     * Sets the <em>InProgress</em> time for this <em>Process</em> in
     * seconds.
     * 
     * NOTE: This time will be added to the PagesPerMinute variable. The actual
     * InProgress time will thus be this variable plus the time for the
     * "printing" of the pages.
     * 
     * @param seconds The time this Process takes to be in progress (has
     *            <em>JDF/@Status="InProgress"</em>).
     * @throws IllegalArgumentException if seconds < 0.
     */
    public void setInProgressTime(int seconds) {
        if (seconds < 0) {
            String msg = "The inProgressTime must be greater than 0, not: "
                    + seconds;
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
        _inProgressTime = seconds;

    }

    /**
     * The pages per minute this <em>Process</em> is able to "print".
     * 
     * @param pagesPerMinute pages per minute this <em>Process</em> can
     *            produce.
     * @throws if pagesPerMinute is less than 0.
     */
    public void setPagesPerMinute(int pagesPerMinute) {
        if (pagesPerMinute < 0) {
            String msg = "The pagesPerMinute must be greater than 0, not: "
                    + pagesPerMinute;
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
        _pagesPerMinute = pagesPerMinute;
    }

}
