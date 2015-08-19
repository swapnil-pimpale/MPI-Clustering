/**
 * DNASequentialClustering:
 * Implements the logic for the sequential version of K-means
 * algorithm on DNA strands
 */
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;


public class DNASequentialClustering {

	private int k;
	private List<DNAStrand> strands;
	
	private static final int MAX_ITERATIONS = 100;
	
	public DNASequentialClustering(int k, List<DNAStrand> dnaStrands) {
		this.k = k;
		this.strands = dnaStrands;
	}

        /**
         * performClustering:
         * Step-1: Construct an initial list of centroids randomly.
         * Step-2: Construct a hashmap between centroids and other points in the cluster according to
         * the distance between them.
         * Step-3: Recalculate the centroid locations
         * Repeat Steps 2 and 3 for numberOfIterations
         * @return
         */
	public List<DNAStrand> performClustering() {
		List<DNAStrand> centroids = new ArrayList<DNAStrand>();
		HashMap<DNAStrand, List<DNAStrand>> centroidToStrands;
		Random rand = new Random();
		int numberOfIterations = MAX_ITERATIONS;
		
		for(int i = 0; i < k; i++) {
			int index = rand.nextInt(strands.size());
			DNAStrand dna = (DNAStrand) strands.get(index).clone();
			
			while(centroids.contains(dna)) {
				index = rand.nextInt(strands.size());
				dna = (DNAStrand) strands.get(index).clone();
			}
			
			centroids.add(dna);
		}
		
		while(numberOfIterations > 0) {
			
			centroidToStrands = makeCentroidToStrandsMap(centroids);
			
			for(DNAStrand dna : strands) {
				
				int minDistance = Integer.MAX_VALUE;
				DNAStrand tmpCentroid = null;
				
				for(DNAStrand centroid : centroids) {
					
					int distance = dna.distance(centroid);
					if(distance < minDistance) {
						minDistance = distance;
						tmpCentroid = centroid;					
					}
				}
				
				centroidToStrands.get(tmpCentroid).add(dna);
			}
			
			centroids = recalculateCentroidLocations(centroidToStrands);
			numberOfIterations--;
		}
		
		return centroids;
		
	}

        /**
         * recalculateCentroidLocations:
         *
         * @param centroidToPoints
         * @return
         */
	private List<DNAStrand> recalculateCentroidLocations(HashMap<DNAStrand, List<DNAStrand>> centroidToPoints) {
		
		Collection<List<DNAStrand>> lists = centroidToPoints.values();
		List<DNAStrand> centroids = new ArrayList<DNAStrand>();
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
			centroids.add(centroid);
		}
		
		return centroids;
	}
	
	public DNABase getMaxCountBase(HashMap<DNABase, Integer> baseCountMap) {
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

	public HashMap<DNABase, Integer> makeBaseCountMap() {
		HashMap<DNABase, Integer> baseCountMap = new HashMap<DNABase, Integer>();
		
		for(DNABase base : DNABase.values()) {
			baseCountMap.put(base, 0);
		}
		
		return baseCountMap;
	}

        /**
         * makeCentroidToStrandsMap:
         * Construct a hashmap between centroids and DNA Strands based on the distances between them
         * @param centroids
         * @return
         */
	public HashMap<DNAStrand, List<DNAStrand>> makeCentroidToStrandsMap(List<DNAStrand> centroids) {
		HashMap<DNAStrand, List<DNAStrand>> centroidToStrands = new HashMap<DNAStrand, List<DNAStrand>>();
		
		for(DNAStrand centroid : centroids) {
			centroidToStrands.put(centroid, new ArrayList<DNAStrand>());
		}
		
		return centroidToStrands;
	}

}
