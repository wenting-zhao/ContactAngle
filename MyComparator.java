import java.util.Comparator;

public class MyComparator implements Comparator<MyLine> {
    public int compare(MyLine l1, MyLine l2) {
        return l2.getSlope() < l1.getSlope() ? -1 : l1.getSlope() == l1.getSlope() ? 0 : 1;
	}
}