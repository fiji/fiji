package math3d;

/**
 * User: Tom Larkworthy
 * Date: 23-Jun-2006
 * Time: 20:23:42
 */
public class Plane {
    double a,b,c,d;   //plane eqn  ax + by + cz + d > 0

    public Plane(double a, double b, double c, double d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    public Plane(Point3d p1, Point3d p2, Point3d p3, Point3d insidePoint){
        double x1=p1.x,x2=p2.x,x3=p3.x;
        double y1=p1.y,y2=p2.y,y3=p3.y;
        double z1=p1.z,z2=p2.z,z3=p3.z;

        a = y1 * (z2 - z3) + y2 * (z3 - z1) + y3 * (z1 - z2);
        b = z1 * (x2 - x3) + z2 * (x3 - x1) + z3 * (x1 - x2);
        c = x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2);
        d = -(x1 * (y2 * z3 - y3 * z2) + x2 * (y3 * z1 - y1 * z3) + x3 * (y1 * z2 - y2 * z1));

        //convert to hessian normal form
        double dist = Math.sqrt(a*a + b*b + c*c);
        a/=dist;
        b/=dist;
        c/=dist;
        d/=dist;

        //check that the inside point is inside
        if(!isInside(insidePoint)){
            //it is not, so we invert the equation
            a*=-1;
            b*=-1;
            c*=-1;
            d*=-1;
        }
    }

    boolean isInside(Point3d p){
        return isInside(p.x,p.y,p.z);
    }
    boolean isInside(double x, double y, double z){
        return a*x + b*y + c*z > 0;
    }

    public Point3d intersection(Line l){
        double x1 = l.p1.x;
        double x2 = l.p2.x;
        double y1 = l.p1.y;
        double y2 = l.p2.y;
        double z1 = l.p1.z;
        double z2 = l.p2.z;


        double denominator = a*(x1-x2) + b*(y1-y2)+c*(z1-z2);
        if(denominator > -.0001 && denominator < .0001) return null;

        double u = (a*x1 + b*y1+c*z1+d)/denominator;

        Point3d ret = new Point3d(x1+u*(x2-x1),
                                  y1+u*(y2-y1),
                                  z1+u*(z2-z1));

        return ret;
    }


    public Point3d getNormal() {
        return new Point3d(a,b,c);
    }
}
