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
package com.bladecoder.engine.util;

import com.badlogic.gdx.math.GeometryUtils;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;

public class PolygonUtils {
	private static final Vector2 tmp = new Vector2();
	private static final Vector2 tmp2 = new Vector2();

	public static void addPoint(Polygon poly, float x, float y, int index) {
		float verts[] = poly.getVertices();

		x -= poly.getX();
		y -= poly.getY();

		int length = verts.length;
		float destination[] = new float[length + 2];

		System.arraycopy(verts, 0, destination, 0, index);
		destination[index] = x;
		destination[index + 1] = y;
		System.arraycopy(verts, index, destination, index + 2, length - index);

		poly.setVertices(destination);
	}

	public static void deletePoint(Polygon poly, int index) {

		float verts[] = poly.getVertices();

		if (verts.length < 8)
			return;

		int length = verts.length;
		float destination[] = new float[length - 2];

//		index = index * 2;

		System.arraycopy(verts, 0, destination, 0, index);
		System.arraycopy(verts, index + 2, destination, index, length - index
				- 2);

		poly.setVertices(destination);
	}

	public static boolean deletePoint(Polygon poly, float x, float y,
			float tolerance) {
		float verts[] = poly.getTransformedVertices();

		for (int i = 0; i < verts.length; i += 2) {
			if (Vector2.dst(x, y, verts[i], verts[i + 1]) < tolerance) {
				deletePoint(poly, i);

				return true;
			}
		}

		return false;
	}

	/**
	 * Adds a point clamped to the edge of the polygon
	 * 
	 * @param poly
	 * @param x
	 * @param y
	 */
	public static void addClampedPoint(Polygon poly, float x, float y) {
		int i = getClampedPoint(poly, x, y, tmp2);

		addPoint(poly, tmp2.x, tmp2.y, i + 2);
	}

	/**
	 * Clamp the point to the nearest polygon segment
	 * 
	 * @param poly
	 *            The polygon
	 * @param x
	 *            The original point X
	 * @param y
	 *            The original point Y
	 * @param dest
	 *            The clamped point
	 * @return The segment where the clamped point belongs
	 */
	public static int getClampedPoint(Polygon poly, float x, float y,
			Vector2 dest) {
		float verts[] = poly.getTransformedVertices();
		float dTmp;

		Intersector.nearestSegmentPoint(verts[0], verts[1], verts[2], verts[3],
				x, y, dest);

		int nearest = 0;
		float d = Vector2.dst(x, y, dest.x, dest.y);

		for (int i = 2; i < verts.length; i += 2) {
			Intersector.nearestSegmentPoint(verts[i], verts[i + 1],
					verts[(i + 2) % verts.length],
					verts[(i + 3) % verts.length], x, y, tmp);
			dTmp = Vector2.dst(x, y, tmp.x, tmp.y);

			if (dTmp < d) {
				d = dTmp;
				nearest = i;
				dest.set(tmp);
			}
		}
		
		// ERROR CONTROL:
		// If the clamped point is not in the walkzone 
		// we search for the nearest walkzone vertex
		if (!PolygonUtils.isPointInside(poly, dest.x, dest.y, true)) {
			EngineLogger.debug("> PolygonalPathFinder: CLAMPED FAILED!!");
			
			tmp.set(verts[0], verts[1]);
			d = Vector2.dst(x, y, tmp.x, tmp.y);
			nearest = 0;
			dest.set(tmp);
			
			for (int i = 2; i < verts.length; i += 2) {
				tmp.set(verts[i], verts[i + 1]);
				dTmp = Vector2.dst(x, y, tmp.x, tmp.y);

				if (dTmp < d) {
					d = dTmp;
					nearest = i;
					dest.set(tmp);
				}
			}
		}

		return nearest;
	}

	public static boolean addClampPointIfTolerance(Polygon poly, float x,
			float y, float tolerance) {
		boolean added = false;

		int i = getClampedPoint(poly, x, y, tmp2);

		if (tmp2.dst(x, y) < tolerance) {
			added = true;
			addPoint(poly, tmp2.x, tmp2.y, i + 2);
		}

		return added;
	}

	public static boolean isVertexConcave(Polygon poly, int index) {
		float verts[] = poly.getTransformedVertices();

		float currentX = verts[index];
		float currentY = verts[index + 1];
		float nextX = verts[(index + 2) % verts.length];
		float nextY = verts[(index + 3) % verts.length];
		float previousX = verts[index == 0 ? verts.length - 2 : index - 2];
		float previousY = verts[index == 0 ? verts.length - 1 : index - 1];

		float leftX = currentX - previousX;
		float leftY = currentY - previousY;
		float rightX = nextX - currentX;
		float rightY = nextY - currentY;

		float cross = (leftX * rightY) - (leftY * rightX);

		return cross < 0;
	}

