package org.fog2.entities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import org.StorageMode.FogStorage;
import org.apache.commons.math3.distribution.ZipfDistribution;
import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.power.PowerDatacenterBroker;
import org.fog.application.AppEdge;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.cplex.DataAllocation;
import org.fog.examples.DataPlacement;
import org.fog.lpFileConstuction.BasisDelayMatrix;
import org.fog.lpFileConstuction.MakeLPFile;
import org.fog.stats.LatencyStats;
import org.fog.stats.PenalityStats;
import org.fog.stats.Stats;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacement;
import org.fog.pmedian.PmedianFormulation;
import org.fog.pmedian.PmedianSolving;
import org.fog.utils.Config;
import org.fog.utils.FogEvents;
import org.fog.utils.FogUtils;
import org.fog.utils.Logger;

public class FogBroker extends PowerDatacenterBroker{
	
	//public static Map<String, List<Integer>> cachePlacementMap = new HashMap<String, List<Integer>>();
	
	public static Application application;
	private DataAllocation dataAllocation;
	
	public static List<Integer> fogDeviceListIds;
	public static Map<String, Integer> nb_overflow_perdevname =new HashMap<String, Integer>();
	/*
	 * Map<Pair<Tuple,rep>,ackBoolean>
	 */
	private Map<Pair<Tuple,Integer>,Boolean> storageACKTestingMap = new HashMap<Pair<Tuple,Integer>,Boolean>();
	
	public static Map<String, List<Double>> productionListMap = new HashMap<String, List<Double>>();
	public static List<Tuple> list_streaming_data = new ArrayList<Tuple>();
	
	public static Map<String,Tuple> list_streaming_dataByName = new HashMap<String, Tuple>();
	
	public static Map<String,List<Integer>> list_actif_consumersbyStream = new HashMap<String,List<Integer>>();

	public FogBroker(String name) throws Exception {
		super(name);
		// TODO Auto-generated constructor stub
		
		fogDeviceListIds = new ArrayList<Integer>();
		
		nb_overflow_perdevname.put("DC", 0);
		nb_overflow_perdevname.put("RPOP", 0);
		nb_overflow_perdevname.put("LPOP", 0);
		nb_overflow_perdevname.put("HGW", 0);
		
		int max = 3 + DataPlacement.nb_HGW + DataPlacement.nb_LPOP + DataPlacement.nb_RPOP +DataPlacement.nb_DC;
		
		for (int i = 3; i < max; i++) {
			fogDeviceListIds.add(i);
		}
	}

