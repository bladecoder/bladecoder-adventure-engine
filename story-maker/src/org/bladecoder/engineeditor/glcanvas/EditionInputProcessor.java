package org.bladecoder.engineeditor.glcanvas;

import org.bladecoder.engine.anim.FrameAnimation;
import org.bladecoder.engine.model.BaseActor;
import org.bladecoder.engine.model.Scene;
import org.bladecoder.engine.model.SpriteActor;
import org.bladecoder.engine.model.SpriteAtlasActor;
import org.bladecoder.engineeditor.Ctx;
import org.w3c.dom.Element;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class EditionInputProcessor extends InputAdapter {
	private static enum DraggingModes {
		NONE, DRAGING_ACTOR, DRAGING_BBOX
	};

	private static enum Corners {
		BOTTOM_LEFT, BOTTOM_RIGHT, TOP_LEFT, TOP_RIGHT
	};

	private final ScnCanvas canvas;
	private boolean touchDown = false;

	private DraggingModes draggingMode = DraggingModes.NONE;
	private Corners corner = Corners.BOTTOM_LEFT;
	private BaseActor actor = null;
	private Vector2 org = new Vector2();

	public EditionInputProcessor(ScnCanvas canvas) {
		this.canvas = canvas;
	}

	@Override
	public boolean touchDown(int x, int y, int pointer, int button) {
		touchDown = button == Buttons.LEFT;
		if (!touchDown)
			return false;

		Scene scn = canvas.getScene();
		if (scn == null)
			return false;

		Vector2 p = canvas.screenToWorld(x, y);
		actor = scn.getFullSearchActorAt(p.x, p.y);

		if (actor == null)
			return false;

		Element da = Ctx.project.getActor(actor.getId());
		
		if(Ctx.project.getSelectedActor() == null ||
				!actor.getId().equals(Ctx.project.getSelectedScene().getId(Ctx.project.getSelectedActor())))
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

		return false;
	}

	@Override
	public boolean touchUp(int x, int y, int pointer, int button) {
		if (!touchDown)
			return false;
		touchDown = false;

		Scene scn = canvas.getScene();
		if (scn == null)
			return false;

		draggingMode = DraggingModes.NONE;
		actor = null;

		Vector2 p = canvas.toScreen(x, y);
		canvas.click(p.x, p.y);

		return false;
	}

	@Override
	public boolean touchDragged(int x, int y, int pointer) {
		if (!touchDown)
			return false;

		Scene scn = canvas.getScene();
		if (scn == null)
			return false;

		Vector2 d = canvas.screenToWorld(x, y);

		d.x -= org.x;
		d.y -= org.y;
		org.x += d.x;
		org.y += d.y;

		if (draggingMode == DraggingModes.DRAGING_ACTOR) {
			if (actor instanceof SpriteActor) {
				Vector2 p = ((SpriteActor) actor).getPosition();

//				((SpriteActor) actor).setPosition(p.x + d.x, p.y + d.y);
				
				Ctx.project.getSelectedScene().setPos(Ctx.project.getSelectedActor(), new Vector2(p.x + d.x, p.y + d.y));
			} else {
				Rectangle bbox = actor.getBBox(); // bbox will be an inmutable
													// object
				bbox.x += d.x;
				bbox.y += d.y;
//				actor.setBbox(bbox);
				
				Ctx.project.getSelectedScene().setBbox(Ctx.project.getSelectedActor(), bbox);
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

//			actor.setBbox(bbox);
			if(actor instanceof SpriteActor) {
				Vector2 pos = ((SpriteActor) actor).getPosition();
				bbox.x -= pos.x;
				bbox.y -= pos.y;
			}
			
			Ctx.project.getSelectedScene().setBbox(Ctx.project.getSelectedActor(), bbox);
		}

		return false;
	}

	@Override
	public boolean keyDown(int keycode) {
		switch (keycode) {
		case Keys.ENTER:
			break;
		case Keys.BACKSPACE:
			break;
		case Keys.UP:
			setDY(1);
			break;
		case Keys.DOWN:
			setDY(-1);
			break;
		case Keys.LEFT:
			setDX(-1);
			break;
		case Keys.RIGHT:
			setDX(1);
			break;
		case Keys.PAGE_UP:
			setFrame(1);
			break;
		case Keys.PAGE_DOWN:
			setFrame(-1);
			break;
		}

		return false;
	}

	private void setFrame(int d) {
		BaseActor actor = canvas.getActor();
		
		if (actor instanceof SpriteAtlasActor) {
			SpriteAtlasActor sa = (SpriteAtlasActor) actor;
			
			int num = sa.getNumFrames();
			int current = sa.getCurrentFrame();
			
			current += d;
			
			if(current >= 0 && current < num)
				sa.setCurrentFrame(current);
		}
	}

	private void setDX(int d) {
		BaseActor actor = canvas.getActor();
		
		if (actor instanceof SpriteAtlasActor) {
			FrameAnimation fa = ((SpriteAtlasActor) actor)
					.getCurrentFrameAnimation();
			
			if(fa.inD != null)
				fa.inD.x += d;
			
			if(fa.outD!=null)
				fa.outD.x -= d;
			
			SpriteActor sa = (SpriteActor) actor;
			Vector2 pos = sa.getPosition();
			pos.x += d;
			
			// TODO Set in ActorDocument
		}
	}

	private void setDY(int d) {
		BaseActor actor = canvas.getActor();
		
		if (actor instanceof SpriteAtlasActor) {
			FrameAnimation fa = ((SpriteAtlasActor) actor)
					.getCurrentFrameAnimation();
			if(fa.inD != null)
				fa.inD.y += d;
			
			if(fa.outD != null)
				fa.outD.y -= d;
			
			SpriteActor sa = (SpriteActor) actor;
			Vector2 pos = sa.getPosition();
			pos.y += d;
			
			// TODO Set in ActorDocument
		}
	}
}
