package org.fog2.entities;


//Java program to implement LRU cache
//using LinkedHashSet
import java.util.*;

import org.fog.application.Application;
import org.fog.cplex.DataAllocation;
import org.fog.examples.DataPlacement;
import org.fog.placement.ModuleMapping;
import org.fog.stats.Stats;
import org.fog2.entities.*;
import org.cloudbus.cloudsim.Log;
import org.fog.utils.Logger;

public class FogCache {

	private Set<String> cache;
	double capacity;//quantité de donnée en MO, GO
	private int size;//Nombre de données dans le cache ex : 10
	private String type;//SSD, HDD, NVME
	public int nodeId = -1;
	public Map<String, Tuple > cachedata= new HashMap<String, Tuple >();
	
	public static String fogName;
	
	public FogCache(double capacity, String fogName)
	{
		this.cache = new LinkedHashSet<String>();
		this.capacity = capacity;
		this.fogName=fogName;


	}

	/* Refers key x with in the LRU cache */
	public void retreive(String tupleType)
	{	 
		////**System.out.println(ModuleMapping.getFogDevNameById(nodeId)+"Retriving Tuple...");
		//Log.writeInLogFile(ModuleMapping.getFogDevNameById(nodeId),"Retriving Tuple...");
		
		//**System.out.println("process retreive in cache class for :"+tupleType);
		
		if (this.cachedata.containsKey(tupleType)) {//donnée dans cache
			//**System.out.println("data in cache to retrieve");
			Tuple tuple = this.cachedata.get(tupleType); //send to consommateur with events
			
			if (cache.contains(tupleType) == false)
			{
				System.out.println("erreur, cache retreive!");
				System.exit(0);
			}
			//change order of data
			//**System.out.println("old order : cache"+cache.toString());
			cache.remove(tupleType);
			cache.add(tupleType);
			//**System.out.println("new order : cache"+cache.toString());
			
		}
		else {//data not in cache
			System.out.println("error, data doesn't exist in cache");
			System.exit(0);
		}
		
	}
	
	private static void incremente_nb_overflow() {
		
		if(fogName.contains("DC")) {
			int nb_overf =FogBroker.nb_overflow_perdevname.get("DC")+1;
			FogBroker.nb_overflow_perdevname .put("DC",nb_overf);
			
		}else if(fogName.contains("RPOP")) {
			int nb_overf =FogBroker.nb_overflow_perdevname.get("RPOP")+1;
			FogBroker.nb_overflow_perdevname .put("RPOP",nb_overf);
			
		}else if(fogName.contains("LPOP")) {
			int nb_overf =FogBroker.nb_overflow_perdevname.get("LPOP")+1;
			FogBroker.nb_overflow_perdevname .put("LPOP",nb_overf);
			
		}else if(fogName.contains("HGW")) {
			int nb_overf =FogBroker.nb_overflow_perdevname.get("HGW")+1;
			FogBroker.nb_overflow_perdevname .put("HGW",nb_overf);
			
		}else {
			System.out.println("nb _ overflow: node name donesn't exist");
			System.exit(0);
		}
	}
	
