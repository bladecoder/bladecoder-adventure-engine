package org.bladecoder.engine.ui;

import org.bladecoder.engine.assets.UIAssetConsumer;
import org.bladecoder.engine.model.Actor;
import org.bladecoder.engine.model.Inventory;
import org.bladecoder.engine.model.SpriteActor;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.util.Config;
import org.bladecoder.engine.util.EngineLogger;
import org.bladecoder.engine.util.RectangleRenderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;

public class InventoryUI implements TouchEventListener, UIAssetConsumer {
	private final static int TOP = 0;
	private final static int DOWN = 1;
	private final static int LEFT = 2;
	private final static int RIGHT = 3;

	private final static String LEFT_ARROW_TILE = "left";
	private final static String RIGHT_ARROW_TILE = "right";
//	private final static String COLLAPSE_TILE = "collapse";
	private final static String UNCOLLAPSE_TILE = "uncollapse";
	private final static String CONFIG_TILE = "config";

	private final static float AUTOCOLLAPSE_TIME = 5;
	private final static float SCROLL_TIME = 0.5f;
	
	private final static float DPI = 160.0f * Gdx.graphics.getDensity();
	private final static Color GRAY = new Color(0.3f, 0.3f, 0.3f, 1f);
	private final static Color BG_COLOR = new Color(0, 0, 0, 0.6f);
//	private final static Color BG_COLOR = new Color(0, 0, 0, 1f);

	private Rectangle bbox = new Rectangle();
	private Rectangle collapsedBbox = new Rectangle();
	private Rectangle configBbox = new Rectangle();

	/** The tile where the item zone starts */
	private int itemsPos = 2;

	/** Visible item size */
	private int itemsSize;

	private int tileSize = 50;
	private int inventoryPos = DOWN;
	private int scrollItemPos = 0;

	private SpriteActor draggedActor = null;

//	private AtlasRegion collapseIcon;
	private AtlasRegion uncollapseIcon;
	private AtlasRegion leftArrowIcon;
	private AtlasRegion rightArrowIcon;
	private AtlasRegion configIcon;

	private boolean collapsed = false;
	private boolean autoCollapse = true;
	private float uncollapseTime = 0;
	private float scrollTime = 0;
	
	private int previousNumItems;

	CommandListener l;
	
	Rectangle viewPort;
	
	Recorder recorder;

	public InventoryUI(Recorder recorder) {
		this.recorder = recorder;
		
		String pos = Config.getProperty(Config.INVENTORY_POS_PROP, "down");
		
		if(pos.trim().equals("top"))
			inventoryPos = TOP;
		else if(pos.trim().equals("left"))
			inventoryPos = LEFT;
		else if(pos.trim().equals("right"))
			inventoryPos = RIGHT;
		else
			inventoryPos = DOWN;
	}

	public void resize(Rectangle v) {
		viewPort = v;

		// calc tilesize as function of resolution and DPI
		// initial tilesize is 1/10 of screen resolution
		this.tileSize = (int) viewPort.height / 10;

		// the minimum height of the inventory is 1/3"
		if (this.tileSize < (int) (DPI / 3)) {
			this.tileSize = (int) (DPI / 3);
			EngineLogger.debug("DPI: " + DPI + " New TILESIZE: "
					+ this.tileSize);
		}

		switch (inventoryPos) {
		case TOP:
			setBbox(0, viewPort.height - this.tileSize, viewPort.width, this.tileSize);
			break;
		case DOWN:
			setBbox(0, 0, viewPort.width, this.tileSize);
			break;
		case LEFT:
			setBbox(0, 0, this.tileSize, viewPort.height);
			break;
		case RIGHT:
			setBbox(viewPort.width - this.tileSize, 0, this.tileSize, viewPort.height);
			break;
		}

		if (bbox.width > bbox.height) // horizontal
			this.itemsSize = (int) viewPort.width / this.tileSize - (itemsPos + 2);
		else
			this.itemsSize = (int) viewPort.height / this.tileSize - (itemsPos + 2);
	}

	@Override
	public void retrieveAssets(TextureAtlas atlas) {
//		collapseIcon = atlas.findRegion(COLLAPSE_TILE);
		uncollapseIcon = atlas.findRegion(UNCOLLAPSE_TILE);

		leftArrowIcon = atlas.findRegion(LEFT_ARROW_TILE);
		rightArrowIcon = atlas.findRegion(RIGHT_ARROW_TILE);

		configIcon = atlas.findRegion(CONFIG_TILE);
	}

