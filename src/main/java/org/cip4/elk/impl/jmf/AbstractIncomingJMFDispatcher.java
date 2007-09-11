package org.cip4.elk.impl.jmf;

import java.util.Map;
import java.util.Set;

import org.cip4.elk.Config;
import org.cip4.elk.jmf.IncomingJMFDispatcher;
import org.cip4.elk.jmf.JMFProcessor;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;


/**
 * An implementation of the functionality for registering, unregistering and
 * accessing JMFProcessors.
 *  
 * @author Claes Buckwalter (clabu@itn.liu.se)
 */
public abstract class AbstractIncomingJMFDispatcher implements IncomingJMFDispatcher
{
    private static final String DEFAULT_PROCESSOR = "default";
    protected Map _processors = new ConcurrentReaderHashMap();
	protected JMFProcessor _defaultProcessor = null;
    protected Config _config;
    
    public void setConfig(Config config) {
        _config = config;
    }
	
	/**
	 * @see org.cip4.elk.jmf.IncomingJMFDispatcher#registerProcessor(java.lang.String, org.cip4.elk.jmf.JMFProcessor)
	 */
	public JMFProcessor registerProcessor(String jmfType, JMFProcessor processor)
	{
		return (JMFProcessor) _processors.put(jmfType, processor);
	}

	/**
	 * @see org.cip4.elk.jmf.IncomingJMFDispatcher#unregisterProcessor(java.lang.String)
	 */
	public JMFProcessor unregisterProcessor(String jmfType)
	{
		return (JMFProcessor) _processors.remove(jmfType);
	}

	/**
	 * @see org.cip4.elk.jmf.IncomingJMFDispatcher#getMessageTypes()
	 */
	public Set getMessageTypes()
	{
		return _processors.keySet();
	}

	/**
	 * @see org.cip4.elk.jmf.IncomingJMFDispatcher#getProcessor(java.lang.String)
	 */
	public JMFProcessor getProcessor(String jmfType)
	{
        Object processor = _processors.get(jmfType);
        if (processor == null) {
            processor = getDefaultProcessor();
        }
        return (JMFProcessor) processor;
	}

	/**
	 * @see org.cip4.elk.jmf.IncomingJMFDispatcher#setDefaultProcessor(org.cip4.elk.jmf.JMFProcessor)
	 */
	public JMFProcessor registerDefaultProcessor(JMFProcessor defaultProcessor)
	{
		JMFProcessor oldProcessor = _defaultProcessor;
		_defaultProcessor = defaultProcessor;
		return oldProcessor;
	}

	/**
	 * @see org.cip4.elk.jmf.IncomingJMFDispatcher#unregisterDefaultProcessor()
	 */
	public JMFProcessor unregisterDefaultProcessor()
	{
		JMFProcessor oldProcessor = _defaultProcessor;
		_defaultProcessor = null;
		return oldProcessor;
	}
    
    public JMFProcessor getDefaultProcessor()
    {
        return _defaultProcessor;
    }
    
    public void setProcessors(Map processors)
    {
        // Extracts the default processor
        if (processors.containsKey(DEFAULT_PROCESSOR)) {
            _defaultProcessor = (JMFProcessor) processors.remove(DEFAULT_PROCESSOR);
        } else if (processors.containsKey(DEFAULT_PROCESSOR.toLowerCase())) {
            _defaultProcessor = (JMFProcessor) processors.remove(DEFAULT_PROCESSOR.toLowerCase());
        }
        // Registers all processors
        _processors.putAll(processors);
    }
}
