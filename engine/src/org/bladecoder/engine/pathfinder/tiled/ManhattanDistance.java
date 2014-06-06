
package org.bladecoder.engine.pathfinder.tiled;

import org.bladecoder.engine.pathfinder.AStarPathFinder.AStarHeuristicCalculator;
import org.bladecoder.engine.pathfinder.NavContext;

/** Implementation of a heuristic calculator for a tile map. It simply calculates the Manhattan distance between two given tiles.
 * @author hneuer */
public class ManhattanDistance implements AStarHeuristicCalculator<NavNodeTileBased> {
	@Override
	public float getCost (NavContext<NavNodeTileBased> map, Object mover, NavNodeTileBased startNode, NavNodeTileBased targetNode) {
		return Math.abs(targetNode.x - startNode.x) + Math.abs(targetNode.y - startNode.y);
	}
}
