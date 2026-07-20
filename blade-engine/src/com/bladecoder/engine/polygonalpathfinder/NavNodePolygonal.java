package com.bladecoder.engine.polygonalpathfinder;

import com.bladecoder.engine.pathfinder.NavNode;

/** 
 * Implementation of a navigation node for a polygonal map
 * 
 * @author rgarcia 
 */
public class NavNodePolygonal extends NavNode<NavNodePolygonal> {
	public float x;
	public float y;
	
	public float getX() {
		return x;
	}
	
	public float getY() {
		return y;
	}
	
	public NavNodePolygonal () {
		this.x = 0;
		this.y = 0;
	}
	
	public NavNodePolygonal (float x, float y) {
		this.x = x;
		this.y = y;
	}
}
