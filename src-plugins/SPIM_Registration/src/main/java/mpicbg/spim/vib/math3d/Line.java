package mpicbg.spim.vib.math3d;

/**
 * User: Tom Larkworthy
 * Date: 23-Jun-2006
 * Time: 21:27:15
 */
public class Line {
    Point3d p1;
    Point3d p2;

    public Line(Point3d p1, Point3d p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    public String toString(){
        return p1 + "-" + p2;
    }

}
