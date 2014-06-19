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
package org.bladecoder.engineeditor.scneditor;

import java.util.ArrayList;

import org.bladecoder.engine.model.Actor;
import org.bladecoder.engine.model.Scene;
import org.bladecoder.engine.model.SceneCamera;
import org.bladecoder.engine.model.SpriteActor;
import org.bladecoder.engine.pathfinder.NavNode;
import org.bladecoder.engine.polygonalpathfinder.NavNodePolygonal;
import org.bladecoder.engine.util.EngineLogger;
import org.bladecoder.engineeditor.Ctx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;

public class CanvasDrawer {
	public static final float CORNER_DIST = 20;

	private static final Color MOUSESELECTION_FILL_COLOR = new Color(0.2f,
			0.2f, 0.8f, 0.4f);
	private static final Color MOUSESELECTION_STROKE_COLOR = new Color(0.2f,
			0.2f, 0.8f, 1f);

	private final ShapeRenderer drawer = new ShapeRenderer();
	private SceneCamera camera;

	public CanvasDrawer() {
	}

	public void setCamera(SceneCamera camera) {
		this.camera = camera;
	}

	public void drawBBoxActors(Scene scn) {
		drawer.setProjectionMatrix(camera.combined);
		drawer.setTransformMatrix(new Matrix4());
		drawer.begin(ShapeType.Line);
		drawer.setColor(Scene.ACTOR_BBOX_COLOR);

		for (Actor a : scn.getActors().values()) {
			Polygon p = a.getBBox();

			if (p == null) {
				EngineLogger.error("ERROR DRAWING BBOX FOR: " + a.getId());
			}

			// Rectangle r = a.getBBox().getBoundingRectangle();

			drawer.polygon(p.getTransformedVertices());
			// drawer.rect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
		}

		for (SpriteActor a : scn.getForegroundActors()) {
			drawer.polygon(a.getBBox().getTransformedVertices());
		}

		drawer.end();
	}

	public void drawBBoxWalkZone(Scene scn, boolean lineOfSight) {

		if (scn.getPolygonalNavGraph() != null) {
			drawer.setProjectionMatrix(camera.combined);
			drawer.setTransformMatrix(new Matrix4());
			drawer.begin(ShapeType.Line);
			
			drawer.setColor(Scene.WALKZONE_COLOR);
			drawer.polygon(scn.getPolygonalNavGraph().getWalkZone()
					.getTransformedVertices());

			ArrayList<Polygon> obstacles = scn.getPolygonalNavGraph()
					.getObstacles();

			drawer.setColor(Scene.OBSTACLE_COLOR);
			for (Polygon p : obstacles) {
				drawer.polygon(p.getTransformedVertices());
			}

			// DRAW LINEs OF SIGHT
			if (lineOfSight) {
				drawer.setColor(Color.WHITE);
				ArrayList<NavNodePolygonal> nodes = scn.getPolygonalNavGraph()
						.getGraphNodes();
				for (NavNodePolygonal n : nodes) {
					for (NavNode n2 : n.neighbors) {
						drawer.line(n.x, n.y, ((NavNodePolygonal) n2).x,
								((NavNodePolygonal) n2).y);
					}
				}
			}
			drawer.end();
		}
	}

	public void drawSelectedActor(Actor selectedActor) {
		// Gdx.gl20.glLineWidth(3);
		Gdx.gl20.glEnable(GL20.GL_BLEND);
		// Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

		Rectangle rect = selectedActor.getBBox().getBoundingRectangle();
		drawer.setProjectionMatrix(camera.combined);
		drawer.setTransformMatrix(new Matrix4());

		drawer.begin(ShapeRenderer.ShapeType.Filled);
		drawer.setColor(MOUSESELECTION_FILL_COLOR);
		drawer.rect(rect.x, rect.y, rect.width, rect.height);
		drawer.end();

		drawer.begin(ShapeRenderer.ShapeType.Line);
		drawer.setColor(MOUSESELECTION_STROKE_COLOR);
		drawer.rect(rect.x, rect.y, rect.width, rect.height);

		// DRAW SELECTION BOUNDS
		if (!(selectedActor instanceof SpriteActor)
				|| !((SpriteActor) selectedActor).isBboxFromRenderer()) {
			float verts[] = selectedActor.getBBox().getTransformedVertices();
			for (int i = 0; i < verts.length; i += 2)
				drawer.rect(verts[i] - CORNER_DIST / 2, verts[i + 1]
						- CORNER_DIST / 2, CORNER_DIST, CORNER_DIST);
		}

		drawer.end();
	}

	public void drawBGBounds() {
		// Gdx.gl20.glEnable(GL20.GL_BLEND);
		// Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

		drawer.setProjectionMatrix(camera.combined);
		drawer.setTransformMatrix(new Matrix4());

		drawer.begin(ShapeRenderer.ShapeType.Line);
		drawer.setColor(Color.MAGENTA);
		drawer.rect(0, 0, Ctx.project.getWorld().getWidth(), Ctx.project
				.getWorld().getHeight());
		drawer.end();
	}

	public void drawPolygonVertices(Polygon p, Color c) {
		float verts[] = p.getTransformedVertices();

		drawer.setProjectionMatrix(camera.combined);
		drawer.setTransformMatrix(new Matrix4());

		drawer.begin(ShapeRenderer.ShapeType.Line);
		drawer.setColor(c);

		for (int i = 0; i < verts.length; i += 2)
			drawer.rect(verts[i] - CORNER_DIST / 2, verts[i + 1] - CORNER_DIST
					/ 2, CORNER_DIST, CORNER_DIST);

		drawer.end();
	}
}
