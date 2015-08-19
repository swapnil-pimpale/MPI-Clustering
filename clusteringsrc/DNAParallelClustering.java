/**
 * DNAParallelClustering: Implements the logic for the parallel version of K-means
 * algorithm on DNA strands
 */
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import mpi.*;

public class DNAParallelClustering {

        /* MPI message tag used when sending over centroid locations to participants */
	private static final int CENTROID_TAG = 1;
        /* MPI message tag used when sending over a portion of DNA strands to participants */
	private static final int DNA_PORTION_TAG = 2;
        /* MPI message tag used when sending over the computation result from participants to Process-0 */
	private static final int RESULT_TAG = 3;
        /* MPI message tag used when informing the participants about completion of the task */
	private static final int COMPLETED_TAG = 4;
	
	private static final int MAX_ITERATIONS = 100;

        /* number of clusters */
	private int k;
        /* list of DNA strands */
	private List<DNAStrand> strands = null;
        /* current list of centroids */
        private List<DNAStrand> centroids = null;
        private List<DNAStrand> strandPortionList = null;
        /* Mapping between a cluster's centroid DNAStrand and the DNAStrands belonging to that cluster */
        private HashMap<DNAStrand, List<DNAStrand>> finalMap = null;
        /* number of processes running in the MPI environment */
	private int numberOfProcesses = 0;
        /* rank of the current process */
	private int myRank;
        /* splitSize = how many DNAStrands does each process get for computation */
	private int splitSize;
	
        /**
         * Constructor that initializes the parallel clustering data structures
         * @param args
         * @param k
         * @param dnaStrands
         * @throws MPIException
         */
	public DNAParallelClustering(String[] args, int k, List<DNAStrand> dnaStrands) throws MPIException {
		this.k = k;		
		initialize(args, dnaStrands);
                /*
                splitSize = (total number of DNA strands) / (number of processes - 1)
                In our design, the Process-0 acts like the master process and assigns computation work
                to other processes. Hence, (numberOfProcesses - 1)
                 */
		splitSize = (int) Math.ceil( ((double) dnaStrands.size()) / ((double) (numberOfProcesses - 1)) );
	}

        /**
         * initialize: get the number of processes in the MPI environment and get current rank.
         * Calculate initial centroid locations.
         * @param args
         * @param dnaStrands
         * @throws MPIException
         */
	private void initialize(String[] args, List<DNAStrand> dnaStrands) throws MPIException {
		MPI.Init(args);

                /* get number of processes */
		numberOfProcesses = MPI.COMM_WORLD.Size();
                /* get current rank */
		myRank = MPI.COMM_WORLD.Rank();
		
		if(myRank==0) {
			this.strands = dnaStrands; 
			createInitialCentroids();
		}
	}

        /**
         * createInitialCentroids:
         * Choose the initial k centroids randomly from within the available DNAStrand.
         */
	private void createInitialCentroids() {
		Random rand = new Random();
		centroids = new ArrayList<DNAStrand>();
		
		for(int i = 0; i < k; i++) {
			int index = rand.nextInt(strands.size());
			DNAStrand dna = (DNAStrand) strands.get(index).clone();
			
			while(centroids.contains(dna)) {
				index = rand.nextInt(strands.size());
				dna = (DNAStrand) strands.get(index).clone();
			}
			
			centroids.add(dna);			
		}
	}

        /**
         * performClustering:
         * Check the rank of the current process and accordingly perform master tasks or
         * participant tasks
         * @return: the final list of k centroids
         * @throws MPIException
         */
	public List<DNAStrand> performClustering() throws MPIException {
		
		long timeTaken;
		long endTime;
		long startTime;
		
		if(myRank==0) {
			/* This process is the master */
                        // set the max number of iterations to 10000
            int numberOfIterations = MAX_ITERATIONS;
			
			while(numberOfIterations > 0) {
                                /* send the centroids and DNAStrand portions to the participants */
				if(numberOfIterations==MAX_ITERATIONS) {
					sendPortionToAllParticipantProcesses();
				}
				sendCentroidsToAllParticipantProcesses();
				
                                /* receive intermedite/final results from the participants */
				receiveFromAllParticipantProcesses();
				                /* recalculate the centroid locations */
				centroids = recalculateCentroidLocations();
				                /* reset finalMap */
				finalMap = null;
                                /* decrement number of iterations */
				numberOfIterations--;
			}

                        /*
                        we are done with all the iterations. Send completion message to all the
                        participants
                         */
			for(int i = 1; i < numberOfProcesses; i++) {
				MPI.COMM_WORLD.Send(null, 0, 0, MPI.OBJECT, i, COMPLETED_TAG);
			}

                        /* Finalize MPI environment on the master */
			MPI.Finalize();
		}
		
		else {
			Object[] strandPortionArray = new Object[splitSize];
            /* receive portions */
			MPI.COMM_WORLD.Recv(strandPortionArray, 0, splitSize, MPI.OBJECT, 0, DNA_PORTION_TAG);
			strandPortionList = convertArrayToList(strandPortionArray);
			
            /* run in a while loop until Process-0 sends a completion message */
			boolean cont = true;			
			while(cont) {
				cont = receiveFromMasterProcess();
			}

                        /* Finalize MPI environment on the participant */
			MPI.Finalize();
		}

                /* return the final centroid list */
		return centroids;
	}

