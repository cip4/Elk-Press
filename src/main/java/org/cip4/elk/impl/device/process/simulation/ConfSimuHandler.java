package org.cip4.elk.impl.device.process.simulation;

import java.util.ArrayList;

/**
 * Configuration of simulation phases has to be done here. Main focus is to fill
 * the <code>simuPhases</code> ArrayList and pass it to the
 * {@link ConventionalPrintingProcessSimu}. This arrayList is also used in the
 * {@link SimuServlet} for the JSP based visualisation.
 * 
 * @author Marco.Kornrumpf@Bertelsmann.de
 * 
 * 
 */
public class ConfSimuHandler {

	private ArrayList simuPhases = null;

	/**
	 * Init methode used by the SimuServlet
	 * 
	 */
	public void init() {
		buildSimuPhases();
	}

	/**
	 * Default constructure used by the ConventionalPrintingProcessSimu
	 * 
	 */
	public ConfSimuHandler() {
		buildSimuPhases();
	}

	/**
	 * Filles a ArrayList with specific simulation phases. Hard coded part to
	 * build a simulation
	 * 
	 */
	private void buildSimuPhases() {

		simuPhases = new ArrayList();

		Setup setup1 = new Setup(5, "Device warming up... Waste=0, Time=5");
		setup1.setStatusDetails(DeviceStatusDetails.WARMINGUP);
		setup1.setDeviceSpeed(0);
		simuPhases.add(setup1);

		Setup setup2 = new Setup(5,
				"Format change for new media size... Waste=0, Time=5");
		setup2.setDeviceSpeed(0);
		setup2.setStatusDetails(DeviceStatusDetails.SIZECHANGE);
		simuPhases.add(setup2);

		Setup setup3 = new Setup(5,
				"Formchange for the new job... Waste=0, Time=5");
		setup3.setDeviceSpeed(0);
		setup3.setStatusDetails(DeviceStatusDetails.FORMCHANGE);
		simuPhases.add(setup3);

		Setup setup4 = new Setup(5,
				"General Setup before starting printing... Waste=0, Time=5");
		setup4.setDeviceSpeed(0);
		setup4.setStatusDetails(DeviceStatusDetails.WASTE);
		simuPhases.add(setup4);

		Running run1 = new Running("Running but producing waste... Waste=100");
		run1.setStatusDetails(DeviceStatusDetails.WASTE);
		run1.setDeviceSpeed(10000);
		run1.setGoodVariance(0);
		run1.setWasteVariance(1);
		simuPhases.add(run1);

		Running run2 = new Running("Running producing good... Good=200");
		run2.setStatusDetails(DeviceStatusDetails.GOOD);
		run2.setDeviceSpeed(20000);
		run2.setGoodVariance(20);
		run2.setWasteVariance(0);
		simuPhases.add(run2);

		Stopped stop1 = new Stopped(5,
				"Customer Approval needed... Waste=0, Time=5");
		stop1.setStatusDetails(DeviceStatusDetails.WAITFORAPPROVAL);
		simuPhases.add(stop1);

		Stopped stop2 = new Stopped(5,
				"New resources as demanded by the Customer not available... Waste=0, Time=5");
		stop2.setStatusDetails(DeviceStatusDetails.MISSRESOURCES);
		simuPhases.add(stop2);

		Setup setup5 = new Setup(5,
				"General Setup before starting printing... Waste=0, Time=5");
		setup5.setDeviceSpeed(0);
		setup5.setStatusDetails(DeviceStatusDetails.WASTE);
		simuPhases.add(setup5);

		Running run3 = new Running("Running but producing waste... Waste=50");
		run3.setStatusDetails(DeviceStatusDetails.WASTE);
		run3.setDeviceSpeed(10000);
		run3.setGoodVariance(0);
		run3.setWasteVariance(5);
		simuPhases.add(run3);

		Running run4 = new Running("Running producing good... Good=200");
		run4.setStatusDetails(DeviceStatusDetails.GOOD);
		run4.setDeviceSpeed(20000);
		run4.setGoodVariance(20);
		run4.setWasteVariance(0);
		simuPhases.add(run4);

		Stopped stop3 = new Stopped(5,
				"New resources as demanded by the Customer not available... Waste=0, Time=5");
		stop3.setStatusDetails(DeviceStatusDetails.PAUSE);
		simuPhases.add(stop3);

		Setup setup6 = new Setup(5,
				"General Setup before starting printing... Waste=0, Time=5");
		setup6.setDeviceSpeed(0);
		setup6.setStatusDetails(DeviceStatusDetails.WASTE);
		simuPhases.add(setup6);

		Running run5 = new Running("Running but producing waste... Waste=50");
		run5.setStatusDetails(DeviceStatusDetails.WASTE);
		run5.setDeviceSpeed(10000);
		run5.setGoodVariance(0);
		run5.setWasteVariance(5);
		simuPhases.add(run5);

		Setup setup7 = new Setup(5, "BlanketWash... Waste=0, Time=5");
		setup7.setDeviceSpeed(0);
		setup7.setStatusDetails(DeviceStatusDetails.BLANKETWASH);
		simuPhases.add(setup7);

		Running run6 = new Running("Running but producing waste... Waste=50");
		run6.setStatusDetails(DeviceStatusDetails.WASTE);
		run6.setDeviceSpeed(10000);
		run6.setGoodVariance(0);
		run6.setWasteVariance(5);
		simuPhases.add(run5);

		Stopped stop4 = new Stopped(5, "Generally Maintenance... Time=5");
		stop4.setStatusDetails(DeviceStatusDetails.MAINTENANCE);
		simuPhases.add(stop4);

		Stopped stop5 = new Stopped(5, "BlanketChange ... Time=5");
		stop5.setStatusDetails(DeviceStatusDetails.BLANKETCHANGE);
		simuPhases.add(stop5);

		Stopped stop6 = new Stopped(5,
				"Sleeves in a Sheetfeed press? ... Time=5");
		stop6.setStatusDetails(DeviceStatusDetails.SLEEVECHANGE);
		simuPhases.add(stop6);

		Setup setup8 = new Setup(5,
				"General Setup before starting printing... Waste=0, Time=5");
		setup8.setDeviceSpeed(0);
		setup8.setStatusDetails(DeviceStatusDetails.WASTE);
		simuPhases.add(setup8);

		Running run7 = new Running("Running but producing waste... Waste=50");
		run7.setStatusDetails(DeviceStatusDetails.WASTE);
		run7.setDeviceSpeed(10000);
		run7.setGoodVariance(0);
		run7.setWasteVariance(5);
		simuPhases.add(run7);

		Running run8 = new Running("Running producing good... Good=50");
		run8.setStatusDetails(DeviceStatusDetails.GOOD);
		run8.setDeviceSpeed(20000);
		run8.setGoodVariance(5);
		run8.setWasteVariance(0);
		simuPhases.add(run8);

		Down down1 = new Down(5, "Technical breakdown... Time= 5");
		down1.setStatusDetails(DeviceStatusDetails.BREAKDOWN);
		simuPhases.add(down1);

		Down down2 = new Down(5, "General repair... Time= 5");
		down2.setStatusDetails(DeviceStatusDetails.REPAIR);
		simuPhases.add(down2);

		Setup setup9 = new Setup(5,
				"General Setup before starting printing... Waste=0, Time=5");
		setup9.setDeviceSpeed(0);
		setup9.setStatusDetails(DeviceStatusDetails.WASTE);
		simuPhases.add(setup9);

		Running run9 = new Running("Running but producing waste... Waste=50");
		run9.setStatusDetails(DeviceStatusDetails.WASTE);
		run9.setDeviceSpeed(10000);
		run9.setGoodVariance(0);
		run9.setWasteVariance(5);
		simuPhases.add(run9);

		Running run10 = new Running("Running producing good... Good=200");
		run10.setStatusDetails(DeviceStatusDetails.GOOD);
		run10.setDeviceSpeed(20000);
		run10.setGoodVariance(20);
		run10.setWasteVariance(0);
		simuPhases.add(run10);

		Stopped stop7 = new Stopped(5, "General breakdown... Time=5");
		stop7.setStatusDetails(DeviceStatusDetails.FAILURE);
		simuPhases.add(stop7);

		Stopped stop8 = new Stopped(5, "PaperJam in press... Time=5");
		stop8.setStatusDetails(DeviceStatusDetails.PAPERJAM);
		simuPhases.add(stop8);

		Stopped stop9 = new Stopped(5,
				"Cover open machine is stopped... Time=5");
		stop9.setStatusDetails(DeviceStatusDetails.COVEROPEN);
		simuPhases.add(stop9);

		Stopped stop10 = new Stopped(5,
				"Door ?!? open machine is stopped... Time=5");
		stop10.setStatusDetails(DeviceStatusDetails.DOOROPEN);
		simuPhases.add(stop10);

		Setup setup10 = new Setup(5,
				"General Setup before starting printing... Waste=0, Time=5");
		setup10.setDeviceSpeed(0);
		setup10.setStatusDetails(DeviceStatusDetails.WASTE);
		simuPhases.add(setup10);

		Running run11 = new Running("Running but producing waste... Waste=50");
		run11.setStatusDetails(DeviceStatusDetails.WASTE);
		run11.setDeviceSpeed(10000);
		run11.setGoodVariance(0);
		run11.setWasteVariance(5);
		simuPhases.add(run11);

		Running run12 = new Running(
				"Running producing good... Good=Rest to be produced");
		run12.setStatusDetails(DeviceStatusDetails.GOOD);
		run12.setDeviceSpeed(20000);
		run12.setGoodVariance(35);
		run12.setWasteVariance(0);
		simuPhases.add(run12);

		Cleanup clean1 = new Cleanup(5, "General cleanup... Time=5");
		clean1.setStatusDetails(DeviceStatusDetails.WASHUP);

		simuPhases.add(clean1);

		Cleanup clean2 = new Cleanup(5, "Cleaning plates... Time=5");
		clean2.setStatusDetails(DeviceStatusDetails.PLATEWASH);

		simuPhases.add(clean2);

		Cleanup clean3 = new Cleanup(5,
				"Cleaning counter pressure cylinder... Time=5");
		clean3.setStatusDetails(DeviceStatusDetails.CYLINDERWASH);

		simuPhases.add(clean3);

		Cleanup clean4 = new Cleanup(5, "Cleaning damping roller... Time=5");
		clean4.setStatusDetails(DeviceStatusDetails.DAMPINGROLLERWASH);

		simuPhases.add(clean4);

		Cleanup clean5 = new Cleanup(5, "Cleaning ink fountain... Time=5");
		clean5.setStatusDetails(DeviceStatusDetails.CLEANINGINKFOUNTAIN);

		simuPhases.add(clean5);

		Cleanup clean6 = new Cleanup(5, "Cleaning ink roller... Time=5");
		clean6.setStatusDetails(DeviceStatusDetails.INKROLLERWASH);

		simuPhases.add(clean6);

	}

	/**
	 * Method to get the simulation phases
	 * 
	 * @return Simulation phases stored in a ArrayList
	 */
	public ArrayList getSimuPhases() {

		return simuPhases;
	}

}
