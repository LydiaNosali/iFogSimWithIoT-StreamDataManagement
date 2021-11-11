package org.fog.stats;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.util.Pair;
import org.fog.examples.DataPlacement;


public class PenalityStats {
	
	public static double Overall_penality=0;
	public static double Overall_read_penality=0;
	public static double Overall_write_penality=0;
	
	public static double Overall_read_penalityIoT=0;
	public static double Overall_write_penalityIoT=0;
	
	public static double Overall_read_penalityStream=0;
	
	
	public static Map<Pair<Integer,String >, Double> penalityPerConsumer = new HashMap<Pair<Integer,String >, Double>();   
	
	
	//IoT
	public static void add_Overall_read_penalityIoT(double penality){
		Overall_read_penalityIoT+= penality;
		Overall_read_penality+= penality;
		Overall_penality+= penality;
	}
	public static void add_Overall_write_penalityIoT(double penality){
		Overall_write_penalityIoT+= penality;
		Overall_write_penality+= penality;
		Overall_penality+= penality;
	}
	
	
	//Stream
	public static void add_Overall_read_penalityStream(double penality){
		Overall_read_penalityStream+= penality;
		Overall_read_penality+= penality;
		Overall_penality+= penality;
	}


	public static void printallpenalitystats() {

		System.out.println("Overal Stream Penality:"+Overall_read_penalityStream);
		System.out.println();
		
		double Overall_penalityIoT = Overall_read_penalityIoT+Overall_write_penalityIoT;
		System.out.println("Overal IoT Penality:"+Overall_penalityIoT);
		System.out.println();
		
//		System.out.println("Overal Read Penality:"+Overall_read_penality);
//		System.out.println("Overal Write Penality:"+Overall_write_penality);
		System.out.println("Overal Penality:"+Overall_penality);
	}
	
	public static void reset_Penality_Stats(){
		//Stream
		Overall_read_penalityStream=0;
		
		//IoT
		Overall_read_penalityIoT=0;
		Overall_write_penalityIoT=0;
		
		
		//Overall
		Overall_penality= 0;
		Overall_write_penality= 0;
		Overall_read_penality= 0;
	}

	//Fonction de pénalité Stream
	public static double penalityStream(double latency) {
		int nb_packet = DataPlacement.size_streaming_data_transfert / DataPlacement.Basis_Exchange_Unit;
		
		if(latency< 50*nb_packet) {
			return 0;
		}
		if (latency<75*nb_packet){
			return 2;
		}
		if(latency<100*nb_packet) {
			return 4;
		}
		if (latency<125*nb_packet){
			return 6;
		}
		if(latency<150*nb_packet) {
			return 8;
		}
		return 10;
	}
	
	
	//Fonciton de pénalité IoT
	public static double penalityIoT(double latency) {
		int nb_packet = DataPlacement.HGW_TUPLE_FILE_SIZE / DataPlacement.Basis_Exchange_Unit;
		
		if(latency< 50*nb_packet) {
			return 0;
		}
		if (latency<75*nb_packet){
			return 2;
		}
		if(latency<100*nb_packet) {
			return 4;
		}
		if (latency<125*nb_packet){
			return 6;
		}
		if(latency<150*nb_packet) {
			return 8;
		}
		return 10;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////
	public static void add_penality_per_consumer(int consumer, String tupleType, double penality) {
		Pair<Integer,String> pair= new Pair<Integer, String>(consumer,tupleType);
		if(penalityPerConsumer.containsKey(pair)) {
			penality+=penalityPerConsumer.get(pair);
		}
		penalityPerConsumer.put(pair, penality);
		
	}
	public static void show_penality_per_consumer() {
		System.out.println("show penalityPerConsumer");
		for (Map.Entry<Pair<Integer,String >, Double> entry : penalityPerConsumer.entrySet()) {
			System.out.println(entry.getKey() + ":"+entry.getValue().toString());
		}
	}
	
	

}
