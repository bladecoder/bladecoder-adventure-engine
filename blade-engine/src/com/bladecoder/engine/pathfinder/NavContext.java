package com.bladecoder.engine.pathfinder;

/** The context describing the current path finding state
 * <p>
 * Original implementation by Kevin Glass from Slick2D.
 * </p>
 * @author hneuer */
public interface NavContext<N extends NavNode<N>> {
	/** Get the object being moved along the path if any */
    Object getMover();

	/** Get the source node */
    N getSourceNode();

	/** Get the distance that has been searched to reach this point */
    float getSearchDistance();
}
