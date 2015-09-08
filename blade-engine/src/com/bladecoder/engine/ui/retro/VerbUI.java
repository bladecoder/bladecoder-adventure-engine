package com.bladecoder.engine.ui.retro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.bladecoder.engine.model.Inventory;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.ui.UI;

public class VerbUI extends Table {
	private static final List<String> VERBS = Arrays.asList("give", "pickup", "use", "open", "talkto", "push", "close", "lookat", "pull");
	private static final List<String> VERBS_DESC = Arrays.asList("Give", "Pick up", "Use", "Open", "Talk to", "Push", "Close", "Lookat", "Pull");

	private static final int VERB_COLS = 3;

	private static final int INVENTORY_COLS = 3;
	private static final int INVENTORY_ROWS = 3;
	
	private final List<RendererDrawable> inventorySlots = new ArrayList<RendererDrawable>();

	private final UI ui;

	private final String DEFAULT_VERB = "lookat";

	private final Label verbInfo;

	private String currentVerb = DEFAULT_VERB;
	
	private String actorDesc;
	
	private String target;

	public VerbUI(UI ui) {
		super(ui.getSkin());

		this.ui = ui;

		verbInfo = new Label(VERBS_DESC.get(VERBS.indexOf(DEFAULT_VERB)), ui.getSkin());
		add(verbInfo).fillX().expandX();
		row();

		Table verbs = createVerbPanel();
		add(verbs).fill().expand();

		Table inventory = createInventoryPanel();
		add(inventory).fill().expand();
	}

	private Table createVerbPanel() {
		Table verbs = new Table(ui.getSkin());

		for (int i = 0; i < VERBS.size(); i++) {
			if (i % VERB_COLS == 0)
				verbs.row();

			TextButton b = new TextButton(VERBS_DESC.get(i), ui.getSkin());
			b.setName(VERBS.get(i));
			b.addListener(new ClickListener() {
				public void clicked(InputEvent event, float x, float y) {
					currentVerb = event.getListenerActor().getName();
					verbInfo.setText(((TextButton) event.getListenerActor()).getText());
				}
			});

			verbs.add(b).fill().expand();
		}

		return verbs;
	}
	
	@Override
	public void act(float delta) {
		super.act(delta);
		
		Inventory inv = World.getInstance().getInventory();
		
		// fill inventory
		for(int i = 0; i < inventorySlots.size(); i++) {
			if(i < inv.getNumItems()) {
				inventorySlots.get(i).setRenderer(inv.getItem(i).getRenderer());
			} else {
				inventorySlots.get(i).setRenderer(null);
			}
		}
	}

	private Table createInventoryPanel() {
		Table inventory = new Table(ui.getSkin());
		
		for(int i=0; i < INVENTORY_COLS * INVENTORY_ROWS; i++) {
			if (i % INVENTORY_COLS == 0)
				inventory.row();
			
			ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
			RendererDrawable r = new RendererDrawable();
			inventorySlots.add(r);
			style.imageUp = r;
			
			ImageButton b = new ImageButton(style);
			inventory.add(b).fill().expand();
			b.setUserObject(i);
		}
		
		return inventory;
	}

	public String getCurrentVerb() {
		return currentVerb;
	}

	public String getTarget() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void setActorDesc(String desc) {
		actorDesc = desc;
		
		String verbDesc = VERBS_DESC.get(VERBS.indexOf(currentVerb));
		
		if(desc != null)
			verbInfo.setText(verbDesc + " " + desc);
		else
			verbInfo.setText(verbDesc);
	}
}
