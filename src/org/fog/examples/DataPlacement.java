package org.fog.examples;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;





import org.StorageMode.ClosestNodeStorage;
import org.StorageMode.CloudStorage;
import org.StorageMode.FogStorage;
import org.StorageMode.GraphPartitionStorage;
import org.StorageMode.ZoningStorage;
import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.cplex.DataAllocation;
import org.fog2.entities.FogCache;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.distribution.DeterministicDistribution;
import org.fog2.entities.Actuator;
import org.fog2.entities.FogDevice;
import org.fog2.entities.FogDeviceCharacteristics;
import org.fog2.entities.Sensor;

public class DataPlacement {

	/* Cloudlet length in million of instructions */
	public static final int SNR_TUPLE_CPU_SIZE = 100;
	public static final int HGW_TUPLE_CPU_SIZE = 200;
	public static final int LPOP_TUPLE_CPU_SIZE = 300;
	public static final int RPOP_TUPLE_CPU_SIZE = 300;
	public static final int DC_TUPLE_CPU_SIZE = 500;
	public static final int ACT_TUPLE_CPU_SIZE = 100;

	/* Cloudlet file size in KB */
	public static final int SNR_TUPLE_FILE_SIZE = 96;

	public static final int HGW_TUPLE_FILE_SIZE = 96 * 10;
	public static final int LPOP_TUPLE_FILE_SIZE = 96 * 10;
	public static final int RPOP_TUPLE_FILE_SIZE = 96 * 10;
	private static final int DC_TUPLE_FILE_SIZE = 96 * 10;

	public static final int ACT_TUPLE_FILE_SIZE = 96;

	/* Basis service latencies */
	public static final float leftLatencyDC = 100;
	public static final float rightLatencyDC = 100;
	public static final float leftLatencyRPOP = 5;
	public static final float rightLatencyRPOP = 5;

	public static final float LatencyDCToRPOP = 100;
	public static final float LatencyRPOPToLPOP = 5;
	public static final float LatencyLPOPToHGW = 50;
	public static final float LatencyHGWToSNR = 10;
	public static final float LatencyHGWToACT = 10;

	/* Basis exchange unit on KB */
	public static final int Basis_Exchange_Unit = 64;

	/* CPU requirement for modules on Fog devices in mips */
	private static final int SERVICE_DC_CPU = 1000; // CPU dans les VMs
	private static final int SERVICE_RPOP_CPU = 1000;
	private static final int SERVICE_LPOP_CPU = 1000;
	private static final int SERVICE_HGW_CPU = 1000;

	/* RAM requirement for modules on Fog devices in Ko */
	public static final int SERVICE_DC_RAM = 100; // RAM dans les VMs
	public static final int SERVICE_RPOP_RAM = 100;
	public static final int SERVICE_LPOP_RAM = 100;
	public static final int SERVICE_HGW_RAM = 100;

