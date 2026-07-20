package com.bladecoder.engineeditor.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.panels.HeaderPanel;
import com.bladecoder.engineeditor.ui.panels.TabPanel;

public class AssetPanel extends HeaderPanel {
	private TabPanel tabPanel;
	
	public AssetPanel(Skin skin) {
		super(skin, "ASSETS");	
		
		tabPanel = new TabPanel(skin);
		tabPanel.addTab("Assets", new AssetsList(skin));
		tabPanel.addTab("Resolutions", new ResolutionList(skin));
		
		setContent(tabPanel);

		Ctx.project.addPropertyChangeListener(Project.NOTIFY_PROJECT_LOADED, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {				
				setTile("ASSETS - " + Ctx.project.getProjectDir().getName());
			}
		});		
	}
}
