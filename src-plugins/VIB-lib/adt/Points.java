/*
 * Created on 29-May-2006
 */
package adt;

import java.awt.*;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.TreeSet;

/**
 * class that represents a collection of Points
 * Optimized for infrequent addAll operations, by simply adding a reference to the child Points object
 */
public class Points {

    //more effecitent the HashSet (?) becuase of addAll points gets called alot
	private HashSet<Point> points = new HashSet<Point>();

    //the chilod field allows us quick addAll methods
    private Points child = null;

	private Point topLeft;
    private Point bottomRight;

	public void addPoint(int x, int y) {
		addPoint(new Point(x, y));
	}

	public void addPoint(Point p) {
        if(topLeft == null) topLeft = new Point(p.x,p.y);
        else{
            if(p.x < topLeft.x) topLeft.x = p.x;
            if(p.y < topLeft.y) topLeft.y = p.y;
        }
        if(bottomRight == null) bottomRight = new Point(p.x,p.y);
        else{
            if(p.x > bottomRight.x) bottomRight.x = p.x;
            if(p.y > bottomRight.y) bottomRight.y = p.y;
        }
		points.add(p);
	}	
	
	public int getSize(){
		return points.size() + (child == null?0:child.getSize());
	}

	public boolean contains(Point p) {
        if(child == null) return points.contains(p);
        else return points.contains(p) || child.contains(p);
	}

    //speedup through linked list impl
	public void addPoints(Points other) {
        if(other.topLeft.x < topLeft.x) topLeft.x = other.topLeft.x;
        if(other.topLeft.y < topLeft.y) topLeft.y = other.topLeft.y;
        if(other.bottomRight.x < bottomRight.x) bottomRight.x = other.bottomRight.x;
        if(other.bottomRight.y < bottomRight.y) bottomRight.y = other.bottomRight.y;

        //rather than adding all the points we will build up a linked list
        //but only in large cases of points
        if(other.points.size() > 5){
            if(child == null){
                child = other;
            }else{
                child.addPoints(other);
            }
        }else{
            points.addAll(other.points);
        }

	}

    //moves all child points into this object
    private void removeChildRef(){
        Points current = child;
        while(current!=null){
            points.addAll(current.points);
            Points old = current;
            current = current.child;
            old.child = null;
        }
        child=null;
    }

	public Iterable<Point> getPoints() {
        //this method is assumed to be called infrequently
        //probably after the Connectivity graph has been made
        //as such all points should be added to the root Points class
        //and the linked list disolved
        removeChildRef();
		return points;
	}

    public Polygon getOutline(){
        removeChildRef();

        Polygon poly = new Polygon();

        SearchNode current = findInitialEdge();
        Point start = current.p;

        do{
            poly.addPoint(current.p.x, current.p.y);
            current.next();
        }while(!current.p.equals(start));

        return poly;
    }

    private SearchNode findInitialEdge() {
        //must be an edge somewhere on the bounds
        for(int x = topLeft.x; x<= bottomRight.x; x++){
            for(int y = topLeft.y; y <= bottomRight.y; y++)
            {
                if(contains(new Point(x, y))){
                    if(y > topLeft.y) System.err.println("border has been calculated incorrectly, but corrected for");
                    return new SearchNode(new Point(x, y), 0);
                }
            }
        }

        throw new RuntimeException("border has been calculated incorrectly");
    }

    class SearchNode{

        Point p;
        int borderDir;   //0 - 7 indicating clockwise dir, 0 being north
        //701
        //6 2
        //543

        public SearchNode(Point p, int borderDir) {
            this.p = p;
            this.borderDir = borderDir;
        }

        //moves the position to the next part of the edge
        public void next(){
            Point next = null;
            int searchDir = borderDir;
            Point knownExterior = getPointInDir(p, borderDir);
            for(int i=1;i<8; i++){

                searchDir = (borderDir + i)%8;
                next = getPointInDir(p, searchDir);
                if(contains(next)) break;
                else{
                    knownExterior = next;
                }
            }
            //if its a single point we might be in the situation where nothing is contained in any direction around
            if(next == null){
                next = p;
                //should not ahppen that often
                System.out.println("single point points = " + points);
            }
            //so we have found the next point
            p = next;
            borderDir = dir(p, knownExterior);
            //now find what is the furthest along we can find a free point next to it
            for(int i=1;i<8; i++){
                searchDir = (borderDir + 1)%8;
                next = getPointInDir(p, searchDir);
                if(contains(next)) break;
                else{
                    borderDir = searchDir;//we have found a better exterior point
                }
            }
        }

        public Point getPointInDir(Point ref, int dir){
            if(dir == 0){
                return new Point(ref.x,ref.y-1);
            }else if(dir ==1){
                return new Point(ref.x+1,ref.y-1);
            }else if(dir ==2){
                return new Point(ref.x+1,ref.y);
            }else if(dir ==3){
                return new Point(ref.x+1,ref.y+1);
            }else if(dir ==4){
                return new Point(ref.x,ref.y+1);
            }else if(dir ==5){
                return new Point(ref.x-1,ref.y+1);
            }else if(dir ==6){
                return new Point(ref.x-1,ref.y);
            }else if(dir ==7){
                return new Point(ref.x-1,ref.y-1);
            }
            throw new RuntimeException("dir = " + dir);
        }

        public int dir(Point ref, Point neighbour){
            if(neighbour.x < ref.x){
                if(neighbour.y < ref.y){
                    return 7;
                }else if(neighbour.y == ref.y){
                    return 6;
                }else{
                    //neighbour.y > ref.y
                    return 5;
                }
            }else if(neighbour.x == ref.x){
                if(neighbour.y < ref.y){
                    return 0;
                }else if(neighbour.y == ref.y){
                    return -1; //should not happen (ref == neighbour)
                }else{
                    //neighbour.y > ref.y
                    return 4;
                }
            }else{
                //neighbour.x > ref.x
                if(neighbour.y < ref.y){
                    return 1;
                }else if(neighbour.y == ref.y){
                    return 2;
                }else{
                    //neighbour.y > ref.y
                    return 3;
                }
            }
        }
    }

}
