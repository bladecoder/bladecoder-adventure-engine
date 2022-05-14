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
import com.badlogic.gdx.scenes.scene2d.utils.UIUtils;
import com.bladecoder.engine.model.AnchorActor;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.ObstacleActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.SceneLayer;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.WalkZoneActor;
import com.bladecoder.engine.util.PolygonUtils;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.EditorLogger;
import com.bladecoder.engineeditor.common.Message;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.undo.UndoBboxPointPos;
import com.bladecoder.engineeditor.undo.UndoDeleteActor;
import com.bladecoder.engineeditor.undo.UndoDepthVector;
import com.bladecoder.engineeditor.undo.UndoPosition;
import com.bladecoder.engineeditor.undo.UndoRefPosition;
import com.bladecoder.engineeditor.undo.UndoRotation;
import com.bladecoder.engineeditor.undo.UndoScale;

public class ScnWidgetInputListener extends ClickListener {
	private final ScnWidget scnWidget;

	public static enum DraggingModes {
		NONE, DRAGGING_ACTOR, DRAGGING_BBOX_POINT, DRAGGING_MARKER_0, DRAGGING_MARKER_100, DRAGGING_REFPOINT,
		ROTATE_ACTOR, SCALE_LOCK_ACTOR, SCALE_ACTOR
	};