	/* Fog devices storage capacity on KB */
	public static final long DC_Storage = 1000000000; // 1TB
	public static final long RPOP_Storage = 100000000; // 100 GB
	public static final long LPOP_Storage = 10000000; // 10 GB
	public static final long HGW_Storage = 1000000; // 1 GB
	
	
	
///////////////////////////////////////////////////////////////////////////////////////	
//	// Grande infra 30% cache zipf 0.8 nb-cache 5 stratégie 1
	
//	public static int nb_HGW = 100;
//	public static final int nb_LPOP = 10; 
//	public static final int nb_RPOP = 5;
//	public static final int nb_DC = 1; 
//	public static final int nb_consperstream = 100;
//	public static final int NB_cache = 5;
//	private static final float Dimension = (float) 0.7; // 70% storage 30% cache
//	public static final float zipf=(float) 0.8;
//	
//	// Strategie 1
//	public static boolean use_overflow = true;
//	public static boolean use_pmedian = true;
//	
//	//Infrastructure 
//	public static boolean generateInfrastrucutre = true;
//	
//	//Fogdevices
//	private static boolean loadStorageCapacity = false;
//	
//	// Stream data
//	public static boolean loadInitial_Stream_Storage = false;
//	public static boolean loadtime_Stream_consumption = false;
//	public static boolean load_stream_consumersList = false;
//	
//	// IoT data
//	public static boolean load_timeList = false;
//	
//	public static final String Strategy = "iFogStorSLA";

///////////////////////////////////////////////////////////////////////////////////////
//	// Grande infra 30% cache zipf 0.8 nb-cache 5 stratégie 3
	
//	public static int nb_HGW = 100;
//	public static final int nb_LPOP = 10; 
//	public static final int nb_RPOP = 5;
//	public static final int nb_DC = 1; 
//	public static final int nb_consperstream = 100;
//	public static final int NB_cache = 5;
//	private static final float Dimension = (float) 0.7; // 70% storage 30% cache
//	public static final float zipf=(float) 0.8;
	
//	// Strategie 3
//	public static boolean use_overflow = false;
//	public static boolean use_pmedian = false;
//	
//	//Infrastructure 
//	public static boolean generateInfrastrucutre = false;
//	
//	//Fogdevices
//	private static boolean loadStorageCapacity = true;
//	
//	// Stream data
//	public static boolean loadInitial_Stream_Storage = true;
//	public static boolean loadtime_Stream_consumption = true;
//	public static boolean load_stream_consumersList = true;
//	
//	// IoT data
//	public static boolean load_timeList = true;
//
//	public static final String Strategy = "iFogStorSLA";
	
	
///////////////////////////////////////////////////////////////////////////////////////	
//	// Grande infra 30% cache zipf 1.2 nb-cache 5 stratégie 1
	
//	public static int nb_HGW = 100;
//	public static final int nb_LPOP = 10; 
//	public static final int nb_RPOP = 5;
//	public static final int nb_DC = 1; 
//	public static final int nb_consperstream = 100;
//	public static final int NB_cache = 5;
//	private static final float Dimension = (float) 0.7; // 70% storage 30% cache
//	public static final float zipf=(float) 1.2;
//
//	// Strategie 1
//	public static boolean use_overflow = true;
//	public static boolean use_pmedian = true;
//
//	//Infrastructure 
//	public static boolean generateInfrastrucutre = false;
//	
//	//Fogdevices
//	private static boolean loadStorageCapacity = true;
//	
//	// Stream data
//	public static boolean loadInitial_Stream_Storage = true;
//	public static boolean loadtime_Stream_consumption = true;
//	public static boolean load_stream_consumersList = true;
//	
//	// IoT data
//	public static boolean load_timeList = true;
//
//	public static final String Strategy = "iFogStorSLA";
	
///////////////////////////////////////////////////////////////////////////////////////
//	// Grande infra 30% cache zipf 1.2 nb-cache 5 stratégie 2
	
//	public static int nb_HGW = 100;
//	public static final int nb_LPOP = 10; 
//	public static final int nb_RPOP = 5;
//	public static final int nb_DC = 1; 
//	public static final int nb_consperstream = 100;
//	public static final int NB_cache = 5;
//	private static final float Dimension = (float) 0.7; // 70% storage 30% cache
//	public static final float zipf=(float) 1.2;
//	
//	// Strategie 2
//	public static boolean use_overflow = false;
//	public static boolean use_pmedian = false;
//	
//	//Infrastructure 
//	public static boolean generateInfrastrucutre = false;
//	
//	//Fogdevices
//	private static boolean loadStorageCapacity = true;
//	
//	// Stream data
//	public static boolean loadInitial_Stream_Storage = true;
//	public static boolean loadtime_Stream_consumption = true;
//	public static boolean load_stream_consumersList = true;
//	
//	// IoT data
//	public static boolean load_timeList = true;
//	public static final String Strategy = "iFogStor";
	
///////////////////////////////////////////////////////////////////////////////////////
//	// Grande infra 30% cache zipf 1.2 nb-cache 5 stratégie 3

//	public static int nb_HGW = 100;
//	public static final int nb_LPOP = 10; 
//	public static final int nb_RPOP = 5;
//	public static final int nb_DC = 1; 
//	public static final int nb_consperstream = 100;
//	public static final int NB_cache = 5;
//	private static final float Dimension = (float) 0.7; // 70% storage 30% cache
//	public static final float zipf=(float) 1.2;
//	
//	// Strategie 3
//	public static boolean use_overflow = false;
//	public static boolean use_pmedian = false;
//	
//	//Infrastructure 
//	public static boolean generateInfrastrucutre = false;
//	
//	//Fogdevices
//	private static boolean loadStorageCapacity = true;
//	
//	// Stream data
//	public static boolean loadInitial_Stream_Storage = true;
//	public static boolean loadtime_Stream_consumption = true;
//	public static boolean load_stream_consumersList = true;
//	
//	// IoT data
//	public static boolean load_timeList = true;
//
//	public static final String Strategy = "iFogStorSLA";
	
///////////////////////////////////////////////////////////////////////////////////////
//	// Grande infra 30% cache zipf 1.2 nb-cache 5 stratégie 4
	
//	public static int nb_HGW = 100;
//	public static final int nb_LPOP = 10; 
//	public static final int nb_RPOP = 5;
//	public static final int nb_DC = 1; 
//	public static final int nb_consperstream = 100;
//	public static final int NB_cache = 5;
//	private static final float Dimension = (float) 0.7; // 70% storage 30% cache
//	public static final float zipf=(float) 1.2;
//
// // Strategie 4
//	public static boolean use_overflow = true;
//	public static boolean use_pmedian = false;
//
//	//Infrastructure 
//	public static boolean generateInfrastrucutre = false;
//
//	//Fogdevices
//	private static boolean loadStorageCapacity = true;
//
//	// Stream data
//	public static boolean loadInitial_Stream_Storage = true;
//	public static boolean loadtime_Stream_consumption = true;
//	public static boolean load_stream_consumersList = true;
//
//	// IoT data
//	public static boolean load_timeList = true;
//
//	public static final String Strategy = "iFogStorSLA";
	
	
///////////////////////////////////////////////////////////////////////////////////////	
//	// Grande infra 30% cache zipf 1.2 nb-cache 5 stratégie 5

//	public static int nb_HGW = 100;
//	public static final int nb_LPOP = 10; 
//	public static final int nb_RPOP = 5;
//	public static final int nb_DC = 1; 
//	public static final int nb_consperstream = 100;
//	public static final int NB_cache = 5;
//	private static final float Dimension = (float) 0.7; // 70% storage 30% cache
//	public static final float zipf=(float) 1.2;
//
//	// Strategie 5
//	public static boolean use_overflow = false;
//	public static boolean use_pmedian = true;
//
//	//Infrastructure 
//	public static boolean generateInfrastrucutre = false;
//
//	//Fogdevices
//	private static boolean loadStorageCapacity = true;
//
//	// Stream data
//	public static boolean loadInitial_Stream_Storage = true;
//	public static boolean loadtime_Stream_consumption = true;
//	public static boolean load_stream_consumersList = true;
//
//	// IoT data
//	public static boolean load_timeList = true;
//
//	public static final String Strategy = "iFogStorSLA";
	
	
///////////////////////////////////////////////////////////////////////////////////////		
//	// Grande infra 30% cache zipf 1.2 nb-cache 2 stratégie 1
	
//	public static int nb_HGW = 100;
//	public static final int nb_LPOP = 10; 
//	public static final int nb_RPOP = 5;
//	public static final int nb_DC = 1; 
//	public static final int nb_consperstream = 100;
//	public static final int NB_cache = 2;
//	private static final float Dimension = (float) 0.7; // 70% storage 30% cache
//	public static final float zipf=(float) 1.2;
//
//	// Strategie 1
//	public static boolean use_overflow = true;
//	public static boolean use_pmedian = true;
//
//	//Infrastructure 
//	public static boolean generateInfrastrucutre = false;
//	
//	//Fogdevices
//	private static boolean loadStorageCapacity = true;
//	
//	// Stream data
//	public static boolean loadInitial_Stream_Storage = true;
//	public static boolean loadtime_Stream_consumption = true;
//	public static boolean load_stream_consumersList = true;
//	
//	// IoT data
//	public static boolean load_timeList = true;
//
//	public static final String Strategy = "iFogStorSLA";
	
///////////////////////////////////////////////////////////////////////////////////////		
//	// Grande infra 30% cache zipf 1.2 nb-cache 2 stratégie 3

//	public static int nb_HGW = 100;
//	public static final int nb_LPOP = 10; 
//	public static final int nb_RPOP = 5;
//	public static final int nb_DC = 1; 
//	public static final int nb_consperstream = 100;
//	public static final int NB_cache = 2;
//	private static final float Dimension = (float) 0.7; // 70% storage 30% cache
//	public static final float zipf=(float) 1.2;
//	
//	// Strategie 3
//	public static boolean use_overflow = false;
//	public static boolean use_pmedian = false;
//	
//	//Infrastructure 
//	public static boolean generateInfrastrucutre = false;
//	
//	//Fogdevices
//	private static boolean loadStorageCapacity = true;
//	
//	// Stream data
//	public static boolean loadInitial_Stream_Storage = true;
//	public static boolean loadtime_Stream_consumption = true;
//	public static boolean load_stream_consumersList = true;
//	
//	// IoT data
//	public static boolean load_timeList = true;
//
//	public static final String Strategy = "iFogStorSLA";
	
///////////////////////////////////////////////////////////////////////////////////////		
//	// Grande infra 30% cache zipf 1.2 nb-cache 10 stratégie 1

//	public static int nb_HGW = 100;
//	public static final int nb_LPOP = 10; 
//	public static final int nb_RPOP = 5;
//	public static final int nb_DC = 1; 
//	public static final int nb_consperstream = 100;
//	public static final int NB_cache = 10;
//	private static final float Dimension = (float) 0.7; // 70% storage 30% cache
//	public static final float zipf=(float) 1.2;
//
//	// Strategie 1
//	public static boolean use_overflow = true;
//	public static boolean use_pmedian = true;
//
//	//Infrastructure 
//	public static boolean generateInfrastrucutre = false;
//	
//	//Fogdevices
//	private static boolean loadStorageCapacity = true;
//	
//	// Stream data
//	public static boolean loadInitial_Stream_Storage = true;
//	public static boolean loadtime_Stream_consumption = true;
//	public static boolean load_stream_consumersList = true;
//	
//	// IoT data
//	public static boolean load_timeList = true;
//
//	public static final String Strategy = "iFogStorSLA";

///////////////////////////////////////////////////////////////////////////////////////		
//	// Grande infra 30% cache zipf 1.2 nb-cache 10 stratégie 3

//	public static int nb_HGW = 100;
//	public static final int nb_LPOP = 10; 
//	public static final int nb_RPOP = 5;
//	public static final int nb_DC = 1; 
//	public static final int nb_consperstream = 100;
//	public static final int NB_cache = 10;
//	private static final float Dimension = (float) 0.7; // 70% storage 30% cache
//	public static final float zipf=(float) 1.2;
//
//	// Strategie 3
//	public static boolean use_overflow = false;
//	public static boolean use_pmedian = false;
//
//	//Infrastructure 
//	public static boolean generateInfrastrucutre = false;
//
//	//Fogdevices
//	private static boolean loadStorageCapacity = true;
//
//	// Stream data
//	public static boolean loadInitial_Stream_Storage = true;
//	public static boolean loadtime_Stream_consumption = true;
//	public static boolean load_stream_consumersList = true;
//
//	// IoT data
//	public static boolean load_timeList = true;
//
//	public static final String Strategy = "iFogStorSLA";
	
///////////////////////////////////////////////////////////////////////////////////////	
//	// Grande infra 50% cache zipf 1.2 nb-cache 5 stratégie 1

//	public static int nb_HGW = 100;
//	public static final int nb_LPOP = 10; 
//	public static final int nb_RPOP = 5;
//	public static final int nb_DC = 1; 
//	public static final int nb_consperstream = 100;
//	public static final int NB_cache = 5;
//	private static final float Dimension = (float) 0.5; // 50% storage 50% cache
//	public static final float zipf=(float) 1.2;
//
//	// Strategie 1
//	public static boolean use_overflow = true;
//	public static boolean use_pmedian = true;
//
//	//Infrastructure 
//	public static boolean generateInfrastrucutre = false;
//
//	//Fogdevices
//	private static boolean loadStorageCapacity = false;
//
//	// Stream data
//	public static boolean loadInitial_Stream_Storage = true;
//	public static boolean loadtime_Stream_consumption = true;
//	public static boolean load_stream_consumersList = true;
//
//	// IoT data
//	public static boolean load_timeList = true;
//
//	public static final String Strategy = "iFogStorSLA";

///////////////////////////////////////////////////////////////////////////////////////		
//	// Grande infra 50% cache zipf 1.2 nb-cache 5 stratégie 3

//	public static int nb_HGW = 100;
//	public static final int nb_LPOP = 10; 
//	public static final int nb_RPOP = 5;
//	public static final int nb_DC = 1; 
//	public static final int nb_consperstream = 100;
//	public static final int NB_cache = 5;
//	private static final float Dimension = (float) 0.5; // 50% storage 50% cache
//	public static final float zipf=(float) 1.2;
//
//	// Strategie 3
//	public static boolean use_overflow = false;
//	public static boolean use_pmedian = false;
//
//	//Infrastructure 
//	public static boolean generateInfrastrucutre = false;
//
//	//Fogdevices
//	private static boolean loadStorageCapacity = true;
//
//	// Stream data
//	public static boolean loadInitial_Stream_Storage = true;
//	public static boolean loadtime_Stream_consumption = true;
//	public static boolean load_stream_consumersList = true;
//
//	// IoT data
//	public static boolean load_timeList = true;
//	public static final String Strategy = "iFogStorSLA";

///////////////////////////////////////////////////////////////////////////////////////	
//	// Grande infra 70% cache zipf 1.2 nb-cache 5 stratégie 1

//	public static int nb_HGW = 100;
//	public static final int nb_LPOP = 10; 
//	public static final int nb_RPOP = 5;
//	public static final int nb_DC = 1; 
//	public static final int nb_consperstream = 100;
//	public static final int NB_cache = 5;
//	private static final float Dimension = (float) 0.3; // 30% storage 70% cache
//	public static final float zipf=(float) 1.2;
//
//	// Strategie 1
//	public static boolean use_overflow = true;
//	public static boolean use_pmedian = true;
//
//	//Infrastructure 
//	public static boolean generateInfrastrucutre = false;
//	
//	//Fogdevices
//	private static boolean loadStorageCapacity = false;
//	
//	// Stream data
//	public static boolean loadInitial_Stream_Storage = true;
//	public static boolean loadtime_Stream_consumption = true;
//	public static boolean load_stream_consumersList = true;
//	
//	// IoT data
//	public static boolean load_timeList = true;
//
//	public static final String Strategy = "iFogStorSLA";

///////////////////////////////////////////////////////////////////////////////////////		
//	// Grande infra 70% cache zipf 1.2 nb-cache 5 stratégie 3

//	public static int nb_HGW = 100;
//	public static final int nb_LPOP = 10; 
//	public static final int nb_RPOP = 5;
//	public static final int nb_DC = 1; 
//	public static final int nb_consperstream = 100;
//	public static final int NB_cache = 5;
//	private static final float Dimension = (float) 0.3; // 30% storage 70% cache
//	public static final float zipf=(float) 1.2;
//
//	// Strategie 3
//	public static boolean use_overflow = false;
//	public static boolean use_pmedian = false;
//
//	//Infrastructure 
//	public static boolean generateInfrastrucutre = false;
//	
//	//Fogdevices
//	private static boolean loadStorageCapacity = true;
//	
//	// Stream data
//	public static boolean loadInitial_Stream_Storage = true;
//	public static boolean loadtime_Stream_consumption = true;
//	public static boolean load_stream_consumersList = true;
//	
//	// IoT data
//	public static boolean load_timeList = true;
//	public static final String Strategy = "iFogStorSLA";


///////////////////////////////////////////////////////////////////////////////////////	
//	// Petite infra 30% cache zipf 1.2 nb-cache 2 stratégie 1
	
