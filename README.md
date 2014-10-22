simpledbscan
============

A Java DBSCAN implementation tailored for the 2D-integer-domain.

[DBSCAN](http://en.wikipedia.org/wiki/DBSCAN) is a density-based clustering
algorithm.

Usage
-----

To use the clustering algorithm you may (1) import and use the DBScan2D class
into your own project or (2) compile the Main class and run it from the
commandline.

The clustering algorithm has two parameters:

*    epsilon -- The distance to look for neighbors. This value is used do look
     in each dimension and in each direction, i.e. the range that is looked up
     is 2 epsilon in the x- and 2 epsilon in the y-dimension.
*    minPts -- The minimum number of points that must be in the neighborhood of
     a point p to declare p a core point.

### (1) Example using the class

    // create a new instance with epsilon set to 5 and minPts set to 3
    DBScan2D clusterer = new DBScan2D(5, 3);
    
    // create or read a list of Point2D objects
    List<Point2D> points = ...
    
    // cluster the points
    int[] assignment = clusterer.cluster(points);
    
The `assignment` array now holds the cluster assignment for each point, s.t. for
each `i` `assignment[i]` denotes the cluster of `points.get(i)`. A value of -1
denotes a noise point and all values >0 denote a cluster id.

### (2) Example usage as commandline tool

    # Compile the code
    javac Main
    
    # Run the Main class
    java Main POINTSFILE [EPSILON COREMINNEIGHBORS]

Reads POINTSFILE, clusters the points and writes the clustering to stdout.
POINTSFILE must be a text file where each line denotes one point in the format
`x y [...]`
and x and y must be interpretable as integers. Only the first two
space-separated integers are considered as input, further characters are
discarded.
The output is in the format
`x y custerid`
where `clusterid` is an integer. A `clusterid` value of -1 denotes a noise point
and all values >0 denote the cluster assignment of the point (x, y).
(Note that a `clusterid` of 0 denotes an unassigned point and should not appear
in the final clustering.)
The Parameters EPSILON and COREMINNEIGHBORS are optional:

*   EPSILON -- The maximum distance where points count as neighbors.
    Default is 4.
*   COREMINNEIGHBORS -- The minimum required points in the epsilon distance to
    count a point as core point. Default is 2.

Test cases
----------

The directory `testcases` contains some standard test cases (blobs, circles,
moons) as POINTSFILEs as well as the desired output (blobs-clustered,
circles-clustered, moons-clustered). Also contained in the directory is an
IPython notebook that was used to create the test data.

