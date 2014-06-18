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
import org.bladecoder.engine.model.SpriteActor;
import org.bladecoder.engine.polygonalpathfinder.PolygonalNavGraph;
import org.bladecoder.engine.util.PolygonUtils;
import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.utils.EditorLogger;
import org.w3c.dom.Element;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class ScnWidgetInputListener extends ClickListener {
	private final ScnWidget scnWidget;

	private static enum DraggingModes {
		NONE, DRAGING_ACTOR, DRAGING_BBOX_POINT, DRAGING_WALKZONE, DRAGING_WALKZONE_POINT, DRAGING_OBSTACLE, DRAGING_OBSTACLE_POINT
	};

	private DraggingModes draggingMode = DraggingModes.NONE;
	private Actor selActor = null;
	private Vector2 org = new Vector2();
	private int vertIndex;
	private Polygon selPolygon = null;
	private int selObstacleIndex = 0;

	private boolean deleteObstacle = false;

	public ScnWidgetInputListener(ScnWidget w) {
		this.scnWidget = w;
	}

	public void setDeleteObstacle(boolean v) {
		Ctx.msg.show(scnWidget.getStage(), "Select Obstacle to Delete");
		deleteObstacle = v;
	}

	@Override
	public void clicked(InputEvent event, float x, float y) {
		Scene scn = scnWidget.getScene();
		if (scn == null)
			return;

		Vector2 p = new Vector2(Gdx.input.getX(), Gdx.input.getY());
		scnWidget.screenToWorldCoords(p);

		if (deleteObstacle) {
			deleteObstacle = false;

			Ctx.msg.hide();
			PolygonalNavGraph pf = scn.getPolygonalNavGraph();
			ArrayList<Polygon> obstacles = scn.getPolygonalNavGraph()
					.getObstacles();

			// SEARCH FOR OBSTACLE
			for (int j = 0; j < obstacles.size(); j++) {
				Polygon o = obstacles.get(j);
				if (o.contains(p.x, p.y)) {
					Ctx.project.getSelectedChapter().deleteObstacle(
							Ctx.project.getSelectedScene(), j);
					pf.getObstacles().remove(j);
					return;
				}
			}
		}

		// DOUBLE CLICK TO CREATE OR DELETE POINTS
		if (getTapCount() == 2) {
			Polygon poly = scnWidget.getSelectedActor().getBBox();

			if (!(scnWidget.getSelectedActor() instanceof SpriteActor)
					|| !((SpriteActor) scnWidget.getSelectedActor()).isBboxFromRenderer()) {
				if (Gdx.input.isKeyPressed(Keys.CONTROL_LEFT)) {

					// Delete the point if selected
					boolean deleted = PolygonUtils.deletePoint(poly, p.x, p.y,
							CanvasDrawer.CORNER_DIST);

					if (deleted) {
						Ctx.project.getSelectedChapter().setBbox(
								Ctx.project.getSelectedActor(), poly);
						return;
					}
				} else {
					boolean created = PolygonUtils.addClampPointIfTolerance(
							poly, p.x, p.y, CanvasDrawer.CORNER_DIST);

					if (created) {
						Ctx.project.getSelectedChapter().setBbox(
								Ctx.project.getSelectedActor(), poly);
						return;
					}
				}
			}

			// Check WALKZONE
			if (scn.getPolygonalNavGraph() != null) {
				poly = scn.getPolygonalNavGraph().getWalkZone();
				ArrayList<Polygon> obstacles = scn.getPolygonalNavGraph()
						.getObstacles();

				if (Gdx.input.isKeyPressed(Keys.CONTROL_LEFT)) {
					// Delete the point if selected
					boolean deleted = PolygonUtils.deletePoint(poly, p.x, p.y,
							CanvasDrawer.CORNER_DIST);

					if (deleted)
						Ctx.project.getSelectedChapter().setWalkZonePolygon(
								Ctx.project.getSelectedScene(), poly);
					else { // check obstacles
						for (int i = 0; i < obstacles.size(); i++) {
							Polygon o = obstacles.get(i);
							deleted = PolygonUtils.deletePoint(o, p.x, p.y,
									CanvasDrawer.CORNER_DIST);
							if (deleted) {
								Ctx.project.getSelectedChapter()
										.setObstaclePolygon(
												Ctx.project.getSelectedScene(),
												i, o);
								break;
							}
						}
					}

				} else {
					boolean created = PolygonUtils.addClampPointIfTolerance(
							poly, p.x, p.y, CanvasDrawer.CORNER_DIST);

					if (created) {
						Ctx.project.getSelectedChapter().setWalkZonePolygon(
								Ctx.project.getSelectedScene(), poly);
					} else {
						for (int i = 0; i < obstacles.size(); i++) {
							Polygon o = obstacles.get(i);
							created = PolygonUtils.addClampPointIfTolerance(o,
									p.x, p.y, CanvasDrawer.CORNER_DIST);
							if (created) {
								Ctx.project.getSelectedChapter()
										.setObstaclePolygon(
												Ctx.project.getSelectedScene(),
												i, o);
								break;
							}
						}
					}
				}
			}
		}
	}

	@Override
	public boolean touchDown(InputEvent event, float x, float y, int pointer,
			int button) {

		super.touchDown(event, x, y, pointer, button);
		EditorLogger.debug("Touch Down - X: " + x + " Y: " + y);

		Scene scn = scnWidget.getScene();
		if (scn == null)
			return false;

		Vector2 p = new Vector2(Gdx.input.getX(), Gdx.input.getY());
		scnWidget.screenToWorldCoords(p);
		org.set(p);

		if (button == Buttons.LEFT) {
			selActor = scnWidget.getSelectedActor();
			Actor a = scn.getFullSearchActorAt(p.x, p.y); // CHECK FOR ACTORS

			if (a != null && a != selActor) {

				selActor = a;
				Element da = Ctx.project.getActor(selActor.getId());
				Ctx.project.setSelectedActor(da);

				draggingMode = DraggingModes.DRAGING_ACTOR;
				return true;
			}

			// SELACTOR VERTEXs DRAGGING

			if (selActor!=null && (!(selActor instanceof SpriteActor)
					|| !((SpriteActor) selActor).isBboxFromRenderer())) {

				Polygon bbox = selActor.getBBox();
				float verts[] = bbox.getTransformedVertices();
				for (int i = 0; i < verts.length; i += 2) {
					if (p.dst(verts[i], verts[i + 1]) < CanvasDrawer.CORNER_DIST) {
						draggingMode = DraggingModes.DRAGING_BBOX_POINT;
						vertIndex = i;
						return true;
					}
				}
			}

			if (a != null) {
				draggingMode = DraggingModes.DRAGING_ACTOR;
				return true;
			}

			if (scn.getPolygonalNavGraph() != null) { // Check WALKZONE

				// CHECK WALKZONE VERTEXS
				Polygon wzPoly = scn.getPolygonalNavGraph().getWalkZone();
				float verts[] = wzPoly.getTransformedVertices();

				for (int i = 0; i < verts.length; i += 2) {
					if (p.dst(verts[i], verts[i + 1]) < CanvasDrawer.CORNER_DIST) {
						draggingMode = DraggingModes.DRAGING_WALKZONE_POINT;
						vertIndex = i;
						return true;
					}
				}

				// CHECK OBSTACLE VERTEXS
				ArrayList<Polygon> obstacles = scn.getPolygonalNavGraph()
						.getObstacles();

				for (int j = 0; j < obstacles.size(); j++) {
					Polygon o = obstacles.get(j);
					verts = o.getTransformedVertices();

					for (int i = 0; i < verts.length; i += 2) {
						if (p.dst(verts[i], verts[i + 1]) < CanvasDrawer.CORNER_DIST) {
							draggingMode = DraggingModes.DRAGING_OBSTACLE_POINT;
							selPolygon = o;
							vertIndex = i;
							selObstacleIndex = j;
							return true;
						}
					}
				}

				// CHECK FOR OBSTACLE DRAGGING
				for (int j = 0; j < obstacles.size(); j++) {
					Polygon o = obstacles.get(j);
					if (o.contains(p.x, p.y)) {
						draggingMode = DraggingModes.DRAGING_OBSTACLE;
						selPolygon = o;
						selObstacleIndex = j;
						return true;
					}
				}

				// CHECK FOR WALKZONE DRAGGING
				if (wzPoly.contains(p.x, p.y)) {
					draggingMode = DraggingModes.DRAGING_WALKZONE;
					return true;
				}

			}

		}

		return true;
	}

	@Override
	public void touchDragged(InputEvent event, float x, float y, int pointer) {
		// EditorLogger.debug("Touch Dragged - X: " + Gdx.input.getX() + " Y: "
		// + Gdx.input.getY());

		super.touchDragged(event, x, y, pointer);

		if (Gdx.input.isButtonPressed(Buttons.LEFT)) {
			Scene scn = scnWidget.getScene();

			if (scn == null)
				return;

			Vector2 d = new Vector2(Gdx.input.getX(), Gdx.input.getY());
			scnWidget.screenToWorldCoords(d);

			d.sub(org);
			org.add(d);

			if (draggingMode == DraggingModes.DRAGING_ACTOR) {
				Polygon p = selActor.getBBox();
				p.translate(d.x, d.y);
				Ctx.project.getSelectedChapter().setPos(
						Ctx.project.getSelectedActor(),
						new Vector2(selActor.getX(), selActor.getY()));

			} else if (draggingMode == DraggingModes.DRAGING_BBOX_POINT) {
				Polygon poly = selActor.getBBox();

				float verts[] = poly.getVertices();
				verts[vertIndex] += d.x;
				verts[vertIndex + 1] += d.y;
				poly.dirty();

				Ctx.project.getSelectedChapter().setBbox(
						Ctx.project.getSelectedActor(), poly);
			} else if (draggingMode == DraggingModes.DRAGING_WALKZONE_POINT) {
				Polygon poly = scn.getPolygonalNavGraph().getWalkZone();

				float verts[] = poly.getVertices();
				verts[vertIndex] += d.x;
				verts[vertIndex + 1] += d.y;
				poly.dirty();

				Ctx.project.getSelectedChapter().setWalkZonePolygon(
						Ctx.project.getSelectedScene(), poly);
			} else if (draggingMode == DraggingModes.DRAGING_OBSTACLE_POINT) {
				float verts[] = selPolygon.getVertices();
				verts[vertIndex] += d.x;
				verts[vertIndex + 1] += d.y;
				selPolygon.dirty();

				Ctx.project.getSelectedChapter().setObstaclePolygon(
						Ctx.project.getSelectedScene(), selObstacleIndex,
						selPolygon);
			} else if (draggingMode == DraggingModes.DRAGING_OBSTACLE) {
				selPolygon.translate(d.x, d.y);
				Ctx.project.getSelectedChapter().setObstaclePolygon(
						Ctx.project.getSelectedScene(), selObstacleIndex,
						selPolygon);
			} else if (draggingMode == DraggingModes.DRAGING_WALKZONE) {
				Polygon poly = scn.getPolygonalNavGraph().getWalkZone();
				poly.translate(d.x, d.y);
				Ctx.project.getSelectedChapter().setWalkZonePolygon(
						Ctx.project.getSelectedScene(), poly);
			}

		} else if (Gdx.input.isButtonPressed(Buttons.RIGHT)
				|| Gdx.input.isButtonPressed(Buttons.MIDDLE)) {

			Vector2 p = new Vector2(Gdx.input.getX(), Gdx.input.getY());
			scnWidget.screenToWorldCoords(p);
			p.sub(org);

			scnWidget.translate(p);
		}
	}

	@Override
	public boolean scrolled(InputEvent event, float x, float y, int amount) {
		super.scrolled(event, x, y, amount);
		EditorLogger.debug("SCROLLED - X: " + x + " Y: " + y);

		scnWidget.zoom(amount);
		return true;
	}

	@Override
	public void touchUp(InputEvent event, float x, float y, int pointer,
			int button) {

		super.touchUp(event, x, y, pointer, button);
		EditorLogger.debug("Touch Up - X: " + x + " Y: " + y);

		draggingMode = DraggingModes.NONE;

		return;
	}

	@Override
	public boolean keyDown(InputEvent event, int keycode) {
		super.keyDown(event, keycode);

		switch (keycode) {
		case Keys.ENTER:
			break;
		case Keys.BACKSPACE:
			break;
		}

		return false;
	}

	@Override
	public void enter(InputEvent event, float x, float y, int pointer,
			com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
		super.enter(event, x, y, pointer, fromActor);
		// EditorLogger.debug("ENTER - X: " + x + " Y: " + y);
		scnWidget.getStage().setScrollFocus(scnWidget);
	}

}
