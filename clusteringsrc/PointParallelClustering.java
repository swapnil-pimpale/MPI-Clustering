/**
 * PointParallelClustering:
 * Implements the logic for the parallel version of K-means
 * algorithm on 2D points
 */
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import mpi.*;


public class PointParallelClustering {

        /* MPI message tags */
	private static final int CENTROID_TAG = 1;
	private static final int POINT_PORTION_TAG = 2;
	private static final int RESULT_TAG = 3;
	private static final int COMPLETED_TAG = 4;
	
	private int k;
	private List<Point> points = null;
        private List<Point> centroids = null;
        private HashMap<Point, List<Point>> finalMap = null;
        private int numberOfProcesses = 0;
	private int myRank;
	private int splitSize;

        /**
         * Constructor that initializes the parallel clustering data structures
         */
	public PointParallelClustering(String[] args, int k, List<Point> points) throws MPIException {
		this.k = k;		
		initialize(args, points);
                /*
                splitSize = (total number of points) / (number of processes - 1)
                In our design, the Process-0 acts like the master process and assigns computation work
                to other processes. Hence, (numberOfProcesses - 1)
                 */
		splitSize = (int) Math.ceil( ((double) points.size()) / ((double) (numberOfProcesses - 1)) );
	}

        /**
         * initialize: get the number of processes in the MPI environment and get current rank.
         * Calculate initial centroid locations.
         */
	private void initialize(String[] args, List<Point> points) throws MPIException {
		MPI.Init(args);
		numberOfProcesses = MPI.COMM_WORLD.Size();
		myRank = MPI.COMM_WORLD.Rank();
		
		if(myRank==0) {
			this.points = points; 
			createInitialCentroids();
		}
	}

        /**
         * createInitialCentroids:
         * Choose the initial k centroids randomly from within the available 2D points.
         */
	private void createInitialCentroids() {
		
		Random rand = new Random();
		centroids = new ArrayList<Point>();
		
		for(int i = 0; i < k; i++) {
			int index = rand.nextInt(points.size());
			Point p = (Point) points.get(index).clone();
			
			while(centroids.contains(p)) {
				index = rand.nextInt(points.size());
				p = (Point) points.get(index).clone();
			}
			
			centroids.add(p);			
		}
	}

