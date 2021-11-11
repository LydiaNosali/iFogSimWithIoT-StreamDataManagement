package org.StorageMode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;


import org.Results.SaveResults;
import org.apache.commons.math3.geometry.spherical.twod.Circle;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.fog.application.AppEdge;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.cplex.CallCplex;
import org.fog.cplex.DataAllocation;
import org.fog.criticalDataPourcentage.CriticalData;
import org.fog.dataConsistency.QuorumConsistency;
import org.fog.dataConsistency.ReadOneWriteAllConsistency;
import org.fog.dataConsistency.ReadOneWriteOneConsistency;
import org.fog.examples.DataPlacement;
import org.fog.stats.PenalityStats;
import org.fog.lpFileConstuction.BasisDelayMatrix;
import org.fog.lpFileConstuction.ConsProdMatrix;
import org.fog.lpFileConstuction.DataSizeVector;
import org.fog.lpFileConstuction.FreeCapacityVector;
import org.fog.lpFileConstuction.MakeLPFile;
import org.fog.stats.LatencyStats;
import org.fog.stats.Stats;
import org.fog.lpFileConstuction.MakeLPFile2;
import org.fog.pMedianOfAllConsumersShortestPaths.AllShortestPathsNodes;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.pmedian.ConsistencyOverheadExact;
import org.fog.pmedian.ConsistencyOverheadiFogStorS;
import org.fog.pmedian.ConsitencyOverheadiFogStorP;
import org.fog.pmedian.Pmedian;
import org.fog.pmedian.PmedianFormulation;
import org.fog.pmedian.PmedianSolving;
import org.fog.utils.TimeKeeper;
import org.fog2.entities.Actuator;
import org.fog2.entities.FogBroker;
import org.fog2.entities.FogDevice;
import org.fog2.entities.Sensor;

public class FogStorage {

	public static Application application;

