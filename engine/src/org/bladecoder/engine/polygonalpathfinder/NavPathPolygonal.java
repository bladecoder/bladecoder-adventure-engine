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

import java.util.ArrayList;

import org.bladecoder.engine.pathfinder.NavPath;

import com.badlogic.gdx.math.Vector2;

/**  
 * Implementation of a navigation path for a polygonal map.
 * 
 * @author rgarcia 
 */
public class NavPathPolygonal implements NavPath<NavNodePolygonal> {
	private final ArrayList<Vector2> resultPath = new ArrayList<Vector2>();

	@Override
	public void fill (NavNodePolygonal startNode, NavNodePolygonal targetNode) {
		
		// TODO Ineficcient add in index 0 and new
		
		resultPath.clear();
		
		NavNodePolygonal current = targetNode;
		while (current != startNode) {
			resultPath.add(0, new Vector2(current.getX(), current.getY()));
			current = (NavNodePolygonal)current.parent;
		}
		
		resultPath.add(0, new Vector2(current.getX(), current.getY()));
	}

	@Override
	public void clear () {
		resultPath.clear();
	}

	@Override
	public int getLength () {
		return resultPath.size();
	}
	
	public ArrayList<Vector2> getPath() {
		return resultPath;
	}
}