	@Override
	public void startEntity() {
		// TODO Auto-generated method stub
		//**System.out.println(getName()+" is starting...");
		
		
		LatencyStats.init_Cache_Hit();
		// IoT data creation and consumption
		intializeproduction2();
		initializeDataRetrieveRequests_global(application);

		
		// Stream data creation and consumption
		do_Send_Initial_Stream_Storage();
		
		if(DataPlacement.use_pmedian)
			try {
				computeP_medianStreamEmplacement();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
//		printStreamPlacementMap();
		do_Send_Create_Retrieve_Stream();
		
	}
	@Override
	
	
	public void processEvent(SimEvent ev) {
		// TODO Auto-generated method stub
		switch(ev.getTag()){
		
		case FogEvents.TUPLE_STORAGE:
			processTupleStorage(ev);
			break;
			
		case FogEvents.TUPLE_STORAGE_ACK:
			processTupleStorageACK(ev);
			break;
			
		case FogEvents.DELAY_TUPLE_STORAGE_ACK:
			processDelayTupleStorageACK(ev);
			break;
			
		case FogEvents.BLOCKED_WRITE_STORAGE:
			processBlockedWriteStorage(ev);
			break;
			
		case FogEvents.TUPLE_RETRIEVE:
			processTupleRetrieve(ev);
			break;
			
		case FogEvents.TUPLE_RETRIEVE_ACK:
			processTupleRetrieveACK(ev);
			break;
			
		case FogEvents.NON_TUPLE_RETRIEVE:
			processNONTupleRetrieveACK(ev);
			break;
			
		case FogEvents.RETRIEVE_STREAM_DATA:
			processStreamDataRetrieve(ev);
			break;
		
			
//		case FogEvents.INITIALIZE_PERIODIC_TUPLE_PRODUCTION:
//			intializePeriodicTupleProduction();
//			break;	
			
			
		default:
			System.out.println("error!!! There is no other event to broker:"+ev.toString());
			System.exit(0);
			break;
		}
	}

	

	public void printStreamPlacementMap() {
		// TODO Auto-generated method stub
		//**System.out.println("printStreamPlacementMap");
		for(String tupleType : DataAllocation.cachePlacementMap.keySet()) {
			//**System.out.println(tupleType+" -> "+DataAllocation.cachePlacementMap.get(tupleType).toString());
		}
	}
	
	public void computeP_medianStreamEmplacement() throws IOException, InterruptedException {
		//**System.out.println("Compute P median emplacement for all data Stream");
		int i = 0;
		System.out.println("DataAllocation.cachePlacementMap"+DataAllocation.cachePlacementMap.toString());
		
		for(String streamName : DataAllocation.cachePlacementMap.keySet()) {
			//**System.out.println();
			Tuple stream = list_streaming_dataByName.get(streamName);
			
			double streamSize = (double) stream.getCloudletFileSize();
			int basisTransferUnit = DataPlacement.Basis_Exchange_Unit;
			int nb_median = DataPlacement.NB_cache;
			List<Integer> nodesList = fogDeviceListIds;
			List<Integer> consumerList = list_actif_consumersbyStream.get(streamName);
			
			//**System.out.println("compute P-median for:"+streamName);
			//**System.out.println("Stream size:"+streamSize);
			//**System.out.println("consumers:"+consumerList.toString());
			//**System.out.println("Nodes:"+nodesList.toString());
			
			
			List<Integer> pmedian = getPmedian(streamSize, basisTransferUnit, nb_median, nodesList, consumerList, i);
			DataAllocation.cachePlacementMap.get(streamName).addAll(pmedian);
			
			//**System.out.println(streamName+" medians:"+DataAllocation.cachePlacementMap.get(streamName).toString());
			
			for(int median : pmedian) {
				send(median, 0, FogEvents.STREAM_INITIAL_STORAGE, stream);
				//**System.out.println(this.getName() + ": send FogEvents.STREAM_INITIAL_STORAGE to median:" + median + " Stream_data:" + streamName);
			}
			
			i++;
			//**System.out.println();

		}
	}


	private List<Integer> getPmedian(double streamSize, int basisTransferUnit, int nb_median, List<Integer> nodesList, List<Integer> consumerList, int i) 
			throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		
		
		//**System.out.println("P-median formualtion");
		long begin_t = Calendar.getInstance().getTimeInMillis();
		PmedianFormulation pMedianFormulation = new PmedianFormulation(nb_median);
		pMedianFormulation.contructionLpFile(3, streamSize, basisTransferUnit, consumerList, nodesList, i);
		long end_t = Calendar.getInstance().getTimeInMillis();
		Stats.pmedianFormulationTime += end_t - begin_t;
		
		
		
		//**System.out.println("P-median solving");
		begin_t = Calendar.getInstance().getTimeInMillis();
		PmedianSolving pMedianSolving = new PmedianSolving();
		pMedianSolving.problemSolving(nodesList, nodesList, i);
		end_t = Calendar.getInstance().getTimeInMillis();
		Stats.pmedianSolvingTime += end_t - begin_t;
		
		
		List<Integer> medians = pMedianSolving.getSolution(nodesList, nodesList,i);		
		return medians;
	}
	
	
	public void do_Send_Initial_Stream_Storage() {
		
		// TODO Auto-generated method stub
		// Streaming data creation
		
		//**System.out.println("initial_Stream_Data_Creation in FogBroker");

		for (Tuple tuple:list_streaming_data) {
			int dc_id;
			
			if(DataPlacement.loadInitial_Stream_Storage) {
				dc_id = DataAllocation.intial_cachePlacementMap.get(tuple.getTupleType());
				
			}else {
				dc_id = (int) (Math.random() * DataPlacement.nb_DC) + 3;
				
				DataAllocation.intial_cachePlacementMap.put(tuple.getTupleType(), dc_id);
			}
			
			send(dc_id, 0, FogEvents.STREAM_INITIAL_STORAGE, tuple);
			////**System.out.println(this.getName() + ": send FogEvents.STREAM_INITIAL_STORAGE to DC:" + dc_id + " Stream_data:" + tuple.getTupleType());
			
			//initial nodes for the stream nodes
			List<Integer> initial_stream_nodes = new ArrayList<Integer>();
			initial_stream_nodes.add(dc_id);
			
			//mise à jour de cachePlacementMap pour rajouter les emplacements initiaux des données dans le cloud
			DataAllocation.cachePlacementMap.put(tuple.getTupleType(), initial_stream_nodes);		
		}
	}
	
	
	public void saveInitial_Stream_Storage() {
		FileWriter writefile;
		
		try {
			
			writefile = new FileWriter("Initial_Stream_Storage.txt");
			BufferedWriter fw = new BufferedWriter(writefile);
			
			for (Tuple tuple:list_streaming_data) {
				
				int dc_id = DataAllocation.cachePlacementMap.get(tuple.getTupleType()).get(0);
				fw.write(tuple.getTupleType()+"\t"+dc_id+"\n");
						
			}
			
			fw.close();
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void loadInitial_Stream_Storage() throws FileNotFoundException, InterruptedException {
		//**System.out.println("loadInitial_Stream_Storage");
		
		FileReader fichier = new FileReader("Initial_Stream_Storage.txt");
		BufferedReader in = null;
		try {

			in = new BufferedReader(fichier);
			String line = null;

			while ((line = in.readLine()) != null) {
				String tupletype;
				int dc_id;

				String[] splited = line.split("\t");
				tupletype = splited[0];
				dc_id = Integer.valueOf(splited[1]);
				
				DataAllocation.intial_cachePlacementMap.put(tupletype, dc_id);

			}
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public void initial_Stream_Data_Creation() {
		// TODO Auto-generated method stub
		// Streaming data creation
		
		//**System.out.println("initial_Stream_Data_Creation");

		Application app = CloudSim.broker.getAppliaction();

		for (int i = 0; i < DataPlacement.nb_streaming_data; i++) {

			long cpuLength = (long) DataPlacement.HGW_TUPLE_CPU_SIZE;
			long nwLength = (long) DataPlacement.size_streaming_data;

			Tuple tuple = new Tuple(app.getAppId(), FogUtils.generateTupleId(), Tuple.UP, cpuLength, 1, nwLength,
					nwLength, new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
			tuple.setUserId(app.getUserId());
			tuple.setTupleType("Stream" + i);

			tuple.setDestModuleName(null);
			tuple.setSrcModuleName(null);
			Logger.debug(getName(), "Sending tuple with tupleId = " + tuple.getCloudletId());
//			Logger.debug(getName(), "Sending tuple "+tuple.getCloudletId()+"to "+tuple.getDestModuleName()+" with delay="+delay);

			tuple.setActualTupleId(i);
			tuple.setTupleVersion(1);

			list_streaming_data.add(tuple);
			list_streaming_dataByName.put(tuple.getTupleType(),tuple);
		}
	}

	public void do_Send_Create_Retrieve_Stream() {
		// TODO Auto-generated method stub
		//Streaming data consumption requests 
		
		//**System.out.println("initial_Stream_Data_Consumption in FogBroker");
//		String[] words= {"A","A","B","B","C","D","J","A","B","B","C","D","A","B","B","C","D","A","B","B","C","D"};
//	    ZipfDistribution zipfDistribution = new ZipfDistribution(DataPlacement.nb_streaming_data- 1, 1);
//        for (int i = 0; i < 10; i++) {
//            int sample = zipfDistribution.sample();
//            System.out.print("sample[" + sample + "]: ");
//            System.out.println(words[sample]);
//        }
        
		
		
		for (int i=30000;i<Config.MAX_SIMULATION_TIME;i+=30000) {
			//choose the consumed data
			String consumed_data;
			
			if(DataPlacement.loadtime_Stream_consumption) {
				consumed_data = DataAllocation.time_Stream_consumption.get(i);
				
			}else {
				ZipfDistribution zipfDistribution = new ZipfDistribution(list_streaming_data.size()-1, DataPlacement.zipf);
				//int k = (int) (Math.random()* list_streaming_data.size());
				int k = zipfDistribution.sample();
				consumed_data = list_streaming_data.get(k).getTupleType();
				DataAllocation.time_Stream_consumption.put(i,consumed_data);
//				System.out.println("zipf consumed data"+consumed_data);
//				System.out.println();
			}
			
			
			//get la liste des consumers actifs for the stream
			List<Integer> actif_consumers = list_actif_consumersbyStream.get(consumed_data);
			
			for (int id_hgw : actif_consumers) {
				//send to fogdevice - hgw the requests
				send(id_hgw, i, FogEvents.CREATE_RETRIEVE_STREAM_DATA, consumed_data);
				//**System.out.println(this.getName()+": send FogEvents.CREATE_RETRIEVE_STREAM_DATA to hgw consumer:"+ id_hgw+" consumed_data:"+consumed_data+" time:"+i);
			}
			////**System.out.println();
		}
		
	}
	
	public void savetime_Stream_consumption() {
		FileWriter writefile;
		
		try {
			
			writefile = new FileWriter("time_Stream_consumption.txt");
			BufferedWriter fw = new BufferedWriter(writefile);
			
			for (int i : DataAllocation.time_Stream_consumption.keySet()) {
				
				String stream = DataAllocation.time_Stream_consumption.get(i);
				fw.write(i+"\t"+stream+"\n");
						
			}
			
			fw.close();
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void loadtime_Stream_consumption() throws FileNotFoundException, InterruptedException {
		//**System.out.println("loadtime_Stream_consumption");
		
		FileReader fichier = new FileReader("time_Stream_consumption.txt");
		BufferedReader in = null;
		try {

			in = new BufferedReader(fichier);
			String line = null;

			while ((line = in.readLine()) != null) {
				String tupletype;
				int time;

				String[] splited = line.split("\t");
				tupletype = splited[1];
				time = Integer.valueOf(splited[0]);
				
				DataAllocation.time_Stream_consumption.put(time,tupletype);

			}
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	////////////////////////////Loi de poisson : entrée : lambda, sortie : nb_consumers
	private static int getPoissonRandom(double mean) {
	    Random r = new Random();
	    double L = Math.exp(-mean);
	    int k = 0;
	    double p = 1.0;
	    do {
	        p = p * r.nextDouble();
	        k++;
	    } while (p > L);
	    return k - 1;
	}
	////////////////////////////////////////
	
	public void initial_Stream_Data_Consumption() {
		// TODO Auto-generated method stub
		//Streaming data consumption requests 
		
		//**System.out.println("initial_Stream_Data_Consumption");
		
		
        
        
		for(Tuple stream : list_streaming_data) {
			
			List<Integer> actif_consumers = new ArrayList<Integer>();
			
			//int nb_cons_actif= (int) (Math.random()*DataPlacement.nb_streaming_cons);
//			double poisson= getPoissonRandom(5);		
//			System.out.println("poisson = "+poisson);
			
			int nb_cons_actif= getPoissonRandom(DataPlacement.nb_consperstream);
			
			//int nb_cons_actif= DataPlacement.nb_consperstream;
			//**System.out.println(stream.getTupleType());
			////**System.out.println("nb actif consumers:"+nb_cons_actif);
			
			if(DataPlacement.load_stream_consumersList) {
				actif_consumers = DataAllocation.stream_consumersList.get(stream.getTupleType());
			
			}else {
				for (int j=0;j<nb_cons_actif;j++) {
					int actif_cons = (int) (Math.random()*DataPlacement.nb_HGW) + DataPlacement.nb_DC + DataPlacement.nb_RPOP + DataPlacement.nb_LPOP + 3;
					
					while(actif_consumers.contains(actif_cons)) {
						actif_cons = (int) (Math.random()*DataPlacement.nb_HGW) + DataPlacement.nb_DC + DataPlacement.nb_RPOP + DataPlacement.nb_LPOP + 3;
					}
									
					//id_consumer
					////**System.out.println("adding to consumers:"+actif_cons);
					actif_consumers.add(actif_cons);
					
				}
				DataAllocation.stream_consumersList.put(stream.getTupleType(), actif_consumers);
			}

			list_actif_consumersbyStream.put(stream.getTupleType(), actif_consumers);
			//**System.out.println(stream.getTupleType()+": consumers"+actif_consumers.toString());
		}
	}
	
	public void savestream_consumersList() {
		//**System.out.println("savestream_consumersList");
		FileWriter writefile;
		
		try {
			
			writefile = new FileWriter("stream_consumersList.txt");
			BufferedWriter fw = new BufferedWriter(writefile);
			
			for (String stream : DataAllocation.stream_consumersList.keySet()) {
				
				List<Integer> consumers = DataAllocation.stream_consumersList.get(stream);
				fw.write(stream+"\t");
				
				for(int i : consumers) {
					fw.write(i+"\t");
				}
				fw.write("\n");		
			}
			
			fw.close();
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void loadstream_consumersList() throws FileNotFoundException, InterruptedException {
		//**System.out.println("loadstream_consumersList");
		
		FileReader fichier = new FileReader("stream_consumersList.txt");
		BufferedReader in = null;
		try {

			in = new BufferedReader(fichier);
			String line = null;

			while ((line = in.readLine()) != null) {
				String tupletype;
				int consId;

				String[] splited = line.split("\t");
				tupletype = splited[0];
				
				List<Integer> consumers = new ArrayList<Integer>();
				
				for(int i=1; i<splited.length;i++){
					consId = Integer.valueOf(splited[i]);
					consumers.add(consId);
				}
							
				DataAllocation.stream_consumersList.put(tupletype, consumers);

			}
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void processStreamDataRetrieve(SimEvent ev) {
		// TODO Auto-generated method stub
		
		//broker asks the prod to send data to consumer 
		String tupleType = (String) ev.getData(); //the data
		int consumerId = (int) ev.getSource();//the consumer id
		
		//**System.out.println("broker processStreamDataRetrieve from "+consumerId+" for stream:"+tupleType);
			
		if (!tupleType.contains("Stream")) { 
			System.out.println("error, tuple type doesn't exist");
			System.exit(0);
		}
		//search for the nearest replica ans send it to the consumer
		List<Integer> allReplicas = new ArrayList<Integer>(dataAllocation.getEmplacementNodeIdCache(tupleType));
		//**System.out.println("allreplicas of:"+tupleType+" : " + allReplicas.toString());
		
		int nearestReplica = getNearestReplicas(allReplicas, consumerId);//the prod

		//int nb_unit = (int) Math.ceil(DataPlacement.size_streaming_data / DataPlacement.Basis_Exchange_Unit);
		
		int nb_unit = 1;
		float latency = nb_unit * (BasisDelayMatrix.getFatestLink(ev.getSource(), nearestReplica));

		/*
		 * tab [0]: tupletype
		 * tab [1]: consumerId
		 */
		Object [] tab = new Object[2];
		tab[0] = tupleType;
		tab[1] = consumerId;
		
		send(nearestReplica, latency, FogEvents.SEND_STREAM_TO_CONSUMER, tab);
		
		//**System.out.println("broker send FogEvents.SEND_STREAM_TO_CONSUMER to nearest cache:"+nearestReplica+" for "+tupleType);

		LatencyStats.add_Overall_read_Latency(latency);
		LatencyStats.addReadLatency(tupleType, latency);
		
		//Mise à jour de la pénalité
//		System.out.println("fogbro penality stream : "+PenalityStats.penalityStream(latency));
//		System.out.println();
		PenalityStats.add_Overall_read_penalityStream(PenalityStats.penalityStream(latency ));
//		System.out.println("add stream penality in processStreamDataRetrieve");
		PenalityStats.add_penality_per_consumer(consumerId, tupleType, PenalityStats.penalityStream(latency));
		
//		//**System.out.println("press enter");
//		//**Scanner sc = new Scanner(System.in);
//		//**String str = sc.nextLine();

	}

	@Override
	public void shutdownEntity() {
		// TODO Auto-generated method stub
//		for(FogDevice fogdev:DataPlacement.fogDevices){
//			fogdev.printAllStoredData();
//		}
		
		//save all maps
		saveInitial_Stream_Storage();
		savestream_consumersList();
		savetime_Stream_consumption();
		
		try {
			Stats.saveConsumption();
			Stats.saveProduction();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void initializePeriodicTuples() {
		//*//**System.out.println("Sending of perioding tuples ");
		Log.writeInLogFile(this.getName(), "\tSending of perioding tuples ");
		
		
		//if there are a list of periodic tuples
		List<AppEdge> periodicEdges = application.getPeriodicEdges(application.getEdges().get(0).getSource());
		for(AppEdge edge : periodicEdges){
			////*//*//**System.out.println("Sending of perdiong tuple :"+edge.toString());
			Log.writeInLogFile(this.getName(), "\tSending of perdiong tuple :"+edge.toString());
			int id = application.getFogDeviceByName(ModuleMapping.getDeviceHostModule(edge.getSource())).getId();
			send(id, edge.getPeriodicity(), FogEvents.SEND_PERIODIC_TUPLE, edge);
		}
	}
	
	public void setAppliaction(Application application){
		this.application=application;
	}
	
	public Application getAppliaction(){
		return application;
	}
	
	public void setDataAllocation(DataAllocation dataAllocation){
		this.dataAllocation=dataAllocation;
	}
	
	public DataAllocation getDataAllocation(){
		return this.dataAllocation;
	}
	
	protected void processBlockedWriteStorage(SimEvent ev){
		Tuple tuple = (Tuple) ev.getData();
				
		if(ev.getSource()!=2){
			//**System.out.println(this.getName()+": Error, this event must be comming from the broker!");
			//**System.out.println(ev.toString());
			System.exit(0);
		}
		
		//*//*//**System.out.println();
		//*//*//**System.out.println("Clock:"+ev.eventTime()+"\t"+this.getName()+ " receive blocked write event from:"+ev.getSource()+" for tuple:"+tuple.toString());
		Log.writeInLogFile(this.getName()," receive blocked write event from:"+ev.getSource()+" for tuple:"+tuple.toString());
				
		List<Integer> allReplicas = new ArrayList<Integer>();
		
		//*//*//**System.out.println("DataAllocation.getEmplacementNodeId("+tuple.getTupleType()+"):"+DataAllocation.getEmplacementNodeId(tuple.getTupleType()).toString());
		
		allReplicas.addAll(dataAllocation.getEmplacementNodeId(tuple.getTupleType()));
		Log.writeInLogFile(this.getName(),"All replicas:"+allReplicas.toString());
		//*//*//**System.out.println(this.getName()+" All replicas:"+allReplicas.toString());
		
		int tupleSourceDevId =  application.getFogDeviceByName(ModuleMapping.getDeviceHostModule(tuple.getSrcModuleName())).getId();
		
		List<Integer> unLockedReplicas = application.getDataConsistencyProtocol().getUnlockedReplicasForWrite(tuple.getTupleType(), dataAllocation);
		Log.writeInLogFile(this.getName(),"UnlockedReplicas:"+unLockedReplicas.toString());
		//*//*//**System.out.println(this.getName()+" UnlockedReplicas:"+unLockedReplicas.toString());
		
		List<Integer> listOfResponseReplicas = application.getDataConsistencyProtocol().getReplicasListRequestForWrite(tupleSourceDevId, unLockedReplicas);
		Log.writeInLogFile(this.getName(),"responseReplicas:"+listOfResponseReplicas.toString());
		//*//*//**System.out.println(this.getName()+" ResponseReplicas:"+listOfResponseReplicas.toString());

				
		int nb_total_replica = allReplicas.size();
		
		//*//*//**System.out.println(this.getName()+" total number of replicas for tuple type is:"+nb_total_replica);
		Log.writeInLogFile(this.getName(), " total number of replicas for tuple type is:"+nb_total_replica);
		
		
		allReplicas.removeAll(unLockedReplicas);
		List<Integer> lockedReplicas = allReplicas;
		Log.writeInLogFile(this.getName(),"lockedReplicas for write:"+lockedReplicas.toString());
		//*//*//**System.out.println(this.getName()+" lockedReplicas for write:"+lockedReplicas.toString());			
						
		if(listOfResponseReplicas.size()<application.getDataConsistencyProtocol().getNumberOfResponseWriteReplica(nb_total_replica)){
			/*
			 * Do no thing and add this write in the blocked ones
			 */

			application.getDataConsistencyProtocol().addCurrentBlockedWrite(tuple,ev.eventTime());
			Stats.incrementBlockedWriteForBlockedWrite();
			

		}else{
			
			/*
			 * Lock All response replicas
			 */
			for(int rep : listOfResponseReplicas){
				Pair<String,Integer> pair = new Pair<String,Integer> (tuple.getTupleType(),rep);
				this.application.getDataConsistencyProtocol().setReplicaStateForWrite(pair, true);
			}
			
			List<Integer> unLockedReplicasNew = this.application.getDataConsistencyProtocol().getUnlockedReplicasForWrite(tuple.getTupleType(), dataAllocation);
			//*//*//**System.out.println(this.getName()+" unLocked Replicas for write New:"+unLockedReplicasNew.toString());
			Log.writeInLogFile(this.getName()," unLocked Replicas for write New:"+unLockedReplicasNew.toString());
			
			/*
			 * Send Storage event to All response replicas
			 */
			float maxLatency = 0;
			for(int rep: listOfResponseReplicas){
				//*//*//**System.out.println(this.getName()+ " Send tuple:"+tuple.getTupleType()+" to the storage node:"+rep);
				Log.writeInLogFile(this.getName(), "Send tuple:"+tuple.getTupleType()+" to the storage node:"+rep);
								
								
				float latency = BasisDelayMatrix.getFatestLink(tupleSourceDevId, rep);
		
				int ex = DataPlacement.Basis_Exchange_Unit;
				long tupleDataSize = tuple.getCloudletFileSize();
				int nb_Unit = (int) (tupleDataSize / ex);
				if(tupleDataSize % ex != 0) nb_Unit++;
											
				send(rep, latency*nb_Unit , FogEvents.TUPLE_STORAGE, tuple);
				if(maxLatency < (latency*nb_Unit)){
					maxLatency = latency*nb_Unit;
				}
			}
			
			/*
			 * add latency 
			 */
			
			LatencyStats.add_Overall_blocked_write_Latency(maxLatency);			
			
			/*
			 * Send Storage event to the eventual update replicas
			 */
			if(unLockedReplicas.removeAll(listOfResponseReplicas)){
				maxLatency =0;
				for(int rep: unLockedReplicas){	
					
					/*
					 * Send storage event for non responds replicas
					 * delay the event for eventually
					 */
					
					Stats.incrementDelayedWrite();
					
					float latency = BasisDelayMatrix.getFatestLink(tupleSourceDevId, rep);
					//latency += Math.random()*DataPlacement.writeDelayRequest;
			
					latency += DataPlacement.writeDelayRequest;
					
					int ex = DataPlacement.Basis_Exchange_Unit;
					long tupleDataSize = tuple.getCloudletFileSize();
					int nb_Unit = (int) (tupleDataSize / ex);
					if(tupleDataSize % ex != 0) nb_Unit++;
										
					//*//*//**System.out.println(this.getName()+ " postpone write tuple:"+tuple.getTupleType()+" to the non selected storage node:"+rep+" in time event:"+String.valueOf(latency+ev.eventTime()));
					Log.writeInLogFile(this.getName(), " postpone write tuple:"+tuple.getTupleType()+" to the non selected storage node:"+rep+" in time event:"+String.valueOf(latency+ev.eventTime()));
					
					send(rep, latency*nb_Unit , FogEvents.DELAY_TUPLE_STORAGE, tuple);
					if(maxLatency < (latency*nb_Unit)){
						maxLatency = latency*nb_Unit;
					}
				}
				LatencyStats.add_Overall_delayed_write_Latency(maxLatency);
			}
			
			
			/*
			 * Add the locked replicas to the Map
			 */
			if(lockedReplicas.size()>0){
				application.getDataConsistencyProtocol().addCurrentLockedReplicas(tuple,lockedReplicas);
				for(int i=0; i<lockedReplicas.size();i++){
					Stats.incrementLockedWrite();
					Stats.incrementDelayedWrite();
				}
			}	
		}
			
//		//**Scanner sc = new Scanner(System.in);
//		//**String str = sc.nextLine();
	}
	
	protected void processTupleStorage(SimEvent ev){
		Tuple tuple = (Tuple) ev.getData();
					
		//*//**System.out.println();
		//*//**System.out.println("Clock:"+ev.eventTime()+"\t"+this.getName()+ " receive Storage event from:"+ev.getSource()+" for tuple:"+tuple.toString());
		Log.writeInLogFile(this.getName()," receive Storage event from:"+ev.getSource()+" for tuple:"+tuple.toString());
		
		if(Stats.production.get(tuple.getTupleType()) == null){
			List<Float> list = new ArrayList<Float>();
			if(tuple.getTupleType().startsWith("TempHGW")){
				if(DataPlacement.sendPerdiodicTuples){
					list.add((float) CloudSim.clock());
				}else{
					list.add((float) CloudSim.clock() - 50);
				}
				
			}else{
				list.add((float) CloudSim.clock());
			}
			
			
			Stats.production.put(tuple.getTupleType(), list);
		}else{
			if(tuple.getTupleType().startsWith("TempHGW")){
				if(DataPlacement.sendPerdiodicTuples){
					Stats.production.get(tuple.getTupleType()).add((float) CloudSim.clock());
				}else{
					Stats.production.get(tuple.getTupleType()).add((float) CloudSim.clock() -50);
				}
	
			}else{
				Stats.production.get(tuple.getTupleType()).add((float) CloudSim.clock());
			}
		}
		
				
		List<Integer> allReplicas = new ArrayList<Integer>();
		
		//*//**System.out.println("DataAllocation.getEmplacementNodeId("+tuple.getTupleType()+"):"+dataAllocation.getEmplacementNodeId(tuple.getTupleType()).toString());
		
		allReplicas.addAll(dataAllocation.getEmplacementNodeId(tuple.getTupleType()));
		Log.writeInLogFile(this.getName(),"All replicas:"+allReplicas.toString());
		//*//**System.out.println(this.getName()+" All replicas:"+allReplicas.toString());
		
		int tupleSourceDevId =  application.getFogDeviceByName(ModuleMapping.getDeviceHostModule(tuple.getSrcModuleName())).getId();
		
		List<Integer> unLockedReplicas = this.application.getDataConsistencyProtocol().getUnlockedReplicasForWrite(tuple.getTupleType(),dataAllocation);
		Log.writeInLogFile(this.getName(),"UnlockedReplicas:"+unLockedReplicas.toString());
		//*//**System.out.println(this.getName()+" UnlockedReplicas:"+unLockedReplicas.toString());
		
		List<Integer> listOfResponseReplicas = this.application.getDataConsistencyProtocol().getReplicasListRequestForWrite(tupleSourceDevId, unLockedReplicas);
		Log.writeInLogFile(this.getName(),"responseReplicas:"+listOfResponseReplicas.toString());
		//*//**System.out.println(this.getName()+" ResponseReplicas:"+listOfResponseReplicas.toString());
		
		int nb_total_replica = allReplicas.size();
				
		//*//**System.out.println(this.getName()+" total number of replicas for tuple type is:"+nb_total_replica);
		Log.writeInLogFile(this.getName(), " total number of replicas for tuple type is:"+nb_total_replica);
		
		for(int i=0;i<nb_total_replica;i++)
			Stats.incrementTotalWrite();
		
		allReplicas.removeAll(unLockedReplicas);
		List<Integer> lockedReplicas = allReplicas;
		Log.writeInLogFile(this.getName(),"lockedReplicas for write:"+lockedReplicas.toString());
		//*//**System.out.println(this.getName()+" lockedReplicas for write:"+lockedReplicas.toString());	
		
		
		
		/*
		 * If there is insufficient free replicas or there are other old blocked write ==> add this write to the blocked write
		 */
						
		if(listOfResponseReplicas.size()<application.getDataConsistencyProtocol().getNumberOfResponseWriteReplica(nb_total_replica) || 
				application.getDataConsistencyProtocol().checkIfTupleIsInCurrentBlockedWrites(tuple.getTupleType())){
			
//			//**System.out.println(this.getName()+"application.getDataConsistencyProtocol().getNumberOfResponseWriteReplica(nb_total_replica)="+application.getDataConsistencyProtocol().getNumberOfResponseWriteReplica(nb_total_replica));
//			//**System.out.println(this.getName()+"listOfResponseReplicas.size()="+listOfResponseReplicas.size());
//			//**System.out.println(this.getName()+"checkIfTupleIsInCurrentBlockedWrites="+application.getDataConsistencyProtocol().checkIfTupleIsInCurrentBlockedWrites(tuple.getTupleType()));
			
			Log.writeInLogFile(this.getName(),"application.getDataConsistencyProtocol().getNumberOfResponseWriteReplica(nb_total_replica)="+application.getDataConsistencyProtocol().getNumberOfResponseWriteReplica(nb_total_replica));
			Log.writeInLogFile(this.getName(),"listOfResponseReplicas.size()="+listOfResponseReplicas.size());
			Log.writeInLogFile(this.getName(),"checkIfTupleIsInCurrentBlockedWrites="+application.getDataConsistencyProtocol().checkIfTupleIsInCurrentBlockedWrites(tuple.getTupleType()));
			
			/*
			 * Add this write to blocked write map
			 * Add the time stamp of the blocked write
			 */
			application.getDataConsistencyProtocol().addCurrentBlockedWrite(tuple, ev.eventTime());	
			
			for(int i=0; i<application.getDataConsistencyProtocol().getNumberOfResponseWriteReplica(nb_total_replica);i++)
				Stats.incrementBlockedWrite();
			
			
//			if(tuple.getTupleType().equals("TempRPOP1")){
//				//**System.out.println(this.getName()+"application.getDataConsistencyProtocol().getNumberOfResponseWriteReplica(nb_total_replica)="+application.getDataConsistencyProtocol().getNumberOfResponseWriteReplica(nb_total_replica));
//				//**System.out.println(this.getName()+"listOfResponseReplicas.size()="+listOfResponseReplicas.size());
//				//**System.out.println(this.getName()+"checkIfTupleIsInCurrentBlockedWrites="+application.getDataConsistencyProtocol().checkIfTupleIsInCurrentBlockedWrites(tuple.getTupleType()));
//				
//			
//				application.getDataConsistencyProtocol().printAllCurrentBlockedWrites();
//				application.getDataConsistencyProtocol().printAllCurrentLockedReplicas();
////				//**Scanner sc = new Scanner(System.in);
////				//**String str = sc.nextLine();
//			}

		}else{
			
			/*
			 * Lock All response replicas
			 */
			for(int rep : listOfResponseReplicas){
				Pair<String,Integer> pair = new Pair<String,Integer> (tuple.getTupleType(),rep);
				this.application.getDataConsistencyProtocol().setReplicaStateForWrite(pair, true);
			}
			
			List<Integer> unLockedReplicasNew = this.application.getDataConsistencyProtocol().getUnlockedReplicasForWrite(tuple.getTupleType(), dataAllocation);
			//*//**System.out.println(this.getName()+" unLocked Replicas for write New:"+unLockedReplicasNew.toString());
			Log.writeInLogFile(this.getName()," unLocked Replicas for write New:"+unLockedReplicasNew.toString());
			
			/*
			 * Send Storage event to All response replicas
			 */
			float maxLatency = 0;
			int consumerId = -1;
			for(int rep: listOfResponseReplicas){
				//*//**System.out.println(this.getName()+ " Send tuple:"+tuple.getTupleType()+" to the storage node:"+rep);
				Log.writeInLogFile(this.getName(), "Send tuple:"+tuple.getTupleType()+" to the storage node:"+rep);
				
				Stats.incrementResponseWrite();
					
				float latency = BasisDelayMatrix.getFatestLink(tupleSourceDevId, rep);
		
				int ex = DataPlacement.Basis_Exchange_Unit;
				long tupleDataSize = tuple.getCloudletFileSize();
				int nb_Unit = (int) (tupleDataSize / ex);
				if(tupleDataSize % ex != 0) nb_Unit++;
				
							
				send(rep, latency*nb_Unit , FogEvents.TUPLE_STORAGE, tuple);
				if(maxLatency < (latency*nb_Unit)){
					maxLatency = latency*nb_Unit;
					consumerId= rep;
					
				}
			}
						
			LatencyStats.add_Overall_write_Latency(maxLatency);
			LatencyStats.addWriteLatency(tuple.getTupleType() , maxLatency);
			
			//Mise à jour de la pénalité
			PenalityStats.add_Overall_write_penalityIoT(PenalityStats.penalityIoT(maxLatency ));
			PenalityStats.add_penality_per_consumer(consumerId, tuple.getTupleType(), PenalityStats.penalityIoT(maxLatency));
			/*
			 * Send Storage event to the eventual update replicas
			 */
			if(unLockedReplicas.removeAll(listOfResponseReplicas)){
				maxLatency =0;
				for(int rep: unLockedReplicas){	
					
					/*
					 * Send storage event for non responds replicas delay the event for eventually
					 */
					
					Stats.incrementDelayedWrite();
					
					float latency = BasisDelayMatrix.getFatestLink(tupleSourceDevId, rep);
					//latency += Math.random()*DataPlacement.writeDelayRequest;
					
					latency += DataPlacement.writeDelayRequest;
			
					int ex = DataPlacement.Basis_Exchange_Unit;
					long tupleDataSize = tuple.getCloudletFileSize();
					int nb_Unit = (int) (tupleDataSize / ex);
					if(tupleDataSize % ex != 0) nb_Unit++;
										
					//*//**System.out.println(this.getName()+ " postpone write tuple:"+tuple.getTupleType()+" to the non selected storage node:"+rep+" in time event:"+String.valueOf(latency+ev.eventTime()));
					Log.writeInLogFile(this.getName(), " postpone write tuple:"+tuple.getTupleType()+" to the non selected storage node:"+rep+" in time event:"+String.valueOf(latency+ev.eventTime()));
					
					send(rep, latency*nb_Unit , FogEvents.DELAY_TUPLE_STORAGE, tuple);
					if(maxLatency < (latency*nb_Unit)){
						maxLatency = latency*nb_Unit;
					}
				}
				LatencyStats.add_Overall_delayed_write_Latency(maxLatency);
			}
			
			
			/*
			 * Add the locked replicas to the Map
			 */
			if(lockedReplicas.size()>0){
				this.application.getDataConsistencyProtocol().addCurrentLockedReplicas(tuple,lockedReplicas);
				for(int i=0; i<lockedReplicas.size();i++){
					Stats.incrementLockedWrite();
					Stats.incrementDelayedWrite();
				}
			}	
		}
			
//		//**Scanner sc = new Scanner(System.in);
//		//**String str = sc.nextLine();
	}
	
	protected void processTupleStorageACK(SimEvent ev){
		Tuple tuple = (Tuple) ev.getData();
		//*//**System.out.println();
		//*//**System.out.println(this.getName()+" from node:"+ev.getSource()+" Storage ACK: "+tuple.toString());
		Log.writeInLogFile(this.getName(), " from node:"+ev.getSource()+" Storage ACK: "+tuple.toString());
		
		Stats.incrementDoneWrite();
		
		this.addStorageAck(tuple,ev.getSource());
		
		/*
		 * Set replicas state unlocked for write map
		 */
		this.application.getDataConsistencyProtocol().setReplicaStateForWrite(new Pair<String,Integer>(tuple.getTupleType(),ev.getSource()), false);
		
		/*
		 * Send to this replicas an old locked write event if there is one in currentLockReplica in Consistency Manager
		 * this is just for this replicas
		 */
		Tuple oldTuple = this.application.getDataConsistencyProtocol().getOldWriteFromCurrentLockedReplicas(new Pair<String,Integer>(tuple.getTupleType(),ev.getSource()));
		
		if(oldTuple!=null){
			float latency = BasisDelayMatrix.getFatestLink(application.getFogDeviceByName(ModuleMapping.getDeviceHostModule(tuple.getSrcModuleName())).getId(), ev.getSource());
			//latency += Math.random()*DataPlacement.writeDelayRequest;
			
			latency += DataPlacement.writeDelayRequest;
	
			int ex = DataPlacement.Basis_Exchange_Unit;
			long tupleDataSize = oldTuple.getCloudletFileSize();
			int nb_Unit = (int) (tupleDataSize / ex);
			if(tupleDataSize % ex != 0) nb_Unit++;

			//*//*//**System.out.println(this.getName()+ " Send old locked tuple write:"+oldTuple.toString()+"\nto the non selected storage node:"+ev.getSource()+" in time event:"+String.valueOf(latency+ev.eventTime()));
			Log.writeInLogFile(this.getName(), " Send old locked tuple write:"+oldTuple.toString()+"\nto the non selected storage node:"+ev.getSource()+" in time event:"+String.valueOf(latency+ev.eventTime()));
			
			send(ev.getSource(), latency*nb_Unit , FogEvents.DELAY_TUPLE_STORAGE, oldTuple);
			LatencyStats.add_Overall_delayed_write_Latency(latency*nb_Unit);
		
		}else{
			
			/*
			 * Check if there are not other locked writes in other replicas for this tuple ==> launch the oldest blocked write , Otherwise, do no thing
			 */			
			if(!application.getDataConsistencyProtocol().checkIfThereAreCurrentLockedReplicaForTuple(tuple.getTupleType(), dataAllocation)){
				
				/*
				 * get old blocked write
				 */
				
				if(application.getDataConsistencyProtocol().checkIfTupleIsInCurrentBlockedWrites(tuple.getTupleType())){
					
					/*
					 * Check if there is the minimum of required replicas is free
					 */
					
					List<Integer> allReplicas = new ArrayList<Integer>();
					
					//*//**System.out.println("DataAllocation.getEmplacementNodeId("+tuple.getTupleType()+"):"+dataAllocation.getEmplacementNodeId(tuple.getTupleType()).toString());
					
					allReplicas.addAll(dataAllocation.getEmplacementNodeId(tuple.getTupleType()));
					Log.writeInLogFile(this.getName(),"All replicas:"+allReplicas.toString());
					//*//**System.out.println(this.getName()+" All replicas:"+allReplicas.toString());
										
					List<Integer> unLockedReplicas = this.application.getDataConsistencyProtocol().getUnlockedReplicasForWrite(tuple.getTupleType(),dataAllocation);
					Log.writeInLogFile(this.getName(),"UnlockedReplicas:"+unLockedReplicas.toString());
					//*//**System.out.println(this.getName()+" UnlockedReplicas:"+unLockedReplicas.toString());
					
					int nb_total_replica = allReplicas.size();
					
					if(!(unLockedReplicas.size()<application.getDataConsistencyProtocol().getNumberOfResponseWriteReplica(nb_total_replica))){
						Pair<Tuple,Double> pair = this.application.getDataConsistencyProtocol().getOldWriteFromCurrentBlockedWrite(tuple.getTupleType());
						oldTuple = pair.getFirst();
						sendNow(2, FogEvents.BLOCKED_WRITE_STORAGE, oldTuple);
												
						/*
						 * Add the blocked write time stamp
						 */
						
						double blockedWriteLatency = ev.eventTime()-pair.getSecond();
						LatencyStats.add_Overall_blocked_write_Latency(blockedWriteLatency);
						
						//*//**System.out.println(this.getName()+ " Send for storage old blocked write:"+oldTuple.toString());
						Log.writeInLogFile(this.getName(), " Send for storage old blocked write:"+oldTuple.toString());
					
					}else{
						//*//**System.out.println(this.getName()+ " the minimum required free replicas is not satisfied for tuple:"+tuple.toString());
						Log.writeInLogFile(this.getName(), " the minimum required free replicas is not satisfied for tuple:"+tuple.toString());
					}				
					
				}

			}else {
				//*//**System.out.println(this.getName()+ " there is a locked write in somme other replica for tuple:"+tuple.toString());
				Log.writeInLogFile(this.getName(), " there is a locked write in somme other replica for tuple:"+tuple.toString());
			}
		}
			
//		//**Scanner sc = new Scanner(System.in);
//		//**String str = sc.nextLine();
	}
	
	protected void processDelayTupleStorageACK(SimEvent ev){
		Tuple tuple = (Tuple) ev.getData();
		//*//**System.out.println();
		//*//**System.out.println(this.getName()+" from node:"+ev.getSource()+" delay Storage ACK: "+tuple.toString());
		Log.writeInLogFile(this.getName(), " from node:"+ev.getSource()+" delay Storage ACK: "+tuple.toString());
		
		Stats.incrementDoneWrite();
		
		this.addStorageAck(tuple, ev.getSource());
		
		if(this.application.getDataConsistencyProtocol().getUnlockedReplicasForWrite(tuple.getTupleType(), dataAllocation).contains(ev.getSource()) ){
			/*
			 * if the replica is unlocked for write
			 * Send to this replicas an update event if there is one in currentLockReplica in Consistency Manager
			 */
			Log.writeInLogFile(this.getName(), " rep:"+ev.getSource()+"\tis unlocked fro write for tuple:"+tuple.getTupleType());
			
			Tuple oldTuple = this.application.getDataConsistencyProtocol().getOldWriteFromCurrentLockedReplicas(new Pair<String,Integer>(tuple.getTupleType(),ev.getSource()));
			
			if(oldTuple!=null){
				float latency = BasisDelayMatrix.getFatestLink(application.getFogDeviceByName(ModuleMapping.getDeviceHostModule(tuple.getSrcModuleName())).getId(), ev.getSource());
				//latency += Math.random()*DataPlacement.writeDelayRequest;
				
				latency += DataPlacement.writeDelayRequest;
		
				int ex = DataPlacement.Basis_Exchange_Unit;
				long tupleDataSize = oldTuple.getCloudletFileSize();
				int nb_Unit = (int) (tupleDataSize / ex);
				if(tupleDataSize % ex != 0) nb_Unit++;
				
				Log.writeInLogFile(this.getName(), " there is an old locked write:"+oldTuple.toString());
				//*//*//**System.out.println(this.getName()+ " Send old locked tuple write:"+oldTuple.toString()+"\nto the non selected storage node:"+ev.getSource()+" in time event:"+String.valueOf(latency+ev.eventTime()));
				Log.writeInLogFile(this.getName(), " Send old locked tuple write:"+oldTuple.toString()+"\nto the non selected storage node:"+ev.getSource()+" in time event:"+String.valueOf(latency+ev.eventTime()));
				
				send(ev.getSource(), latency*nb_Unit , FogEvents.DELAY_TUPLE_STORAGE, oldTuple);
				LatencyStats.add_Overall_delayed_write_Latency(latency*nb_Unit);
			
			}else{
				
				Log.writeInLogFile(this.getName(), " there is no an old locked write for tuple"+tuple.toString());
				/*
				 * Check if there are not other locked writes in other replicas for this tuple ==> launch the oldest blocked write , Otherwise, do no thing
				 */			
				if(!application.getDataConsistencyProtocol().checkIfThereAreCurrentLockedReplicaForTuple(tuple.getTupleType(), dataAllocation)){
					
					/*
					 * get old blocked write
					 */
					Log.writeInLogFile(this.getName(), " searching for old blocked writes for tuple:"+tuple.toString());
					
					if(application.getDataConsistencyProtocol().checkIfTupleIsInCurrentBlockedWrites(tuple.getTupleType())){
						
						/*
						 * Check if there is the minimum of required replicas is free
						 */
						
						List<Integer> allReplicas = new ArrayList<Integer>();
						
						//*//*//**System.out.println("DataAllocation.getEmplacementNodeId("+tuple.getTupleType()+"):"+DataAllocation.getEmplacementNodeId(tuple.getTupleType()).toString());
						
						allReplicas.addAll(dataAllocation.getEmplacementNodeId(tuple.getTupleType()));
						Log.writeInLogFile(this.getName(),"All replicas:"+allReplicas.toString());
						//*//*//**System.out.println(this.getName()+" All replicas:"+allReplicas.toString());
											
						List<Integer> unLockedReplicas = application.getDataConsistencyProtocol().getUnlockedReplicasForWrite(tuple.getTupleType(), dataAllocation);
						Log.writeInLogFile(this.getName(),"UnlockedReplicas:"+unLockedReplicas.toString());
						//*//*//**System.out.println(this.getName()+" UnlockedReplicas:"+unLockedReplicas.toString());
						
						int nb_total_replica = allReplicas.size();
						
						if(!(unLockedReplicas.size()<application.getDataConsistencyProtocol().getNumberOfResponseWriteReplica(nb_total_replica))){
							
							Pair<Tuple,Double> pair = application.getDataConsistencyProtocol().getOldWriteFromCurrentBlockedWrite(tuple.getTupleType());
							oldTuple = pair.getFirst();
							sendNow(2, FogEvents.BLOCKED_WRITE_STORAGE, oldTuple);
													
							/*
							 * Add the blocked write time stamp
							 */
							
							double blockedWriteLatency = ev.eventTime()-pair.getSecond();
							LatencyStats.add_Overall_blocked_write_Latency(blockedWriteLatency);

							//*//*//**System.out.println(this.getName()+ " Send for storage old blocked write:"+oldTuple.toString());
							Log.writeInLogFile(this.getName(), " Send for storage old blocked write:"+oldTuple.toString());
						
						}else{
							//*//*//**System.out.println(this.getName()+ " the minimum required free replicas is not satisfied for tuple:"+tuple.toString());
							Log.writeInLogFile(this.getName(), " the minimum required free replicas is not satisfied for tuple:"+tuple.getTupleType());
						}				
						
					}else{
						//*//*//**System.out.println(this.getName()+ " there is no blocked write for tuple:"+tuple.getTupleType());
						Log.writeInLogFile(this.getName(), " there is no blocked write for tuple:"+tuple.getTupleType());
					}
				
				}else {
					//*//*//**System.out.println(this.getName()+ " there is a locked write in somme replica for tuple:"+tuple.toString());
					Log.writeInLogFile(this.getName(), " there is a locked write in somme replica for tuple:"+tuple.toString());
				}
			}
		}
		
//		//**Scanner sc = new Scanner(System.in);
//		//**String str = sc.nextLine();
	}
	
	protected void processTupleRetrieve(SimEvent ev){
		/*
		 * get data map and chose a replicas set 
		 * 
		 */
		
		//**System.out.println("processTupleRetrieve");
		Object [] tab = (Object []) ev.getData();
		
		/*
		 * Send an event to a datahost of a replica  to send this replica to the consumer
		 * tab [0]: Data Consumer Id
		 * tab [1]: destinator service name
		 * tab [2]: edge
		 * tab [3]: list of response replicas
		 * tab [4]: reserved for the median
		 * tab [5]: requestId
		 */
		
		Stats.incrementTotalRead();
		
		AppEdge edge = (AppEdge) tab[2];
//		System.out.println("edge:"+edge.toString());
		
		String consumer = (String) tab[1];
//		System.out.println("consumer:"+consumer);
			
//		System.out.println("consumerNodeId:"+tab[0].toString());
		int consumerNodeId = (Integer) tab[0];
		
		
		
		
		

		Pair<String, String> key = new Pair<String, String>(edge.getTupleType(), consumer);
		
		
		// save time list of data consumption (reads)
		if(Stats.consumption.get(key) == null){
			List<Float> list = new ArrayList<Float>();
			list.add((float) CloudSim.clock());
			Stats.consumption.put(key, list);
		
		}else{
			Stats.consumption.get(key).add((float) CloudSim.clock());
		}
		
		
		
		//*//*//**System.out.println();
		//**System.out.println(this.getName()+": Retrieve: from consumer:"+((String) tab[1])+" for edge:"+edge.toString());
		Log.writeInLogFile(this.getName(),": Retrieve: from consumer:"+((String) tab[1])+" for edge:"+edge.toString());
		List<Integer> allReplicas = null;
		
		if(edge.getTupleType().contains("Temp")) {
			allReplicas = new ArrayList<Integer>(dataAllocation.getEmplacementNodeId(edge.getTupleType()));
			
		}else {
			System.out.println("error, tuple type does't exist");
			System.exit(0);
		}
						
		
		Log.writeInLogFile(this.getName(),"All replicas:"+allReplicas.toString());
		
		List<Integer> unLockedReplicas = this.application.getDataConsistencyProtocol().getUnlockedReplicasForRead(edge.getTupleType(), dataAllocation);
		Log.writeInLogFile(this.getName(),"UnlockedReplicas:"+unLockedReplicas.toString());
		//**//**System.out.println(this.getName()+" UnlockedReplicas:"+unLockedReplicas.toString());
		
		List<Integer> listOfResponseReplicas = this.application.getDataConsistencyProtocol().getReplicasListRequestForRead(ev.getSource(), unLockedReplicas);
		Log.writeInLogFile(this.getName(),"responseReplicas:"+listOfResponseReplicas.toString());
		//**//**System.out.println(this.getName()+" ResponseReplicas:"+listOfResponseReplicas.toString());
		
				
		int nb_total_replica = allReplicas.size();
		
		if(nb_total_replica> 1) {
			System.out.println("error, nb_total_replica should be = 1 while it is "+nb_total_replica);
			System.exit(0);
		}
			
		
		//**//**System.out.println(this.getName()+" total number of replicas for tuple type is:"+nb_total_replica);
		Log.writeInLogFile(this.getName(), " total number of replicas for tuple type is:"+nb_total_replica);
		
		
		if(listOfResponseReplicas.size()<application.getDataConsistencyProtocol().getNumberOfResponseReadReplica(nb_total_replica)){
			//*//**System.out.println("Number of replicas violation!!!");
			for(int i=0; i< (application.getDataConsistencyProtocol().getNumberOfResponseReadReplica(nb_total_replica)-listOfResponseReplicas.size());i++)
				Stats.incrementReplicaViolationInRead();
			
			Stats.incrementNonServedRead();
			
			//System.exit(0);
		
		}else{
			/*
			 * Lock All response replicas
			 */
			//tab[0] = ev.getSource(); consumerNodeId
			tab[0] = consumerNodeId;
			tab[3] = listOfResponseReplicas;
						
			for(int rep : listOfResponseReplicas){
				Pair<String,Integer> pair = new Pair<String,Integer> (edge.getTupleType(),rep);
				int nb_current_reader = this.application.getDataConsistencyProtocol().getReplicaStateForRead(pair);
				nb_current_reader++;
				this.application.getDataConsistencyProtocol().setReplicaStateForRead(pair, nb_current_reader);
			}
			
			unLockedReplicas = this.application.getDataConsistencyProtocol().getUnlockedReplicasForRead(edge.getTupleType(), dataAllocation);
			Log.writeInLogFile(this.getName(),"Unlocked Replicas for read New:"+unLockedReplicas.toString());
	
			int nearestReplicas = getNearestReplicas(allReplicas, consumerNodeId);
			
//			//**System.out.println("listOfResponseReplicas avant."+listOfResponseReplicas.toString());
			
			listOfResponseReplicas.removeAll(listOfResponseReplicas);
			listOfResponseReplicas.add(nearestReplicas);

			float latency;

			if (application.getFogDeviceById(ev.getSource()).getName().startsWith("HGW")) {
				latency = BasisDelayMatrix.getFatestLink(application.getFogDeviceById(ev.getSource()).getParentId(),
						listOfResponseReplicas.get(0));

			} else {
				latency = BasisDelayMatrix.getFatestLink(ev.getSource(), listOfResponseReplicas.get(0));

			}

			/*
			 * add penality lydia
			 */
			
			int rep = listOfResponseReplicas.get(0);
			send(rep, latency, FogEvents.SEND_DATA_TO_CONSUMER, tab);
			//**System.out.println("broker send FogEvents.SEND_DATA_TO_CONSUMER to " + rep + " for " + edge.getTupleType());

			LatencyStats.add_Overall_read_Latency(latency);
			LatencyStats.addReadLatency(edge.getTupleType(), latency);
			
			//Mise à jour de la pénalité
//			PenalityStats.add_Overall_read_penalityIoT(PenalityStats.penalityIoT(latency ));
//			PenalityStats.add_penality_per_consumer(rep, edge.getTupleType(), PenalityStats.penalityIoT(latency));
//			PenalityStats.add_Overall_penalityIoT(PenalityStats.penalityIoT(latency));
		
		}
		
//		//**System.out.println("press enter");
//		//**Scanner sc = new Scanner(System.in);
//		//**String str = sc.nextLine();
		
	}
	
//	private void checkNearestReplicasForRead(int devId, List<>){
//		
//	}
	
	
	
	private int getNearestReplicas(List<Integer> allReplicas, int consumerId) {
		// TODO Auto-generated method stub.
//		//**System.out.println();
//		//**System.out.println("consumer: "+consumerId);
//		//**System.out.println("Replicas: "+allReplicas.toString());
//		
		int nearestRep = allReplicas.get(0);
		float latency = BasisDelayMatrix.getFatestLink(nearestRep, consumerId);
		
		
		for (Integer replica : allReplicas) {
			if (BasisDelayMatrix.getFatestLink(replica, consumerId)<latency) {
				latency = BasisDelayMatrix.getFatestLink(replica, consumerId);
				nearestRep = replica;
			}
				
		}
		
		return nearestRep;
	}

	protected void processNONTupleRetrieveACK(SimEvent ev){
		Object [] tab = (Object[]) ev.getData();
		/*
		 * tab [0]: DataCons Id
		 * tab [1]: destinater service name
		 * tab [2]: edge
		 * tab [3]: list of response replicas
		 * tab [4]: reserved to last version
		 * tab [5]: requestId
		 */
		AppEdge edge = (AppEdge) tab[2];
		String tupleType = edge.getTupleType();
		int storageNode = ev.getSource();
		String consumerModule = (String) tab[1];
		//**//**System.out.println();
		//**//**System.out.println(this.getName()+" Non Retrieve ACK: "+tupleType+" consumerModule:"+consumerModule+" replicas:"+storageNode);
		Log.writeInLogFile(this.getName(), "Non Retrieve ACK: "+tupleType+" consumerModule:"+consumerModule+" replicas:"+storageNode);
	
		
		/*
		 * must do this for all response replicas
		 * unlock response replicas
		 * for all unlocked replicas, search the old locked write 
		 */
		List<Integer> listResponseReplicas = (List<Integer>) tab[3];
		
		if(listResponseReplicas!= null){
			
			float maxlatency = 0;
			for(Integer rep: listResponseReplicas){
				/*
				 * Unlock all response replicas
				 */
				int nb_current_reader = application.getDataConsistencyProtocol().getReplicaStateForRead(new Pair<String,Integer>(tupleType, rep));
				nb_current_reader--;
				application.getDataConsistencyProtocol().setReplicaStateForRead(new Pair<String,Integer>(tupleType, rep),nb_current_reader);
				
				if(application.getDataConsistencyProtocol().getUnlockedReplicasForWrite(tupleType, dataAllocation).contains(rep)){
					/*
					 * check if there are old blocked writes, and send delay writes for them
					 */
					Tuple oldTuple = application.getDataConsistencyProtocol().getOldWriteFromCurrentLockedReplicas(new Pair<String,Integer>(tupleType,rep));
					
					if(oldTuple!=null){
						float latency = BasisDelayMatrix.getFatestLink(application.getFogDeviceByName(ModuleMapping.getDeviceHostModule(edge.getSource())).getId(), rep);
						//latency += Math.random()*DataPlacement.writeDelayRequest;
						
						latency += DataPlacement.writeDelayRequest;
						int ex = DataPlacement.Basis_Exchange_Unit;
						long tupleDataSize = oldTuple.getCloudletFileSize();
						int nb_Unit = (int) (tupleDataSize / ex);
						if(tupleDataSize % ex != 0) nb_Unit++;
						
						
						//**//**System.out.println(this.getName()+ " Send old tuple write:"+oldTuple.toString()+" triggred by a non retrieve event  \nto the non selected storage node:"+rep+" in time event:"+String.valueOf(latency+ev.eventTime()));
						Log.writeInLogFile(this.getName(), " Send old tuple write:"+oldTuple.toString()+" triggred by a non retrieve event  \nto the non selected storage node:"+rep+" in time event:"+String.valueOf(latency+ev.eventTime()));
						
						send(rep, latency*nb_Unit , FogEvents.DELAY_TUPLE_STORAGE, oldTuple);
						if(maxlatency< (latency*nb_Unit))
							maxlatency = latency*nb_Unit;

					}
					
				}else{
					//**//**System.out.println(this.getName()+ " there are other reader from this replicas! non delayed tuple storage are checked in replica:"+rep);
					Log.writeInLogFile(this.getName(), " there are other reader from this replicas! non delayed tuple storage are checked in replica:"+rep);
					
				}
			}
			
			LatencyStats.add_Overall_delayed_write_Latency(maxlatency);
					
			/*
			 * Check if there are not other locked writes in other replicas for this tuple ==> launch the oldest blocked write , Otherwise, do no thing
			 */			
			if(!application.getDataConsistencyProtocol().checkIfThereAreCurrentLockedReplicaForTuple(tupleType, dataAllocation)){
				
				/*
				 * get old blocked write
				 */
				
				if(application.getDataConsistencyProtocol().checkIfTupleIsInCurrentBlockedWrites(tupleType)){
					
					/*
					 * Check if there is the minimum of required replicas is free
					 */
					
					List<Integer> allReplicas = new ArrayList<Integer>();
					
					////**System.out.println("DataAllocation.getEmplacementNodeId("+tupleType+"):"+DataAllocation.getEmplacementNodeId(tupleType).toString());
					
					allReplicas.addAll(dataAllocation.getEmplacementNodeId(tupleType));
					Log.writeInLogFile(this.getName(),"All replicas:"+allReplicas.toString());
					//*//**System.out.println(this.getName()+" All replicas:"+allReplicas.toString());
										
					List<Integer> unLockedReplicas = this.application.getDataConsistencyProtocol().getUnlockedReplicasForWrite(tupleType, dataAllocation);
					Log.writeInLogFile(this.getName(),"UnlockedReplicas:"+unLockedReplicas.toString());
					//**//**System.out.println(this.getName()+" UnlockedReplicas:"+unLockedReplicas.toString());
					
					int nb_total_replica = allReplicas.size();
					
					if(!(unLockedReplicas.size()<application.getDataConsistencyProtocol().getNumberOfResponseWriteReplica(nb_total_replica))){
						
						Pair<Tuple,Double> pair = this.application.getDataConsistencyProtocol().getOldWriteFromCurrentBlockedWrite(tupleType);
						Tuple oldTuple = pair.getFirst();
						sendNow(2, FogEvents.BLOCKED_WRITE_STORAGE, oldTuple);
												
						/*
						 * Add the blocked write time stamp
						 */
						
						double blockedWriteLatency = ev.eventTime()-pair.getSecond();
						LatencyStats.add_Overall_blocked_write_Latency(blockedWriteLatency);
												
						//**//**System.out.println(this.getName()+ " Send for storage old blocked write:"+oldTuple.toString());
						Log.writeInLogFile(this.getName(), " Send for storage old blocked write:"+oldTuple.toString());
					
					}else{
						//**//**System.out.println(this.getName()+ " the minimum required free replicas is not satisfied for tuple:"+tupleType);
						Log.writeInLogFile(this.getName(), " the minimum required free replicas is not satisfied for tuple:"+tupleType);
					}				
					
				}else{
					//**//**System.out.println(this.getName()+ " there is no blocked write for tuple:"+tupleType);
					Log.writeInLogFile(this.getName(), " there is no blocked write for tuple:"+tupleType);
				}
				
			}else {
				//**//**System.out.println(this.getName()+ " there is a locked write in somme replica for tuple:"+tupleType);
				Log.writeInLogFile(this.getName(), " there is a locked write in somme replica for tuple:"+tupleType);
			}
		}
		
//		//**Scanner sc = new Scanner(System.in);
//		//**String str = sc.nextLine();
	}
	
	protected void processTupleRetrieveACK(SimEvent ev){
		Object [] tab = (Object[]) ev.getData();
		//**//**System.out.println();
		/*
		 * tab [0]: DataCons Id <--- tuple
		 * tab [1]: destinater service name
		 * tab [2]: edge
		 * tab [3]: list of response replicas
		 * tab [4]: reserved to last version
		 * tab [5]: requestId
		 */
		
		
		Tuple tuple = (Tuple) tab[5];
		List<Integer> listResponseReplicas = (List<Integer>) tab[3];
		
		int sourceNodeId = ev.getSource();
		
		String tupleType = tuple.getTupleType();
				
		if(listResponseReplicas!= null){
			
			/*
			 * must do this for all response replicas
			 */
			float maxlatency = 0;
			
			for(Integer rep: listResponseReplicas){
	
				//**//**System.out.println(this.getName()+" Retrieve ACK: "+tuple.toString()+" consuemer:"+sourceNodeId+" replicas:"+rep);
				Log.writeInLogFile(this.getName(), "Retrieve ACK: "+tuple.toString()+" consuemer:"+sourceNodeId+" replicas:"+rep);
				
				/*
				 * Unlock all response replicas
				 */			
				int nb_current_reader = application.getDataConsistencyProtocol().getReplicaStateForRead(new Pair<String,Integer>(tuple.getTupleType(), rep));
				nb_current_reader--;
				application.getDataConsistencyProtocol().setReplicaStateForRead(new Pair<String,Integer>(tuple.getTupleType(), rep),nb_current_reader);
				
				if(application.getDataConsistencyProtocol().getUnlockedReplicasForWrite(tuple.getTupleType(), dataAllocation).contains(rep)){
					/*
					 * check if there are old locked writes, and send delay writes for them
					 */
					Tuple oldTuple = application.getDataConsistencyProtocol().getOldWriteFromCurrentLockedReplicas(new Pair<String,Integer>(tuple.getTupleType(),rep));
					
					if(oldTuple!=null){
						float latency = BasisDelayMatrix.getFatestLink(application.getFogDeviceByName(ModuleMapping.getDeviceHostModule(tuple.getSrcModuleName())).getId(), rep);
						//latency += Math.random()*DataPlacement.writeDelayRequest;
						
						latency += DataPlacement.writeDelayRequest;
				
						int ex = DataPlacement.Basis_Exchange_Unit;
						long tupleDataSize = oldTuple.getCloudletFileSize();
						int nb_Unit = (int) (tupleDataSize / ex);
						if(tupleDataSize % ex != 0) nb_Unit++;
						
						
						//**//**System.out.println(this.getName()+ " Send old tuple write:"+oldTuple.toString()+"\nto the non selected storage node:"+rep+" in time event:"+String.valueOf(latency+ev.eventTime()));
						Log.writeInLogFile(this.getName(), " Send old tuple write:"+oldTuple.toString()+"\nto the non selected storage node:"+rep+" in time event:"+String.valueOf(latency+ev.eventTime()));
						
						send(rep, latency*nb_Unit , FogEvents.DELAY_TUPLE_STORAGE, oldTuple);
						
						if(maxlatency< (latency*nb_Unit))
							maxlatency = latency*nb_Unit;
	
					}
					
				}else{
					//**//**System.out.println(this.getName()+ " there are other reader from this replicas! non delayed tuple storage are checked in replica:"+rep);
					Log.writeInLogFile(this.getName(), " there are other reader from this replicas! non delayed tuple storage are checked in replica:"+rep);
					
				}
			}
			
			LatencyStats.add_Overall_delayed_write_Latency(maxlatency);
			
			/*
			 * Check if there are not other locked writes in other replicas for this tuple ==> launch the oldest blocked write , Otherwise, do no thing
			 */			
			if(!application.getDataConsistencyProtocol().checkIfThereAreCurrentLockedReplicaForTuple(tupleType, dataAllocation)){
				
				/*
				 * get old blocked write
				 */
				
				if(application.getDataConsistencyProtocol().checkIfTupleIsInCurrentBlockedWrites(tuple.getTupleType())){
					
					/*
					 * Check if there is the minimum of required replicas is free
					 */
					
					List<Integer> allReplicas = new ArrayList<Integer>();
					
					//*//*//**System.out.println("DataAllocation.getEmplacementNodeId("+tuple.getTupleType()+"):"+DataAllocation.getEmplacementNodeId(tuple.getTupleType()).toString());
					
					allReplicas.addAll(dataAllocation.getEmplacementNodeId(tuple.getTupleType()));
					Log.writeInLogFile(this.getName(),"All replicas:"+allReplicas.toString());
					//**//**System.out.println(this.getName()+" All replicas:"+allReplicas.toString());
										
					List<Integer> unLockedReplicas = this.application.getDataConsistencyProtocol().getUnlockedReplicasForWrite(tuple.getTupleType(), dataAllocation);
					Log.writeInLogFile(this.getName(),"UnlockedReplicas:"+unLockedReplicas.toString());
					//**//**System.out.println(this.getName()+" UnlockedReplicas:"+unLockedReplicas.toString());
					
					int nb_total_replica = allReplicas.size();
					
					if(!(unLockedReplicas.size()<application.getDataConsistencyProtocol().getNumberOfResponseWriteReplica(nb_total_replica))){
						
						Pair<Tuple,Double> pair = this.application.getDataConsistencyProtocol().getOldWriteFromCurrentBlockedWrite(tupleType);
						Tuple oldTuple = pair.getFirst();
						sendNow(2, FogEvents.BLOCKED_WRITE_STORAGE, oldTuple);
												
						/*
						 * Add the blocked write time stamp
						 */
						
						double blockedWriteLatency = ev.eventTime()-pair.getSecond();
						LatencyStats.add_Overall_blocked_write_Latency(blockedWriteLatency);
						
						//**//**System.out.println(this.getName()+ " Send for storage old blocked write:"+oldTuple.toString());
						Log.writeInLogFile(this.getName(), " Send for storage old blocked write:"+oldTuple.toString());
					
					}else{
						//**//**System.out.println(this.getName()+ " the minimum required free replicas is not satisfied for tuple:"+tuple.toString());
						Log.writeInLogFile(this.getName(), " the minimum required free replicas is not satisfied for tuple:"+tuple.getTupleType());
					}				
					
				}else{
					//**//**System.out.println(this.getName()+ " there is no blocked write for tuple:"+tuple.getTupleType());
					Log.writeInLogFile(this.getName(), " there is no blocked write for tuple:"+tuple.getTupleType());
				}
							
			}else {
				//**//**System.out.println(this.getName()+ " there is a locked write in somme replica for tuple:"+tupleType);
				Log.writeInLogFile(this.getName(), " there is a locked write in somme replica for tuple:"+tupleType);
			}
		
		}
		
//		//**Scanner sc = new Scanner(System.in);
//		//**String str = sc.nextLine();
	}
	
//	public void initializeDataRetrieveRequests_estimation(Application application){
//		//*//*//**System.out.println("Initialize consumption requests!");
//		for(AppEdge edge : application.getEdges()){
//			if(edge.getSource().startsWith("s-"))
//				continue;
//			if(edge.getDestination().get(0).startsWith("DISPLAY"))
//				continue;
//			
//			for(String moduleName : edge.getDestination()){
//				String fogDevName = ModuleMapping.getDeviceHostModule(moduleName);
//				FogDevice fogdev = application.getFogDeviceByName(fogDevName);
//				/*
//				 * tab [0]: DataProd Id
//				 * tab [1]: destinater service name
//				 * tab [2]: edge
//				 * tab [3]: list of response replicas
//				 * tab [4]: is reserved for the median replicas
//				 * tab [5]: requestId
//				 */
//				Object [] tab = new Object[6];
//				tab[0] = application.getFogDeviceByName(ModuleMapping.getDeviceHostModule(edge.getSource())).getId();
//				tab[1] = moduleName;
//				tab[2] = edge;
//				tab[3] = null;
//				tab[4] = -1;
//				tab[5] = -1;
//				
//				//**System.out.println("initializeDataRetrieveRequests_estimation");
//				//**System.out.println("edge: "+edge.toString());
//				//**System.out.println("consusmer: "+moduleName);
//				//**System.out.println("consumerNodeId: not defined yet");
//				
//				
//				
//				
//				List<Double> consumptionTimes = new ArrayList<Double>();
//				consumptionTimes = Stats.loadConsumption(edge.getTupleType(), moduleName);
//				
//				for(Double delay : consumptionTimes){
//					Log.writeInLogFile(this.getName(),"Send Initialize consumption for:"+moduleName+"\tnode:"+fogdev.getName());
//					send(fogdev.getId(), delay,FogEvents.INITIALIZE_CONSUMPTION, tab);
//				}
//
//			}
//		}
//	}
	
	
	public void initializeDataRetrieveRequests_global(Application application){
		//**System.out.println("Initialize consumption requests for IoT data!");
		//**System.out.println("application.getEdges().size:"+application.getEdges().size());
		for(AppEdge edge : application.getEdges()){
			if(edge.getSource().startsWith("s-"))
				continue;
			if(edge.getDestination().get(0).startsWith("DISPLAY"))
				continue;
			
			for(String moduleName : edge.getDestination()){ // consumers services
				String fogDevName = ModuleMapping.getDeviceHostModule(moduleName);
				FogDevice fogdev = application.getFogDeviceByName(fogDevName);

				
				List<Double> timeList = null;
				
				if(DataPlacement.load_timeList) {
					timeList = Stats.loadConsumption(moduleName, edge.getTupleType());
					
				}else {
					timeList = getConsumptionTimeList(moduleName, edge.getTupleType());
				}
				
				
				Object [] tab = new Object[6];
				tab[0] = fogdev.getId();
				tab[1] = moduleName;
				tab[2] = edge;
				tab[3] = timeList;
				tab[4] = null;
				tab[5] = null;
				
				
				Log.writeInLogFile(this.getName(),"initializeDataRetrieveRequests_global");
				Log.writeInLogFile(this.getName(),"edge: "+edge.toString());
				Log.writeInLogFile(this.getName(),"consusmer: "+moduleName);
				Log.writeInLogFile(this.getName(),"consumerNodeId: not defined yet");
				
				sendNow(fogdev.getId(), FogEvents.INITIALIZE_CONSUMPTION, tab);
			}
		}
	}
	
	
	private List<Double> getConsumptionTimeList(String moduleName, String tupleType) {
		// TODO Auto-generated method stub
		
		List<Double> listconsumption = new ArrayList<Double>();

		
		double time = (float) (Math.random() * DataPlacement.DataConsRequestInterval + 5000);
		
		while(time < Config.MAX_SIMULATION_TIME) {
			listconsumption.add(time);
			time += (float) (Math.random() * DataPlacement.DataConsRequestInterval + 5000);
		}

		
		return listconsumption;
	}

	public void intializeproduction(){
		//**System.out.println("intialize production of IoT data");
		
		for(AppEdge edge : application.getEdges()) {
			String tupleType = edge.getTupleType();
			
			if(tupleType.startsWith("TempSNR") || tupleType.startsWith("TempAct") )
				continue;
		
			
			String serviceName = FogStorage.application.getEdgeMap().get(tupleType).getSource();
			FogDevice fogdev =   FogStorage.application.getFogDeviceByName(ModuleMapping.getDeviceHostModule(serviceName));
			
			List<Double> timeList = null;
			
			if(DataPlacement.load_timeList) {
				timeList = Stats.loadProduction(tupleType);
				
			}else {
				timeList = getProductionTimeList(tupleType);
			}

			
			productionListMap.put(tupleType, timeList);
			

		}
	}
	
	
	public void intializeproduction2(){
		//**System.out.println("intialize production of IoT data");
		
		for(AppEdge edge : application.getEdges()) {
			String tupleType = edge.getTupleType();
			
			if(tupleType.startsWith("TempSNR") || tupleType.startsWith("TempAct") )
				continue;
		
			
			String serviceName = FogStorage.application.getEdgeMap().get(tupleType).getSource();
			FogDevice fogdev =   FogStorage.application.getFogDeviceByName(ModuleMapping.getDeviceHostModule(serviceName));
			
			List<Double> timeList = null;
			
			timeList = productionListMap.get(tupleType);
			
			
			Object [] tab = new  Object[3];
			tab[0] = edge;
			tab[1] = timeList;
			
			
			//**//**System.out.println(this.getName()+"\tSend initialize periodic tuple production to "+fogdev.getName()+" for tuple "+tupleType);
			sendNow(fogdev.getId(), FogEvents.INITIALIZE_PERIODIC_TUPLE_PRODUCTION, tab);
		}
	}
	
	public void printAllProductionTimeList() {
		//**System.out.println("printAllProductionTimeList");
		for(String tupletype: productionListMap.keySet()) {
			//**System.out.println(tupletype+": "+productionListMap.get(tupletype).toString());
		}
	}
	
	
	
//	public void intializePeriodicTupleProduction() {
//		
//		AppEdge edge = application.getEdges().get(0);
//		
//		String tupleType = edge.getTupleType();
//		String serviceName = FogStorage.application.getEdgeMap().get(tupleType).getSource();
//		FogDevice fogdev =   FogStorage.application.getFogDeviceByName(ModuleMapping.getDeviceHostModule(serviceName));
//		
//		AppModule appModule = application.getModuleByName(edge.getSource());
//
//		float latency = 0;
//		
//		if(fogdev.getName().startsWith("HGW")){
//			latency = 50;
//		}
//
//		List<Double> timeList = Stats.loadProduction(tupleType);
//
//		for(Double time: timeList){
//			int version = appModule.getVersion();
//			version++;
//			
//			Tuple tuple = FogBroker.application.createTuple(edge, this.getId());
//			
//			appModule.setVersion(version);
//			tuple.setTupleVersion(version);
//			
//			LatencyStats.add_Overall_write_Latency(latency);
//			LatencyStats.addWriteLatency(tupleType , latency);
//			
//			Log.writeInLogFile(this.getName(), "Send tuple storage time:"+time+"\ttuple:"+tuple.toString());
//			send(this.getId(), time, FogEvents.TUPLE_STORAGE, tuple);
//		}
//						
//	}
	
//	public void intializePeriodicTupleProduction() {
//		
//		AppEdge edge = application.getEdges().get(0);
//		
//		String tupleType = edge.getTupleType();
//		String serviceName = FogStorage.application.getEdgeMap().get(tupleType).getSource();
//		FogDevice fogdev =   FogStorage.application.getFogDeviceByName(ModuleMapping.getDeviceHostModule(serviceName));
//		
//		AppModule appModule = application.getModuleByName(edge.getSource());
//
//		float latency = 0;
//		
//		if(fogdev.getName().startsWith("HGW")){
//			latency = 50;
//		}
//
//		List<Double> timeList = Stats.loadProduction(tupleType);
//
//		for(Double time: timeList){
//			int version = appModule.getVersion();
//			version++;
//			
//			Tuple tuple = FogBroker.application.createTuple(edge, this.getId());
//			
//			appModule.setVersion(version);
//			tuple.setTupleVersion(version);
//			
//			LatencyStats.add_Overall_write_Latency(latency);
//			LatencyStats.addWriteLatency(tupleType , latency);
//			
//			Log.writeInLogFile(this.getName(), "Send tuple storage time:"+time+"\ttuple:"+tuple.toString());
//			send(this.getId(), time, FogEvents.TUPLE_STORAGE, tuple);
//		}
//						
//	}
	
	private List<Double> getProductionTimeList(String tupleType) {
		// TODO Auto-generated method stub
		List<Double> list = new ArrayList<Double>();
		
		if(tupleType.startsWith("TempHGW")) {
			double time = 1010.1;
			while(time < Config.MAX_SIMULATION_TIME) {
				list.add(time);
				time += 10000;
			}
		}
		
		if(tupleType.startsWith("TempLPOP")) {
			double time = 2010.1;
			while(time < Config.MAX_SIMULATION_TIME) {
				list.add(time);
				time += 30010.1 + Math.random()* 30010.1;
			}
		}
		
		if(tupleType.startsWith("TempRPOP")) {
			double time = 3010.1;
			while(time < Config.MAX_SIMULATION_TIME) {
				list.add(time);
				time +=  60010.1 + Math.random()* 60010.1;
			}
		}
		return list;
	}

	private void addStorageAck(Tuple tuple, int rep){
		Pair<Tuple,Integer> pair = new Pair<Tuple,Integer>(tuple,rep);
		
		if(this.storageACKTestingMap.containsKey(pair)){
			System.out.println("error, storing tuple more than ones!");
			System.exit(0);
		}
		
		this.storageACKTestingMap.put(pair, true);
		
	}
	
	
	
		
}
