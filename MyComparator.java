import java.util.Comparator;

public class MyComparator implements Comparator<MyLine> {
    public int compare(MyLine l1, MyLine l2) {
	    if (l1.getSlope() >= l2.getSlope()) {
	        return -1;
	    } 
	    else if (l1.getSlope() < l2.getSlope()) {
	        return 1;
	    }
	    return 0;
	}
}