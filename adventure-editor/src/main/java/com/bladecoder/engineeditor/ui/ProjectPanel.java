package com.bladecoder.engineeditor.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.panels.HeaderPanel;
import com.bladecoder.engineeditor.ui.panels.TabPanel;

public class ProjectPanel extends HeaderPanel {

	private TabPanel tabPanel;
	private SceneList sceneList;
	private ChapterList chapterList;
	private SoundList soundList;

	public ProjectPanel(Skin skin) {
		super(skin, "ADVENTURE");

		tabPanel = new TabPanel(skin);
		sceneList = new SceneList(skin);
		chapterList = new ChapterList(skin);
		soundList = new SoundList(skin);

		setContent(tabPanel);

		tabPanel.addTab("Scenes", sceneList);
		tabPanel.addTab("Chapters", chapterList);
		tabPanel.addTab("Game Props", new WorldProps(skin));
		tabPanel.addTab("Sounds", soundList);
		tabPanel.addTab("Assets", new AssetsList(skin));
		tabPanel.addTab("Resolutions", new ResolutionList(skin));

		Ctx.project.addPropertyChangeListener(Project.NOTIFY_PROJECT_LOADED, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				chapterList.addElements();
				setTile(Ctx.project.getTitle() != null
						? Ctx.project.getTitle() + " (" + Ctx.project.getWorld().getScenes().size() + " scenes)"
						: "ADVENTURE GAME");
			}
		});
	}
}
