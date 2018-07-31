package com.bladecoder.engine.ui.retro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button.ButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Inventory;
import com.bladecoder.engine.ui.SceneScreen;
import com.bladecoder.engine.util.DPIUtils;

public class VerbUI extends Table {
	private final static float MARGIN = 1;
	
	private static final List<String> VERBS = Arrays.asList("give", "pickup", "use", "open", "talkto", "push", "close",
			"lookat", "pull");
	private static final List<String> VERBS_DESC = Arrays.asList("Give", "Pick up", "Use", "Open", "Talk to", "Push",
			"Close", "Lookat", "Pull");

	private static final int VERB_COLS = 3;

	private static final int INVENTORY_COLS = 3;
	private static final int INVENTORY_ROWS = 3;

	private final List<ImageButton> inventorySlots = new ArrayList<ImageButton>();

	private final SceneScreen sceneScreen;
	private final String DEFAULT_VERB = "lookat";
	private final Label infoLine;

	private String currentVerb = DEFAULT_VERB;
	private InteractiveActor target;

	private VerbUIStyle style;
	
	private int scroll = 0;
	
	private Table arrowPanel;
	private Table invPanel;

	public VerbUI(SceneScreen scn) {
		super(scn.getUI().getSkin());

		style = scn.getUI().getSkin().get(VerbUIStyle.class);

		if (style.background != null)
			setBackground(style.background);

		this.sceneScreen = scn;

		infoLine = new Label(VERBS_DESC.get(VERBS.indexOf(DEFAULT_VERB)), style.infoLineLabelStyle);
		infoLine.setAlignment(Align.center);
		add(infoLine).colspan(3).fillX().expandX();
		row();

		Table verbs = createVerbPanel();
		add(verbs).fill().expand();
		
		arrowPanel = createArrowPanel();
		add(arrowPanel).fillY().expandY();

		invPanel = createInventoryPanel();
		add(invPanel).fill().expand();
	}
	
