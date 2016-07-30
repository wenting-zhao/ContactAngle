import ij.*;
import ij.measure.ResultsTable;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;
import ij.plugin.ContrastEnhancer;
import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.awt.geom.Point2D;
import java.awt.geom.Arc2D;

public class ContactAngle_ implements PlugInFilter {
	ImagePlus imp;
	ArrayList<Point> points; // holds all white pixels on the image
	Random random;
	Overlay overlay;
	ImageProcessor ip;
	int[][] pixels;
	/** 
	 * double[] radii, xs, ys store each circle's radius and center coordinate
	 * in samples. In this plugin, there will be 1000 circle samples.
	 */
	double[] radii, xs, ys;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		overlay = new Overlay();
		points = new ArrayList<Point>();
		random = new Random();
		radii = new double[1000];
		xs = new double[1000];
		ys = new double[1000];
		return DOES_ALL;
	}

	public void run(ImageProcessor i_ip) {
		new WaitForUserDialog("Please select the region of drop, then click OK.").show();
		ip = i_ip.crop();
		Roi roi = imp.getRoi();
		double roiWidth = roi.getFloatWidth();
		ip.findEdges();
		ip.fillOutside(roi);
		imp.deleteRoi();

		// Stretch the contrast to normalize the brightness (roughly) and ensure some pixels are white
		ContrastEnhancer contrastEnhancer = new ContrastEnhancer();
		contrastEnhancer.stretchHistogram(ip, 0.01);
		
		pixels = ip.getIntArray();
		points = getWhitePixels();
		int[] manywhites = new int[1000];
		for (int i = 0; i < 1000; i++) {
			// Point p1, p2, p3 are three points randomly picked from all white points
			Point p1 = points.get(random.nextInt(points.size()));
			Point p2 = points.get(random.nextInt(points.size()));
			Point p3 = points.get(random.nextInt(points.size()));

			/** 
			 * check if the distances between p1 and other two points are too close,
			 * if so, repeat sampling p2 or p3 until they are far enough.
			 */ 
			while (p1.distance(p2) < roiWidth/20) {
				p2 = points.get(random.nextInt(points.size()));
			}
			while (p1.distance(p3) < roiWidth/20) {
				p3 = points.get(random.nextInt(points.size()));
			}

			/** 
			 * calculate the intersection of two normal lines to determine the center
			 */
			MyLine normal1 = getNormal(p1.getX(), p1.getY(), p2.getX(), p2.getY());
			MyLine normal2 = getNormal(p1.getX(), p1.getY(), p3.getX(), p3.getY());
			Point2D.Double intersection = normal1.findIntersection(normal2.x1, normal2.y1, normal2.x2, normal2.y2);
			Double radius;
			if (intersection != null) {
				radius = intersection.distance(p2);
				radii[i] = radius;
				xs[i] = intersection.getX();
				ys[i] = intersection.getY();
				manywhites[i] = checkMatch(radii[i], xs[i], ys[i]);
			}
			else i--;
		}

		int lightMax = getMaximum(manywhites);
		Point2D.Double center_max = new Point2D.Double(xs[lightMax], ys[lightMax]);
		double radius = radii[lightMax];

		ArrayList<Point> pointsOutsideCircle = new ArrayList<Point>();
		for (int i = 0; i < points.size(); i++) {
			Point p = points.get(i);
			if (p.getX() < center_max.getX() - 1.1*radius || p.getX() > center_max.getX() + 1.1*radius) {
				pointsOutsideCircle.add(p);
			}
		}
		ArrayList<MyLine> surfaces = getSurface(pointsOutsideCircle);
		Collections.sort(surfaces, new MyComparator());
		
		double slope_median = surfaces.get(surfaces.size() / 2).getSlope();
		double[] intercepts = new double[pointsOutsideCircle.size()];
		for (int i = 0; i < intercepts.length; i++) {
			Point p = pointsOutsideCircle.get(i);
			intercepts[i] = p.getY() - slope_median * p.getX();
		}
		double intercept_median = getMedian(intercepts);
		MyLine surface = new MyLine(0, intercept_median, ip.getWidth(), ip.getWidth()*slope_median+intercept_median);
		drawCircle(center_max, radius, Color.red);
		drawLine(0, intercept_median, ip.getWidth(), ip.getWidth()*slope_median+intercept_median, Color.black);
		double contactAngle = getContactAngle(surface, center_max, radius);
		drawResultsTable(contactAngle);
	}

	private ArrayList<Point> getWhitePixels() {
		ArrayList<Point> whitePixels = new ArrayList<Point>();
		for (int i = 0; i < pixels[0].length; i++) {
			for (int j = 0; j < pixels[1].length; j++) {
				int pixel = pixels[i][j];
				Color c = new Color(pixel);
				int r = c.getRed();
				int g = c.getGreen();
				int b = c.getBlue();
				int brightness = (r + g + b) / 3;
				pixels[i][j] = brightness;
				if (brightness > 100) {
					Point p = new Point(i, j);
					whitePixels.add(p);
				}
			}
		}
		return whitePixels;
	}

	private int getMaximum(int[] numArray) {
		int indexOfMaximum = 0;
		int tempMaximum = 0;
	    for (int i = 0; i < numArray.length; i++) {
			if (tempMaximum < numArray[i]) {
				tempMaximum = numArray[i];
				indexOfMaximum = i;
			}
			//IJ.log(Integer.toString(i)+" "+Integer.toString(indexOfMaximum)+" "+Integer.toString(numArray[i])); // debugging
		}
		return indexOfMaximum;
	}

	private int checkMatch(double radius, double center_x, double center_y) {
		int lightPixels = 0;
		for (double theta = 0; theta < 2*Math.PI; theta += Math.PI/200) {
			double x_onCircle = radius*Math.cos(theta)+center_x;
			double y_onCircle = radius*Math.sin(theta)+center_y;
			if (x_onCircle > ip.getWidth() || x_onCircle < 0 || y_onCircle > ip.getHeight() || y_onCircle < 0) return 0;
			int brightness = pixels[(int)(x_onCircle)][(int)(y_onCircle)];
			if (brightness > 200) {
				lightPixels++;
			}
		}
		return lightPixels;
	}

	private double getContactAngle(MyLine surface, Point2D.Double center, double r) {
		ArrayList<Point2D.Double> circleLineIntersections = surface.getCircleLineIntersection(center, r);
		Point2D.Double p1 = circleLineIntersections.get(0);
		Point2D.Double p2 = circleLineIntersections.get(1);
		double dx = p1.distance(p2) / 2;
		double dy = Math.sqrt(Math.pow(r, 2)-Math.pow(dx, 2));
		if (((p1.getY()+p2.getY())/2) < center.getY()) dy = -dy;
		double contactAngle = Math.atan2(dx, dy);
		contactAngle = 180 - Math.toDegrees(contactAngle);

		if (p1.getX() > p2.getX()) {
			Point2D.Double temp = p1;
			p1 = p2;
			p2 = temp;
		}
		// draw tangent line
		double dx2 = center.getX() - p1.getX();
		double dy2 = center.getY() - p1.getY();
		drawLine(p1.getX() + 5*dy2, p1.getY() - 5*dx2, p1.getX(), p1.getY(), Color.black);
		double dx3 = center.getX() - p2.getX();
		double dy3 = center.getY() - p2.getY();
		drawLine(p2.getX() - 5*dy3, p2.getY() + 5*dx3, p2.getX(), p2.getY(), Color.black);

		// draw the arc used to indicate the the contact angle
		double surfaceAngle = Math.tan(surface.getSlope());
		surfaceAngle = Math.toDegrees(surfaceAngle);
		Arc2D.Double arc = new Arc2D.Double(p1.getX()-30, p1.getY()-30, 60, 60, surfaceAngle, contactAngle-3, Arc2D.OPEN);
		drawArc(arc, Color.blue);
		Arc2D.Double arc2 = new Arc2D.Double(p2.getX()-30, p2.getY()-30, 60, 60, 180-contactAngle-surfaceAngle+3, contactAngle-3, Arc2D.OPEN);
		drawArc(arc2, Color.blue);
		drawText(String.format("%.1f", contactAngle), p2.getX()+30, p2.getY()-50);

		return contactAngle;
	}

	private MyLine getNormal(double x1, double y1, double x2, double y2) {
		double dx = x1 - x2;
  		double dy = y1 - y2;
  		double mid_x = (x1 + x2) / 2;
  		double mid_y = (y1 + y2) / 2;
		MyLine normal = new MyLine(mid_x + dy, mid_y - dx, mid_x, mid_y);
		return normal;
	}

	private double getMedian(double[] numArray) {
		Arrays.sort(numArray);
		double median;
		if (numArray.length % 2 == 0){
			median = (numArray[numArray.length/2] + numArray[numArray.length/2 - 1]) / 2;
		}
		else {
			median = numArray[numArray.length/2];
		}
		return median;
	}

	private ArrayList<MyLine> getSurface(ArrayList<Point> points) {
		ArrayList<MyLine> surfaces = new ArrayList<MyLine>();
		for (int i = 0; i < points.size(); i++) {
			Point p1 = points.get(i);
			for (int j = i+1; j < points.size(); j++) {
				Point p2 = points.get(j);
				if (p1.getX() != p2.getX()) {
					MyLine l = new MyLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
					surfaces.add(l);
				}
			}
		}
		return surfaces;
	}

	void drawCircle(Point2D.Double center, double radius, Color c) {
		Roi circle = new OvalRoi(center.getX()-radius, center.getY()-radius, radius*2, radius*2);
		circle.setStrokeWidth(2);
		circle.setStrokeColor(c);
		overlay.add(circle);
		imp.setOverlay(overlay);
	}

	void drawLine(double x1, double y1, double x2, double y2, Color c) {
		Roi line = new Line(x1, y1, x2, y2);
		line.setStrokeWidth(2);
		line.setStrokeColor(c);
		overlay.add(line);
		imp.setOverlay(overlay);
	}

	void drawArc(Arc2D.Double i_arc, Color c) {
		Roi arc = new ShapeRoi(i_arc);
		arc.setStrokeWidth(3);
		arc.setStrokeColor(c);
		overlay.add(arc);
		imp.setOverlay(overlay);
	}

	void drawText(String s, double x, double y) {
		Roi text = new TextRoi(x, y, s, new Font("SansSerif", Font.PLAIN, 28));
		text.setStrokeColor(Color.black);
		overlay.add(text);
		imp.setOverlay(overlay);
	}

	void drawResultsTable(double contactAngle) {
		ResultsTable table = Analyzer.getResultsTable();
		if (table == null) {
        	table = new ResultsTable();
        	Analyzer.setResultsTable(table);
		}
		table.incrementCounter();
		table.addValue("Contact Angle", contactAngle);
		double total = 0;
		double[] angles = table.getColumnAsDoubles(0);
		for(int i = 0; i < angles.length; i++) {
			total += angles[i];
		}
		double mean = total / angles.length;
		table.addValue("Mean", mean);
		double median = getMedian(angles);
		table.addValue("Median", median);
		table.show("Results");
	}
}