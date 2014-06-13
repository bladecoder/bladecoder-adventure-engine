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
package org.bladecoder.engine.tilepathfinder;

/**
 * The description for the data we're pathfinding over. This provides the contract
 * between the data being searched (i.e. the in game map) and the path finding
 * generic tools
 * 
 * @author Kevin Glass
 */
public interface TileBasedMap {
	/**
	 * Get the width of the tile map. The slightly odd name is used
	 * to distiguish this method from commonly used names in game maps.
	 * 
	 * @return The number of tiles across the map
	 */
	public int getWidthInTiles();

	/**
	 * Get the height of the tile map. The slightly odd name is used
	 * to distiguish this method from commonly used names in game maps.
	 * 
	 * @return The number of tiles down the map
	 */
	public int getHeightInTiles();
	
	/**
	 * Notification that the path finder visited a given tile. This is 
	 * used for debugging new heuristics.
	 * 
	 * @param x The x coordinate of the tile that was visited
	 * @param y The y coordinate of the tile that was visited
	 */
	public void pathFinderVisited(int x, int y);
	
	/**
	 * Check if the given location is blocked, i.e. blocks movement of 
	 * the supplied mover.
	 * 
	 * @param mover The mover that is potentially moving through the specified
	 * tile.
	 * @param x The x coordinate of the tile to check
	 * @param y The y coordinate of the tile to check
	 * @return True if the location is blocked
	 */
	public boolean blocked(Movers mover, int x, int y);
	
	/**
	 * Get the cost of moving through the given tile. This can be used to 
	 * make certain areas more desirable. A simple and valid implementation
	 * of this method would be to return 1 in all cases.
	 * 
	 * @param mover The mover that is trying to move across the tile
	 * @param sx The x coordinate of the tile we're moving from
	 * @param sy The y coordinate of the tile we're moving from
	 * @param tx The x coordinate of the tile we're moving to
	 * @param ty The y coordinate of the tile we're moving to
	 * @return The relative cost of moving across the given tile
	 */
	public float getCost(Movers mover, int sx, int sy, int tx, int ty, int dirx, int diry);
}
