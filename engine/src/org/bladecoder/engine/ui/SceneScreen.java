package org.bladecoder.engine.ui;

import org.bladecoder.engine.model.BaseActor;
import org.bladecoder.engine.model.Scene;
import org.bladecoder.engine.model.Transition;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.model.World.AssetState;
import org.bladecoder.engine.util.EngineLogger;
import org.bladecoder.engine.util.RectangleRenderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class SceneScreen implements Screen, CommandListener {

	CommandListener l;
	Pointer pointer;

	private PieMenu pie;
	private InventoryUI inventoryUI;
	private DialogUI dialogUI;
	private TextManagerUI textManagerUI;

	private BaseActor selectedActor = null;

	private boolean pieMode;
	boolean dragging = false;
	
	Rectangle viewPort;
	Recorder recorder;

	public SceneScreen(Pointer pointer, CommandListener l, boolean pieMode, Recorder recorder) {
		this.pointer = pointer;
		this.l = l;
		this.recorder = recorder;

		pie = new PieMenu(recorder);
		textManagerUI = new TextManagerUI();

		inventoryUI = new InventoryUI(recorder);
		inventoryUI.setCommandListener(this);

		dialogUI = new DialogUI(recorder);
		dialogUI.setCommandListener(this);
		
		this.pieMode = pieMode;
	}

	public void update() {
		World w = World.getInstance();

		if (w.getCurrentDialog() == null && !w.inCutMode()) {

			BaseActor a = null;

			if (w.getInventory().isVisible()) {
				Vector3 input = pointer.getPosition();
				a = inventoryUI.getItemAt(input.x, input.y);
			}

			if (a == null) {
				Vector3 input = w.getSceneCamera().getInputUnProject(viewPort);

				a = w.getCurrentScene().getActorAt(input.x, input.y);
			}

			pointer.setTarget(a);
		} else {
			pointer.setTarget(null);

			if (pie.isVisible()) {
				pie.hide();
				pointer.setFreezeHotSpot(false);
			}

			inventoryUI.cancelDragging();
		}
	}

	@Override
	public void draw(SpriteBatch batch) {
		World w = World.getInstance();
		
		if (w.getAssetState() != AssetState.LOADED)
			return;

		if (EngineLogger.debugMode()) {
			Vector3 mousepos = w.getSceneCamera().getInputUnProject(viewPort);

			StringBuilder sb = new StringBuilder();
			sb.append("Mouse ( ");
			sb.append((int) mousepos.x);
			sb.append(", ");
			sb.append((int) mousepos.y);
			sb.append(") FPS:");
			sb.append(Gdx.graphics.getFramesPerSecond());

			if (w.getCurrentScene().getBackgroundMap() != null) {
				sb.append(" Map: ");
				sb.append((int) (w.getCurrentScene().getBackgroundMap().getDepth(mousepos.x,
						mousepos.y) * 10));
			}

			String strDebug = sb.toString();

			TextBounds b = EngineLogger.getDebugFont().getBounds(strDebug);
			RectangleRenderer.draw(batch, 0, viewPort.height - b.height - 10, b.width,
					b.height + 10, Color.BLACK);
			EngineLogger.getDebugFont().draw(batch, strDebug, 0, viewPort.height);
		}		
		
		if (World.getInstance().getCurrentDialog() != null && 
				!recorder.isPlaying()) { // DIALOG MODE
			
			if (!World.getInstance().inCutMode()) {
				Vector3 input = pointer.getPosition();
				dialogUI.draw(batch, (int)input.x, (int)input.y);
			}

			textManagerUI.draw(batch);
			pointer.draw(batch, false);
		} else {

			textManagerUI.draw(batch);
			
			Vector3 input = pointer.getPosition();
			inventoryUI.draw(batch, (int)input.x, (int)input.y);

			if (pieMode)
				pie.draw(batch);

			if (!World.getInstance().inCutMode() && !recorder.isPlaying())
				pointer.draw(batch, dragging);
		}
		
		Transition t = World.getInstance().getCurrentScene().getTransition();
		
		if (t != null) {
			t.draw(batch, viewPort.width, viewPort.height);
		}
		
		recorder.draw(batch);
	}

	@Override
	public void resize(Rectangle v) {
		viewPort = v;
		
		pie.resize(v);
		inventoryUI.resize(v);
		dialogUI.resize(v);
		textManagerUI.resize(v);
	}

	@Override
	public void dispose() {
		textManagerUI.dispose();
		dialogUI.dispose();
	}

	@Override
	public void createAssets() {
		textManagerUI.createAssets();
		dialogUI.createAssets();
		pie.createAssets();
		inventoryUI.createAssets();
	}

	@Override
	public void retrieveAssets(TextureAtlas atlas) {
		pie.retrieveAssets(atlas);
		inventoryUI.retrieveAssets(atlas);
		dialogUI.retrieveAssets(atlas);
		textManagerUI.retrieveAssets(atlas);
	}

	@Override
	public void touchEvent(int type, float x, float y, int pointer, int button) {
		World w = World.getInstance();

		if (w.isPaused() || recorder.isPlaying())
			return;

		switch (type) {
		case TOUCH_UP:

			if (w.inCutMode() && !recorder.isRecording()) {
				w.getTextManager().next();
			} else if (w.getCurrentDialog() != null) {
				dialogUI.touchEvent(TouchEventListener.TOUCH_UP, x, y, pointer, button);
			} else if (w.getCurrentScene().getOverlay() != null) {
					w.getCurrentScene().getOverlay().click();
			} else if (dragging) {
				inventoryUI.touchEvent(TouchEventListener.TOUCH_UP, x, y, pointer, button);
				dragging = false;
			} else if (button == 1 && !pieMode) {
				this.pointer.toggleSelectedVerb();
			} else if (pie.isVisible()) {
				this.pointer.setFreezeHotSpot(false);
				pie.touchEvent(TouchEventListener.TOUCH_UP, x, y, pointer, button);
			} else if (inventoryUI.contains(x, y)) {
				inventoryUI.touchEvent(TouchEventListener.TOUCH_UP, x, y, pointer, button);
			} else {
				sceneClick();
			}
			break;

		case TOUCH_DOWN:
			if (pie.isVisible()) {
				pie.touchEvent(TouchEventListener.TOUCH_DOWN, x, y, pointer, button);
			} else if (!w.inCutMode() && inventoryUI.contains(x, y)) {
				inventoryUI.touchEvent(TouchEventListener.TOUCH_DOWN, x, y, pointer, button);
			}

			break;

		case DRAG:
			if (inventoryUI.contains(x, y) && inventoryUI.getItemAt(x, y) != null && !dragging) {

				inventoryUI.touchEvent(TouchEventListener.DRAG, x, y, pointer, button);

				dragging = true;

				if (pie.isVisible()) {
					pie.hide();
					this.pointer.setFreezeHotSpot(false);
				}
			}
			break;
		}
	}

	private void sceneClick() {
		World w = World.getInstance();

		Vector3 unprojectScroll = w.getSceneCamera().getInputUnProject(viewPort);

		Scene s = w.getCurrentScene();

		BaseActor a = s.getActorAt(unprojectScroll.x, unprojectScroll.y);

		if (a != null) {

			if (EngineLogger.debugMode()) {
				EngineLogger.debug(a.toString());
			}

			actorClick(a);
		} else if (s.getPlayer() != null) {
			if (s.getPlayer().getVerb("goto") != null) {
				if(recorder.isRecording()) {
					recorder.add(s.getPlayer().getId(), "goto", null);
				}
				
				s.getPlayer().runVerb("goto");
			} else {
				Vector2 pos = new Vector2(unprojectScroll.x, unprojectScroll.y);
				
				if(recorder.isRecording()) {
					recorder.add(pos);
				}
				
				s.getPlayer().goTo(pos, null);
			}
		}
	}

	private void actorClick(BaseActor a) {
			if (a.getVerb("leave") != null) {
				if(recorder.isRecording()) {
					recorder.add(a.getId(), "leave", null);
				}
				
				a.runVerb("leave");
			} else if (!pieMode) {
				if(recorder.isRecording()) {
					recorder.add(a.getId(), pointer.getSelectedVerb(), null);
				}
				
				a.runVerb(pointer.getSelectedVerb()); 
			} else {
				Vector3 unprojectScreen = pointer.getPosition();
				pie.show(a, unprojectScreen.x, unprojectScreen.y);
				pointer.setFreezeHotSpot(true);
			}
	}	
	
	@Override
	public void runCommand(String command, Object param) {

		if (command.equals(CommandListener.RUN_VERB_COMMAND)) {
			selectedActor = (BaseActor) param;
			actorClick(selectedActor);
		} else if (command.equals(DialogUI.DIALOG_END_COMMAND)) {
			World.getInstance().setCurrentDialog(null);
		} else {
			l.runCommand(command, param);
		}
	}

	public void resetUI() {

		if (pie.isVisible()) {
			pie.hide();
			pointer.setFreezeHotSpot(false);
		}

		pointer.setTarget(null);

		dragging = false;
	}

	public InventoryUI getInventoryUI() {
		return inventoryUI;
	}

}
