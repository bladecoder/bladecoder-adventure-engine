/*******************************************************************************
 * Copyright 2014 Rafael Garcia Moreno.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.bladecoder.engine.tilepathfinder.heuristics;

import org.bladecoder.engine.tilepathfinder.AStarHeuristic;
import org.bladecoder.engine.tilepathfinder.Movers;
import org.bladecoder.engine.tilepathfinder.TileBasedMap;

/**
 * A heuristic that drives the search based on the Manhattan distance
 * between the current location and the target
 * 
 * @author Kevin Glass
 */
public class ManhattanHeuristic implements AStarHeuristic {
	/** The minimum movement cost from any one square to the next */
	private int minimumCost;
	
	/**
	 * Create a new heuristic 
	 * 
	 * @param minimumCost The minimum movement cost from any one square to the next
	 */
	public ManhattanHeuristic(int minimumCost) {
		this.minimumCost = minimumCost;
	}
	
	/**
	 * @see AStarHeuristic#getCost(TileBasedMap, Movers, int, int, int, int)
	 */
	public float getCost(TileBasedMap map, Movers mover, int x, int y, int tx,
			int ty) {
		return minimumCost * (Math.abs(x-tx) + Math.abs(y-ty));
	}

}
