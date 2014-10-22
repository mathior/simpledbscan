import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TODO: find standard dbscan testcases to ensure correct behaviour </p>
 * <p>
 * Note: typical clustering test cases don't apply here, because
 * <ul>
 * <li>the dimension domain is not float/double</li>
 * <li>clustering test cases are typically normalized/scaled, i.e. centered
 * at the origin</li>
 * </ul>
 * </p>
 */

/**
 * A simple DBScan implementation tailored for the 2D-integer-domain.
 * <p>
 * 
 * @author chris.braeutigam@gmail.com
 * @version $Id: DBScan2D.java,v 1.1 2013/10/07 18:37:35 apui9892 Exp $
 * 
 */
public class DBScan2D {
	/*
	 * assignment code: 0 unassigned -1 noise i > 0 cluster i
	 */

	// STATE

	/*
	 * Holds unclassified points.
	 */
	private Set<Point2D> unclassifiedPoints;

	/*
	 * Holds the assigned cluster id for each point. Indexes in assignment must
	 * correspond to indexes in points, such that assignment[i] holds the
	 * cluster id for points.get(i) for all i.
	 */
	private int[] assignment;

	/*
	 * Both arrays holds references to the points. xSorted is sorted according
	 * to the x dimension and ySorted is sorted according to the y dimension.
	 * This allows fast one-dimensional neighborhood lookups.
	 */
	private Point2D[] xSorted;
	private Point2D[] ySorted;

	/*
	 * A helper map that maps points to important information. The int[] must be
	 * a 3-element array, where each element is an index into another array.
	 * Those elements are associated with a point p, such that <br> [0] is the
	 * index of p in xSorted array<br> [1] is the index of p in ySorted
	 * array<br> [2] is the index of p in assignment array<br> Those information
	 * is necessary to look up a given point p efficiently in the other arrays.
	 */
	private Map<Point2D, int[]> helper;

	/*
	 * The neighbors set holds the neighbors of a point p after
	 * computeNeighborhood(p) is called, neighborsFirstDimension is a helper for
	 * the computation.
	 */
	private Set<Point2D> neighborsFirstDimension;
	private Set<Point2D> neighbors;

	/*
	 * Holds the density reachable hull after computeDensityReachableHull() is
	 * called. The computation starts with all points that are currently in
	 * neighbors, adds them to the hull and then continues adding all neighbors
	 * of neighbors to the hull, until no new neighbors can be added to the
	 * hull.
	 */
	private Set<Point2D> densityReachableHull;

	// PARAMETERS

	/*
	 * The epsilon distance to look for neighbors. This value is used do look in
	 * each dimension and in each direction, i.e. the range that is looked up is
	 * 2*epsilon in x- and 2*epsilon in y-dimension.
	 */
	private final int epsilon;

	/*
	 * The minimum number of points that must be in the neighborhood of a point
	 * p to declare p a core point.
	 */
	private final int minPts;

	/**
	 * Create a new {@link DBScan2D} instance with a epsilon distance and minPts
	 * needed neighbors for a core point.<br>
	 * The epsilon distance is the distance to look for neighbors from a given
	 * point p. The epsilon value is used do look in each dimension and in each
	 * direction, i.e. the range that is looked up is (p.x - epsilon) until (p.x
	 * + epsilon) and (p.y - epsilon) until (p.y + epsilon) (2*epsilon in each
	 * dimension).<br>
	 * The minPts value is the minimum number of points, that must be in the
	 * epsilon-neighborhood of a point p, to declare p as core point (i.e. p is
	 * a core point iff |epsilon-neighborhood of p| >= minPts).
	 * 
	 * @param epsilon
	 *            Distance to look for neighbors.
	 * @param minPts
	 *            Minimum number of points in the epsilon-neighborhood of a
	 *            point p to declare p a core point.
	 */
	public DBScan2D(int epsilon, int minPts) {
		assert epsilon > 0 : "epsilon must be > 0";
		assert minPts > 0 : "minimum core points must be > 0";
		this.epsilon = epsilon;
		this.minPts = minPts;
	}