	private static float TOLERANCE_IS_POINT_INSIDE = 3f;

	public static boolean isPointInside(Polygon polygon, float x, float y,
			boolean toleranceOnOutside) {
		float verts[] = polygon.getTransformedVertices();

		boolean inside = false;

		float oldX = verts[verts.length - 2];
		float oldY = verts[verts.length - 1];

		float oldSqDist = Vector2.dst2(oldX, oldY, x, y);

		for (int i = 0; i < verts.length; i += 2) {
			float newX = verts[i];
			float newY = verts[i + 1];
			float newSqDist = Vector2.dst2(newX, newY, x, y);

			if (oldSqDist + newSqDist + 2.0f * Math.sqrt(oldSqDist * newSqDist)
					- Vector2.dst2(newX, newY, oldX, oldY) < TOLERANCE_IS_POINT_INSIDE)
				return toleranceOnOutside;

			float leftX = newX;
			float leftY = newY;
			float rightX = oldX;
			float rightY = oldY;

			if (newX > oldX) {
				leftX = oldX;
				leftY = oldY;
				rightX = newX;
				rightY = newY;
			}

			if (leftX < x
					&& x <= rightX
					&& (y - leftY) * (rightX - leftX) < (rightY - leftY)
							* (x - leftX))
				inside = !inside;

			oldX = newX;
			oldY = newY;
			oldSqDist = newSqDist;
		}

		return inside;
	}

	public static boolean inLineOfSight(Vector2 p1, Vector2 p2, Polygon polygon, boolean obstacle) {
		tmp.set(p1);
		tmp2.set(p2);

		float verts[] = polygon.getTransformedVertices();

		for (int i = 0; i < verts.length; i += 2) {
			if (lineSegmentsCross(tmp.x, tmp.y, tmp2.x, tmp2.y, verts[i],
					verts[i + 1], verts[(i + 2) % verts.length], verts[(i + 3)
							% verts.length]))
				return false;
		}

		tmp.add(tmp2);
		tmp.x /= 2;
		tmp.y /= 2;
		
		boolean result = PolygonUtils.isPointInside(polygon, tmp.x, tmp.y, !obstacle);
		
		return obstacle?!result:result;
	}

	private static float TOLERANCE_LINE_SEGMENTS_CROSS = 0.01f;
	
	public static boolean lineSegmentsCross(float ax, float ay, float bx,
			float by, float cx, float cy, float dx, float dy) {
		float denominator = ((bx - ax) * (dy - cy)) - ((by - ay) * (dx - cx));

		if (denominator < TOLERANCE_LINE_SEGMENTS_CROSS && denominator > -TOLERANCE_LINE_SEGMENTS_CROSS) {
			return false;
		}

		float numerator1 = ((ay - cy) * (dx - cx)) - ((ax - cx) * (dy - cy));

		float numerator2 = ((ay - cy) * (bx - ax)) - ((ax - cx) * (by - ay));

		if ((numerator1 < TOLERANCE_LINE_SEGMENTS_CROSS && numerator1 > -TOLERANCE_LINE_SEGMENTS_CROSS) || (numerator2 < TOLERANCE_LINE_SEGMENTS_CROSS && numerator2 > -TOLERANCE_LINE_SEGMENTS_CROSS)) {
			return false;
		}

		float r = numerator1 / denominator;
		float s = numerator2 / denominator;

		return (r > TOLERANCE_LINE_SEGMENTS_CROSS && r < 1 - TOLERANCE_LINE_SEGMENTS_CROSS) && (s > TOLERANCE_LINE_SEGMENTS_CROSS && s < 1 - TOLERANCE_LINE_SEGMENTS_CROSS);
	}
	
	static public void ensureClockWise (float[] polygon, int offset, int count) {
		if (GeometryUtils.isClockwise(polygon, offset, count)) return;
		int lastX = offset + count - 2;
		for (int i = offset, n = offset + count / 2; i < n; i += 2) {
			int other = lastX - i;
			float x = polygon[i];
			float y = polygon[i + 1];
			polygon[i] = polygon[other];
			polygon[i + 1] = polygon[other + 1];
			polygon[other] = x;
			polygon[other + 1] = y;
		}
	}
}
