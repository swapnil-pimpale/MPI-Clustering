/**
 * Driver: This class is the starting point of the application
 * It performs the following tasks:
 * - Creates new Driver object
 * - parses command line
 * - Based on the DataType (point / DNA strand) reads 2d points or DNA strands
 * - Based on the mode (sequential / parallel) calls appropriate routines
 * - Prints the final list of k centroids
 */
import java.io.BufferedReader;
import mpi.*;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class Driver {

	private int k;
	private Mode mode;
	private DataType dataType;
	private List<Point> points;
	private List<DNAStrand> dnaStrands;
	private static final long startTime = System.nanoTime();
	
	public static void main(String[] args) throws MPIException {
		
		Driver driver = new Driver();
		driver.parseCommandLineArgs(args);

		if (driver.getDataType() == DataType.point) {
			driver.read2DPoints();
			List<Point> finalCentroids = null;
			if (driver.getMode() == Mode.sequential) {
				PointSequentialClustering psc = new PointSequentialClustering(driver.getK(), driver.getPoints());

				finalCentroids = psc.performClustering();

				driver.printPointCentroids(finalCentroids);

			} else if (driver.getMode() == Mode.parallel) {
				PointParallelClustering ppc = new PointParallelClustering(args, driver.getK(), driver.getPoints());

				finalCentroids = ppc.performClustering();

				driver.printPointCentroids(finalCentroids);

			} else {
				//error
				System.err.println("Invalid mode");
			}
		} else if (driver.getDataType() == DataType.dna) {
			driver.readDNAStrands();
			List<DNAStrand> finalCentroids = null;
			if (driver.getMode() == Mode.sequential) {
				DNASequentialClustering dsc = new DNASequentialClustering(driver.getK(), driver.getDNAStrands());

				finalCentroids = dsc.performClustering();

				driver.printDNACentroids(finalCentroids);

			} else if (driver.getMode() == Mode.parallel) {
				DNAParallelClustering dpc = new DNAParallelClustering(args, driver.getK(), driver.getDNAStrands());

				finalCentroids = dpc.performClustering();

				driver.printDNACentroids(finalCentroids);

			} else {
				//error
				System.err.println("Invalid mode");
			}
		} else {
			//error
			System.err.println("Invalid data type");
		}
	}

        /**
         * printDNACentroids:
         * Print final DNA centroids
         * @param finalCentroids
         */
	private void printDNACentroids(List<DNAStrand> finalCentroids) {
		if(finalCentroids!=null) {
			System.out.println("The final cluster centroids: ");
			for(DNAStrand dna : finalCentroids) {
				if(dna!=null) {
					System.out.println(dna);
				}
			}
			long endTime = System.nanoTime();
			long timeTaken = endTime-startTime;
			System.out.println("Time taken to find cluster centroids " +(timeTaken)+ " nanoseconds, or "
					+( ((double) timeTaken)/1000000000.0 )+" seconds" );
		}
	}

        /**
         * printPointCentroids:
         * Print final 2D centroids
         * @param finalCentroids
         */
	private void printPointCentroids(List<Point> finalCentroids) {
		if(finalCentroids!=null) {
			System.out.println("The final cluster centroids: ");
			for(Point p : finalCentroids) {
				if(p!=null) {
					System.out.println(p);
				}
			}
			long endTime = System.nanoTime();
			long timeTaken = endTime-startTime;
			System.out.println("Time taken to find cluster centroids " +(timeTaken)+ " nanoseconds, or "
					+( ((double) timeTaken)/1000000000.0 )+" seconds" );
		}
	}

        /**
         * readDNAStrands:
         * read from the CSV file
         */
	private void readDNAStrands() {
		try {
			BufferedReader br = new BufferedReader(new FileReader("DNA_DataGenerator/cluster.csv"));
			String line;
			dnaStrands = new ArrayList<DNAStrand>();
			
			while ((line = br.readLine()) != null) {
				DNAStrand strand = new DNAStrand(line);
				dnaStrands.add(strand);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

        /**
         * read2DPoints:
         * read from the CSV file
         */
	private void read2DPoints() {
		try {
			BufferedReader br = new BufferedReader(new FileReader("2D_DataGenerator/cluster.csv"));
			String line;
			points = new ArrayList<Point>();
			
			while ((line = br.readLine()) != null) {
				Point p = new Point(line);
				points.add(p);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Mode getMode() {
		return mode;
	}
	
	private DataType getDataType() {
		return dataType;
	}

	private int getK() {
		return k;
	}

        /**
         * parseCommandLineArgs:
         * The command line looks like this
         * mpirun -np x -machinefile machines.txt java -cp $CLASSPATH Driver k mode DataType
         * @param args
         */
	private void parseCommandLineArgs(String[] args) {
                /* number of clusters */
		k = Integer.parseInt(args[0]);
                /* Mode: Sequential / Parallel */
		mode = Mode.valueOf(args[1]);
                /* DataType: 2D / DNA */
		dataType = DataType.valueOf(args[2]);
	}
	
	private List<Point> getPoints() {
		return points;
	}
	
	private List<DNAStrand> getDNAStrands() {
		return dnaStrands;
	}

}