	/**
	 * Setup the state for a list of points to cluster. This *must* be done
	 * before the clustering.
	 * 
	 * @param points
	 */
	private void setup(List<Point2D> points) {
		// setup and fill unclassified points
		unclassifiedPoints = new HashSet<Point2D>();
		unclassifiedPoints.addAll(points);

		// setup the helper map
		helper = new HashMap<Point2D, int[]>();

		// setup the assignments array. this array will be filled with
		// meaningful values during the clustering process.
		assignment = new int[points.size()];
		Arrays.fill(assignment, 0); // 0 means unassigned
		// assignment index must correspond with points list index
		for (int i = 0; i < points.size(); ++i) {
			// setup the assignment array position for each point
			helper.put(points.get(i), new int[] { -1, -1, i });
		}

		// setup the x- and y-dimension sorted arrays
		xSorted = points.toArray(new Point2D[points.size()]);
		ySorted = points.toArray(new Point2D[points.size()]);
		Arrays.sort(xSorted, new Comparator<Point2D>() {
			@Override
			public int compare(Point2D o1, Point2D o2) {
				if (o1.x < o2.x) {
					return -1;
				} else if (o1.x > o2.x) {
					return 1;
				} else {
					return 0;
				}
			}
		});

		// for (int i = 1; i < xSorted.length; ++i) {
		// assert xSorted[i-1].x <= xSorted[i].x;
		// }

		Arrays.sort(ySorted, new Comparator<Point2D>() {
			@Override
			public int compare(Point2D o1, Point2D o2) {
				if (o1.y < o2.y) {
					return -1;
				} else if (o1.y > o2.y) {
					return 1;
				} else {
					return 0;
				}
			}
		});

		// for (int i = 1; i < ySorted.length; ++i) {
		// assert ySorted[i-1].y <= ySorted[i].y;
		// }

		// setup xSorted array index for each point
		for (int i = 0; i < xSorted.length; ++i) {
			helper.get(xSorted[i])[0] = i;
		}

		// setup ySorted array index for each point
		for (int i = 0; i < ySorted.length; ++i) {
			helper.get(ySorted[i])[1] = i;
		}

		// setup state sets for neighbors and density reachable hull
		/*
		 * TODO: Some memory is wasted here, but the size of points is a
		 * reasonable upper bound for all these sets.
		 */
		neighborsFirstDimension = new HashSet<>(points.size());
		neighbors = new HashSet<>(points.size());
		densityReachableHull = new HashSet<>(points.size());
	}

	/**
	 * Chooses and removes a point from the set of unclassified points.
	 * 
	 * @return An unclassified point.
	 */
	private Point2D chooseUnclassifiedPoint() {
		Point2D tmp = unclassifiedPoints.iterator().next();
		unclassifiedPoints.remove(tmp);
		return tmp;
	}

	/**
	 * Computes the epsilon-neighborhood for a point. After calling this method,
	 * the neighborhood points are in the neighbors set.
	 * 
	 * @param point
	 */
	private void computeNeighborhood(Point2D point) {

		neighborsFirstDimension.clear();
		neighbors.clear();

		// set range borders
		int minX = point.x - epsilon;
		int maxX = point.x + epsilon;
		int minY = point.y - epsilon;
		int maxY = point.y + epsilon;

		// retrieve xSorted index for point
		int xSortedIndex = helper.get(point)[0];

		// assert xSorted[xSortedIndex].equals(v);

		// lookup neighbors in x-dimension with x <= point.x
		int i = xSortedIndex - 1;
		while (i >= 0 && xSorted[i].x >= minX) {
			neighborsFirstDimension.add(xSorted[i]);
			--i;
		}

		// lookup neighbors in x-dimension with x >= point.x
		i = xSortedIndex + 1;
		while (i < xSorted.length && xSorted[i].x <= maxX) {
			neighborsFirstDimension.add(xSorted[i]);
			++i;
		}

		// retrieve ySorted index for point
		int ySortedIndex = helper.get(point)[1];

		// assert ySorted[ySortedIndex].equals(v);

		// lookup neighbors in y-dimension with y <= point.y
		i = ySortedIndex - 1;
		while (i >= 0 && ySorted[i].y >= minY) {
			// add to neighbors only points that are already in x range
			if (neighborsFirstDimension.contains(ySorted[i])) {
				neighbors.add(ySorted[i]);
			}
			--i;
		}

		// lookup neighbors in y-dimension with y >= point.y
		i = ySortedIndex + 1;
		while (i < ySorted.length && ySorted[i].y <= maxY) {
			// add to neighbors only points that are already in x range
			if (neighborsFirstDimension.contains(ySorted[i])) {
				neighbors.add(ySorted[i]);
			}
			++i;
		}

	}