	public static int nb_HGW = 10; 
	public static final int nb_LPOP = 5; 
	public static final int nb_RPOP = 1;
	public static final int nb_DC = 1;
	public static final int nb_consperstream = 10;
	public static final int NB_cache = 2;
	private static final float Dimension = (float) 0.7; // 70% storage 30% cache
	public static final float zipf=(float) 1.2;
	
	// Strategie 1
	public static boolean use_overflow = true;
	public static boolean use_pmedian = true;
	
	//Infrastructure 
	public static boolean generateInfrastrucutre = true;
	
	//Fogdevices
	private static boolean loadStorageCapacity = false;
	
	// Stream data
	public static boolean loadInitial_Stream_Storage = false;
	public static boolean loadtime_Stream_consumption = false;
	public static boolean load_stream_consumersList = false;
	
	// IoT data
	public static boolean load_timeList = false;
	
	public static final String Strategy = "iFogStorSLA";
	
	
///////////////////////////////////////////////////////////////////////////////////////		
//	// Petite infra 30% cache zipf 1.2 nb-cache 2 stratégie 3

//	public static int nb_HGW = 10; 
//	public static final int nb_LPOP = 5; 
//	public static final int nb_RPOP = 1;
//	public static final int nb_DC = 1;
//	public static final int nb_consperstream = 10;
//	public static final int NB_cache = 2;
//	private static final float Dimension = (float) 0.7; // 70% storage 30% cache
//	public static final float zipf=(float) 1.2;
//	
//	// Strategie 3
//	public static boolean use_overflow = false;
//	public static boolean use_pmedian = false;
//	
//	//Infrastructure 
//	public static boolean generateInfrastrucutre = false;
//	
//	//Fogdevices
//	private static boolean loadStorageCapacity = true;
//	
//	// Stream data
//	public static boolean loadInitial_Stream_Storage = true;
//	public static boolean loadtime_Stream_consumption = true;
//	public static boolean load_stream_consumersList = true;
//	
//	// IoT data
//	public static boolean load_timeList = true;
//
//	public static final String Strategy = "iFogStorSLA";
	
	
///////////////////////////////////////////////////////////////////////////////////////		
//	// Moyenne infra 30% cache zipf 1.2 nb-cache 2 stratégie 1

//	public static int nb_HGW = 50; 
//	public static final int nb_LPOP = 5; 
//	public static final int nb_RPOP = 1;
//	public static final int nb_DC = 1;
//	public static final int nb_consperstream = 50;
//	public static final int NB_cache = 2;
//	private static final float Dimension = (float) 0.7; // 70% storage 30% cache
//	public static final float zipf=(float) 1.2;
//
//	// Strategie 1
//	public static boolean use_overflow = true;
//	public static boolean use_pmedian = true;
//
//	//Infrastructure 
//	public static boolean generateInfrastrucutre = true;
//
//	//Fogdevices
//	private static boolean loadStorageCapacity = false;
//
//	// Stream data
//	public static boolean loadInitial_Stream_Storage = false;
//	public static boolean loadtime_Stream_consumption = false;
//	public static boolean load_stream_consumersList = false;
//
//	// IoT data
//	public static boolean load_timeList = false;
//
//	public static final String Strategy = "iFogStorSLA";
	
///////////////////////////////////////////////////////////////////////////////////////		
//	// Moyenne infra 30% cache zipf 1.2 nb-cache 2 stratégie 3

//	public static int nb_HGW = 50; 
//	public static final int nb_LPOP = 5; 
//	public static final int nb_RPOP = 1;
//	public static final int nb_DC = 1;
//	public static final int nb_consperstream = 50;
//	public static final int NB_cache = 2;
//	private static final float Dimension = (float) 0.7; // 70% storage 30% cache
//	public static final float zipf=(float) 1.2;
//
//	// Strategie 3
//	public static boolean use_overflow = false;
//	public static boolean use_pmedian = false;
//	
//	//Infrastructure 
//	public static boolean generateInfrastrucutre = false;
//	
//	//Fogdevices
//	private static boolean loadStorageCapacity = true;
//	
//	// Stream data
//	public static boolean loadInitial_Stream_Storage = true;
//	public static boolean loadtime_Stream_consumption = true;
//	public static boolean load_stream_consumersList = true;
//	
//	// IoT data
//	public static boolean load_timeList = true;
//
//	public static final String Strategy = "iFogStorSLA";
	
	
	public static final int size_streaming_data = 300*1024;
	public static final int size_streaming_data_transfert = size_streaming_data / 100;
	
