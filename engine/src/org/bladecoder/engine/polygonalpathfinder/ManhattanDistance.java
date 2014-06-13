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
