
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
