/**
 * Point:
 * This class represents a single point in 2 dimension.
 * It stores the point's x and y co-ordinates
 */
import java.io.Serializable;


public class Point implements Cloneable, Serializable {
        /* The x and y co-ordinates */
	private double x;
	private double y;

        /* Construct a point from a string read from CSV */
	public Point(String str) {
		String[] arr = str.split(",");
		
		setX(Double.parseDouble(arr[0]));
		setY(Double.parseDouble(arr[1]));
	}

        /* Construct a point given it's x and y co-ordinates */
	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}

        /**
         * distance:
         * calculate distance between two points using the Euclidean distance formula
         * @param o
         * @return
         */
	public double distance (Point o) {
		return Math.sqrt( Math.pow(x-o.getX(),2) +  Math.pow(y-o.getY(),2));
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
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
	
	@Override 
	public boolean equals(Object o) {
		if(!(o instanceof Point)) {
			return false;
		}
		Point p = (Point) o;
		return (x == p.getX() && y == p.getY());
	}
	
	@Override
	public int hashCode() {	
		int result = 17;
			
		long x_hash = Double.doubleToLongBits(x);
		x_hash = (int) (x_hash^(x_hash>>>32));
		long y_hash = Double.doubleToLongBits(y);
		y_hash = (int) (y_hash^(y_hash>>>32));	
		result += 31 * x_hash;
		result += 31 * y_hash;
	
		return result;
	}
	
	@Override
	public String toString() {
		return "x coordinate: " + x + " y coordinate: " + y;
	}
	
}
