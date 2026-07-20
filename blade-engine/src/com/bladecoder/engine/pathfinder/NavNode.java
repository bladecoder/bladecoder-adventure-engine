package com.bladecoder.engine.pathfinder;

import com.badlogic.gdx.utils.Array;

/** A single node in the navigation graph. It contains an array of neighbor nodes, that way it is possible to build up arbitrary
 * navigation graphs, e.g. a tiled map with no diagonal movement may contain up to 4 neighbors for each node.
 * @author hneuer */
public class NavNode<N extends NavNode<?>> {
	/** The parent of this node, how we reached it in the search */
	public N parent;
	/** The list of all adjacent neighbor nodes. */
	public final Array<N> neighbors = new Array<N>();
	/** Algorithm specific data. */
	protected Object algoData;
}