	public double store(Tuple tuple, int nodeId, double fogNodeOverflowFreeCapacity, double fogNodeOverflowCapacity)
	{	 
		if (DataPlacement.use_overflow) {
			// Log.writeInLogFile(ModuleMapping.getFogDevNameById(nodeId),"cache storing");
			//System.out.println("Process storing in cache");
			// **System.out.println("node "+nodeId+" process store in cache class
			// for:"+tuple.getTupleType());
			// **System.out.println("Stream size:"+tuple.getCloudletFileSize());

			if (this.cachedata.containsKey(tuple.getTupleType())) {// donnée dans cache
				// **System.out.println("donnée existante");

				// **System.out.println("old order : cache "+nodeId +" = "+cache.toString());
				cache.remove(tuple.getTupleType());
				cache.add(tuple.getTupleType());
				// **System.out.println("new order : cache"+nodeId +" = "+cache.toString());

				// **System.out.println("press enter");
				// **Scanner sc = new Scanner(System.in);
				// **String str = sc.nextLine();

				return fogNodeOverflowFreeCapacity;

			} else {// data not in cache (dh)
					// **System.out.println("donnée inexistante");

				double cache_used_capacity = getCacheUsedCapacity();
				// **System.out.println("cache_used_capacity:"+cache_used_capacity);

				double cache_free_capacity = capacity - cache_used_capacity;
				// **System.out.println("cache_free_capacity = "+cache_free_capacity);

				if (tuple.getCloudletFileSize() > capacity + fogNodeOverflowCapacity) {
					// impossible à stocker la donnée, donnée volumineuse
					// **System.out.println("donnée impossible à stocker, trop large");

					// **System.out.println("press enter");
					// **Scanner sc = new Scanner(System.in);
					// **String str = sc.nextLine();
					return fogNodeOverflowFreeCapacity;
				}

				if (tuple.getCloudletFileSize() <= cache_free_capacity) {
					// cache est suffisant
					// **System.out.println("donnée inexistante, cache suffisant");

					// **System.out.println("modifiying cache order");
					cache.add(tuple.getTupleType());
					// **System.out.println("new cache order " + nodeId + " = " + cache.toString());

					// **System.out.println("adding data to storerd cachedata");
					this.cachedata.put(tuple.getTupleType(), tuple);

					// **System.out.println("adding node id to emplacement list in data allocation
					// cache map");
					DataAllocation.processCacheAdd(nodeId, tuple.getTupleType());

					// **System.out.println("press enter");
					// **Scanner sc = new Scanner(System.in);
					// **String str = sc.nextLine();
					return fogNodeOverflowFreeCapacity;

				}

				// cache est insuffisant
				if (cache_free_capacity < 0) {
					// si cache completement rempli + overflow existant
					// **System.out.println("Overflow : cache_used_capacity > capacity");

					cache_free_capacity = 0;
					// **System.out.println("cache_free_capacity:" + cache_free_capacity);
				}

				// overflow possible
				if (tuple.getCloudletFileSize() <= (fogNodeOverflowFreeCapacity + cache_free_capacity)) {
					// donnée inexistante, cache insuffisant, overflow possible
					//System.out.println("donnée inexistante, cache insuffisant, overflow");
					// possible");

					// **System.out.println("modifiying cache order");
					Stats.nb_overflow++;
					incremente_nb_overflow();
					
					cache.add(tuple.getTupleType());
					// **System.out.println("new cache order " + nodeId + " = " + cache.toString());

					// **System.out.println("adding data to storerd cachedata");
					this.cachedata.put(tuple.getTupleType(), tuple);

					// **System.out.println("adding node id to emplacement list in data allocation
					// cache map");
					DataAllocation.processCacheAdd(nodeId, tuple.getTupleType());

					// **System.out.println("press enter");
					// **Scanner sc = new Scanner(System.in);
					// **String str = sc.nextLine();

					double free = fogNodeOverflowFreeCapacity + cache_free_capacity - tuple.getCloudletFileSize();
					
					// **System.out.println("Free storage capacity:"+free);
					return free;

				} else {
					// overflow impossible ==> LRU
					double restFree = fogNodeOverflowFreeCapacity;

					// **System.out.println("donnée inexistante, cache insuffisant, overflow
					// impossible, LRU");
					String lru;

					// DataAllocation.printCachePlacementMap();

					do {
						// **System.out.println("Apply LRU");
						lru = cache.iterator().next();
						// **System.out.println("old order cache " + nodeId + " = " + cache.toString());
						restFree += cachedata.get(lru).getCloudletFileSize();
						// **System.out.println("remove LRU:" + lru + " from cache, cachedata and
						// cacheemplacement map");
						cache.remove(lru);
						cachedata.remove(lru);
						DataAllocation.processCacheRemove(nodeId, lru);

					} while (restFree < tuple.getCloudletFileSize());

					// **System.out.println("modifiying cache order");
					cache.add(tuple.getTupleType());
					// **System.out.println("new cache order " + nodeId + " = " + cache.toString());

					// **System.out.println("adding data to storerd cachedata");
					this.cachedata.put(tuple.getTupleType(), tuple);

					// **System.out.println("adding node id to emplacement list in data allocation
					// cache map");
					DataAllocation.processCacheAdd(nodeId, tuple.getTupleType());

					// DataAllocation.printCachePlacementMap();

					// mise à jour des capacities
					// LRU just for overflow
					if (tuple.getCloudletFileSize() <= fogNodeOverflowCapacity) {
						// **System.out.println("overflow");
						// **System.out.println("press enter");
						// **Scanner sc = new Scanner(System.in);
						// **String str = sc.nextLine();
						double free = restFree - tuple.getCloudletFileSize();
						Stats.nb_overflow++;
						incremente_nb_overflow();
						// **System.out.println("Free storage capacity:"+free);
						return restFree - tuple.getCloudletFileSize();
					}

					// LRU Overflow + cache
					// **System.out.println("press enter");
					// **Scanner sc = new Scanner(System.in);
					// **String str = sc.nextLine();
					cache_used_capacity = getCacheUsedCapacity();
					cache_free_capacity = capacity - cache_used_capacity;
					if(cache_free_capacity < 0) {
						Stats.nb_overflow++;
						incremente_nb_overflow();
						
					}
					double free = fogNodeOverflowCapacity;
					// **System.out.println("Free storage capacity:"+free);
					return fogNodeOverflowCapacity;

				}
			}
		}else {//overflow désactivé
			
			// Log.writeInLogFile(ModuleMapping.getFogDevNameById(nodeId),"cache storing");
			// **System.out.println("Process storing in cache");
			// **System.out.println("node "+nodeId+" process store in cache class
			// for:"+tuple.getTupleType());
			// **System.out.println("Stream size:"+tuple.getCloudletFileSize());

			if (this.cachedata.containsKey(tuple.getTupleType())) {// donnée dans cache
				// **System.out.println("donnée existante");

				// **System.out.println("old order : cache "+nodeId +" = "+cache.toString());
				cache.remove(tuple.getTupleType());
				cache.add(tuple.getTupleType());
				// **System.out.println("new order : cache"+nodeId +" = "+cache.toString());

				// **System.out.println("press enter");
				// **Scanner sc = new Scanner(System.in);
				// **String str = sc.nextLine();

				return fogNodeOverflowFreeCapacity;

			} else {// data not in cache (dh)
					// **System.out.println("donnée inexistante");

				double cache_used_capacity = getCacheUsedCapacity();
				// **System.out.println("cache_used_capacity:"+cache_used_capacity);

				double cache_free_capacity = capacity - cache_used_capacity;
				// **System.out.println("cache_free_capacity = "+cache_free_capacity);

				if (tuple.getCloudletFileSize() > capacity) {
					// impossible à stocker la donnée, donnée volumineuse
					//System.out.println("donnée impossible à stocker, trop large");

					// **System.out.println("press enter");
					// **Scanner sc = new Scanner(System.in);
					// **String str = sc.nextLine();
					return fogNodeOverflowFreeCapacity;
				}

				if (tuple.getCloudletFileSize() <= cache_free_capacity) {
					// cache est suffisant
					// **System.out.println("donnée inexistante, cache suffisant");

					// **System.out.println("modifiying cache order");
					cache.add(tuple.getTupleType());
					// **System.out.println("new cache order " + nodeId + " = " + cache.toString());

					// **System.out.println("adding data to storerd cachedata");
					this.cachedata.put(tuple.getTupleType(), tuple);

					// **System.out.println("adding node id to emplacement list in data allocation
					// cache map");
					DataAllocation.processCacheAdd(nodeId, tuple.getTupleType());

					// **System.out.println("press enter");
					// **Scanner sc = new Scanner(System.in);
					// **String str = sc.nextLine();
					return fogNodeOverflowFreeCapacity;
				}

				// cache est insuffisant => LRU
				// **System.out.println("donnée inexistante, cache insuffisant,
				// LRU");
				String lru;
				// DataAllocation.printCachePlacementMap();
				do {
					// **System.out.println("Apply LRU");
					lru = cache.iterator().next();
					// **System.out.println("old order cache " + nodeId + " = " + cache.toString());
					cache_free_capacity += cachedata.get(lru).getCloudletFileSize();
					// **System.out.println("remove LRU:" + lru + " from cache, cachedata and
					// cacheemplacement map");
					cache.remove(lru);
					cachedata.remove(lru);
					DataAllocation.processCacheRemove(nodeId, lru);

				} while (cache_free_capacity < tuple.getCloudletFileSize());

				// **System.out.println("modifiying cache order");
				cache.add(tuple.getTupleType());
				// **System.out.println("new cache order " + nodeId + " = " + cache.toString());

				// **System.out.println("adding data to storerd cachedata");
				this.cachedata.put(tuple.getTupleType(), tuple);

				// **System.out.println("adding node id to emplacement list in data allocation
				// cache map");
				DataAllocation.processCacheAdd(nodeId, tuple.getTupleType());

				// DataAllocation.printCachePlacementMap();
				return fogNodeOverflowFreeCapacity;

			}
		}
	}
	
