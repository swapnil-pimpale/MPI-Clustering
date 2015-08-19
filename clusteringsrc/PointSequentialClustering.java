import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;


public class PointSequentialClustering {

	private int k;
	private List<Point> points;
	
	public PointSequentialClustering(int k, List<Point> points) {
		this.k = k;
		this.points = points;
	}

	public List<Point> performClustering() {
		List<Point> centroids = new ArrayList<Point>();
		List<Point> oldCentroids;
		HashMap<Point, List<Point>> centroidToPoints;
		Random rand = new Random();

		for(int i=0;i<k;i++) {
			int index = rand.nextInt(points.size());
			Point p = (Point) points.get(index).clone();
			
			while(centroids.contains(p)) {
				index = rand.nextInt(points.size());
				p = (Point) points.get(index).clone();
			}
			
			centroids.add(p);
			
		}
		
		do {
			centroidToPoints = makeCentroidToPointsMap(centroids);
			
			for(Point p : points) {
				
				double minDistance = Double.MAX_VALUE;
				Point tmpCentroid = null;
				
				for(Point centroid : centroids) {
					
					double distance = p.distance(centroid);
					
					if(distance < minDistance) {
						minDistance = distance;
						tmpCentroid = centroid;					
					}
					
				}
				
				centroidToPoints.get(tmpCentroid).add(p);
				
			}
			oldCentroids = new ArrayList<Point>(centroids);
			centroids = recalculateCentroidLocations(centroidToPoints);
			
		} while(!oldCentroids.containsAll(centroids));
		
		return centroids;
	}
	
	private List<Point> recalculateCentroidLocations(HashMap<Point, List<Point>> centroidToPoints) {
		
		Collection<List<Point>> lists = centroidToPoints.values();
		List<Point> centroids = new ArrayList<Point>();
		
		for(List<Point> lp : lists) {
			
			if(lp.size()==0) {
				System.out.println("There exists a cluster centroid with no points"
						+ " assigned to it. You may end up with fewer clusters than expected.");
				continue;
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
			centroids.add(newCentroid);
		}
		
		return centroids;
	}

	public HashMap<Point, List<Point>> makeCentroidToPointsMap(List<Point> centroids) {
		HashMap<Point, List<Point>> centroidToPoints = new HashMap<Point, List<Point>>();
		
		for(Point centroid : centroids) {
			centroidToPoints.put(centroid, new ArrayList<Point>());
		}
		
		return centroidToPoints;
	}
	

}
