
package org.bladecoder.engine.polygonalpathfinder;

import org.bladecoder.engine.pathfinder.AStarPathFinder.AStarHeuristicCalculator;
import org.bladecoder.engine.pathfinder.NavContext;

/** 
 * Implementation of a heuristic calculator for a polygonal map. It simply calculates the Manhattan distance between two points.
 * 
 * @author rgarcia
 */
public class ManhattanDistance implements AStarHeuristicCalculator<NavNodePolygonal> {
	@Override
	public float getCost (NavContext<NavNodePolygonal> map, Object mover, NavNodePolygonal startNode, NavNodePolygonal targetNode) {
		float sx = startNode.getX();
		float sy = startNode.getY();

		float tx = targetNode.getX();
		float ty = targetNode.getY();
		
		return Math.abs(tx - sx) + Math.abs(ty - sy);
	}
}
