package org.bladecoder.engine.pathfinder.heuristics;

import org.bladecoder.engine.pathfinder.AStarHeuristic;
import org.bladecoder.engine.pathfinder.Movers;
import org.bladecoder.engine.pathfinder.TileBasedMap;

/**
 * A heuristic that uses the tile that is closest to the target
 * as the next best tile. In this case the sqrt is removed
 * and the distance squared is used instead
 * 
 * @author Kevin Glass
 */
public class ClosestSquaredHeuristic implements AStarHeuristic {

	/**
	 * @see AStarHeuristic#getCost(TileBasedMap, Movers, int, int, int, int)
	 */
	public float getCost(TileBasedMap map, Movers mover, int x, int y, int tx, int ty) {		
		float dx = tx - x;
		float dy = ty - y;
		
		return ((dx*dx)+(dy*dy));
	}

}