	public static final int nb_streaming_data = 500;
	public static final int nb_streaming_cons= nb_HGW;
	
	public static final int nb_SnrPerHGW = 1;
	public static final int nb_ActPerHGW = 1;
	
	public static int critical_data_pourcentage = 0;

	/* nb services on each level */
	public static int nb_Service_HGW;
	public static final int nb_Service_LPOP = nb_LPOP * 100 / 100;
	public static final int nb_Service_RPOP = nb_RPOP * 100 / 100;
	public static final int nb_Service_DC = nb_DC * 100 / 100;

	/* Services config */
	public static final long SERVICE_DC_BW = 1000;
	public static final int SERVICE_DC_MIPS = 1000;

	public static final long SERVICE_RPOP_BW = 1000;
	public static final int SERVICE_RPOP_MIPS = 1000;

	public static final long SERVICE_LPOP_BW = 1000;
	public static final int SERVICE_LPOP_MIPS = 1000;

	public static final long SERVICE_HGW_BW = 1000;
	public static final int SERVICE_HGW_MIPS = 1000;

	public static final String CloudStorage = "CloudStorage";
	public static final String ClosestNode = "ClosestNode";
	public static final String FogStorage = "FogStorage";
	public static final String ZoningStorage = "ZoningStorage";
	public static final String GraphPartitionStorage = "GraphPartitionStorage";