	public void draw(SpriteBatch batch, int inputX, int inputY) {
		Inventory inventory = World.getInstance().getInventory();
		
		if (!inventory.isVisible())
			return;

		if (autoCollapse) {
			// If the number of items changes show the inventory.
			// To give feedback to the user when pickup some item.
			if(previousNumItems != inventory.getNumItems()) {
				previousNumItems = inventory.getNumItems();
				collapse(false);
				
				// Set the scroll position to show the last item
			}
			
			updateAutocollapse(inputX, inputY);
		}

		if (collapsed) {
			batch.draw(uncollapseIcon, collapsedBbox.x, collapsedBbox.y,
					collapsedBbox.width, collapsedBbox.height);
		} else {
			boolean horizontal = bbox.width > bbox.height;

			RectangleRenderer.draw(batch, bbox.getX(), bbox.getY(),
					bbox.getWidth(), bbox.getHeight(), BG_COLOR);

//			if(!autoCollapse)
//				batch.draw(collapseIcon, collapsedBbox.x, collapsedBbox.y,
//					collapsedBbox.width, collapsedBbox.height);

			batch.draw(uncollapseIcon, collapsedBbox.x, collapsedBbox.y,
					tileSize, tileSize);		
			
			batch.draw(configIcon, configBbox.x, configBbox.y,
					configBbox.width, configBbox.height);

			// DRAW LEFT ARROW
			if (scrollItemPos == 0) {
				batch.setColor(GRAY);
				batch.draw(leftArrowIcon, horizontal ? (itemsPos - 1)
						* tileSize : bbox.x, horizontal ? bbox.y
						: (itemsPos - 1) * tileSize, tileSize, tileSize);
				batch.setColor(Color.WHITE);
			} else {
				batch.draw(leftArrowIcon, horizontal ? (itemsPos - 1)
						* tileSize : bbox.x, horizontal ? bbox.y
						: (itemsPos - 1) * tileSize, tileSize, tileSize);
			}

			// DRAW RIGHT ARROW
			if (scrollItemPos + itemsSize >= inventory.getNumItems()) {
				batch.setColor(GRAY);
				batch.draw(rightArrowIcon, horizontal ? (itemsPos + itemsSize)
						* tileSize : bbox.x, horizontal ? bbox.y
						: (itemsPos + itemsSize) * tileSize, tileSize, tileSize);
				batch.setColor(Color.WHITE);
			} else {
				batch.draw(rightArrowIcon, horizontal ? (itemsPos + itemsSize)
						* tileSize : bbox.x, horizontal ? bbox.y
						: (itemsPos + itemsSize) * tileSize, tileSize, tileSize);
			}

			// DRAW ITEMS
			for (int i = scrollItemPos; i < inventory.getNumItems()
					&& itemsSize > i - scrollItemPos; i++) {

				SpriteActor a = inventory.getItem(i);

//				if (!a.isLoaded()) {
//					EngineLogger.error("Inventory item NOT loaded: " + a.getId());
//					continue;
//				}

				float size = tileSize / a.getHeight() / a.getScale();

				a.getRenderer().draw(batch, 
						horizontal ? (i + itemsPos - scrollItemPos)	* tileSize + a.getWidth() * size / 2: bbox.x + a.getWidth() * size / 2, 
						horizontal ? bbox.y : (i+ itemsPos - scrollItemPos) * tileSize, 
								size);
			}
		}

		// DRAW DRAGGING
		if (draggedActor != null) {
			
			float h = draggedActor.getHeight() / draggedActor.getScale();
			
			float size = tileSize / h * 1.3f;
			
			draggedActor.getRenderer().draw(batch, inputX,
					inputY - h * size / 2, size);
			
			
			// Scroll the inventory if the draggedActor is over one inventory arrow
			scrollTime += Gdx.graphics.getDeltaTime();

			if (scrollTime > SCROLL_TIME) {
				scrollTime = 0;
				updateScroll(inputX, inputY);
			}			
		}

	}

