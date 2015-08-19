import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class DNAStrandGenerator {
	
	public static void main(String[] args) {
		
		int pointsPerCluster = Integer.parseInt(args[0]);
		int numberOfClusters = Integer.parseInt(args[1]);
		int lengthOfDNAStrand = Integer.parseInt(args[2]);
		int thresholdDistanceBetweenBaseStrands = lengthOfDNAStrand/2;
		int thresholdDistanceBetweenStrandAndBaseStrand = thresholdDistanceBetweenBaseStrands/2;
		String fileName = "cluster.csv";
		
		List<DNAStrand> allBaseStrands = new ArrayList<DNAStrand>();
		List<DNAStrandCluster> allStrandClusters = new ArrayList<DNAStrandCluster>();
		
		for(int i=0;i<numberOfClusters;i++) {
			
			DNAStrandCluster dnaStrandCluster = new DNAStrandCluster(pointsPerCluster, lengthOfDNAStrand);
			dnaStrandCluster.generateRandomBaseStrand();
			DNAStrand baseStrand = dnaStrandCluster.getBaseStrand();
			
			while(baseStrand.minDistanceFromOtherBaseStrands(allBaseStrands) < thresholdDistanceBetweenBaseStrands) {
				dnaStrandCluster.generateRandomBaseStrand();
				baseStrand = dnaStrandCluster.getBaseStrand();
			}
			
			allBaseStrands.add(baseStrand);			
			allStrandClusters.add(dnaStrandCluster);
		}
		
		/*System.out.println("Threshold distance between base strands: "+thresholdDistanceBetweenBaseStrands);
		System.out.println("Threshold distance between a strand in a cluster and it's base strand"
				+ ": "+thresholdDistanceBetweenStrandAndBaseStrand);
		System.out.println("Please note that the base strand is always strand 1.");
		*/
		
		for(DNAStrandCluster strandCluster : allStrandClusters) {
			strandCluster.generateDNAStrandsForCluster(thresholdDistanceBetweenStrandAndBaseStrand);
			//strandCluster.printStrandsInCluster();	
			System.out.println("Generates strands for a single cluster");
		}
		
		File f = new File(fileName);
		if(f.exists()) {
			f.delete();
		}
		
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(fileName, true));
			//for(DNAStrandCluster strandCluster : allStrandClusters) {
			for (int i = 0; i < allStrandClusters.size(); i++) {
				DNAStrandCluster strandCluster = allStrandClusters.get(i);
				strandCluster.writeToFile(bw);
				if(i<allStrandClusters.size()-1) {
					bw.newLine();
				}
			}
			bw.close();
			System.out.println("Finished writing DNA strands to file.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	

	}

}
