/**
 * Created on May 22, 2006, 11:07:15 AM
 * org.cip4.elk.impl.util.security.AuthenticationServlet.java
 * Project Name: Elk
 */
package org.cip4.elk.impl.util.security;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cip4.elk.impl.spring.ElkSpringConfiguration;
import org.cip4.elk.util.security.AuthenticationHandler;
import org.cip4.elk.util.security.TrustEntry;
import org.springframework.beans.factory.BeanFactory;

/**
 * TODO JAVADOC
 *
 * @author Markus Nyman, (markus@myman.se)
 * 
 */
public class AuthenticationServlet extends HttpServlet implements Servlet {
	
	private static final long serialVersionUID = 13243546576879L;
	
	public static final String SERVLET_NAME = "Authentication Servlet";
	
	private static Log log = LogFactory.getLog(AuthenticationServlet.class);
	
	protected BeanFactory _factory;

	private String nameServerUrl = "http://"; 

	public AuthenticationServlet() {
		super();
	}
	
	
	public void init() throws ServletException {
		super.init();
		log.info("Initializing " + getServletName() + "...");
		initBeanFactory();
		log.info("Initialized " + getServletName() + ".");
	}
	
	/**
	 * Inititializes the Spring BeanFactory.
	 */
	private void initBeanFactory() {
		_factory = ElkSpringConfiguration.getBeanFactory();
	}
	
	public String getServletName() {
		return SERVLET_NAME;
	}
	
	public String getServletInfo() {
		return getServletName(); // TODO Replace this with version info
	}
	
	/*
	 * (non-Java-doc)
	 * 
	 * @see javax.servlet.http.HttpServlet#doGet(HttpServletRequest request,
	 *      HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String command = request.getParameter("cmd");
		//String certID = request.getParameter("id");
		
		if (command == null || command.length() == 0) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		if (command.equals("showAuthentication")) {
			showAuthentication(request, response);
		} else if (command.equals("reject")) {
			reject(request, response);
		} else if (command.equals("accept")) {
			accept(request, response);
		} else if (command.equals("view")) {
			view(request, response);
		} else if (command.equals("new")) {
			newTrustEntry(request, response);
		} else if (command.equals("register")) {
			registerToNameServer(request, response);
		} else {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
			"Unkonwn command.");
		}
	}
	
	/*
	 * (non-Java-doc)
	 * 
	 * @see javax.servlet.http.HttpServlet#doPost(HttpServletRequest request,
	 *      HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
	}
	
	/**
	 * Shows the device's configuration.
	 * @param req
	 * @param res
	 * @throws ServletException
	 * @throws IOException
	 */
	private void accept(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {      
		AuthenticationHandler authHandler = (AuthenticationHandler) _factory.getBean("authHandler");
		String trustID = req.getParameter("id");
		String localRole = req.getParameter("role");
		if (localRole.equals("server")) {
			TrustEntry te = authHandler.getClientTrustEntry(trustID);
			te.setLocalStatus(2);
		}
		else {
			TrustEntry te = authHandler.getServerTrustEntry(trustID);
			te.setLocalStatus(2);

		}	
		res.sendRedirect("auth?cmd=showAuthentication");
	}
	
	/**
	 * Shows all subscriptions registered with the device.
	 * @param req
	 * @param res
	 * @throws ServletException
	 * @throws IOException
	 */
	private void reject(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		AuthenticationHandler authHandler = (AuthenticationHandler) _factory.getBean("authHandler");
		String trustID = req.getParameter("id");
		String localRole = req.getParameter("role");
		if (localRole.equals("server")) {
			TrustEntry te = authHandler.getClientTrustEntry(trustID);
			if (te != null)
				te.setLocalStatus(5);
		}
		else {
			TrustEntry te = authHandler.getServerTrustEntry(trustID);
			if (te != null)
				te.setLocalStatus(5);

		}	
		res.sendRedirect("auth?cmd=showAuthentication");
	}
	
	/**
	 * Deletes the specified subscription.
	 * @param req
	 * @param res
	 * @throws ServletException
	 * @throws IOException
	 */
	private void view(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		AuthenticationHandler authHandler = (AuthenticationHandler) _factory.getBean("authHandler");
		
		String trustID = req.getParameter("id");
		String localRole = req.getParameter("role");
		
		if (localRole.equals("server")) {
			TrustEntry te = authHandler.getClientTrustEntry(trustID);
			req.setAttribute("trustEntry", te);
		}
		else {
			TrustEntry te = authHandler.getServerTrustEntry(trustID);
			req.setAttribute("trustEntry", te);
		}	
		req.getRequestDispatcher("/config/showCert.jsp").forward(req, res);        
	}
	
	
	private void newTrustEntry(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		AuthenticationHandler authHandler = (AuthenticationHandler) _factory.getBean("authHandler");
		
		String remoteID = req.getParameter("name");
		String remoteUrl = req.getParameter("url");
		if (remoteID != null && !remoteID.equals(""))
			authHandler.addAllowedTrustRelation(remoteID, remoteUrl);

		res.sendRedirect("auth?cmd=showAuthentication");
		}

	
	
	/**
	 * 
	 * 
	 * @param req
	 * @param res
	 * @throws ServletException
	 * @throws IOException
	 */
	private void showAuthentication(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		AuthenticationHandler authHandler = (AuthenticationHandler) _factory.getBean("authHandler");
		req.setAttribute("authHandler", authHandler);
		req.setAttribute("nsurl", nameServerUrl);
		req.getRequestDispatcher("/config/showAuthentication.jsp").forward(req, res);
	}
	

	private void registerToNameServer(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		final AuthenticationHandler authHandler = (AuthenticationHandler) _factory.getBean("authHandler");

		final String nameServerUrl = req.getParameter("url");
		if (nameServerUrl != null && authHandler instanceof ElkAuthenticationHandler) {
			this.nameServerUrl = nameServerUrl;
			// Send request later
			Thread thr = new Thread(new Runnable() {
				public void run() {
					try {
						Thread.sleep(2000);
					} catch (Exception e) {
						e.printStackTrace();
					}
					((ElkAuthenticationHandler) authHandler).registerToNameServer(nameServerUrl);
				}
			});
			thr.start();
		}
		res.sendRedirect("auth?cmd=showAuthentication");
	}

	
}