	private Table createArrowPanel() {
		Table arrows = new Table();
		
		arrows.defaults().pad(MARGIN);
		
		ImageButton.ImageButtonStyle s = new ImageButton.ImageButtonStyle(style.inventoryButtonStyle);
		s.imageUp = style.upArrow;

		ImageButton up = new ImageButton(s);

		arrows.add(up).fillY().expandY();

		up.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				if(scroll > 0)
					scroll--;
			}
		});
		
		arrows.row();
		
		ImageButton.ImageButtonStyle s2 = new ImageButton.ImageButtonStyle(style.inventoryButtonStyle);
		s2.imageUp = style.downArrow;

		ImageButton down = new ImageButton(s2);

		arrows.add(down).fillY().expandY();

		down.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				Inventory inv = sceneScreen.getUI().getWorld().getInventory();
				
				int itemsLeft = inv.getNumItems() - scroll * INVENTORY_COLS;
				
				if(itemsLeft > inventorySlots.size())
					scroll++;
			}
		});
		
		return arrows;
	}

	private Table createVerbPanel() {
		Table verbs = new Table();
		
		verbs.defaults().pad(MARGIN);

		for (int i = 0; i < VERBS.size(); i++) {
			if (i % VERB_COLS == 0)
				verbs.row();

			TextButton b = new TextButton(VERBS_DESC.get(i), style.verbButtonStyle);
			b.setName(VERBS.get(i));
			b.addListener(new ClickListener() {
				public void clicked(InputEvent event, float x, float y) {
					currentVerb = event.getListenerActor().getName();
					infoLine.setText(((TextButton) event.getListenerActor()).getText());
					target = null;
				}
			});

			verbs.add(b).fill().expand();
		}

		return verbs;
	}
	
	@Override
	public void sizeChanged() {
		super.sizeChanged();

		for(Actor a:arrowPanel.getChildren()) {
			ImageButton b = (ImageButton)a;
			float h = (getHeight() / 2)  - style.infoLineLabelStyle.font.getLineHeight() / 2 - DPIUtils.getSpacing();
			float ih = b.getImage().getDrawable().getMinHeight();
			float iw = b.getImage().getDrawable().getMinWidth() *  h / ih;
			b.getImageCell().maxSize(iw, h);
		}

		arrowPanel.invalidateHierarchy();
	}

	@Override
	public void act(float delta) {
		super.act(delta);

		Inventory inv = sceneScreen.getUI().getWorld().getInventory();

		// fill inventory
		for (int i = 0; i < inventorySlots.size(); i++) {
			RendererDrawable r = (RendererDrawable) inventorySlots.get(i).getImage().getDrawable();
			
			int pos = scroll * INVENTORY_COLS + i;

			if (pos < inv.getNumItems()) {
				r.setRenderer(inv.get(pos).getRenderer());				
			} else {
				r.setRenderer(null);
			}
			
			inventorySlots.get(i).getImage().invalidate();
		}
	}

	private Table createInventoryPanel() {
		Table inventory = new Table();
		
		inventory.defaults().pad(MARGIN);

		for (int i = 0; i < INVENTORY_COLS * INVENTORY_ROWS; i++) {
			if (i % INVENTORY_COLS == 0)
				inventory.row();

			ImageButton.ImageButtonStyle s = new ImageButton.ImageButtonStyle(style.inventoryButtonStyle);
			RendererDrawable r = new RendererDrawable();
			s.imageUp = r;

			ImageButton b = new ImageButton(s);

			inventory.add(b).fill().expand();
			b.setUserObject(i);
			inventorySlots.add(b);

			b.addListener(new ClickListener() {
				public void clicked(InputEvent event, float x, float y) {
					int i = (Integer) event.getListenerActor().getUserObject();
					Inventory inv = sceneScreen.getUI().getWorld().getInventory();
					target = null;

					if (i < inv.getNumItems()) {
						InteractiveActor actor = inv.get(i);

						if (currentVerb.equals("use") || currentVerb.equals("give")) {
							target = actor;
						} else {
							sceneScreen.runVerb(actor, currentVerb, null);
						}
					}
				}
			});
			
			b.getImageCell().pad(MARGIN).expand().fill();
		}

		return inventory;
	}

	public String getCurrentVerb() {
		return currentVerb;
	}

	public String getTarget() {
		return target == null ? null : target.getId();
	}

	public void setCurrentActor(InteractiveActor actor) {
		String verbDesc = VERBS_DESC.get(VERBS.indexOf(currentVerb));
		String desc = getTranslatedDesc(actor);

		if (target != null) {
			String prep;

			if (currentVerb.equals("give")) {
				prep = " to ";
			} else {
				prep = " with ";
			}

			if (desc != null)
				infoLine.setText(verbDesc + " " + getTranslatedDesc(target) + prep + desc);
			else
				infoLine.setText(verbDesc + " " + getTranslatedDesc(target) + prep);
		} else {
			if (desc != null)
				infoLine.setText(verbDesc + " " + desc);
			else
				infoLine.setText(verbDesc);
		}
	}

	private String getTranslatedDesc(InteractiveActor actor) {
		String desc = null;

		if (actor != null && actor.getDesc() != null) {
			desc = actor.getDesc();

			if (desc.charAt(0) == I18N.PREFIX)
				desc = I18N.getString(desc.substring(1));
		}

		return desc;
	}

	public void show() {
		setVisible(true);
	}

	public void hide() {
		target = null;
		currentVerb = DEFAULT_VERB;
		setCurrentActor(null);

		setVisible(false);
	}

	/** The style for the VerbUI */
	static public class VerbUIStyle {
		/** Optional. */
		public Drawable background;

		public TextButtonStyle verbButtonStyle;
		public ButtonStyle inventoryButtonStyle;
		public LabelStyle infoLineLabelStyle;
		public Drawable upArrow;
		public Drawable downArrow;

		public VerbUIStyle() {
		}

		public VerbUIStyle(VerbUIStyle style) {
			background = style.background;
			verbButtonStyle = style.verbButtonStyle;
			inventoryButtonStyle = style.inventoryButtonStyle;
			infoLineLabelStyle = style.infoLineLabelStyle;
			upArrow = style.upArrow;
			downArrow = style.downArrow;
		}
	}
}
