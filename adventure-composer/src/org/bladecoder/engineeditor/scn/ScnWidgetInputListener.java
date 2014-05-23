package org.bladecoder.engineeditor.scn;

import org.bladecoder.engine.model.Actor;
import org.bladecoder.engine.model.Scene;
import org.bladecoder.engine.model.SpriteActor;
import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.utils.EditorLogger;
import org.w3c.dom.Element;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;

public class ScnWidgetInputListener extends InputListener {
	private final ScnWidget scnWidget;
	private final Vector2 lastTouch = new Vector2();

	private static enum DraggingModes {
		NONE, DRAGING_ACTOR, DRAGING_BBOX
	};

	private static enum Corners {
		BOTTOM_LEFT, BOTTOM_RIGHT, TOP_LEFT, TOP_RIGHT
	};

	private DraggingModes draggingMode = DraggingModes.NONE;
	private Corners corner = Corners.BOTTOM_LEFT;
	private Actor actor = null;
	private Vector2 org = new Vector2();

	public ScnWidgetInputListener(ScnWidget w) {
		this.scnWidget = w;
	}

	@Override
	public boolean touchDown(InputEvent event, float x, float y, int pointer,
			int button) {
		
		EditorLogger.debug("Touch Down - X: " + x + " Y: " + y);

		if (button == Buttons.LEFT) {

			Scene scn = scnWidget.getScene();
			if (scn == null)
				return false;
			

			Vector2 p = new Vector2(Gdx.input.getX(), Gdx.input.getY());
			scnWidget.screenToWorldCoords(p);
			actor = scn.getFullSearchActorAt(p.x, p.y);

			if (actor == null)
				return false;

			Element da = Ctx.project.getActor(actor.getId());

			if (Ctx.project.getSelectedActor() == null
					|| !actor.getId().equals(
							Ctx.project.getSelectedChapter().getId(
									Ctx.project.getSelectedActor())))
				Ctx.project.setSelectedActor(da);

			Rectangle bbox = actor.getBBox();

			if (p.dst(bbox.x, bbox.y) < CanvasDrawer.CORNER_DIST) {
				draggingMode = DraggingModes.DRAGING_BBOX;
				corner = Corners.BOTTOM_LEFT;
			} else if (p.dst(bbox.x + bbox.width, bbox.y) < CanvasDrawer.CORNER_DIST) {
				draggingMode = DraggingModes.DRAGING_BBOX;
				corner = Corners.BOTTOM_RIGHT;
			} else if (p.dst(bbox.x, bbox.y + bbox.height) < CanvasDrawer.CORNER_DIST) {
				draggingMode = DraggingModes.DRAGING_BBOX;
				corner = Corners.TOP_LEFT;
			} else if (p.dst(bbox.x + bbox.width, bbox.y + bbox.height) < CanvasDrawer.CORNER_DIST) {
				draggingMode = DraggingModes.DRAGING_BBOX;
				corner = Corners.TOP_RIGHT;
			} else {
				draggingMode = DraggingModes.DRAGING_ACTOR;
			}

			org.set(p);

		} else if (button == Buttons.RIGHT || button == Buttons.MIDDLE) {
			// PAN
			Vector2 p = new Vector2(Gdx.input.getX(), Gdx.input.getY());
			scnWidget.screenToWorldCoords(p);
			lastTouch.set(p);
		}

		return true;
	}

	@Override
	public void touchDragged(InputEvent event, float x, float y, int pointer) {
//		EditorLogger.debug("Touch Dragged - X: " + Gdx.input.getX() + " Y: " +  Gdx.input.getY());

		if (Gdx.input.isButtonPressed(Buttons.LEFT)) {
			Scene scn = scnWidget.getScene();
			
			if (scn == null)
				return;

			Vector2 d = new Vector2(Gdx.input.getX(), Gdx.input.getY());
			scnWidget.screenToWorldCoords(d);

			d.x -= org.x;
			d.y -= org.y;
			org.x += d.x;
			org.y += d.y;

			if (draggingMode == DraggingModes.DRAGING_ACTOR) {
				if (actor instanceof SpriteActor) {
					Vector2 p = ((SpriteActor) actor).getPosition();

					// ((SpriteActor) actor).setPosition(p.x + d.x, p.y + d.y);

					Ctx.project.getSelectedChapter().setPos(
							Ctx.project.getSelectedActor(),
							new Vector2(p.x + d.x, p.y + d.y));
				} else {
					Rectangle bbox = actor.getBBox(); // bbox will be an
														// inmutable
														// object
					bbox.x += d.x;
					bbox.y += d.y;
					// actor.setBbox(bbox);

					Ctx.project.getSelectedChapter().setBbox(
							Ctx.project.getSelectedActor(), bbox);
				}
			} else if (draggingMode == DraggingModes.DRAGING_BBOX) {
				Rectangle bbox = actor.getBBox(); // bbox will be an inmutable
													// object

				switch (corner) {
				case BOTTOM_LEFT:
					bbox.x += d.x;
					bbox.y += d.y;
					bbox.width -= d.x;
					bbox.height -= d.y;
					break;
				case BOTTOM_RIGHT:
					bbox.y += d.y;
					bbox.width += d.x;
					bbox.height -= d.y;
					break;
				case TOP_LEFT:
					bbox.x += d.x;
					bbox.width -= d.x;
					bbox.height += d.y;
					break;
				case TOP_RIGHT:
					bbox.width += d.x;
					bbox.height += d.y;
					break;
				default:
					break;
				}

				// actor.setBbox(bbox);
				if (actor instanceof SpriteActor) {
					Vector2 pos = ((SpriteActor) actor).getPosition();
					bbox.x -= pos.x;
					bbox.y -= pos.y;
				}

				Ctx.project.getSelectedChapter().setBbox(
						Ctx.project.getSelectedActor(), bbox);
			}

		} else if (Gdx.input.isButtonPressed(Buttons.RIGHT)
				|| Gdx.input.isButtonPressed(Buttons.MIDDLE)) {
			

			Vector2 p = new Vector2(Gdx.input.getX(), Gdx.input.getY());
			scnWidget.screenToWorldCoords(p);
			Vector2 delta = new Vector2(p);
			delta.sub(lastTouch);

			scnWidget.translate(delta);
		}
	}

	@Override
	public boolean scrolled(InputEvent event, float x, float y, int amount) {
		EditorLogger.debug("SCROLLED - X: " + x + " Y: " + y);
		
		scnWidget.zoom(amount);
		return true;
	}

	@Override
	public void touchUp(InputEvent event, float x, float y, int pointer,
			int button) {
		
		EditorLogger.debug("Touch Up - X: " + x + " Y: " + y);

		draggingMode = DraggingModes.NONE;
		actor = null;

		return;
	}

	@Override
	public boolean keyDown(InputEvent event, int keycode) {
		switch (keycode) {
		case Keys.ENTER:
			break;
		case Keys.BACKSPACE:
			break;
		}

		return false;
	}
	
	@Override
	public void enter(InputEvent event, float x, float y, int pointer,  com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
		EditorLogger.debug("ENTER - X: " + x + " Y: " + y);
		scnWidget.getStage().setScrollFocus(scnWidget);
	}

}
