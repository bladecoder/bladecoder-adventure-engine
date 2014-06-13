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

package org.bladecoder.engine.pathfinder;

import com.badlogic.gdx.utils.Array;

/** A single node in the navigation graph.
 * @author hneuer */
public class NavNode {
	/** The parent of this node, how we reached it in the search */
	public NavNode parent;
	/** The list of all adjacent neighbor nodes. */
	public final Array<NavNode> neighbors = new Array<NavNode>();
	/** Algorithm specific data. */
	protected Object algoData;
}
