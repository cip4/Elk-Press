package org.cip4.elk.helk.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cip4.elk.impl.device.process.simulation.ConfSimuHandler;
import org.cip4.elk.impl.device.process.simulation.Setup;
import org.cip4.elk.impl.spring.ElkSpringConfiguration;
import org.springframework.beans.factory.BeanFactory;

/**
 * Servlet implementation class for Servlet. Used to display the simulation
 * phases
 * 
 * @author Marco.Kornrumpf@Bertelsmann.de
 * 
 */
public class SimuServlet extends javax.servlet.http.HttpServlet implements
		javax.servlet.Servlet {

	private static final long serialVersionUID = 5651441075983924460L;

	protected BeanFactory _factory;

	protected ConfSimuHandler _simu;

	protected Setup _setup;

	public SimuServlet() {
		super();

	}

	public void init() throws ServletException {
		super.init();

		initBeanFactory();
		initSimu();
	}

	private void initSimu() {
		_simu = (ConfSimuHandler) _factory.getBean("simu");

	}

	private void initBeanFactory() {
		_factory = ElkSpringConfiguration.getBeanFactory();
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

		if (command == null || command.length() == 0) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		if (command.equals("showConfSimu")) {
			showConfSimu(request, response);
		} else {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
					"Unkonwn command.");
		}
	}

	private void showConfSimu(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {

		req.setAttribute("simu", _simu);
		req.getRequestDispatcher("/simu/confSimu.jsp").forward(req, res);
	}

	/*
	 * (non-Java-doc)
	 * 
	 * @see javax.servlet.http.HttpServlet#doPost(HttpServletRequest request,
	 *      HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}
}