	public static final List<String> storageModes = Arrays.asList(FogStorage);
	//public static final List<String> storageModes = Arrays.asList(CloudStorage,FogStorage);

	public static final List<Integer> nb_zones_list = Arrays.asList(2,5,10);
	public static final List<Integer> nb_partitions_list = Arrays.asList(2,5,10);

	
	public static int nb_zone;
	
	public static int nb_partitions;

	public static String storageMode;

	public static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	public static List<Sensor> sensors = new ArrayList<Sensor>();
	public static List<Actuator> actuators = new ArrayList<Actuator>();

//	private static final List<Integer> dataConsPerDataProdList = Arrays.asList(1,3,5,10,15);
	private static final List<Integer> dataConsPerDataProdList = Arrays.asList(5);
		
	public static int nb_DataCons_By_DataProd;

	public static boolean parallel = true;
	
	
	
	public static final String zoning = "zoning";
	public static final String mixed = "mixed";
	public static final String distributed = "distributed";
	
	//private static final List<String> dataflows = Arrays.asList(zoning,mixed,distributed);
	public static final List<String> dataflows = Arrays.asList(distributed);
	
	
	
	public static String dataflow_used;
	
	public static int min_data_replica;
	public static int max_data_replica;
//	public static String floydPath ="~/Bureau/libFloydWarshallFloatPrallel.so";
	public static String floydPath ="/home/lydia/eclipse-workspace/iFogSimWithDataConsistencyCachePenality_PotentielNodes-/libFloydWarshallFloatPrallel.so";
	
	
	
	public static int nb_shortest_Paths_Nodes = 5;
	
//	private static final List<Integer> QWList = Arrays.asList(5,4,3,3,2,1);
//	private static final List<Integer> QRList = Arrays.asList(1,2,3,2,2,1);
	
	private static final List<Integer> QWList = Arrays.asList(1);
	private static final List<Integer> QRList = Arrays.asList(1);
	
	public static int QW;
	public static int QR;
	public static int NB_REP;
	
	public static int nb_externCons=0;
	
	public static boolean trace_flag = true; // mean trace events
	public static Calendar calendar;
	public static int num_user = 1; // number of cloud users
	public static boolean generate_log_file = false;
	
	public static final float DataConsRequestInterval= 10000;
	public static final double writeDelayRequest = 3000;
	public static final double DelayedWriteInLockReplicaTime = 3000;
	public static boolean load_consumption_times;
	
	/* SNR periodic samples in ms*/
	public static int SNR_TRANSMISSION_TIME = 1000;
	