	private void updateAutocollapse(int inputX, int inputY) {

		if (collapsed) {
			if (collapsedBbox.contains(inputX, inputY)
					|| inputY < 5) {
				collapse(false);
			}
		} else {

			if (bbox.contains(inputX, inputY)) {
				uncollapseTime = 0;
			} else {

				uncollapseTime += Gdx.graphics.getDeltaTime();

				if (uncollapseTime > AUTOCOLLAPSE_TIME) {
					uncollapseTime = 0;
					collapse(true);
				}
			}
		}
	}
	
	
	/**
	 * Scrolls the inventory if the coordinates are over one inventory arrow
	 * 
	 * @param x X coordinate over inventory
	 * @param y Y coordinate over inventory
	 */
	private void updateScroll(float x, float y) {
		Inventory inventory = World.getInstance().getInventory();
		int selected = (int) ((bbox.width > bbox.height ? x : y) / tileSize);

		if (selected == itemsPos - 1 && scrollItemPos > 0) {
			scrollItemPos--;
		} else if (selected == itemsPos + itemsSize
				&& scrollItemPos + itemsSize < inventory
						.getNumItems()) {
			scrollItemPos++;
		}		
	}	

	public void setCommandListener(CommandListener l) {
		this.l = l;
	}

	public boolean contains(float x, float y) {
		Inventory inventory = World.getInstance().getInventory();
		
		if (!inventory.isVisible())
			return false;

		if (collapsed)
			return collapsedBbox.contains(x, y);
		else
			return bbox.contains(x, y);
	}

	private void setBbox(float x, float y, float width, float height) {
		bbox.set(x, y, width, height);

		collapsedBbox.set(bbox.x, bbox.y, tileSize, tileSize);

		configBbox.set(bbox.width < bbox.height ? bbox.x : bbox.width
				- tileSize, bbox.width < bbox.height ? bbox.height - tileSize
				: bbox.y, tileSize, tileSize);
	}

	public void cancelDragging() {
		draggedActor = null;
	}

	private void startDragging(float x, float y) {
		draggedActor = getItemAt(x, y);
	}

	private void stopDragging(int inputX, int inputY) {
		Vector3 mousepos = World.getInstance().getSceneCamera()
				.getInputUnProject(viewPort);
		
		Actor targetActor = World.getInstance().getCurrentScene()
				.getActorAt(mousepos.x, mousepos.y);

		// if targetActor is not found in scene search inventory
		if (targetActor == null) {		
			targetActor = getItemAt(inputX, inputY);
		}

		if (targetActor != null) {
			use(targetActor, draggedActor);
		}

		draggedActor = null;
	}

	private void use(Actor a1, Actor a2) {
		if (a1.getVerb("use", a2.getId()) != null) {
			if(recorder.isRecording()) {
				recorder.add(a1.getId(), "use", a2.getId());
			}
			
			a1.runVerb("use", a2.getId());
		} else {
			if(recorder.isRecording()) {
				recorder.add(a2.getId(), "use", a1.getId());
			}
			
			a2.runVerb("use", a1.getId());
		}
	}

	public SpriteActor getItemAt(float x, float y) {
		Inventory inventory = World.getInstance().getInventory();

		if (collapsed == false && bbox.contains(x, y)) {

			int selected = (int) ((bbox.width > bbox.height ? x : y) / tileSize);

			if (selected >= itemsPos && selected < itemsPos + itemsSize) { // item
																			// clicked
				int item = selected - itemsPos + scrollItemPos;

				if (item < inventory.getNumItems()) {
					return inventory.getItem(item);
				}

			}
		}

		return null;
	}

	public void collapse(boolean collapse) {
		this.collapsed = collapse;
	}

	@Override
	public void touchEvent(int type, float x, float y, int pointer, int button) {
		switch (type) {
		case TOUCH_UP:
			if (draggedActor != null) {
				stopDragging((int)x , (int)y);
			} else if (collapsedBbox.contains(x, y)) {
				if (collapsed)
					collapse(false);
				else
					collapse(true);
			} else if (configBbox.contains(x, y)) {
				l.runCommand(CommandListener.CONFIG_COMMAND, null);
			} else {
				Actor actor = getItemAt(x, y);

				if (actor != null) {
					l.runCommand(CommandListener.RUN_VERB_COMMAND, actor);
				} else {
					updateScroll(x,y);
				}
			}
			break;

		case TOUCH_DOWN:
			// TODO if points over collapse or options. color it
			break;

		case DRAG:
			startDragging(x, y);
			break;
		}
	}

	public int getInventoryPos() {
		return inventoryPos;
	}

	public void setInventoryPos(int inventoryPos) {
		this.inventoryPos = inventoryPos;
	}

	@Override
	public void dispose() {
		
	}

	@Override
	public void createAssets() {
		
	}

}