	public FogStorage() {

	}
	
	
	public void sim() throws Exception{
		System.out.println("----------------------------------------------------------");
		System.out.println(DataPlacement.FogStorage);
		Log.writeInLogFile("DataPlacement","----------------------------------------------------------");
		Log.writeInLogFile("DataPlacement", DataPlacement.FogStorage);

		org.fog.examples.Log.writeSolvingTime(DataPlacement.nb_HGW,"----------------------------------------------------------------------");
		org.fog.examples.Log.writeSolvingTime(DataPlacement.nb_HGW, "consProd:"+ DataPlacement.nb_DataCons_By_DataProd + "		storage mode:"+ DataPlacement.FogStorage);
		CloudSim.init(DataPlacement.num_user, DataPlacement.calendar, DataPlacement.trace_flag);
		String appId = "Data_Placement"; // identifier of the application
		FogBroker broker = new FogBroker("broker");
		System.out.println("Creating of the Fog devices!");
		Log.writeInLogFile("DataPlacement","Creating of the Fog devices!");
		DataPlacement.createFogDevices();
		//DataPlacement.printDevices();
		
		System.out.println("Creating of Sensors and Actuators!");
		Log.writeInLogFile("DataPlacement","Creating of Sensors and Actuators!");
		DataPlacement.createSensorsAndActuators(broker.getId(), appId);

		/* Module deployment */
		System.out.println("Creating and Adding modules to devices");
		Log.writeInLogFile("DataPlacement","Creating and Adding modules to devices");
		ModuleMapping moduleMapping = ModuleMapping.createModuleMapping(); // initializing a module mapping
		moduleMapping.addModulesToFogDevices();
		moduleMapping.setModuleToHostMap();
		
		application = DataPlacement.createApplication(appId, broker.getId(),DataPlacement.fogDevices);

		application.setUserId(broker.getId());
		application.setFogDeviceList(DataPlacement.fogDevices);

		System.out.println("Controller!");
		Log.writeInLogFile("DataPlacement", "Controller!");
		Controller controller = new Controller("master-controller",DataPlacement.fogDevices, DataPlacement.sensors, DataPlacement.actuators, moduleMapping);
		controller.submitApplication(application, 0);

		TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

		System.out.println("Basis Delay Matrix computing!");
		Log.writeInLogFile("DataPlacement","Basis Delay Matrix computing!");
		BasisDelayMatrix delayMatrix = new BasisDelayMatrix(DataPlacement.fogDevices,DataPlacement.nb_Service_HGW, DataPlacement.nb_Service_LPOP, DataPlacement.nb_Service_RPOP,
				DataPlacement.nb_Service_DC, DataPlacement.nb_HGW, DataPlacement.nb_LPOP, DataPlacement.nb_RPOP, DataPlacement.nb_DC, application);
		
		
		if(DataPlacement.generateInfrastrucutre) {
			
			delayMatrix.getDelayMatrix(DataPlacement.fogDevices);
			
			BasisDelayMatrix.saveBasisDelayMatrix();
			BasisDelayMatrix.savemAdjacenceMatrix();
			BasisDelayMatrix.savemFlowMatrix();
			
			/*
			 * Connecting the application modules (vertices) in the
			 * application model (directed graph) with edges
			 */
			application.addAppEdgesToApplication();
			

			/*
			 * Defining the input-output relationships (represented by
			 * selectivity) of the application modules.
			 */
			application.addTupleMappingFraction();
			

			/* saving the configurations */
			System.out.println("Saving infrastructure ...");
			Log.writeInLogFile("DataPlacement", "Saving infrastructure ...");
			application.saveApplicationEdges();
			application.saveTupleMappingFraction();
			System.out.println("End of saving");
			Log.writeInLogFile("DataPlacement", "End of saving");
			
			
		}else {
			
			System.out.println("Loading ....");
			Log.writeInLogFile("DataPlacement", "Loading ....");
			
			BasisDelayMatrix.loadBasisDelayMatrix();
			BasisDelayMatrix.loadmAdjacenceMatrix();
			BasisDelayMatrix.loadmFlowMatrix();
			

			application.loadApplicationEdges();
			application.loadTupleMappingFraction();
			
			System.out.println("Loaded");
			Log.writeInLogFile("DataPlacement", "Loaded");
			
		}
		
		
		application.setTupleList();
		CriticalData critical = new CriticalData();
		application.setDataConsistencyMap(critical.getCriticalData(application.getTupleList(),DataPlacement.critical_data_pourcentage));
		application.setDataConsistencyProtocol(new QuorumConsistency(DataPlacement.QW, DataPlacement.QR));
		
		
		/* set application in the broker */
		broker.setAppliaction(application);
		
		broker.intializeproduction();
		broker.printAllProductionTimeList();

		/* generate write and read basis delay files */
		delayMatrix.generateBasisWriteDelayFile(DataPlacement.nb_HGW);
		delayMatrix.generateBasisReadDelayFile(DataPlacement.nb_HGW);

		/* generate Data Size vector */
		System.out.println("Generating of Data Size!");
		Log.writeInLogFile("DataPlacement", "Generating of Data Size!");
		DataSizeVector dataSizeVec = new DataSizeVector(application.getEdgeMap(), DataPlacement.nb_Service_HGW,DataPlacement.nb_Service_LPOP, DataPlacement.nb_Service_RPOP, application);
		dataSizeVec.generateDataSizeFile();

		/* generate ConsProd matrix */
		System.out.println("Generating of ConsProdMap!");
		Log.writeInLogFile("DataPlacement","Generating of ConsProdMap!");
		ConsProdMatrix consProdMap = new ConsProdMatrix(application.getEdgeMap(),DataPlacement.nb_Service_HGW, DataPlacement.nb_Service_LPOP, DataPlacement.nb_Service_RPOP, DataPlacement.nb_Service_DC);
		consProdMap.generateConsProdFile();
		
		/* generate Free Capacity vector */
		System.out.println("Generating of Free Capacity!");
		Log.writeInLogFile("DataPlacement","Generating of Free Capacity!");
		FreeCapacityVector freeCapacity = new FreeCapacityVector(DataPlacement.fogDevices, DataPlacement.nb_HGW, DataPlacement.nb_LPOP, DataPlacement.nb_RPOP, DataPlacement.nb_DC);
		freeCapacity.generateFreeCapacityFile();
		// //System.out.println("\n"+freeCapacity.toString());

		System.out.println("Generating of Data Actors!");
		Log.writeInLogFile("DataPlacement","Generating of Data Actors!");
		generateDataActorsFile();

		long begin_t = Calendar.getInstance().getTimeInMillis();
		System.out.println("Making LP file...");
		Log.writeInLogFile("DataPlacement", "Making LP file...");
		MakeLPFile mlpf = new MakeLPFile(DataPlacement.nb_HGW);
		long end_t = Calendar.getInstance().getTimeInMillis();
		long period_tformulation = end_t - begin_t;
		org.fog.examples.Log.writeProblemFormulationTime(DataPlacement.nb_HGW,"consProd:"+ DataPlacement.nb_DataCons_By_DataProd + "		storage mode:"+DataPlacement.storageMode+"		Making Cplex File:"+String.valueOf(period_tformulation));

		int dataHost, dataCons, dataProd;
		dataHost = DataPlacement.nb_HGW + DataPlacement.nb_LPOP + DataPlacement.nb_RPOP + DataPlacement.nb_DC;
		dataProd = DataPlacement.nb_Service_HGW + DataPlacement.nb_Service_LPOP + DataPlacement.nb_Service_RPOP;
		dataCons = DataPlacement.nb_Service_LPOP + DataPlacement.nb_Service_RPOP + DataPlacement.nb_Service_DC;

		begin_t = Calendar.getInstance().getTimeInMillis();
		CallCplex cc = new CallCplex(DataPlacement.nb_HGW + "cplex_"+DataPlacement.nb_DataCons_By_DataProd+".lp", dataProd,dataHost);
		cc.problemSolving(DataPlacement.nb_HGW);
		end_t = Calendar.getInstance().getTimeInMillis();
		long period_tsolving = end_t - begin_t;
		// org.fog.examples.Log.writeLog(nb_HGW,"Solving TimeDP:"+String.valueOf(period_t));

		/* recuperation of data allocation */
		DataAllocation dataAllocation = new DataAllocation();
		if (DataPlacement.generateInfrastrucutre)
			dataAllocation.setDataPlacementMap(DataPlacement.nb_HGW, application);
		else
			dataAllocation.loadDataAllocationMap();
		org.fog.examples.Log.writeDataAllocationStats(DataPlacement.nb_HGW,"------------------------------------------\n"+ DataPlacement.nb_DataCons_By_DataProd+"\n"+
				DataPlacement.storageMode+"\n"+ dataAllocation.dataAllocationStats(application));

		//DataAllocation.printDataAllocationMap(application);
		
		// test storage capacity
		//DataPlacement.testStorageCapacity();
		
		
		broker.setDataAllocation(dataAllocation);
		
		org.fog.examples.Log.writeDataAllocationStats(DataPlacement.nb_HGW,"------------------------------------------\n"+ DataPlacement.nb_DataCons_By_DataProd + "\n"+ DataPlacement.storageMode + "\n"+ dataAllocation.dataAllocationStats(application));
		dataAllocation.saveDataAllocationMap();
		

		dataAllocation.createStorageListIneachStroageNode(application);
		//dataAllocation.printDataAllocationMap(application);

		application.getDataConsistencyProtocol().initializeLockReplicaMap(dataAllocation);
		application.getDataConsistencyProtocol().printLockReplicaMap();
		application.initializeTupeToFogDeviceIdMap();

		CloudSim.broker = broker;
		DataPlacement.sendPerdiodicTuples = true;
		DataPlacement.cond = 0;
		DataPlacement.estimatedTuple = "";
		
		
		
		if(DataPlacement.loadInitial_Stream_Storage)
			broker.loadInitial_Stream_Storage();
		
		if(DataPlacement.loadtime_Stream_consumption)
			broker.loadtime_Stream_consumption();
		
		if(DataPlacement.load_stream_consumersList)
			broker.loadstream_consumersList();
		
		
		broker.initial_Stream_Data_Creation();
		broker.initial_Stream_Data_Consumption();

		
		
//		if(DataPlacement.use_pmedian)
//			broker.computeP_medianStreamEmplacement();
		
		broker.printStreamPlacementMap();
		
		//System.exit(0);
		
		CloudSim.startSimulation();
			
		

		application.getDataConsistencyProtocol().addBlockedWritesLatency();
		
		CloudSim.stopSimulation();
		
		System.out.println("End of simulation!");

//		System.out.println(DataPlacement.storageMode);
//		System.out.println("Read Latency:"+ LatencyStats.getOverall_read_Latency());
//		System.out.println("Write Latency:"+ LatencyStats.getOverall_write_Latency());
//		System.out.println("Overall Latency:"+ LatencyStats.getOverall_Latency());
//
//	
//		Log.writeInLogFile("DataPlacement", DataPlacement.storageMode);
//		Log.writeInLogFile("DataPlacement", "Read Latency:"+ LatencyStats.getOverall_read_Latency());
//		Log.writeInLogFile("DataPlacement", "Write Latency:"+ LatencyStats.getOverall_write_Latency());
//		Log.writeInLogFile("DataPlacement", "Overall Latency:"+ LatencyStats.getOverall_Latency());
//
//		PenalityStats.printallpenalitystats();
		
		System.out.println("==========================================");
		
		System.out.println("Nombre d'overflow:"+Stats.nb_overflow);
		System.out.println();
		
		System.out.println("Temps de formulation p-median:"+Stats.pmedianFormulationTime);
		System.out.println("Temps de résolution p-median:"+Stats.pmedianSolvingTime);
		System.out.println();
		
		System.out.println("Temps de formulation Gap:"+period_tformulation);
		System.out.println("Temps de résolution Gap:"+period_tsolving);
		System.out.println();
		PenalityStats.printallpenalitystats();
		
		//System.out.println("nb_overflow_perdevname : "+broker.nb_overflow_perdevname.toString());
		
//		System.out.println("Nb overflow  per level");
//		System.out.println("nb_overflow_DC : "+FogBroker.nb_overflow_perdevname.get("DC"));
//		System.out.println("nb_overflow_RPOP : "+FogBroker.nb_overflow_perdevname.get("RPOP"));
//		System.out.println("nb_overflow_LPOP : "+FogBroker.nb_overflow_perdevname.get("LPOP"));
//		System.out.println("nb_overflow_HGW : "+FogBroker.nb_overflow_perdevname.get("HGW"));
		
		System.out.println("Cache Hit per level");
		System.out.println("nb_hit_DC : "+LatencyStats.cache_hit_perlevel.get("DC"));
		System.out.println("nb_hit_RPOP : "+LatencyStats.cache_hit_perlevel.get("RPOP"));
		System.out.println("nb_hit_LPOP : "+LatencyStats.cache_hit_perlevel.get("LPOP"));
		System.out.println("nb_hit_HGW : "+LatencyStats.cache_hit_perlevel.get("HGW"));
		
		
		//SAVE RESULTS
		try {
			//System.out.println("save Storage capacities");
			FileWriter file = new FileWriter("RESULTS.txt",true);
			BufferedWriter fw = new BufferedWriter(file);
			
			//param
			
			//infra
			fw.write("taille infra :"+ "\t" + DataPlacement.nb_HGW + "\t" + "nb_cache : "+ "\t" + DataPlacement.NB_cache + "\t" + "taille du cache : " + "\t" + DataPlacement.cache+"\n");
			
			fw.write("stratégie : " + "\t" + DataPlacement.Strategy+ "\n");
			
			//zipf number
			fw.write("zipf : " + "\t" + DataPlacement.zipf+ "\n");
			
			//overflow et p-median  
			fw.write("use_overflow : " + "\t" + DataPlacement.use_overflow + "\t" + "use_pmedian : " + "\t" + DataPlacement.use_pmedian + "\n");
			
			//latence
			fw.write("Overall Latency : " + "\t" + LatencyStats.getOverall_Latency() + "\n");
			
			//Penalite
			double Overall_penalityIoT = PenalityStats.Overall_read_penalityIoT + PenalityStats.Overall_write_penalityIoT;
			
			fw.write("Overal Stream Penality : " + "\t" + PenalityStats.Overall_read_penalityStream + "\n");
			fw.write("Overal IoT Penality : " + "\t" + Overall_penalityIoT + "\n");
			fw.write("Overal Penality : " + "\t" + PenalityStats.Overall_penality + "\n");
			
			//nb debordement
			fw.write("Nombre d'overflow : " + "\t" + Stats.nb_overflow + "\n");
			
			//temps de GAP et P-median
			long p_median = Stats.pmedianFormulationTime+Stats.pmedianSolvingTime;
			long gap = period_tformulation+period_tsolving;
			
			fw.write("Temps p-median : " + "\t" + p_median + "\n");
			fw.write("Temps Gap : " + "\t" + gap + "\n");
			
			//cache_hit per level
			fw.write("nb_hit_DC : " + "\t"+LatencyStats.cache_hit_perlevel.get("DC") + "\n");
			fw.write("nb_hit_RPOP : " + "\t"+LatencyStats.cache_hit_perlevel.get("RPOP") + "\n");
			fw.write("nb_hit_LPOP : " + "\t"+LatencyStats.cache_hit_perlevel.get("LPOP") + "\n");
			fw.write("nb_hit_HGW : " + "\t"+LatencyStats.cache_hit_perlevel.get("HGW") + "\n");
			
			
			fw.write("==============="+ "\t"+ "\n");
			fw.close();
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		
//		Log.writeInLogFile("DataPlacement", "Read Penality:"+ PenalityStats.getOverall_read_penality());
//		Log.writeInLogFile("DataPlacement", "Write Penality:"+ PenalityStats.getOverall_write_penality());
//		Log.writeInLogFile("DataPlacement", "Overall Penality:"+ PenalityStats.getOverall_penality());
		
//		SaveResults.saveLatencyTimes(DataPlacement.nb_DataCons_By_DataProd, DataPlacement.storageMode, -1,
//				LatencyStats.getOverall_read_Latency(),
//				LatencyStats.getOverall_write_Latency(),
//				LatencyStats.getOverall_Latency());
//
//		LatencyStats.reset_Overall_Letency();
//		LatencyStats.reset_Overall_write_Letency();
//		LatencyStats.reset_Overall_read_Letency();
		
		SaveResults.saveAllStats();
		LatencyStats.saveLatencyMap(0);

		LatencyStats.reset_LatencyMapStats();
		LatencyStats.reset_ALLStats();
		org.fog.stats.PenalityStats.reset_Penality_Stats();
		Stats.reset_AllStats();

		application.getDataConsistencyProtocol().clearConsistencyData();
		application.resetStoredData();
		System.gc();

		org.fog.examples.Log.writeNbCombinaison(DataPlacement.nb_HGW, DataPlacement.Consistencyprotocol);
		org.fog.examples.Log.writeSimulationTime(DataPlacement.nb_HGW,DataPlacement.Consistencyprotocol);
		
		

		PenalityStats.reset_Penality_Stats();
		System.out.println("VRGame finished!");

//		DataPlacement.fogDevices.clear();
//		DataPlacement.sensors.clear();
		DataPlacement.actuators.clear();
		System.gc();
		System.exit(0);

	}

	private long allCombinExact() {
		long nb_combin = 0;
		
		int nb_nodes = DataPlacement.nb_DC + DataPlacement.nb_RPOP+ DataPlacement.nb_LPOP + DataPlacement.nb_HGW;
		int nb_tuple = application.getEdges().size()- (DataPlacement.nb_HGW * DataPlacement.nb_SnrPerHGW + DataPlacement.nb_HGW * DataPlacement.nb_ActPerHGW);
		
		System.out.println("nb_nodes=" + nb_nodes + "\tnb_tuples=" + nb_tuple);
		System.out.println("DataPlacement.min_data_replica="+ DataPlacement.min_data_replica+ "\tDataPlacement.max_data_replica"+ DataPlacement.max_data_replica);
		
		for (int i = DataPlacement.min_data_replica; i < DataPlacement.max_data_replica + 1; i++) {
			nb_combin = nb_combin+ getCombin(i, nb_nodes);
		}

		return nb_combin * nb_tuple;
	}

	private long getCombin(int p, int n) {
		// TODO Auto-generated method stub
		long f = fact(n) / (fact(p) * fact(n - p));
		System.out.println("P=" + p + "\tn=" + n+"\tC_n^p = "+f);
		return f;
	}

	private long fact(int f) {
		long fa = 1;
		for (int i = 1; i < f + 1; i++) {
			fa = fa *i;
		}
		return fa;
	}

	private void generateDataActorsFile() {
		int dataHost, dataCons, dataProd;
		dataHost = DataPlacement.nb_HGW + DataPlacement.nb_LPOP
				+ DataPlacement.nb_RPOP + DataPlacement.nb_DC;
		dataProd = DataPlacement.nb_Service_HGW + DataPlacement.nb_Service_LPOP
				+ DataPlacement.nb_Service_RPOP;
		dataCons = DataPlacement.nb_Service_HGW + DataPlacement.nb_Service_LPOP
				+ DataPlacement.nb_Service_RPOP + DataPlacement.nb_Service_DC;

		File fichier = new File(DataPlacement.nb_HGW + "dataActors_"
				+ DataPlacement.nb_DataCons_By_DataProd + ".txt");
		FileWriter fw;
		try {
			fw = new FileWriter(fichier);
			fw.write(dataHost + "\t");
			fw.write(dataProd + "\t");
			fw.write(dataCons + "\t");
			fw.write(DataPlacement.Basis_Exchange_Unit + "\t");
			fw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
