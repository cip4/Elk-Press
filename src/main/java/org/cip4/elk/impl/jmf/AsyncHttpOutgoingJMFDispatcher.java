package org.cip4.elk.impl.jmf;

import org.cip4.elk.Config;
import org.cip4.jdflib.jmf.JDFJMF;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.node.JDFNode;

import EDU.oswego.cs.dl.util.concurrent.Executor;
import EDU.oswego.cs.dl.util.concurrent.ThreadedExecutor;

/**
 * An {@link org.cip4.elk.jmf.OutgoingJMFDispatcher}that dispatches JMF and JDF
 * over HTTP. JMF Signals and JDF instances are dispatched asynchronously. Other
 * JMF messages are dispatched synchronously.
 * 
 * @author Claes Buckwalter (clabu@itn.liu.se)
 * @version $Id: AsyncHttpOutgoingJMFDispatcher.java,v 1.3 2005/07/13 15:58:29 buckwalter Exp $
 */
public class AsyncHttpOutgoingJMFDispatcher extends
        SyncHttpOutgoingJMFDispatcher {

    private Executor _executor;
    private boolean _asynchronous = true;

    /**
     * Creates an outgoing dispatcher that dispatches JMF Signals and JDF
     * instances asynchronously; other JMF messages are dispatched
     * synchronously.
     * 
     * @param config the configuration used by the device that owns this
     *            dispatcher
     */
    public AsyncHttpOutgoingJMFDispatcher(Config config) {
        this(config, true);
    }

    /**
     * Creates an outgoing dispatcher that dispatches JMF Signals and JDF
     * instances asynchronously or synchronously; other JMF messages are always
     * dispatched synchronously.
     * 
     * @param config the configuration used by the device that owns this
     *            dispatcher
     * @param asynchronous true for asynchronous dispatching; false for
     *            synchronous dispatching
     */
    public AsyncHttpOutgoingJMFDispatcher(Config config, boolean asynchronous) {
        super(config);
        setAsynchronous(asynchronous);
        _executor = new ThreadedExecutor();
    }

    /**
     * Sets whether dispatching should be done synchronously or asynchronously.
     * 
     * @param asyncMode true to dispatch asynchronously; false to dispatch
     *            synchronously
     */
    public void setAsynchronous(boolean asynchronous) {
        _asynchronous = asynchronous;
    }

    /**
     * Returns true if dispatching is done asynchronously.
     * 
     * @return true if dispatching is done asynchronously; false otherwise
     */
    public boolean isAsynchronous() {
        return _asynchronous;
    }

    /**
     * Sends a JDF instance asynchronously or synchronously.
     * 
     * @param jdf the JDF instance to send
     * @param url the URL to send the JDF instance to
     * @see org.cip4.elk.impl.jmf.SyncHttpOutgoingJMFDispatcher#dispatchJDF(org.cip4.jdflib.node.JDFNode,
     *      java.lang.String)
     */
    public void dispatchJDF(JDFNode jdf, String url) {
        if (_asynchronous) {
            // Async dispatch
            log.debug("Dispatching JDF (" + jdf.getJobID(true) + ") to " + url
                    + " asynchronously...");
            asyncDispatchJDF(jdf, url);
        } else {
            // Sync dispatch
            log.debug("Dispatching JDF (" + jdf.getJobID(true) + ") to " + url
                    + " synchronously...");
            syncDispatchJDF(jdf, url);
        }
    }

    /**
     * Sends a JDF instance asynchronously, causing this method to return
     * immediately.
     * 
     * @param jdf the JDF instance to send
     * @param url the URL to send the JDF instance to
     */
    private void asyncDispatchJDF(final JDFNode jdf, final String url) {
        try {
            _executor.execute(new Runnable() {
                public void run() {
                    syncDispatchJDF(jdf, url);
                }
            });
        } catch (InterruptedException ie) {
            log.error("Could not dispatch JDF (" + jdf.getJobID(true)
                    + ") to URL " + url + ".", ie);
        }
    }

    /**
     * Sends a JDF instance synchronously.
     * 
     * @param jdf the JDF instance to send
     * @param url the URL to send the JDF instance to
     */
    private void syncDispatchJDF(JDFNode jdf, String url) {
        super.dispatchJDF(jdf, url);
    }

    /**
     * Sends a JMF <em>Signal</em> message asynchronously, causing this method
     * to return immediately.
     * 
     * @param jmfSignal the JMF Signal to send
     * @param url the URL to send the Signal to
     * @see org.cip4.elk.impl.jmf.SyncHttpOutgoingJMFDispatcher#dispatchSignal(org.cip4.jdflib.jmf.JDFJMF,
     *      java.lang.String)
     */
    public void dispatchSignal(JDFJMF jmfSignal, String url) {
        if (_asynchronous) {
            // Async dispatch
            log.debug("Dispatching JMF Signal ("
                    + jmfSignal.getMessage(0).getID() + ") to " + url + " asynchronously...");
            asyncDispatchSignal(jmfSignal, url);
        } else {
            // Sync dispatch
            log.debug("Dispatching JMF Signal ("
                    + jmfSignal.getMessage(0).getID() + ") to " + url
                    + " synchronously...");
            syncDispatchSignal(jmfSignal, url);
        }
    }

    /**
     * Sends a JMF Signal asynchronously, causing this method to return
     * immediately.
     * 
     * @param jdf the JMF Signal to send
     * @param url the URL to send the JDF instance to
     */
    private void asyncDispatchSignal(final JDFJMF jmfSignal, final String url) {
        try {
            _executor.execute(new Runnable() {
                public void run() {
                    syncDispatchSignal(jmfSignal, url);
                }
            });
            log.debug("Dispatched JMF Signal (" + jmfSignal.getMessage(0).getID()
                    + ") to " + url + ".");
        } catch (InterruptedException ie) {
            log.error("Could not dispatch JMF Signal ("
                    + jmfSignal.getMessage(0).getID() + ") to " + url + ".", ie);
        }
    }

    /**
     * Forwards to the parent class's {@link SyncHttpOutgoingJMFDispatcher}
     * implementation of
     * {@link org.cip4.elk.jmf.OutgoingJMFDispatcher#dispatchSignal(JDFJMF, String)}.
     * 
     * @param jmfSignal the JMF Signal to send
     * @param url the URL to send the JMF Signal to
     */
    private void syncDispatchSignal(JDFJMF jmfSignal, String url) {
        super.dispatchSignal(jmfSignal, url);
    }

    /**
     * Dispatches a JMF message synchronously, not returning until the message
     * has been sent. This method simply forwards to the parent class's
     * implementation of
     * {@link org.cip4.elk.jmf.OutgoingJMFDispatcher#dispatchSignal(JDFJMF, String)}.
     * 
     * @param jmf the JMF message to send
     * @param url the URL to send the JMF message to
     * @see org.cip4.elk.impl.jmf.SyncHttpOutgoingJMFDispatcher#dispatchJMF(org.cip4.jdflib.jmf.JDFJMF,
     *      java.lang.String)
     */
    public JDFResponse dispatchJMF(JDFJMF jmf, String url) {
        // Sync dispatch
        JDFResponse response = null;
        log.debug("Dispatching JMF message (" + jmf.getMessage(0).getID() + ") to " + url
                + " synchronously...");
        response = super.dispatchJMF(jmf, url);
        return response;
    }
}
