package com.bladecoder.engine.ui.retro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class VerbUI extends Table {
	private final List<String> VERBS = new ArrayList<String>(
		    Arrays.asList("give", "pickup", "use", "open", "talkto", "push", "close", "lookat", "pull")); 
	
	private final List<String> VERBS_DESC = new ArrayList<String>(
		    Arrays.asList("Give", "Pick up", "Use", "Open", "Talk to", "Push", "Close", "Lookat", "Pull"));
	
	private final int COLS = 3;
	private final SceneScreen sceneScreen;
	
	private final String DEFAULT_VERB = "lookat";
	
	private final Label verbInfo;
	
	private String currentVerb = DEFAULT_VERB;
	
	public VerbUI(SceneScreen scn) {
		super(scn.getUI().getSkin());
		
		sceneScreen = scn;
		
		
		verbInfo = new Label(VERBS_DESC.get(VERBS.indexOf(DEFAULT_VERB)), scn.getUI().getSkin());
		add(verbInfo).fillX().expandX();
		row();
		
		Table verbs = new Table(scn.getUI().getSkin());
		
		for(int i = 0; i < VERBS.size(); i++) {
			if(i % COLS == 0)
				verbs.row();
			
			TextButton b = new TextButton(VERBS_DESC.get(i), scn.getUI().getSkin());
			b.setName(VERBS.get(i));
			b.addListener(new ClickListener() {
				public void clicked(InputEvent event, float x, float y) {
					currentVerb = event.getListenerActor().getName();
					verbInfo.setText(((TextButton)event.getListenerActor()).getText());
				}
			});
			
			verbs.add(b).fill().expand();
		}
		
		add(verbs).fill().expand();
		
		Table inventory = new Table(scn.getUI().getSkin());
		inventory.add("INVENTORY").fill().expand();
		add(inventory).fill().expand();
	}
	
	public String getCurrentVerb() {
		return currentVerb;
	}

	public String getTarget() {
		// TODO Auto-generated method stub
		return null;
	}
}
