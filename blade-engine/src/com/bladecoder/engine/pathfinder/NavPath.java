package com.bladecoder.engine.pathfinder;

/** A navigation path.
 * @author hneuer */
public interface NavPath<N extends NavNode<N>> {
	/** Fills the navigation path between the start and target node.
	 * <p>
	 * Note that current implementations have to follow the path backward from the targetNode to the startNode (following the
	 * parent relation).
	 * <p> */
    void fill(N startNode, N targetNode);

	/** Returns the length of the path, i.e. the number of reached nodes. */
    int getLength();

	/** Clear the path. */
    void clear();
}