	private DraggingModes draggingMode = DraggingModes.NONE;
	private BaseActor selActor = null;
	private Vector2 org = new Vector2();
	private Vector2 undoOrg = new Vector2();
	private float undoRot = 0;
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
		if (getTapCount() == 2 && scnWidget.getSelectedActor() != null) {

			Polygon poly = scnWidget.getSelectedActor().getBBox();

			if ((!(scnWidget.getSelectedActor() instanceof SpriteActor)
					|| !((SpriteActor) scnWidget.getSelectedActor()).isBboxFromRenderer())
					&& !(scnWidget.getSelectedActor() instanceof AnchorActor)) {
				if (UIUtils.ctrl()) {

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
		}
	}

	@Override
	public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {

		super.touchDown(event, x, y, pointer, button);
		// EditorLogger.debug("Touch Down - X: " + x + " Y: " + y);

		Scene scn = scnWidget.getScene();
		if (scn == null)
			return false;

		Vector2 p = new Vector2(Gdx.input.getX(), Gdx.input.getY());
		scnWidget.screenToWorldCoords(p);
		org.set(p);

		if (button == Buttons.LEFT) {
			selActor = scnWidget.getSelectedActor();

			if (selActor != null) {

				// ORIGIN DRAGGING
				if (selActor instanceof InteractiveActor) {
					Vector2 refPoint = ((InteractiveActor) selActor).getRefPoint();

					float orgX = selActor.getX() + refPoint.x;
					float orgY = selActor.getY() + refPoint.y;

					float dst = Vector2.dst(p.x, p.y, orgX, orgY);

					if (dst < Scene.ANCHOR_RADIUS) {
						draggingMode = DraggingModes.DRAGGING_REFPOINT;
						undoOrg.set(refPoint.x, refPoint.y);
						return true;
					}
				}

				// VERTEXs DRAGGING
				if ((!(selActor instanceof SpriteActor) || !((SpriteActor) selActor).isBboxFromRenderer())
						&& !(scnWidget.getSelectedActor() instanceof AnchorActor)) {

					Polygon bbox = selActor.getBBox();
					float verts[] = bbox.getTransformedVertices();
					for (int i = 0; i < verts.length; i += 2) {
						if (p.dst(verts[i], verts[i + 1]) < CanvasDrawer.CORNER_DIST) {
							draggingMode = DraggingModes.DRAGGING_BBOX_POINT;
							vertIndex = i;

							float v[] = bbox.getVertices();
							undoOrg.set(v[i], v[i + 1]);
							return true;
						}
					}
				}

				// CLICK IN MOVE ICON
				if (!(selActor instanceof AnchorActor)
						&& scnWidget.inTransformIcon(p.x, p.y, DraggingModes.DRAGGING_ACTOR)) {
					draggingMode = DraggingModes.DRAGGING_ACTOR;
					undoOrg.set(selActor.getX(), selActor.getY());
					return true;
				}

				if (selActor instanceof AnchorActor) {
					float orgX = selActor.getX();
					float orgY = selActor.getY();

					float dst = Vector2.dst(p.x, p.y, orgX, orgY);

					if (dst < Scene.ANCHOR_RADIUS) {
						draggingMode = DraggingModes.DRAGGING_ACTOR;
						undoOrg.set(selActor.getX(), selActor.getY());
						return true;
					}
				}

				// CHECK CLICK IN TRANSFORM ICON
				if (selActor instanceof SpriteActor) {
					if (scnWidget.inTransformIcon(p.x, p.y, DraggingModes.ROTATE_ACTOR)) {
						draggingMode = DraggingModes.ROTATE_ACTOR;
						undoRot = ((SpriteActor) selActor).getRot();
						return true;
					}

					if (!((SpriteActor) selActor).getFakeDepth()) {
						if (scnWidget.inTransformIcon(p.x, p.y, DraggingModes.SCALE_ACTOR)) {
							draggingMode = DraggingModes.SCALE_ACTOR;
							undoOrg.set(((SpriteActor) selActor).getScaleX(), ((SpriteActor) selActor).getScaleY());
							return true;
						}

						if (scnWidget.inTransformIcon(p.x, p.y, DraggingModes.SCALE_LOCK_ACTOR)) {
							draggingMode = DraggingModes.SCALE_LOCK_ACTOR;
							undoOrg.set(((SpriteActor) selActor).getScaleX(), ((SpriteActor) selActor).getScaleY());
							return true;
						}
					}
				}
			}

			// CHECK FOR CLICK INSIDE ACTOR TO CHANGE SELECTION
			BaseActor a = getActorAt(scn, p.x, p.y);

			if (a != null && a != selActor) {
				selActor = a;
				BaseActor da = Ctx.project.getActor(selActor.getId());
				Ctx.project.setSelectedActor(da);

				return true;
			}

			// if (a != null) {
			// draggingMode = DraggingModes.DRAGGING_ACTOR;
			// undoOrg.set(selActor.getX(), selActor.getY());
			// return true;
			// }

			// CHECK FOR DRAGGING DEPTH MARKERS
			Vector2 depthVector = scnWidget.getScene().getDepthVector();
			if (depthVector != null) {
				p.set(0, depthVector.x);
				scnWidget.worldToScreenCoords(p);
				if (Vector2.dst(p.x - 40, p.y, x, y) < 50) {
					draggingMode = DraggingModes.DRAGGING_MARKER_0;

					Vector2 dv = scnWidget.getScene().getDepthVector();
					undoOrg.set(dv.x, dv.y);
					return true;
				}

				p.set(0, depthVector.y);
				scnWidget.worldToScreenCoords(p);
				if (Vector2.dst(p.x - 40, p.y, x, y) < 50) {
					draggingMode = DraggingModes.DRAGGING_MARKER_100;
					Vector2 dv = scnWidget.getScene().getDepthVector();
					undoOrg.set(dv.x, dv.y);
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

			float angle = d.angle(org);

			d.sub(org);
			org.add(d);

			if (draggingMode == DraggingModes.DRAGGING_ACTOR) {
				selActor.setPosition(selActor.getX() + d.x, selActor.getY() + d.y);
				Ctx.project.setModified(this, Project.POSITION_PROPERTY, null, selActor);
			} else if (draggingMode == DraggingModes.ROTATE_ACTOR) {
				if (selActor instanceof SpriteActor) {
					SpriteActor sa = (SpriteActor) selActor;
					sa.setRot(sa.getRot() - angle);
					Ctx.project.setModified();
				}
			} else if (draggingMode == DraggingModes.SCALE_ACTOR) {
				if (selActor instanceof SpriteActor) {
					SpriteActor sa = (SpriteActor) selActor;

					float sx = (org.x - d.x) / org.x - 1;
					float sy = (org.y - d.y) / org.y - 1;

					sa.setScale(sa.getScaleX() + sx, sa.getScaleY() + sy);
					Ctx.project.setModified();
				}
			} else if (draggingMode == DraggingModes.SCALE_LOCK_ACTOR) {
				if (selActor instanceof SpriteActor) {
					SpriteActor sa = (SpriteActor) selActor;

					float s = (org.x - d.x) / org.x - 1;

					sa.setScale(sa.getScaleX() + s, sa.getScaleY() + s);
					Ctx.project.setModified();
				}
			} else if (draggingMode == DraggingModes.DRAGGING_REFPOINT) {
				Vector2 refPoint = ((InteractiveActor) selActor).getRefPoint();
				refPoint.add(d.x, d.y);
				Ctx.project.setModified();
			} else if (draggingMode == DraggingModes.DRAGGING_BBOX_POINT) {
				Polygon poly = selActor.getBBox();

				float verts[] = poly.getVertices();
				verts[vertIndex] += d.x;
				verts[vertIndex + 1] += d.y;
				poly.dirty();

				Ctx.project.setModified();
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
	public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
		super.scrolled(event, x, y, amountX, amountY);
		// EditorLogger.debug("SCROLLED - X: " + x + " Y: " + y);

		if(Math.abs(amountY) < 1)
			amountY *= 10;

		scnWidget.zoom((int) amountY);
		return true;
	}

	@Override
	public void touchUp(InputEvent event, float x, float y, int pointer, int button) {

		super.touchUp(event, x, y, pointer, button);
		// EditorLogger.debug("Touch Up - X: " + x + " Y: " + y);

		if (draggingMode == DraggingModes.DRAGGING_ACTOR) {
			Ctx.project.getUndoStack().add(new UndoPosition(selActor, new Vector2(undoOrg)));
		} else if (draggingMode == DraggingModes.ROTATE_ACTOR) {
			Ctx.project.getUndoStack().add(new UndoRotation((SpriteActor) selActor, undoRot));
		} else if (draggingMode == DraggingModes.SCALE_ACTOR || draggingMode == DraggingModes.SCALE_LOCK_ACTOR) {
			Ctx.project.getUndoStack().add(new UndoScale((SpriteActor) selActor, new Vector2(undoOrg)));
		} else if (draggingMode == DraggingModes.DRAGGING_REFPOINT) {
			Ctx.project.getUndoStack().add(new UndoRefPosition((InteractiveActor) selActor, new Vector2(undoOrg)));
		} else if (draggingMode == DraggingModes.DRAGGING_BBOX_POINT) {
			Ctx.project.getUndoStack().add(new UndoBboxPointPos(selActor, vertIndex, new Vector2(undoOrg)));
		} else if (draggingMode == DraggingModes.DRAGGING_MARKER_0
				|| draggingMode == DraggingModes.DRAGGING_MARKER_100) {
			Ctx.project.getUndoStack().add(new UndoDepthVector(scnWidget.getScene(), new Vector2(undoOrg)));
		}

		draggingMode = DraggingModes.NONE;

		return;
	}

	@Override
	public boolean keyUp(InputEvent event, int keycode) {
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

		if (scnWidget.getStage() == null || scnWidget.getStage().getKeyboardFocus() != scnWidget)
			return false;

		switch (keycode) {

		case Keys.ENTER:
			break;
		case Keys.BACKSPACE:
			break;
		case Keys.Z:
			if (UIUtils.ctrl()) {
				Ctx.project.getUndoStack().undo();
			}
			break;

		case Keys.FORWARD_DEL:
			BaseActor a = Ctx.project.getSelectedActor();
			if (a == null)
				return false;
			Ctx.project.getUndoStack().add(new UndoDeleteActor(Ctx.project.getSelectedScene(), a));
			Ctx.project.getSelectedScene().removeActor(a);
			Ctx.project.setModified(this, Project.NOTIFY_ELEMENT_DELETED, null, a);
			break;

		case Keys.S:
			if (UIUtils.ctrl()) {
				try {
					Ctx.project.saveProject();
				} catch (IOException e1) {
					String msg = "Something went wrong while saving the actor.\n\n" + e1.getClass().getSimpleName()
							+ " - " + e1.getMessage();
					Message.showMsgDialog(scnWidget.getStage(), "Error", msg);

					EditorLogger.printStackTrace(e1);
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

		case Keys.PLUS:
			case 72: // '+'
			scnWidget.zoom(-1);
			break;

		case Keys.MINUS:
			case 76: // '-'
			scnWidget.zoom(1);
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
		scnWidget.getStage().setKeyboardFocus(scnWidget);
	}

	/**
	 * Returns the actor at the position. Including not interactive actors.
	 */
	private BaseActor getActorAt(Scene s, float x, float y) {

		// 1. Search for ANCHOR Actors
		for (BaseActor a : s.getActors().values()) {
			if (a instanceof AnchorActor && Ctx.project.isEditorVisible(a)) {
				float dst = Vector2.dst(x, y, a.getX(), a.getY());

				if (dst < Scene.ANCHOR_RADIUS)
					return a;
			}
		}

		// 2. Search for INTERACTIVE Actors
		for (SceneLayer layer : s.getLayers()) {

			if (!layer.isVisible())
				continue;

			// Obtain actors in reverse (close to camera)
			for (int i = layer.getActors().size() - 1; i >= 0; i--) {
				BaseActor a = layer.getActors().get(i);

				if (a.hit(x, y) && Ctx.project.isEditorVisible(a)) {
					return a;
				}
			}
		}

		// 3. Search for OBSTACLE actors
		for (BaseActor a : s.getActors().values()) {
			if (a instanceof ObstacleActor && a.hit(x, y) && Ctx.project.isEditorVisible(a)) {
				return a;
			}
		}

		// 4. Search for WALKZONE actors
		for (BaseActor a : s.getActors().values()) {
			if (a instanceof WalkZoneActor && a.hit(x, y) && Ctx.project.isEditorVisible(a)) {
				return a;
			}
		}

		return null;
	}

}
