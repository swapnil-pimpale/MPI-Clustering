/**
 * DNAStrand:
 * This class represents a single DNA Strand
 */
import java.util.Arrays;
import java.util.List;
import java.util.Random;


public class DNAStrand implements Cloneable {

        /* A DNA Strand is an array of DNA bases */
	private DNABase[] DNA_Array;
	private DNABase[] possibleDNABases = DNABase.values();

        /**
         * DNAStrand: constructor
         * To construct the base strand
         * @param lengthOfDNAStrand
         */
	public DNAStrand(int lengthOfDNAStrand) {
		DNA_Array = new DNABase[lengthOfDNAStrand];
		Random rand = new Random();
		
		for(int i=0;i<DNA_Array.length;i++) {
			int nextIndex = rand.nextInt(possibleDNABases.length);
			DNA_Array[i] = possibleDNABases[nextIndex];
		}
		
	}

        /**
         * DNAStrand: Constructor
         * to construct other DNA strands in the cluster based on the base strand and
         * thresholdDistanceBetweenStrandAndBaseStrand
         * @param baseStrand
         * @param thresholdDistanceBetweenStrandAndBaseStrand: distance between a base strand
         * and any thread should always be less than this value. We do this to generate well
         * laid out DNA strands
         */
	public DNAStrand(DNAStrand baseStrand, int thresholdDistanceBetweenStrandAndBaseStrand) {
		DNA_Array = baseStrand.getDNA_Array().clone();
		Random rand = new Random();
		
		for(int i=1;i<thresholdDistanceBetweenStrandAndBaseStrand;i++) {
			int nextIndex = rand.nextInt(DNA_Array.length);
			DNABase value = DNA_Array[nextIndex];
			Random randEnum = new Random();
			int randEnumIndex = randEnum.nextInt(possibleDNABases.length);
			DNABase differentValue = possibleDNABases[randEnumIndex];
			
			while(differentValue==value) {
				randEnumIndex = randEnum.nextInt(possibleDNABases.length);
				differentValue = possibleDNABases[randEnumIndex];
			}
			
			DNA_Array[nextIndex] = differentValue;
			
		}
		
	}

        /**
         * DNAStrand: Constructor
         * construct a DNA strand from a string
         * @param str
         */
	public DNAStrand(String str) {
		String[] arr = str.split(",");
		DNA_Array = new DNABase[arr.length];
		
		for (int i = 0; i < arr.length; i++) {
			DNA_Array[i] = DNABase.valueOf(arr[i]);
		}
	}

	public DNAStrand(DNABase[] centroidArray) {
		DNA_Array = new DNABase[centroidArray.length];
		
		for(int i=0;i<DNA_Array.length;i++) {
			DNA_Array[i] = centroidArray[i];
		}
	}

        /**
         * minDistanceFromOtherBaseStrands:
         * Calculate the mininum distance of this strand from all the base strands
         * @param allBaseStrands
         * @return
         */
	public int minDistanceFromOtherBaseStrands(List<DNAStrand> allBaseStrands) {
		
		int min_Distance = Integer.MAX_VALUE;
		
		for(DNAStrand otherBaseStrand : allBaseStrands) {
			int distance = distance(otherBaseStrand);
			
			if(distance < min_Distance) {
				min_Distance = distance;
			}
		}
		
		return min_Distance;
	}

        /**
         * distance:
         * calculate the distance of this strand from otherStrand
         * @param otherStrand
         * @return
         */
	public int distance(DNAStrand otherStrand) {
		DNABase[] otherStrandArray = otherStrand.getDNA_Array();
		int distance = 0;
		
		for(int i=0;i<DNA_Array.length;i++) {
			if(DNA_Array[i] != otherStrandArray[i]) {
				distance++;
			}
		}
	
		return distance;
	}
	
	public DNABase[] getDNA_Array() {
		return DNA_Array;
	}

	public void printDNAStrand() {
		for(int i=0;i<DNA_Array.length;i++) {
			System.out.print(DNA_Array[i] + ",");
		}
		System.out.println();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof DNAStrand)) {
			return false;
		}
		
		DNAStrand otherStrand = (DNAStrand) obj;
		
		for(int i=0;i<DNA_Array.length;i++) {
			if(DNA_Array[i]!=otherStrand.getDNA_Array()[i]) {
				return false;
			}
		}
		
		return true;
		
	}
	
	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * Arrays.hashCode(DNA_Array);
		result = 31 * Arrays.hashCode(possibleDNABases);		
		return result;
	}
	
	@Override
	public String toString() {
		String dna = "";
		for(int i=0;i<DNA_Array.length;i++) {
			dna += DNA_Array[i];
			if(i<DNA_Array.length-1) {
				dna += ",";
			}
		}
		return dna;
	}
	
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
}
