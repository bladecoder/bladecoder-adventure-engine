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

package com.bladecoder.engine.polygonalpathfinder;

import com.bladecoder.engine.pathfinder.NavNode;

/** 
 * Implementation of a navigation node for a polygonal map
 * 
 * @author rgarcia 
 */
public class NavNodePolygonal extends NavNode {
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