        /**
         * recalculateCentroidLocations:
         * recalculate centroid locations based on intermediate results from participant processes
         * @return
         */
	private List<DNAStrand> recalculateCentroidLocations() {

                /* Get all the DNAStrand lists corresponding to every centroid DNAStrand */
		Collection<List<DNAStrand>> lists = finalMap.values();
                /* create temporary centroids list */
		List<DNAStrand> tmpCentroids = new ArrayList<DNAStrand>();
                /* get the length of a single DNA */
		int strandSize = strands.get(0).getDNA_Array().length;

		for(List<DNAStrand> l_dna : lists) {
			
			if(l_dna.size()==0) {
				System.out.println("There exists a cluster centroid with no points"
						+ " assigned to it. You may end up with fewer clusters than expected.");
				continue; //cluster has no points
			}
			
			int strandIndex = 0;
			DNABase[] centroidArray = new DNABase[strandSize];
			while(strandIndex < strandSize) {
				
				HashMap<DNABase, Integer> baseCountMap = makeBaseCountMap();
				
				for(DNAStrand dna : l_dna) {
					
					DNABase base = dna.getDNA_Array()[strandIndex];
					int baseCount = baseCountMap.get(base);
					baseCount++;
					baseCountMap.put(base, baseCount);
					
				}
				
				DNABase maxCountBase = getMaxCountBase(baseCountMap);
				centroidArray[strandIndex] = maxCountBase;
				strandIndex++;
			}
			DNAStrand centroid = new DNAStrand(centroidArray);
			tmpCentroids.add(centroid);
		}
		
		return tmpCentroids;

	}
	
	private DNABase getMaxCountBase(HashMap<DNABase, Integer> baseCountMap) {
		int count = Integer.MIN_VALUE;
		DNABase maxCountBase = null;
		
		for(DNABase base : DNABase.values()) {
			
			if(baseCountMap.get(base) > count) {
				count = baseCountMap.get(base);
				maxCountBase = base;
			}
		}
		return maxCountBase;
	}

	private HashMap<DNABase, Integer> makeBaseCountMap() {
		
		HashMap<DNABase, Integer> baseCountMap = new HashMap<DNABase, Integer>();
		
		for(DNABase base : DNABase.values()) {
			baseCountMap.put(base, 0);
		}
		
		return baseCountMap;
	}

        /**
         * receiveFromAllParticipantProcesses:
         * get intermediate/final result from the participant processes and merge all the
         * results
         * @throws MPIException
         */
	private void receiveFromAllParticipantProcesses() throws MPIException {
		
		Iterator<DNAStrand> itr = strands.iterator();
		
		for(int idx = 1; idx < numberOfProcesses; idx++) {
                        /* receive the result in a single object */
			Object[] result = new Object[1];
			MPI.COMM_WORLD.Recv(result, 0, 1, MPI.OBJECT, idx, RESULT_TAG);
			List<Integer> centroidIndices = (List<Integer>) result[0];
			//mergeMap(map);
			itr = updateFinalMap(centroidIndices, itr);
		}
		
	}

    private Iterator<DNAStrand> updateFinalMap(List<Integer> centroidIndices, Iterator<DNAStrand> itr) {
    	if(finalMap==null) {
			finalMap = new HashMap<DNAStrand, List<DNAStrand>>();
			for(DNAStrand centroid : centroids) {
				finalMap.put(centroid, new ArrayList<DNAStrand>());
			}
		}
    	
    	for(Integer index : centroidIndices) {   		
    		DNAStrand dna = itr.next();
    		DNAStrand centroid = centroids.get(index);
    		finalMap.get(centroid).add(dna);
    	}
    	
    	return itr;
	}

		/**
         * sendToAllParticipantProcesses:
         * Send centroids and portion of DNAStrands to the participant nodes
         * @throws MPIException
         */
	private void sendCentroidsToAllParticipantProcesses() throws MPIException {

		for(int i=1;i<numberOfProcesses;i++) {
			Object[] centroidArray = {centroids};
			MPI.COMM_WORLD.Send(centroidArray, 0, centroidArray.length, MPI.OBJECT, i, CENTROID_TAG);
		}
		
	}

