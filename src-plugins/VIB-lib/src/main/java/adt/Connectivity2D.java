/*
 * Created on 29-May-2006
 */
package adt;

import java.awt.Point;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * a structure that partitions points added into it into connected islands (of Points)
 * Probably could do with a speedup...
 */
public class Connectivity2D {
	HashSet<Points> islands = new HashSet<Points>();

    Point topLeft;
    Point bottomRight;

	public void addPoint(int x, int y) {
		addPoint(new Point(x, y));
	}

    /**
     * the contract of calling this method is that the new point must be gauranteed to
     * be the most far right lowest point (i.e. its x&y are greater or equal to all other points)
     * this gives a slight speedup
     * (though I think the bottle neck is the merging of Points)
     * @param p
     */
    public void addLowerRightPoint(Point p){
        //update the bounds
		if(topLeft == null) topLeft = new Point(p.x,p.y);
        else{
            if(p.x < topLeft.x) topLeft.x = p.x;
            if(p.y < topLeft.y) topLeft.y = p.y;
        }
        if(bottomRight == null) bottomRight = new Point(p.x,p.y);
        
        addPoint(p, getUpperLeftNeigbours(p));
    }

    public void addPoint(Point p) {
        //update the bounds
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
        addPoint(p, getNeighbours(p));
	}

    private void addPoint(Point p, Set<Point> neigbours){

        //add the point to the relevant island
		Points masterIsland = null;
		HashSet<Points> slaveIslands = new HashSet<Points>();

		//go through every neuighbour of p
		for(Point neighbour:neigbours){

			//check it against every possible island
			for(Points island:islands){

				//if an island contains the neighbour, then this point is part of that island
				if(island.contains(neighbour)){
					island.addPoint(p);

					if(masterIsland == null){
						//so we have a definite island for this point now
						masterIsland = island;
					}else if(island != masterIsland){
						//this point has joined some islands!
						//add the extra islands to a datastructure for later processing
						slaveIslands.add(island);
					}
				}
			}
		}

		if(masterIsland == null){
			//this means this point is an island of its own
			Points island = new Points();
			island.addPoint(p);
			islands.add(island);
		}else{
			//the master island will stay, and all slave islands will be removed and merged with the master
			//the point will have allready been added to the island lists
			for(Points slave : slaveIslands){
				masterIsland.addPoints(slave);
				islands.remove(slave);
			}
		}
    }


	
	public Iterable<Points> getIslands(){
		return islands;
	}

    /**
     * 4 nieghbours per point
     * @param p
     * @return
     */
	public static Set<Point> getNeighbours(final Point p){
		HashSet<Point> neighbours = new HashSet<Point>();
		for(int x=-1;x<2;x++)
			for(int y=-1;y<2;y++)
			{
				if(x==0&&y==0)continue;
				neighbours.add(new Point(p.x+x,p.y+y));
			}
		return neighbours;
	}

    /**
     * 2 negihbours (with lower x or y values
     * @param p
     * @return
     */
	public static Set<Point> getUpperLeftNeigbours(final Point p){
		HashSet<Point> neighbours = new HashSet<Point>();
	    neighbours.add(new Point(p.x-1,p.y));
        neighbours.add(new Point(p.x,p.y-1));
		return neighbours;
	}

	public String toString(){
		StringBuffer buf = new StringBuffer();
		int islandIndex =0;
		for(Points island:islands){
			buf.append("\n").append("island: ").append(islandIndex++);
			
			for(Point p:island.getPoints()){
				buf.append(p).append("\t");
				
			}
		}
		
		return buf.toString();
		
	}

}