	/**
	 * Computes the density reachable hull. Uses the current neighbors, adds
	 * them to the hull and successively adds all neighbors of neighbors to the
	 * hull, until no new points can be added to the hull. After calling this
	 * method, the set densityReachableHull contains all points that are density
	 * reachable.
	 */
	private void computeDensityReachableHull() {
		densityReachableHull.clear();
		// setup a stack for unprocessed neighbor points
		Deque<Point2D> stack = new ArrayDeque<Point2D>();
		densityReachableHull.addAll(neighbors);
		stack.addAll(neighbors);

		// add all neighbors of neighbors to the hull
		while (!stack.isEmpty()) {
			Point2D reachablePoint = stack.pop();
			computeNeighborhood(reachablePoint);
			for (Point2D p : neighbors) {
				// if a point is not already in the hull, put it on the stack
				// and add it to the hull
				if (!densityReachableHull.contains(p)) {
					stack.push(p);
					densityReachableHull.add(p);
				}
			}
		}
	}

	/**
	 * Cluster a list of points. The returned int[] (here called assignment)
	 * holds cluster id's such that assignment[i] is the cluster id for
	 * points.get(i).<br>
	 * The assignment values are:<br>
	 * -1 : noise<br>
	 * 0 : unassigned (should not appear in assignments)<br>
	 * i > 1 : cluster i
	 * 
	 * @param points
	 *            A list of 2-D points to cluster.
	 * @return An assignment array holding cluster id's for all points in the
	 *         given points list. Each index corresponds with a position in the
	 *         points list, such that assignment[i] is the cluster id for
	 *         points.get(i).
	 */
	public int[] cluster(List<Point2D> points) {

		setup(points);

		// perform a textbook-dbscan
		int clusterId = 0;
		while (!unclassifiedPoints.isEmpty()) {
			Point2D p = chooseUnclassifiedPoint();
			computeNeighborhood(p);
			if (neighbors.size() >= minPts) {
				++clusterId;
				assignment[helper.get(p)[2]] = clusterId;
				computeDensityReachableHull();
				for (Point2D pInHull : densityReachableHull) {
					assignment[helper.get(pInHull)[2]] = clusterId;
					unclassifiedPoints.remove(pInHull);
				}
			} else {
				assignment[helper.get(p)[2]] = -1;
			}
		}

		return assignment;
	}

	public static void simpleTest() {
		DBScan2D clusterer = new DBScan2D(5, 3);
		@SuppressWarnings("serial")
		List<Point2D> points = new ArrayList<Point2D>() {
			{
				add(new Point2D(1, 1));
				add(new Point2D(2, 1));
				add(new Point2D(1, 2));
				add(new Point2D(2, 2));

				add(new Point2D(1, 10));
				add(new Point2D(2, 10));
				add(new Point2D(1, 11));
				add(new Point2D(2, 11));

			}
		};
		int[] assignment = clusterer.cluster(points);
		for (int i : assignment) {
			System.out.println(i);
		}

	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		simpleTest();
	}

}
