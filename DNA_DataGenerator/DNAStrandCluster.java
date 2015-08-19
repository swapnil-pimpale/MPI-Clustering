/**
 * DNAStrandCluster:
 * This class represents a single DNA Strand cluster
 */
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class DNAStrandCluster {

        /* List of strands belonging to this cluster */
	private List<DNAStrand> strands;
        /* The base strand / centroid of this cluster */
	private DNAStrand baseStrand;
        /* number of points per cluster */
	private int pointsPerCluster;
        /* length of a DNA strand */
	private int lengthOfDNAStrand;
	
	public DNAStrandCluster(int pointsPerCluster, int lengthOfDNAStrand) {
		strands = new ArrayList<DNAStrand>();
		this.pointsPerCluster = pointsPerCluster;
		this.lengthOfDNAStrand = lengthOfDNAStrand;
	}

        /* generate a random base strand */
	public void generateRandomBaseStrand() {
		baseStrand = new DNAStrand(lengthOfDNAStrand);
	}

        /**
         * generateDNAStrandsForCluster:
         * @param thresholdDistanceBetweenStrandAndBaseStrand
         */
	public void generateDNAStrandsForCluster(int thresholdDistanceBetweenStrandAndBaseStrand) {
		strands.add(baseStrand);
		for(int i = 1; i < pointsPerCluster; i++) {
			generateOneDNAStrandForCluster(thresholdDistanceBetweenStrandAndBaseStrand);
		}
	}

        /**
         * generateOneDNAStrandForCluster:
         * generate a single strand based on base strand and thresholdDistanceBetweenStrandAndBaseStrand
         * @param thresholdDistanceBetweenStrandAndBaseStrand distance between a base strand
         * and any thread should always be less than this value. We do this to generate cluster strands
         * close to one base strand and away from the other base strands
         */
	public void generateOneDNAStrandForCluster(int thresholdDistanceBetweenStrandAndBaseStrand) {
		
		DNAStrand clusterStrand = new DNAStrand(baseStrand, thresholdDistanceBetweenStrandAndBaseStrand);
		
		while(strands.contains(clusterStrand)) {
			clusterStrand = new DNAStrand(baseStrand, thresholdDistanceBetweenStrandAndBaseStrand);
		}
		
		strands.add(clusterStrand);
	}
	
	public DNAStrand getBaseStrand() {
		return baseStrand;
	}

	public void printStrandsInCluster() {
		int i = 1;
		int maxDist = 0;
		System.out.println("================Start of Cluster============");
		for(DNAStrand strand : strands) {
			System.out.print("Strand "+i+" : ");
			strand.printDNAStrand();
			i++;
		}
		
		for(int k = 0;k < strands.size(); k++) {
			
			for(int j = k+1; j < strands.size(); j++) {
				System.out.println("Distance between strand "+(k+1)+" and strand "+(j+1)+ " is "
						+strands.get(k).distance(strands.get(j)));
				if(strands.get(k).distance(strands.get(j)) > maxDist) {
					maxDist = strands.get(k).distance(strands.get(j));
				}
			}
		}
		
		System.out.println("Max distance within a cluster is "+ maxDist);
		
		System.out.println("================End of Cluster============");
	}

	public void writeToFile(BufferedWriter bw) {
		for(int i = 0; i < strands.size(); i++) {
			try {
				DNAStrand strand = strands.get(i);
				bw.write(strand.toString());
				if(i<strands.size() - 1) {
					bw.newLine();
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	
}
