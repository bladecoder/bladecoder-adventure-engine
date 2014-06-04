package org.bladecoder.engine.polygonalpathfinder;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;

/**
 * Finds the shortest path between 2 points in a world defined by a walkzone and several obstacles.
 * 
 * 
 * @author rgarcia
 */
public class PolygonalPathFinder {
	private Polygon walkZone;
	private ArrayList<Polygon> obstacles = new ArrayList<Polygon>();
	private ArrayList<Vector2> resultPath = new ArrayList<Vector2>();
	
	List<Vector2> findPath(float sx, float sy, float tx, float ty) {
		// 1. First verify if both the start and end points of the path are inside the polygon. If the end point is outside the polygon you can optionally clamp it back inside.
		// 2. Then start by checking if both points are in line-of-sight. If they are, there’s no need for pathfinding, just walk there!
		// 3. Otherwise, add the start and end points of your path as new temporary nodes to the graph.
		// 4. Connect them to every other node that they can see on the graph.
		// 5. Run your A* implementation on the graph to get your path. This path is guaranteed to be as direct as possible!
		// 6. Finally, remove the two temporary nodes from the graph.
				
		return resultPath;
	}

	public Polygon getWalkZone() {
		return walkZone;
	}

	public void setWalkZone(Polygon walkZone) {
		this.walkZone = walkZone;
	}
	
	public void addObstacle(Polygon obstacle) {
		obstacles.add(obstacle);
	}
}
