
/**
 * Models a 2D point.
 * 
 * @author chris.braeutigam@gmail.com
 * @author michael.voelske@uni-weimar.de
 * @version $Id: Point2D.java,v 1.1 2013/10/07 18:37:35 apui9892 Exp $
 * 
 */
public class Point2D {

	public final int x;
	public final int y;

	public Point2D(int x, int y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof Point2D) {
			Point2D other = (Point2D) obj;
			return this.x == other.x && this.y == other.y;
		}
		return false;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + x + "," + y + ")";
	}

}
