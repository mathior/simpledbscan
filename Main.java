import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {
	
	// default values for epsilon distance and minimum neighbors for a core point
	private static int EPSILON = 4;
	private static int CORE_MIN_NEIGHBORS = 2;
	
	private static void printUsageAndExit(int exitStatus) throws IOException {
		List<String> lines = Files.readAllLines(FileSystems.getDefault()
				.getPath("usage.txt"), StandardCharsets.UTF_8);
		for (String line : lines) {
			System.out.println(line);
		}
		System.exit(exitStatus);
	}

	public static void main(String[] args) throws IOException {
		
		// check arguments
		if (args.length != 1 && args.length != 3) {
			printUsageAndExit(1);
		}
		
		Path path = FileSystems.getDefault().getPath(args[0]);
		if (!Files.isRegularFile(path)) {
			printUsageAndExit(1);
		}
		
		if (args.length == 3) {
			EPSILON = Integer.parseInt(args[1]);
			CORE_MIN_NEIGHBORS = Integer.parseInt(args[1]);
		}

		// read input file
		List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

		// convert input to a list of points

		List<Point2D> points = new ArrayList<>();

		for (String line : lines) {
			String[] parts = line.split(" ");
			points.add(new Point2D(Integer.parseInt(parts[0]),
					               Integer.parseInt(parts[1])));
		}
		
		// cluster points list
		
		DBScan2D dbScan = new DBScan2D(EPSILON, CORE_MIN_NEIGHBORS);
		int[] clustering = dbScan.cluster(points);

		// write clustering result
		
		for (int i = 0; i < clustering.length; ++i) {
			System.out.format("%s %s %s\n",
					points.get(i).x, points.get(i).y, clustering[i]);
		}
	}

}
