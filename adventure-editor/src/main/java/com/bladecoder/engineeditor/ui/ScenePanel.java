package com.bladecoder.engineeditor.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.panels.HeaderPanel;
import com.bladecoder.engineeditor.ui.panels.TabPanel;

public class ScenePanel extends HeaderPanel {
	
	private TabPanel tabPanel;
//	private VerbList verbList;	
	private ActorList actorList;
	private LayerList layerList;
//	private SceneProps sceneProps;
	
	public ScenePanel(Skin skin) {
		super(skin, "SCENE");		
		tabPanel = new TabPanel(skin);
//		verbList = new VerbList(skin);
		actorList = new ActorList(skin);
		layerList = new LayerList(skin);
//		sceneProps = new SceneProps(skin);
				
		setContent(tabPanel);		
		
		tabPanel.addTab("Actors", actorList);
//		tabPanel.addTab("Verbs", verbList);
		tabPanel.addTab("Layers", layerList);
//		tabPanel.addTab("Scene Props", sceneProps);
		
		Ctx.project.addPropertyChangeListener(Project.NOTIFY_SCENE_SELECTED, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				Scene s = Ctx.project.getSelectedScene();
				
				if(s != null) {								
					setTile("SCENE " + s.getId());
					actorList.addElements(s, Arrays.asList(s.getActors().values().toArray(new BaseActor[0])));
					layerList.addElements(s, s.getLayers());		
				} else {
					setTile("SCENE");
					actorList.addElements(null, null);
					layerList.addElements(null, null);		
				}
				
//				verbList.addElements(doc, scn, "verb");	
//				sceneProps.setSceneDocument(doc, scn);
				
			}
		});	
	}
}
