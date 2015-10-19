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
package com.bladecoder.engineeditor.scneditor;

import java.io.IOException;

import javax.xml.transform.TransformerException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.model.AnchorActor;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.util.PolygonUtils;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.undo.UndoOp;

public class ScnWidgetInputListener extends ClickListener {
	private final ScnWidget scnWidget;

	private static enum DraggingModes {
		NONE, DRAGGING_ACTOR, DRAGGING_BBOX_POINT, DRAGGING_WALKZONE, DRAGGING_WALKZONE_POINT, DRAGGING_MARKER_0, DRAGGING_MARKER_100
	};

	private DraggingModes draggingMode = DraggingModes.NONE;
	private BaseActor selActor = null;
	private Vector2 org = new Vector2();
	private Vector2 undoOrg = new Vector2();
	private int vertIndex;

	public ScnWidgetInputListener(ScnWidget w) {
		this.scnWidget = w;
	}

	@Override
	public void clicked(InputEvent event, float x, float y) {
		Scene scn = scnWidget.getScene();
		if (scn == null)
			return;

		Vector2 p = new Vector2(Gdx.input.getX(), Gdx.input.getY());
		scnWidget.screenToWorldCoords(p);


		// DOUBLE CLICK TO CREATE OR DELETE POINTS
		if (getTapCount() == 2) {
			// Check WALKZONE
			if (scn.getPolygonalNavGraph() != null && scnWidget.getShowWalkZone()) {
				Polygon poly = scn.getPolygonalNavGraph().getWalkZone();

				if (Gdx.input.isKeyPressed(Keys.CONTROL_LEFT)) {
					// Delete the point if selected
					boolean deleted = PolygonUtils.deletePoint(poly, p.x, p.y, CanvasDrawer.CORNER_DIST);

					if (deleted) {
						Ctx.project.getSelectedChapter().setWalkZonePolygon(Ctx.project.getSelectedChapter().getSceneById(Ctx.project.getSelectedScene().getId()), poly);
						return;
					}

				} else {

					boolean created = PolygonUtils.addClampPointIfTolerance(poly, p.x, p.y, CanvasDrawer.CORNER_DIST);

					if (created) {
						Ctx.project.getSelectedChapter().setWalkZonePolygon(Ctx.project.getSelectedChapter().getSceneById(Ctx.project.getSelectedScene().getId()), poly);
						return;
					}
				}
			}

			if (scnWidget.getSelectedActor() != null) {

				Polygon poly = scnWidget.getSelectedActor().getBBox();

				if ((!(scnWidget.getSelectedActor() instanceof SpriteActor)
						|| !((SpriteActor) scnWidget.getSelectedActor()).isBboxFromRenderer()) &&
						!(scnWidget.getSelectedActor() instanceof AnchorActor)
						) {
					if (Gdx.input.isKeyPressed(Keys.CONTROL_LEFT)) {

						// Delete the point if selected
						boolean deleted = PolygonUtils.deletePoint(poly, p.x, p.y, CanvasDrawer.CORNER_DIST);

						if (deleted) {
							Ctx.project.getSelectedChapter().setBbox(Ctx.project.getSelectedChapter().getActor(
									Ctx.project.getSelectedChapter().getSceneById(Ctx.project.getSelectedScene().getId()),
									Ctx.project.getSelectedActor().getId()), poly);
							return;
						}
					} else {
						boolean created = PolygonUtils.addClampPointIfTolerance(poly, p.x, p.y,
								CanvasDrawer.CORNER_DIST);

						if (created) {
							Ctx.project.getSelectedChapter().setBbox(Ctx.project.getSelectedChapter().getActor(
									Ctx.project.getSelectedChapter().getSceneById(Ctx.project.getSelectedScene().getId()),
									Ctx.project.getSelectedActor().getId()), poly);
							return;
						}
					}
				}
			}
		}
	}