	private void sendPortionToAllParticipantProcesses() throws MPIException {
	
		//now split the DNAStrands and send
		Object[] dnaStrandsArray = strands.toArray();
		int i = 1;
		int offset = 0;
		int count = splitSize;
                /* perform the splitting of strands based on splitSize */
		while (offset + count <= dnaStrandsArray.length) {
			MPI.COMM_WORLD.Send(dnaStrandsArray, offset, count, MPI.OBJECT, i, DNA_PORTION_TAG);
			i++;
			offset = offset + count;
		}

                /* send strands which might be remaining from the last split */
		if (offset < dnaStrandsArray.length) {
			count = dnaStrandsArray.length - offset;
			MPI.COMM_WORLD.Send(dnaStrandsArray, offset, count, MPI.OBJECT, i, DNA_PORTION_TAG);
		}
		
	}
	
        /**
         * mergeMap:
         * add the strands received from the participants to the finalMap
         * @param map
         */
	private void mergeMap(HashMap<DNAStrand, List<DNAStrand>> map) {

                /* Create new finalMap if not already existing */
		if(finalMap==null) {
			finalMap = new HashMap<DNAStrand, List<DNAStrand>>();
			for(DNAStrand centroid : centroids) {
				finalMap.put(centroid, new ArrayList<DNAStrand>());
			}
		}
		
		for(DNAStrand centroid : centroids) {
			List<DNAStrand> strandsToBeAdded = map.get(centroid);
			if(strandsToBeAdded!=null) {
				finalMap.get(centroid).addAll(strandsToBeAdded);			
			}
		}

	}

        /**
         * receiveFromMasterProcess:
         * receive centroids / DNA Portions / Completed message from the Process-0
         * @return
         * @throws MPIException
         */
	private boolean receiveFromMasterProcess() throws MPIException {

		Status status;
		boolean cont = true;
		
		Object[] centroidArray = new Object[1];
		status = MPI.COMM_WORLD.Recv(centroidArray, 0, 1, MPI.OBJECT, 0, MPI.ANY_TAG);
		if (status.tag == COMPLETED_TAG) {
			cont = false;
			return cont;
		} else if (status.tag == CENTROID_TAG) {
                        /* store centroid list */
			List<DNAStrand> centroidList = (List<DNAStrand>) centroidArray[0];	        
                        
            /*HashMap<DNAStrand, List<DNAStrand>> centroidToStrand = makeCentroidToStrandsMap(centroidList,
                                                                    strandPortionList);*/
			List<Integer> centroidIndices = strandsToCentroidIndices(centroidList);
            Object[] resultObj = {centroidIndices};
            MPI.COMM_WORLD.Send(resultObj, 0, 1, MPI.OBJECT, 0, RESULT_TAG);
		}

		return cont;

	}

	private List<Integer> strandsToCentroidIndices(List<DNAStrand> centroidList) {
		
		List<Integer> list = new ArrayList<Integer>();
		
		for(DNAStrand dna : strandPortionList) {
			
			double minDistance = Double.MAX_VALUE;
			int centroidIndex = -1;
			
			for(int i=0;i<centroidList.size();i++) {
				
				DNAStrand centroid = centroidList.get(i);
				double distance = dna.distance(centroid);
				
				if(distance < minDistance) {
					minDistance = distance;
					centroidIndex = i;				
				}
				
			}
			
			list.add(centroidIndex);
			
		}
		
		return list;
	}

	private List<DNAStrand> convertArrayToList(Object[] strandPortionArray) {
		List<DNAStrand> list = new ArrayList<DNAStrand>();
		for(Object obj : strandPortionArray) {
			if(obj!=null) {
				list.add((DNAStrand) obj);
			}
		}
		return list;
	}

        /**
         * makeCentroidToStrandsMap:
         * Construct a hashmap between centroids and DNA Strands based on the distances between them
         * @param centroidList
         * @param strandPortionList
         * @return
         */
	private HashMap<DNAStrand, List<DNAStrand>> makeCentroidToStrandsMap(List<DNAStrand> centroidList, List<DNAStrand> strandPortionList) {

		HashMap<DNAStrand, List<DNAStrand>> centroidToStrands = new HashMap<DNAStrand, List<DNAStrand>>();
		
		for(DNAStrand centroid : centroidList) {
			centroidToStrands.put(centroid, new ArrayList<DNAStrand>());
		}
		
		for(DNAStrand dna : strandPortionList) {
			
			double minDistance = Double.MAX_VALUE;
			DNAStrand tmpCentroid = null;
			
			for(DNAStrand centroid : centroidList) {
				
				double distance = dna.distance(centroid);
				
				if(distance < minDistance) {
					minDistance = distance;
					tmpCentroid = centroid;					
				}
				
			}
			
			centroidToStrands.get(tmpCentroid).add(dna);
			
		}
		
		return centroidToStrands;

	}
	
}