	public static final String Quorum = "Quorum";
	public static final String ReadOneWriteAll = "ReadOneWriteAll";
	public static final String ReadOneWriteOne = "ReadOneWriteOne";
	public static final String Strong="Strong";
	public static final String Weak="Weak";
	public static String Consistencyprotocol = "Critical = "+critical_data_pourcentage;
	
	//private static final List<String> DataConsistencyProtocls = Arrays.asList(Quorum,ReadOneWriteAll,ReadOneWriteOne);
	
	//private static final List<String> DataConsistencyProtocls = Arrays.asList(ReadOneWriteAll);
	
	private static final List<String> DataConsistencyProtocls = Arrays.asList(Quorum);
	public static final float cache = (float) 1-Dimension;
	private static final float Saturation = (float) 0.2;

	public static String dcp;
	public static int sim=0;
	public static int nb_simulation=1;
	public static boolean submitApplication;
	public static double snrFraction = 0.0;
	public static double serviceFraction = 0.0;
	public static boolean sendPerdiodicTuples;
	public static int cond;
	public static String estimatedTuple;
	
	


	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			Log.disable();
			calendar = Calendar.getInstance();
			Log.initializeLogFile();
			System.out.println();
			
			System.out.println("Starting the simulation!");
			Log.writeInLogFile("DataPlacement", "Starting the simulation!");
			
			nb_Service_HGW = nb_HGW;
			

