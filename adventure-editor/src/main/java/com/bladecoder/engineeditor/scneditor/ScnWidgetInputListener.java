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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.bladecoder.engine.model.AnchorActor;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.util.PolygonUtils;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.undo.UndoDeleteActor;
import com.bladecoder.engineeditor.undo.UndoPosition;
import com.bladecoder.engineeditor.undo.UndoWalkZonePosition;
import com.bladecoder.engineeditor.utils.Message;

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
						Ctx.project.setModified();
						return;
					}

				} else {

					boolean created = PolygonUtils.addClampPointIfTolerance(poly, p.x, p.y, CanvasDrawer.CORNER_DIST);

					if (created) {
						Ctx.project.setModified();
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
							Ctx.project.setModified();
							return;
						}
					} else {
						boolean created = PolygonUtils.addClampPointIfTolerance(poly, p.x, p.y,
								CanvasDrawer.CORNER_DIST);

						if (created) {
							Ctx.project.setModified();
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
					undoOrg.set(wzPoly.getX(), wzPoly.getY());
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
				
				selActor.setPosition(selActor.getX()+ d.x, selActor.getY() + d.y);
				Ctx.project.setModified(this, Project.POSITION_PROPERTY, null, selActor);

			} else if (draggingMode == DraggingModes.DRAGGING_BBOX_POINT) {
				Polygon poly = selActor.getBBox();

				float verts[] = poly.getVertices();
				verts[vertIndex] += d.x;
				verts[vertIndex + 1] += d.y;
				poly.dirty();

				Ctx.project.setModified();
			} else if (draggingMode == DraggingModes.DRAGGING_WALKZONE_POINT) {
				Polygon poly = scn.getPolygonalNavGraph().getWalkZone();

				float verts[] = poly.getVertices();
				verts[vertIndex] += d.x;
				verts[vertIndex + 1] += d.y;
				poly.dirty();

				Ctx.project.setModified();
			} else if (draggingMode == DraggingModes.DRAGGING_WALKZONE) {
				Polygon poly = scn.getPolygonalNavGraph().getWalkZone();
				poly.translate(d.x, d.y);
			} else if (draggingMode == DraggingModes.DRAGGING_MARKER_0) {
				Vector2 depthVector = scnWidget.getScene().getDepthVector();

				depthVector.x += d.y;
				Ctx.project.setModified();
				updateFakeDepth();
			} else if (draggingMode == DraggingModes.DRAGGING_MARKER_100) {
				Vector2 depthVector = scnWidget.getScene().getDepthVector();

				depthVector.y += d.y;
				Ctx.project.setModified();
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
			Ctx.project.getUndoStack().add(new UndoPosition(selActor, new Vector2(undoOrg)));
		} else if (draggingMode == DraggingModes.DRAGGING_WALKZONE) {
			Ctx.project.getUndoStack().add(new UndoWalkZonePosition(scnWidget.getScene().getPolygonalNavGraph().getWalkZone(), new Vector2(undoOrg)));
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
			Ctx.project.getUndoStack().add(new UndoPosition(selActor, new Vector2(undoOrg)));
		}
		
		return false;
	}

	@Override
	public boolean keyDown(InputEvent event, int keycode) {
		super.keyDown(event, keycode);
		Polygon p = null;

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
			BaseActor a = Ctx.project.getSelectedActor();
			if( a == null)
				return false;
			Ctx.project.getUndoStack().add(new UndoDeleteActor(Ctx.project.getSelectedScene(), a));
			Ctx.project.getSelectedScene().removeActor(a);
			Ctx.project.setModified(this, Project.NOTIFY_ELEMENT_DELETED, null, a);
			break;
			
		case Keys.S:
			if (Gdx.input.isKeyPressed(Keys.CONTROL_LEFT)) {
				try {
					Ctx.project.saveProject();
				} catch (IOException e1) {
					String msg = "Something went wrong while saving the actor.\n\n"
							+ e1.getClass().getSimpleName()
							+ " - "
							+ e1.getMessage();
					Message.showMsgDialog(scnWidget.getStage(), "Error", msg);

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
			
			Ctx.project.setModified(this, Project.POSITION_PROPERTY, null, selActor);
			break;
			
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