        /**
         * performClustering:
         * Check the rank of the current process and accordingly perform master tasks or
         * participant tasks
         * @return: the final list of k centroids
         * @throws MPIException
         */
	public List<Point> performClustering() throws MPIException {
		
		if(myRank==0) {
			/* This process is the master */
			List<Point> oldCentroids; 
			do {
				oldCentroids = new ArrayList<Point>(centroids);
                                /* send the centroids and DNAStrand portions to the participants */
				sendToAllParticipantProcesses();
                                /* receive intermedite/final results from the participants */
				receiveFromAllParticipantProcesses();
                                /* recalculate the centroid locations */
				centroids = recalculateCentroidLocations();
                                /* reset finalMap */
				finalMap = null;
				
			} while(!oldCentroids.containsAll(centroids));
			
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
			boolean cont = true;
			while(cont) {			
				cont = receiveFromMasterProcess();
			}

                        /* Finalize MPI environment on the master */
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
	private List<Point> recalculateCentroidLocations() {

                /* Get all the 2D points lists corresponding to every centroid */
		Collection<List<Point>> lists = finalMap.values();
                /* create temporary centroids list */
		List<Point> tmpCentroids = new ArrayList<Point>();

                /*
                For every centroid list calculate the mean of x and y coordinates of all
                 points in that list. This will be our new/recalculated centroid
                 */
		for(List<Point> lp : lists) {
			
			if(lp.size()==0) {
				System.out.println("There exists a cluster centroid with no points"
						+ " assigned to it. You may end up with fewer clusters than expected.");
				continue; //cluster has no points
			}
			
			double x = 0;
			double y = 0;
			
			for(Point p : lp) {
				x += p.getX();
				y += p.getY();
			}
			x = x/(double) lp.size();
			y = y/(double) lp.size();
			
			Point newCentroid = new Point(x, y);
			tmpCentroids.add(newCentroid);
		}
		
		return tmpCentroids;

	}

        /**
         * receiveFromAllParticipantProcesses:
         * get intermediate/final result from the participant processes and merge all the
         * results
         * @throws MPIException
         */
	private void receiveFromAllParticipantProcesses() throws MPIException {
		
		for(int idx=1;idx<numberOfProcesses;idx++) {	
			Object[] result = new Object[1];	
			MPI.COMM_WORLD.Recv(result, 0, 1, MPI.OBJECT, idx, RESULT_TAG);
			HashMap<Point, List<Point>> map = (HashMap<Point, List<Point>>) result[0];
			mergeMap(map);	
		}
		
	}

        /**
         * sendToAllParticipantProcesses:
         * Send centroids and portion of DNAStrands to the participant nodes
         * @throws MPIException
         */
	private void sendToAllParticipantProcesses() throws MPIException {
		
		for(int i=1;i<numberOfProcesses;i++) {
			Object[] centroidArray = {centroids};
			MPI.COMM_WORLD.Send(centroidArray, 0, centroidArray.length, MPI.OBJECT, i, CENTROID_TAG);
		}
		
		//now split the points and send
		Object[] pointsArray = points.toArray();
		int i = 1;
		int offset = 0;
		int count = splitSize;
                /* perform the splitting of points based on splitSize */
		while (offset + count <= pointsArray.length) {
			MPI.COMM_WORLD.Send(pointsArray, offset, count, MPI.OBJECT, i, POINT_PORTION_TAG);
			i++;
			offset = offset + count;
		}

                /* send points which might be remaining from the last split */
		if (offset < pointsArray.length) {
			count = pointsArray.length - offset;
			MPI.COMM_WORLD.Send(pointsArray, offset, count, MPI.OBJECT, i, POINT_PORTION_TAG);
		}
		
	}

        /**
         * mergeMap:
         * add the points received from the participants to the finalMap
         * @param map
         */
	private void mergeMap(HashMap<Point, List<Point>> map) {
		
		if(finalMap == null) {
			finalMap = new HashMap<Point, List<Point>>();
			for(Point centroid : centroids) {
				finalMap.put(centroid, new ArrayList<Point>());
			}
		}
		
		for(Point centroid : centroids) {
			List<Point> pointsToBeAdded = map.get(centroid);
			if(pointsToBeAdded != null) {
				finalMap.get(centroid).addAll(pointsToBeAdded);			
			}
		}

	}

        /**
         * receiveFromMasterProcess:
         * receive centroids / points Portions / Completed message from the Process-0
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
			List<Point> centroidList = (List<Point>) centroidArray[0];
	                Object[] pointsArray = new Object[splitSize];
                        /* receive portions */
	                status = MPI.COMM_WORLD.Recv(pointsArray, 0, splitSize, MPI.OBJECT, 0, MPI.ANY_TAG);
	                if (status.tag == COMPLETED_TAG) {
			        cont = false;
				return cont;
			} else if (status.tag == POINT_PORTION_TAG) {
				List<Point> pointPortionList = convertArrayToList(pointsArray);
		                HashMap<Point, List<Point>> centroidToPoints = makeCentroidToPointsMap(centroidList, pointPortionList);
		                Object[] resultObj = {centroidToPoints};
		                MPI.COMM_WORLD.Send(resultObj, 0, 1, MPI.OBJECT, 0, RESULT_TAG);
			}   
		}

		return cont;
	}

	private List<Point> convertArrayToList(Object[] array) {
		List<Point> list = new ArrayList<Point>();
		for(Object obj : array) {
			if(obj!=null) {
				list.add((Point) obj);
			}
		}
		return list;
	}

        /**
         * makeCentroidToPointsMap:
         * Construct a hashmap between centroids and 2D points based on the distances between them
         * @param centroidList
         * @param pointPortionList
         * @return
         */
	private HashMap<Point, List<Point>> makeCentroidToPointsMap(List<Point> centroidList, List<Point> pointPortionList) {
		
		HashMap<Point, List<Point>> centroidToPoints = new HashMap<Point, List<Point>>();
		
		for(Point centroid : centroidList) {
			centroidToPoints.put(centroid, new ArrayList<Point>());
		}
		
		for(Point p : pointPortionList) {
			
			double minDistance = Double.MAX_VALUE;
			Point tmpCentroid = null;
			
			for(Point centroid : centroidList) {
				
				double distance = p.distance(centroid);
				
				if(distance < minDistance) {
					minDistance = distance;
					tmpCentroid = centroid;					
				}
				
			}
			
			centroidToPoints.get(tmpCentroid).add(p);
			
		}
		
		return centroidToPoints;
		
	}

}