			for(int dpdc: dataConsPerDataProdList){

				nb_DataCons_By_DataProd=dpdc;
				System.out.println("nb_DataCons_By_DataProd="+ nb_DataCons_By_DataProd);
				
				Log.initializeLogFile();
				Log.writeInLogFile("DataPlacement", "nb_DataCons_By_DataProd="+ nb_DataCons_By_DataProd);
								
				for(String df: dataflows){
					//dataflow_used = df;
					
					
											
					for(int simul=0;simul<nb_simulation;simul++){
						sim = simul;
						
//						if(sim % 2 ==0){
//							DataPlacement.Consistencyprotocol = DataPlacement.Strong;
//						}else{
//							DataPlacement.Consistencyprotocol = DataPlacement.Weak;
//						}
//						
						
						
						dataflow_used = zoning;
						//dataflow_used = distributed;
						
						submitApplication= true;
						load_consumption_times = true;
						
						if(critical_data_pourcentage==0){
							load_consumption_times = false;
						}
							
						//for(String dcp2 : DataConsistencyProtocls){
						
						//for(int protocol = 0; protocol<QWList.size(); protocol++){	
							
							DataPlacement.dcp = DataPlacement.Quorum;
							
							//System.out.println("Data consistency protocl:"+protocol);
							
							DataPlacement.QW = QWList.get(0);
							DataPlacement.QR = QRList.get(0);
							
							for(String storMode : storageModes){
								storageMode = storMode;
								
								long b_sim, e_sim;
								b_sim = Calendar.getInstance().getTimeInMillis();
		
								if (storageMode.equals(FogStorage)) {
									FogStorage fog = new FogStorage();
									fog.sim();
						
						
								} 
//									else if (storageMode.equals(ZoningStorage)) {
//									ZoningStorage zoning = new ZoningStorage();
//									zoning.sim();
//		
//								} else if (storageMode.equals(GraphPartitionStorage)) {
//									GraphPartitionStorage graphpartition = new org.StorageMode.GraphPartitionStorage();
//									graphpartition.sim();
//						
//								} 
								
								e_sim = Calendar.getInstance().getTimeInMillis();
								org.fog.examples.Log.writeSimulationTime(nb_HGW, "all strategies simulaion time (minutes):\t"+String.valueOf((e_sim - b_sim)/60000));
							}
							
						//}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			// System.out.println("Unwanted errors happen");
		}
	}

	

	/**
	 * Create Fog Devices
	 * 
	 * @param userId
	 * @param appId
	 */

	public static void createFogDevices() {
		
		if(!DataPlacement.loadStorageCapacity ) {
			try {
				//System.out.println("save Storage capacities");
				FileWriter file = new FileWriter("saveStorageCapacities.txt");
				BufferedWriter fw = new BufferedWriter(file);
				//fw.write("");
				fw.close();
				
			} catch (Exception e) {
				// TODO: handle exception
			}
		
		}
		
		DataPlacement.fogDevices.clear();
		/* create Datacenters */
		for (int i = 0; i < nb_DC; i++) {
			// FogDevice DC = createFogDevice(fogId, nodeName, mips, ram, upBw,
			// downBw, level, ratePerMips, busyPower, idlePower);
			FogDevice DC = createFogDevice("DC" + i, 44800, 40000, 10000, 10000, 4, 0.01, 16 * 103, 16 * 83.25);
			DC.setParentId((int) -1);
			fogDevices.add(DC);
		}

		/* create RPOP */
		for (int i = 0; i < nb_RPOP; i++) {
			FogDevice RPOP = createFogDevice("RPOP" + i, 2800, 4000, 10000, 10000, 3, 0.0, 107.339, 83.4333);

			RPOP.setParentId((i / (nb_RPOP / nb_DC)) + 3);
			RPOP.setUplinkLatency(LatencyDCToRPOP);
			fogDevices.add(RPOP);
		}

		/* create LPOP */
		for (int i = 0; i < nb_LPOP; i++) {
			FogDevice LPOP = createFogDevice("LPOP" + i, 2800, 4000, 10000, 10000, 2, 0.0, 107.339, 83.4333);

			LPOP.setParentId((i / (nb_LPOP / nb_RPOP)) + nb_DC + 3);
			LPOP.setUplinkLatency(LatencyRPOPToLPOP);
			fogDevices.add(LPOP);
		}

		for (int i = 0; i < nb_HGW; i++) {
			FogDevice HGW = createFogDevice("HGW" + i, 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333);

			HGW.setParentId((i / (nb_HGW / nb_LPOP)) + nb_DC + nb_RPOP + 3);
			HGW.setUplinkLatency(LatencyLPOPToHGW);
			fogDevices.add(HGW);
		}
	}

	
	/**
	 * Create Sensors and actuators
	 * 
	 * @param userId
	 * @param appId
	 */
	

	public static void createSensorsAndActuators(int userId, String appId) {
		/* create HGW */
		int id_snr = 0;
		int id_act = 0;
		for (int i = 0; i < nb_HGW; i++) {
			FogDevice HGW = fogDevices.get(i + nb_DC + nb_RPOP + nb_LPOP);

			/* create sensors */
			for (int j = 0; j < nb_SnrPerHGW; j++, id_snr++) {
				Sensor snr = new Sensor("s-" + id_snr, "TempSNR"+ (int) (id_snr), userId, appId,new DeterministicDistribution(SNR_TRANSMISSION_TIME)); 
				sensors.add(snr);
				snr.setGatewayDeviceId(HGW.getId());
				snr.setLatency(LatencyHGWToSNR); 
			}

			/* create actuators */
			for (int k = 0; k < nb_ActPerHGW; k++, id_act++) {
				Actuator act = new Actuator("a-" + id_act, userId, appId,"DISPLAY" + (int) (id_act));
				actuators.add(act);
				act.setGatewayDeviceId(HGW.getId());
				act.setLatency(LatencyHGWToACT); 
			}

		}

	}

	
	public static long storageAllocation(String name) {
		long steady_state_storage ;
		if (name.startsWith("DC")) {
			steady_state_storage = (long) (Math.random()*DC_Storage*Dimension);
			
			if(steady_state_storage < (DC_Storage*(Dimension)*(Saturation))){
				//archivage
				steady_state_storage = (long)  (DC_Storage*(Dimension));
			}
			
			return steady_state_storage;
		}
			
		else if (name.startsWith("RPOP")) {
			steady_state_storage = (long) (Math.random()*RPOP_Storage*Dimension);
			
			if(steady_state_storage < (RPOP_Storage*(Dimension)*(Saturation))){
				steady_state_storage =  (long)  (RPOP_Storage*(Dimension));
			}
			
			return steady_state_storage;
		}
			
		else if (name.startsWith("LPOP")) {
			steady_state_storage = (long) (Math.random()*LPOP_Storage*Dimension);
			
			if(steady_state_storage < (LPOP_Storage*(Dimension)*(Saturation))){
				steady_state_storage =  (long)  (LPOP_Storage*(Dimension));
			}
			
			return steady_state_storage;
		}
			
		else if (name.startsWith("HGW")) {
			steady_state_storage = (long) (Math.random()*HGW_Storage*Dimension);
			
			if(steady_state_storage < (HGW_Storage*(Dimension)*(Saturation))){
				steady_state_storage =  (long)  (HGW_Storage*(Dimension));
			}
			
			return steady_state_storage;
		}
			
		else
			return -1;
	}
	
	public static double cacheAllocation(String name, double storage) {
		if (DataPlacement.Dimension==0.7)
			return storage * 3/7;
		
		if (DataPlacement.Dimension==0.3)
			return storage * 7/3;
		
		return storage;

	}
	
	
	private static FogDevice createFogDevice(String nodeName, long mips,
			int ram, long upBw, long downBw, int level, double ratePerMips,
			double busyPower, double idlePower) {

		List<Pe> peList = new ArrayList<Pe>();

		// 3. Create PEs and add these into a list.
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); 

		int hostId = FogUtils.generateEntityId();
		
		double storage = -1; 
		if(DataPlacement.loadStorageCapacity ) {
			
			try {

				FileReader fichier = new FileReader("saveStorageCapacities.txt");
				BufferedReader in = null;
				in = new BufferedReader(fichier);
				String line = null;

				while ((line = in.readLine()) != null) {
					String name;
					double stora;

					String[] splited = line.split("\t");
					name = splited[0];
					stora = Double.valueOf(splited[1]);
					if (name.equals(nodeName)) {
						storage=stora;
						break;
					}
				}
				in.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}else {
			storage = storageAllocation(nodeName); 
			try {
				//System.out.println("save Storage capacities");
				FileWriter file = new FileWriter("saveStorageCapacities.txt",true);
				BufferedWriter fw = new BufferedWriter(file);
				fw.write(nodeName+"\t"+storage+"\n");
				fw.close();
				
			} catch (Exception e) {
				// TODO: handle exception
			}
		}

		// host storage
		int bw = 10000;
		
		double cachestorage = cacheAllocation(nodeName, storage); // host storage


		PowerHost host = new PowerHost(hostId, new RamProvisionerSimple(ram),
				new BwProvisionerOverbooking(bw), storage, peList,
				new StreamOperatorScheduler(peList), new FogLinearPowerModel(busyPower, idlePower));

		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);

		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this
										// resource
		double costPerBw = 0.0; // the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>();
		
		FogCache fogcache = new FogCache(cachestorage, nodeName);

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(arch, os, vmm, host, time_zone, cost, costPerMem,costPerStorage, costPerBw, fogcache);

		int right = getRight(nodeName);
		int left = getleft(nodeName);

		FogDevice fogdevice = null;
		try {
			fogdevice = new FogDevice(nodeName, characteristics,new AppModuleAllocationPolicy(hostList), storageList,
					right, left, getRightLatency(nodeName, right),getLeftLatency(nodeName, left), 10, upBw, downBw, 0,ratePerMips);
		} catch (Exception e) {
			e.printStackTrace();
		}

		fogdevice.setLevel(level);
		return fogdevice;
	}
	

	private static float getRightLatency(String nodeName, int right) {
		if ((nodeName.startsWith("DC")) && (right != -1))
			return rightLatencyDC;
		else if ((nodeName.startsWith("RPOP")) && (right != -1))
			return rightLatencyRPOP;
		return -1;
	}
	

