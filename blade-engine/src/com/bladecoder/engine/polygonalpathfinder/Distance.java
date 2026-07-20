package com.bladecoder.engine.polygonalpathfinder;

import com.bladecoder.engine.pathfinder.AStarPathFinder.AStarHeuristicCalculator;
import com.bladecoder.engine.pathfinder.NavContext;

/** 
 * Implementation of a heuristic calculator for a polygonal map. It simply calculates the distance between two points.
 * 
 * @author rgarcia
 */
public class Distance implements AStarHeuristicCalculator<NavNodePolygonal> {
	@Override
	public float getCost (NavContext<NavNodePolygonal> map, Object mover, NavNodePolygonal startNode, NavNodePolygonal targetNode) {
		float sx = startNode.getX();
		float sy = startNode.getY();

		float tx = targetNode.getX();
		float ty = targetNode.getY();
		
		float a = sx - tx;
		float b = sy - ty;
		
		return (float)Math.sqrt(a * a + b * b);
	}
}
