import java.awt.geom.Point2D;
import java.util.ArrayList;

public class MyLine {
	
    public double x1, y1, x2, y2;

    public MyLine(double ix1, double iy1, double ix2, double iy2) {
        this.x1 = ix1;
        this.y1 = iy1;
        this.x2 = ix2;
        this.y2 = iy2;
    }

    public double getSlope() {
        double slope = (y2 - y1) / (x2 - x1);
        return slope;
    }

    public Point2D.Double findIntersection(double x3, double y3, double x4, double y4) {
        if ((y1-y2)*(x3-x4) < 1.1*(x1-x2)*(y3-y4) && (y1-y2)*(x3-x4) > 0.9*(x1-x2)*(y3-y4)) {
            return null;
        }
        else {
            double x = ((x1*y2-x2*y1)*(x3-x4)-(x1-x2)*(x3*y4-x4*y3))
                       / ((x1-x2)*(y3-y4)-(y1-y2)*(x3-x4));
            double y = ((x1*y2-x2*y1)*(y3-y4)-(y1-y2)*(x3*y4-x4*y3))
                       / ((x1-x2)*(y3-y4)-(y1-y2)*(x3-x4));
            Point2D.Double intersection = new Point2D.Double(x, y);
            return intersection;
        }
    }

    // http://stackoverflow.com/questions/13053061/circle-line-intersection-points
    public ArrayList<Point2D.Double> getCircleLineIntersection(Point2D.Double center, double radius) {
        double baX = x2 - x1;
        double baY = y2 - y1;
        double caX = center.getX() - x1;
        double caY = center.getY() - y1;

        double a = baX * baX + baY * baY;
        double bBy2 = baX * caX + baY * caY;
        double c = caX * caX + caY * caY - radius * radius;

        double pBy2 = bBy2 / a;
        double q = c / a;

        double disc = pBy2 * pBy2 - q;
        if (disc < 0) {
            return null;
        }
        // if disc == 0 ... dealt with later
        double tmpSqrt = Math.sqrt(disc);
        double abScalingFactor1 = -pBy2 + tmpSqrt;
        double abScalingFactor2 = -pBy2 - tmpSqrt;

        ArrayList<Point2D.Double> points = new ArrayList<Point2D.Double>();
        Point2D.Double p1 = new Point2D.Double(x1 - baX * abScalingFactor1, y1
                - baY * abScalingFactor1);
        if (disc == 0) { // abScalingFactor1 == abScalingFactor2
            points.add(p1);
            return points;
        }
        Point2D.Double p2 = new Point2D.Double(x1 - baX * abScalingFactor2, y1
                - baY * abScalingFactor2);
        points.add(p1);
        points.add(p2);
        return points;
    }

    //http://floating-point-gui.de/errors/comparison/
    private boolean nearlyEqual(double a, double b, double epsilon) {
        final double absA = Math.abs(a);
        final double absB = Math.abs(b);
        final double diff = Math.abs(a - b);

        if (a == b) { // shortcut, handles infinities
            return true;
        } else if (a == 0 || b == 0 || diff < Float.MIN_NORMAL) {
            // a or b is zero or both are extremely close to it
            // relative error is less meaningful here
            return diff < (epsilon * Float.MIN_NORMAL);
        } else { // use relative error
            return diff / (absA + absB) < epsilon;
        }
    }
}