	public double LruForIoTDataStorage(Tuple tuple, int nodeId, double free_capacity)
	{	 
		String lru;
		
		//DataAllocation.printCachePlacementMap();
		//**System.out.println("Free_capacity = "+free_capacity);

		do {
			//**System.out.println("Applying LRU for IoT Storage");
			
			lru = cache.iterator().next();
			//**System.out.println("old order cache " + nodeId + " = " + cache.toString());
			free_capacity += cachedata.get(lru).getCloudletFileSize();
			//**System.out.println("remove LRU:" + lru + " from cache, cachedata and cacheemplacement map");
			cache.remove(lru);
			cachedata.remove(lru);
			DataAllocation.processCacheRemove(nodeId, lru);
			//**System.out.println("New order cache " + nodeId + " = " + cache.toString());
			//**System.out.println("Free_capacity = "+free_capacity);

		} while (free_capacity < tuple.getCloudletFileSize());
		
		return free_capacity;
	}
	
	public double getCacheUsedCapacity() {
		// TODO Auto-generated method stub
		double capacity = 0;
		
		for (Tuple tuple : cachedata.values()) {
			capacity += tuple.getCloudletFileSize();
		}
		
		return capacity;
	}

	// displays contents of cache in Reverse Order
	public void display()
	{
	LinkedList<String> list = new LinkedList<>(cache);
	
	// The descendingIterator() method of java.util.LinkedList
	// class is used to return an iterator over the elements
	// in this LinkedList in reverse sequential order
	Iterator<String> itr = list.descendingIterator(); 
	
	while (itr.hasNext())
			System.out.print(itr.next() + " ");
	}
	
	public void put(String key)
	{
		
	if (cache.size() == size) {
			String firstKey = cache.iterator().next();
			cache.remove(firstKey);
			DataAllocation.processCacheRemove(this.nodeId, this.type);

		}

		cache.add(key);
		DataAllocation.processCacheAdd(this.nodeId, this.type);
		

		
	}

	public void printAllStoredStreams() {
		// TODO Auto-generated method stub
		//**System.out.println("printAllStoredStreams");
		//**System.out.println(cache.toString());
	}
	
}
