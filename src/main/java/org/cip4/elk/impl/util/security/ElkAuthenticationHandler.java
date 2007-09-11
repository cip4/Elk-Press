/**
 * Created on Mar 22, 2006, 3:33:22 PM
 * org.cip4.elk.impl.util.security.ElkAuthenticationHandler.java
 * Project Name: Elk
 */
package org.cip4.elk.impl.util.security;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cip4.elk.Config;
import org.cip4.elk.lifecycle.Lifecycle;
import org.cip4.elk.util.security.AbstractAuthenticationHandler;
import org.cip4.elk.util.security.AuthenticationHandler;
import org.cip4.elk.util.security.DefaultKeyManager;
import org.cip4.elk.util.security.DefaultTrustEntry;
import org.cip4.elk.util.security.KeyManager;
import org.cip4.elk.util.security.RFAJMFProcessor;
import org.cip4.elk.util.security.RemoteHost;
import org.cip4.elk.util.security.TrustEntry;
/**
 * This is an implementation of a AuthenticationHandler that manages
 * trust relations to allow communication over https
 * 
 * 
 *
 * @author Markus Nyman, (markus@myman.se)
 * 
 */
public class ElkAuthenticationHandler extends AbstractAuthenticationHandler
		implements AuthenticationHandler, Lifecycle {

	private Log log = LogFactory.getLog(this.getClass().getName());
	
	// a flag to avoid multiple initializations
	private boolean initiated = false;
	
	// a Config object used only to get the ID of the device
	private Config _config;
	
	// A prefix that is set in all properties in the propertiesfile
	// Used to allow same implementation for all KeyManagers
	private static String propertyPrefix = "elk";
	
	// The Propertiesfile
	private static String DEF_PROPERTIES = "org/cip4/elk/impl/util/security/rfa.properties";
	
	// The file to serialize TrustEntries to
	private static String teFileName = "elkte.dat";
	
	// The folder for the above file
	private static String trustEntryFile = System.getProperty("user.home") + System.getProperty("file.separator") + teFileName;
	
	// Private Properties. read from PROPERTIES file
	private Properties _properties;
	
	// The secure URL for this device
	// initiated when it is requested the first time
	private String serverSecureUrl = null;

	// The URL for this device
	// initiated when it is requested the first time
	private String serverUrl = null;

	// The RFAJMFProcessor that handles all in and outgoing JMFs
	private RFAJMFProcessor _rfaProcessor;
		
	// KeyManager that handles all keys and keystores
	private KeyManager _keyManager;
		

	
	/**
	 * Only Constructor. Initializes KeyManager and Properties
	 *
	 */
	public ElkAuthenticationHandler() {
		_keyManager = new DefaultKeyManager();
		_properties = loadConfiguration("rfa.properties");
		
	}
	
	
	/**
	 * Getter for the KeyManager
	 */
	public KeyManager getKeyManager() {
		return _keyManager;
	}
	
	
	/**
	 * Shut down process.
	 * This method saves the current trust relations
	 * into persistent memory. Stores a file to the local
	 * user folder.
	 */
	public void shutDown() {
		log.debug("Shutting down Elk AuthHandler.");
		log.debug("TrustEntries: " + getTrustEntries().toString());
		serializeTrustEntries(trustEntryFile);
		//((DefaultAuthenticationGUI)_ui).dispose();

	}
	
	
	/**
	 * Calls this.shutDown()
	 */
	public void destroy() {
		shutDown();
	}
	
	
	/**
	 * Init steps for the AuthenticationHandler.
	 * Loads the trust relation status and initializes
	 * the Keyanager
	 */
	public void init() {
		if (!initiated) {

			// load the trustAntries from disk
			loadTrustEntries(trustEntryFile);

			// config has to be set here!!!
			if (_config != null) {
				// the key manager should only be initiated once
				((DefaultKeyManager)_keyManager).init(_properties, propertyPrefix, getLocalID());
				initiated = true;
			}
			else
				log.error("Error initializing KeyManager.");
		}

	}
	
	
	public synchronized String getUrl() {
		if (serverUrl == null)
			serverUrl = _properties.getProperty("elk.ssl.server.url",
					"http://localhost:8080/elk-printing/jmf");
		return serverUrl;		
	}
	
	
	/**
	 * Returns the secure URL of this party
	 */
	public synchronized String getSecureUrl() {
		if (serverSecureUrl == null)
			serverSecureUrl = _properties.getProperty("elk.ssl.server.secureurl",
					getDefaultSecureUrl());
		return serverSecureUrl;
	}
	
	
	/**
	 * Get the Default Secure URL.
	 * 
	 * 
	 * @return URL string that is not null nor empty
	 */
	private String getDefaultSecureUrl() {
		int port = 8443;
		String hostName = null;
		String defaultPath = "/elk-printing/jmf/";

		//Set s = _config.getConfigParameterNames();

		// Get the specified HTTPS port
		String portString = _properties.getProperty("elk.ssl.port");
		if (portString != null)  {
			try {
				port = Integer.parseInt(portString);
			} catch (NumberFormatException e) {
				log.debug("Error parsing port specified in rfa.properties: " 
						+ portString + ". Using port: " + port);
			}
		}
		
		String useRawIPString = _properties.getProperty("elk.ssl.server.userawip", "false");
		boolean useRawIP = useRawIPString.equalsIgnoreCase("true");
		// Get the hostname
		try {
			InetAddress addr = InetAddress.getLocalHost();
			if (useRawIP)
				hostName = addr.getHostAddress();
			else
				hostName = addr.getCanonicalHostName();
		} catch (UnknownHostException e) {
	    	log.debug("Local host name could not be retrieved.");
	    }
		if (hostName == null)
			hostName = "localhost";
		
	    return "https://" + hostName + ":" + port + defaultPath;	
	}


	/**
	 * Loads the properties from a properties file.
	 * 
	 * This method is copied from 
	 * 
	 * org.cip4.elk.alces.util.ConfigurationHandler
	 * 
	 * @author Marco Kornrumpf (Marco.Kornrumpf@Bertelsmann.de)
	 * @author Markus Nyman (markus.cip4@myman.se)
	 * 
	 * @param propsFile properties file
	 * @return non null, possibly empty, properties object
	 */
	public Properties loadConfiguration(String propsFile) {
		
		InputStream stream = ElkAuthenticationHandler.class.getClassLoader()
		.getResourceAsStream(propsFile);
		// Loads the properties file
		if (stream == null)
			stream = ElkAuthenticationHandler.class.getClassLoader()
			.getResourceAsStream(DEF_PROPERTIES);
		Properties props = new Properties();
		
		log.info("Loading configuration from '" + propsFile + "'...");

        try {
            props.load(stream);

        } catch (FileNotFoundException fnfe) {
            System.err.println("Could not find the properties file '"
                            + propsFile
                            + "'. Defaults will be used. No error while using Automated-Alces");
        } catch (IOException ioe) {
            System.err.println("Could not load the properties file '"
                            + propsFile
                            + "'. Defaults will be used. No error while using Automated-Alces");
        }
        log.info("Configuration loaded.");
        
        return props;
    }
    
	
	/**
	 * Notifies the user by printing a debug message
	 */
	public void notifyUser(Object message) {
		//_ui.notifyMessage((String)message);
		log.debug("Notification: " + (String)message);
	}
	
	
	/**
	 * Returns the RFAJMFProcessor referenced by this AuthHandler
	 */
	public RFAJMFProcessor getRFAProcessor() {
		return _rfaProcessor;
	}
	
	
	/**
	 * Setter for the RFAJMFProcessor
	 * @param rfaProcessor
	 */
	public void setRFAProcessor(RFAJMFProcessor rfaProcessor) {
		_rfaProcessor = rfaProcessor;
	}
	
	
	/**
	 * Getter for this party's unique ID
	 */
	public String getLocalID() {
		if (getConfig() != null)
			return getConfig().getID();
		else return "Unresolved ID";
		
	}
		
	
	/**
	 * The Configuration object. Currently only used for getting
	 * the ID of the device
	 * @return
	 */
	public Config getConfig() {
		return _config;
	}
	
	
	/**
	 * Setter fo the config object.
	 * 
	 * @param config
	 */
	public void setConfig(Config config) {		
		log.debug("Configuration object set.");
		_config = config;
	}
	

	/**
	 * This method overrides the AbstractAuthenticationHandlers method
	 * This shouldn�t be needed but is here to allow that the
	 * reply URL is set manually in the properties file
	 */
	public TrustEntry initIncomingClientConnection(String remoteID, 
			String certificate, String url, RemoteHost hostInfo) {
		if (url == null)
			return super.initIncomingClientConnection(remoteID, certificate,
					_properties.getProperty("elk.ssl.defaulturl"), hostInfo);
		else
			return super.initIncomingClientConnection(remoteID, certificate,
					url, hostInfo);

	}
	
	
	/**
	 * This method overrides the AbstractAuthenticationHandlers method
	 * This shouldn�t be needed but is here to allow that the
	 * reply URL is set manually in the properties file
	 */
	public TrustEntry initIncomingServerConnection(String remoteID,
			String certificate, String url, RemoteHost hostInfo) {
		if (url == null)
			return super.initIncomingServerConnection(remoteID, certificate,
					_properties.getProperty("elk.ssl.defaulturl"), hostInfo);
		else
			return super.initIncomingServerConnection(remoteID, certificate,
					url, hostInfo);
	}
	
	
	/**
	 * Adds a remote server trust entry and starts the querying
	 * process
	 */
	public void startServerRFAProcess(TrustEntry trustEntry) {	
		addServerTrustEntry(trustEntry);
		((DefaultTrustEntry)trustEntry).startProcess();
	}
	
	
	/**
	 * Adds a remote client trust entry to the local store and
	 * starts the querying process of the TrustEntry
	 */
	public void startClientRFAProcess(TrustEntry trustEntry) {	
		addClientTrustEntry(trustEntry);
		((DefaultTrustEntry)trustEntry).startProcess();
	}
	
	
	/**
	 * Adds a trust relation to a remote server
	 */
	public boolean addServerTrust(String remoteServerID) {
		TrustEntry trustEntry = getServerTrustEntry(remoteServerID);
		if (trustEntry != null) {
			_keyManager.addTrust(trustEntry);
			//_ui.notifyTrustEntry(null);
			return true;
		}
		else 
			return false;	
	}
	
	
	/**
	 * Adds a trust relation to a remote client
	 */
	public boolean addClientTrust(String remoteClientID) {
		TrustEntry trustEntry = getClientTrustEntry(remoteClientID);
		if (trustEntry != null) {
			_keyManager.addTrust(trustEntry);
			//_ui.notifyTrustEntry(null);
			return true;
		}
		else 
			return false;	
	}
	
	
	/**
	 * Deletes the trust relation for a remote server
	 */
	public boolean deleteServerTrust(String remoteServerID) {
		TrustEntry trustEntry = getServerTrustEntry(remoteServerID);
		if (trustEntry != null) {
			_keyManager.deleteTrust(trustEntry);
			deleteServerTrustEntry(trustEntry.getRemoteID());
			//_ui.notifyTrustEntry(null);
			return true;
		}
		else 
			return false;	
	}
	
	
	/**
	 * Deletes the trust relation for the remote client.
	 */
	public boolean deleteClientTrust(String remoteClientID) {
		TrustEntry trustEntry = getClientTrustEntry(remoteClientID);
		if (trustEntry != null) {
			_keyManager.deleteTrust(trustEntry);
			deleteClientTrustEntry(trustEntry.getRemoteID());
			//_ui.notifyTrustEntry(null);
			return true;
		}
		else 
			return false;	
	}
	
	
	/**
	 * Getter for the RFAJMFProcessor
	 */
	public RFAJMFProcessor getRFAJMFProcessor() {
		return _rfaProcessor;
	}
	
	
	// TEMPORARY
	// registers at a nameserver
	public boolean registerToNameServer(String url) {
        return ((ElkRFAJMFProcessor)getRFAProcessor()).registerDevice(url, getLocalID(), getUrl());
	}
	
	
	
}