	@Override
	public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {

		super.touchDown(event, x, y, pointer, button);
//		EditorLogger.debug("Touch Down - X: " + x + " Y: " + y);

		Scene scn = scnWidget.getScene();
		if (scn == null)
			return false;

		Vector2 p = new Vector2(Gdx.input.getX(), Gdx.input.getY());
		scnWidget.screenToWorldCoords(p);
		org.set(p);

		if (button == Buttons.LEFT) {
			selActor = scnWidget.getSelectedActor();

			if (scn.getPolygonalNavGraph() != null && scnWidget.getShowWalkZone()) { // Check
																						// WALKZONE

				// CHECK WALKZONE VERTEXS
				Polygon wzPoly = scn.getPolygonalNavGraph().getWalkZone();
				float verts[] = wzPoly.getTransformedVertices();

				for (int i = 0; i < verts.length; i += 2) {
					if (p.dst(verts[i], verts[i + 1]) < CanvasDrawer.CORNER_DIST) {
						draggingMode = DraggingModes.DRAGGING_WALKZONE_POINT;
						vertIndex = i;
						return true;
					}
				}


				// CHECK FOR WALKZONE DRAGGING
				if (wzPoly.contains(p.x, p.y)) {
					draggingMode = DraggingModes.DRAGGING_WALKZONE;
					return true;
				}

			}

			// SELACTOR VERTEXs DRAGGING
			if (selActor != null
					&& (!(selActor instanceof SpriteActor) || !((SpriteActor) selActor).isBboxFromRenderer()) &&
					!(scnWidget.getSelectedActor() instanceof AnchorActor)) {

				Polygon bbox = selActor.getBBox();
				float verts[] = bbox.getTransformedVertices();
				for (int i = 0; i < verts.length; i += 2) {
					if (p.dst(verts[i], verts[i + 1]) < CanvasDrawer.CORNER_DIST) {
						draggingMode = DraggingModes.DRAGGING_BBOX_POINT;
						vertIndex = i;
						return true;
					}
				}
			}

			BaseActor a = scn.getActorAt(p.x, p.y); // CHECK FOR ACTORS

			if (a != null && a != selActor) {

				selActor = a;
				BaseActor da = Ctx.project.getActor(selActor.getId());
				Ctx.project.setSelectedActor(da);

				draggingMode = DraggingModes.DRAGGING_ACTOR;
				undoOrg.set(selActor.getX(), selActor.getY());
				return true;
			}

			if (a != null) {
				draggingMode = DraggingModes.DRAGGING_ACTOR;
				undoOrg.set(selActor.getX(), selActor.getY());
				return true;
			}

			// CHECK FOR DRAGGING DEPTH MARKERS
			Vector2 depthVector = scnWidget.getScene().getDepthVector();
			if (depthVector != null) {
				p.set(0, depthVector.x);
				scnWidget.worldToScreenCoords(p);
				if (Vector2.dst(p.x - 40, p.y, x, y) < 50) {
					draggingMode = DraggingModes.DRAGGING_MARKER_0;
					return true;
				}

				p.set(0, depthVector.y);
				scnWidget.worldToScreenCoords(p);
				if (Vector2.dst(p.x - 40, p.y, x, y) < 50) {
					draggingMode = DraggingModes.DRAGGING_MARKER_100;
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

			if (draggingMode == DraggingModes.DRAGGING_ACTOR) {
				Polygon p = selActor.getBBox();
				p.translate(d.x, d.y);
				Ctx.project.getSelectedChapter().setPos(Ctx.project.getSelectedChapter().getActor(
						Ctx.project.getSelectedChapter().getSceneById(Ctx.project.getSelectedScene().getId()),
						Ctx.project.getSelectedActor().getId()),
						new Vector2(selActor.getX(), selActor.getY()));

			} else if (draggingMode == DraggingModes.DRAGGING_BBOX_POINT) {
				Polygon poly = selActor.getBBox();

				float verts[] = poly.getVertices();
				verts[vertIndex] += d.x;
				verts[vertIndex + 1] += d.y;
				poly.dirty();

				Ctx.project.getSelectedChapter().setBbox(Ctx.project.getSelectedChapter().getActor(
						Ctx.project.getSelectedChapter().getSceneById(Ctx.project.getSelectedScene().getId()),
						Ctx.project.getSelectedActor().getId()), poly);
			} else if (draggingMode == DraggingModes.DRAGGING_WALKZONE_POINT) {
				Polygon poly = scn.getPolygonalNavGraph().getWalkZone();

				float verts[] = poly.getVertices();
				verts[vertIndex] += d.x;
				verts[vertIndex + 1] += d.y;
				poly.dirty();

				Ctx.project.getSelectedChapter().setWalkZonePolygon(Ctx.project.getSelectedChapter().getSceneById(Ctx.project.getSelectedScene().getId()), poly);
			} else if (draggingMode == DraggingModes.DRAGGING_WALKZONE) {
				Polygon poly = scn.getPolygonalNavGraph().getWalkZone();
				poly.translate(d.x, d.y);
				Ctx.project.getSelectedChapter().setWalkZonePolygon(Ctx.project.getSelectedChapter().getSceneById(Ctx.project.getSelectedScene().getId()), poly);
			} else if (draggingMode == DraggingModes.DRAGGING_MARKER_0) {
				Vector2 depthVector = scnWidget.getScene().getDepthVector();

				depthVector.x += d.y;

				Ctx.project.getSelectedChapter().getSceneById(Ctx.project.getSelectedScene().getId()).setAttribute("depth_vector", Param.toStringParam(depthVector));
				Ctx.project.getSelectedChapter().setModified();
				updateFakeDepth();
			} else if (draggingMode == DraggingModes.DRAGGING_MARKER_100) {
				Vector2 depthVector = scnWidget.getScene().getDepthVector();

				depthVector.y += d.y;
				Ctx.project.getSelectedChapter().getSceneById(Ctx.project.getSelectedScene().getId()).setAttribute("depth_vector", Param.toStringParam(depthVector));
				Ctx.project.getSelectedChapter().setModified();
				updateFakeDepth();
			}

		} else if (Gdx.input.isButtonPressed(Buttons.RIGHT) || Gdx.input.isButtonPressed(Buttons.MIDDLE)) {

			Vector2 p = new Vector2(Gdx.input.getX(), Gdx.input.getY());
			scnWidget.screenToWorldCoords(p);
			p.sub(org);

			scnWidget.translate(p);
		}
	}

	private void updateFakeDepth() {
		Scene scn = scnWidget.getScene();
		for (BaseActor a : scn.getActors().values()) {
			if (a instanceof SpriteActor) {
				a.setPosition(a.getX(), a.getY());
			}
		}
	}

	@Override
	public boolean scrolled(InputEvent event, float x, float y, int amount) {
		super.scrolled(event, x, y, amount);
		// EditorLogger.debug("SCROLLED - X: " + x + " Y: " + y);

		scnWidget.zoom(amount);
		return true;
	}

	@Override
	public void touchUp(InputEvent event, float x, float y, int pointer, int button) {

		super.touchUp(event, x, y, pointer, button);
//		EditorLogger.debug("Touch Up - X: " + x + " Y: " + y);

		if (draggingMode == DraggingModes.DRAGGING_ACTOR) {
//			UndoOp undoOp = new UndoSetAttr(Ctx.project.getSelectedChapter(), Ctx.project.getSelectedActor(), "pos",
//					Param.toStringParam(undoOrg));
//			Ctx.project.getUndoStack().add(undoOp);
		}

		draggingMode = DraggingModes.NONE;

		return;
	}
	
	@Override
	public boolean keyUp (InputEvent event, int keycode) {
		switch (keycode) {
		case Keys.UP:
		case Keys.DOWN:
		case Keys.LEFT:
		case Keys.RIGHT:
//			UndoOp undoOp = new UndoSetAttr(Ctx.project.getSelectedChapter(), Ctx.project.getSelectedActor(), "pos",
//					Param.toStringParam(undoOrg));
//			Ctx.project.getUndoStack().add(undoOp);
		}
		
		return false;
	}

	@Override
	public boolean keyDown(InputEvent event, int keycode) {
		super.keyDown(event, keycode);
		Polygon p = null;
		UndoOp undoOp = null;

		switch (keycode) {

		case Keys.ENTER:
			break;
		case Keys.BACKSPACE:
			break;
		case Keys.Z:
			if (Gdx.input.isKeyPressed(Keys.CONTROL_LEFT)) {
				Ctx.project.getUndoStack().undo();
			}
			break;
			
		case Keys.FORWARD_DEL:
			if(Ctx.project.getSelectedActor() == null)
				return false;
//			undoOp = new UndoDeleteElement(Ctx.project.getSelectedChapter(), Ctx.project.getSelectedActor());
//			Ctx.project.getUndoStack().add(undoOp);
//			Ctx.project.getSelectedChapter().deleteElement(Ctx.project.getSelectedActor());
			break;
			
		case Keys.S:
			if (Gdx.input.isKeyPressed(Keys.CONTROL_LEFT)) {
				try {
					Ctx.project.saveProject();
				} catch (TransformerException | IOException e1) {
					String msg = "Something went wrong while saving the actor.\n\n"
							+ e1.getClass().getSimpleName()
							+ " - "
							+ e1.getMessage();
					Ctx.msg.show(scnWidget.getStage(), msg, 4);

					e1.printStackTrace();
				}
			}
			break;
			
		case Keys.UP:
		case Keys.DOWN:
		case Keys.LEFT:
		case Keys.RIGHT:
			selActor = scnWidget.getSelectedActor();
			p = selActor.getBBox();
			undoOrg.set(p.getX(), p.getY());
			break;
//
//		case Keys.DOWN:
//			selActor = scnWidget.getSelectedActor();
//			p = selActor.getBBox();
//			undoOrg.set(p.getX(), p.getY());
//			p.translate(0, -1);
//			Ctx.project.getSelectedChapter().setPos(Ctx.project.getSelectedActor(),
//					new Vector2(selActor.getX(), selActor.getY()));
//			undoOp = new UndoSetAttr(Ctx.project.getSelectedChapter(), Ctx.project.getSelectedActor(), "pos",
//					Param.toStringParam(undoOrg));
//			Ctx.project.getUndoStack().add(undoOp);
//			break;
//
//		case Keys.LEFT:
//			selActor = scnWidget.getSelectedActor();
//			p = selActor.getBBox();
//			undoOrg.set(p.getX(), p.getY());
//			p.translate(-1, 0);
//			Ctx.project.getSelectedChapter().setPos(Ctx.project.getSelectedActor(),
//					new Vector2(selActor.getX(), selActor.getY()));
//			undoOp = new UndoSetAttr(Ctx.project.getSelectedChapter(), Ctx.project.getSelectedActor(), "pos",
//					Param.toStringParam(undoOrg));
//			Ctx.project.getUndoStack().add(undoOp);
//			break;
//
//		case Keys.RIGHT:
//			selActor = scnWidget.getSelectedActor();
//			p = selActor.getBBox();
//			undoOrg.set(p.getX(), p.getY());
//			p.translate(1, 0);
//			Ctx.project.getSelectedChapter().setPos(Ctx.project.getSelectedActor(),
//					new Vector2(selActor.getX(), selActor.getY()));
//			undoOp = new UndoSetAttr(Ctx.project.getSelectedChapter(), Ctx.project.getSelectedActor(), "pos",
//					Param.toStringParam(undoOrg));
//			Ctx.project.getUndoStack().add(undoOp);
//			break;			
			
		}

		return false;
	}

	@Override
	public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
		super.enter(event, x, y, pointer, fromActor);
		// EditorLogger.debug("ENTER - X: " + x + " Y: " + y);
		scnWidget.getStage().setScrollFocus(scnWidget);
	}

}