	private static float getLeftLatency(String nodeName, int left) {
		if ((nodeName.startsWith("DC")) && (left != -1))
			return leftLatencyDC;
		else if ((nodeName.startsWith("RPOP")) && (left != -1))
			return leftLatencyRPOP;
		return -1;
	}
	

	private static int getleft(String nodeName) {
		int fogId;
		if ((nodeName.startsWith("DC"))) {
			fogId = Integer.valueOf(nodeName.substring(2));
			if (fogId > 0) {
				return fogId - 1 + 3;
			} else {
				return -1;
			}
		} else if ((nodeName.startsWith("RPOP"))) {
			fogId = Integer.valueOf(nodeName.substring(4)) + nb_DC;
			if (fogId > (nb_DC)) {
				return fogId - 1 + 3;
			} else {
				return -1;
			}
		} else
			return -1;
	}
	

	private static int getRight(String nodeName) {
		int fogId;
		if ((nodeName.startsWith("DC"))) {
			fogId = Integer.valueOf(nodeName.substring(2));
			if ((nb_DC > 1) && (fogId < (nb_DC - 1))) {
				return fogId + 1 + 3;
			} else {
				return -1;
			}
		} else if ((nodeName.startsWith("RPOP"))) {

			fogId = Integer.valueOf(nodeName.substring(4)) + nb_DC;
			if ((nb_RPOP > 1) && (fogId < (nb_DC + nb_RPOP) - 1)) {
				return fogId + 1 + 3;
			} else {
				return -1;
			}
		} else
			return -1;
	}


	/**
	 * Create Application Add Modules Add AppEdges "Data flow" Add Tuples
	 * Mapping "Tuples Frequencies" Add AppLoop "Control"
	 * 
	 * @param appId
	 * @param userId
	 * @return
	 */
	
	
	private static List<String> getArrayListOfServices() {
		List<String> modulesList = new ArrayList<String>();
		try {
			if (DataPlacement.nb_DC > 0) {
				for (int i = 0; i < DataPlacement.nb_Service_DC; i++) {
					modulesList.add("ServiceDC" + i);
				}
			}
			if (DataPlacement.nb_RPOP > 0) {
				for (int i = 0; i < DataPlacement.nb_Service_RPOP; i++) {
					modulesList.add("ServiceRPOP" + i);
				}
			}
			if (DataPlacement.nb_LPOP > 0) {
				for (int i = 0; i < DataPlacement.nb_Service_LPOP; i++) {
					modulesList.add("ServiceLPOP" + i);
				}
			}
			if (DataPlacement.nb_HGW > 0) {
				for (int i = 0; i < DataPlacement.nb_Service_HGW; i++) {
					modulesList.add("ServiceHGW" + i);
					for (int j = 0; j < DataPlacement.nb_SnrPerHGW; j++) {
						modulesList.add("s-" + (int) (j + i * DataPlacement.nb_SnrPerHGW));
					}
					for (int k = 0; k < DataPlacement.nb_ActPerHGW; k++) {
						modulesList.add("DISPLAY"+ (int) (k + i * DataPlacement.nb_ActPerHGW));
					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			// System.out.println("Error in getArrayListOfServices()!");
		}

		return modulesList;
	}

    public static  Application createApplication(String appId, int userId){
		Application application = new Application(appId, userId);
		application.addServicesToApplication();

		/*
		 * Defining application loops to monitor the latency of. Here, we add
		 * only one loop for monitoring : EEG(sensor) -> Client -> Concentration
		 * Calculator -> Client -> DISPLAY (actuator)
		 */
		final AppLoop loop = new AppLoop(getArrayListOfServices());

		List<AppLoop> loops = new ArrayList<AppLoop>() {{add(loop);}};
		application.setLoops(loops);
		return application;
	}
    
    public static Application createApplication(String appId, int userId, List<FogDevice> listOfFogDevices){
		Application application = new Application(appId, userId);
		application.addServicesToApplication(listOfFogDevices);

		/*
		 * Defining application loops to monitor the latency of. Here, we add
		 * only one loop for monitoring : EEG(sensor) -> Client -> Concentration
		 * Calculator -> Client -> DISPLAY (actuator)
		 */
		final AppLoop loop = new AppLoop(getArrayListOfServices());

		List<AppLoop> loops = new ArrayList<AppLoop>() {{add(loop);}};
		application.setLoops(loops);
		return application;
	}

	/**
	 * Print devices "FogDevices, Actuators, Sensors"
	 */

	public static void printDevices() {
		// System.out.println("\nFog devices : ");
		for (FogDevice fogdev : fogDevices) {
			 System.out.println(fogdev.getName()+"  idEntity = "+fogdev.getId()+" up= "+fogdev.getParentId()+" left ="+fogdev.getLeftId()+" leftLatency = "+fogdev.getLeftLatency()+" right ="+fogdev.getRightId()+" rightLatency="+fogdev.getRightLatency()+" children = "+fogdev.getChildrenIds()+" childrenLatencies ="+fogdev.getChildToLatencyMap()+" Storage = "+fogdev.getVmAllocationPolicy().getHostList().get(0).getStorage()+" |	");
		}

		// System.out.println("\nSensors : ");
//		for (Sensor snr : sensors) {
//			 System.out.println(snr.getName()+"  HGW_ID = "+snr.getGatewayDeviceId()+" TupleType = "+snr.getTupleType()+" Latency = "+snr.getLatency()+" |	");
//		}
//		// System.out.println("\nActuators : ");
//		for (Actuator act : actuators) {
//			 System.out.println(act.getName()+" GW_ID = "+act.getGatewayDeviceId()+" Act_Type= "+act.getActuatorType()+" Latency = "+act.getLatency()+" |	");
//		}
		 System.out.println("\n");

	}

}
