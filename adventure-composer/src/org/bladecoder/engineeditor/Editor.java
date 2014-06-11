package org.bladecoder.engineeditor;

import java.io.File;

import org.bladecoder.engineeditor.model.Project;
import org.bladecoder.engineeditor.scneditor.ScnEditor;
import org.bladecoder.engineeditor.ui.ActorPanel;
import org.bladecoder.engineeditor.ui.AssetPanel;
import org.bladecoder.engineeditor.ui.ProjectPanel;
import org.bladecoder.engineeditor.ui.ProjectToolbar;
import org.bladecoder.engineeditor.ui.ScenePanel;
import org.bladecoder.engineeditor.utils.EditorLogger;
import org.bladecoder.engineeditor.utils.Message;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.SplitPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

//TODO: Show COPYRIGHT_STR + VERSION_STR
//TODO: Set TITLE in window bar. Set '*' in the title when modified
//TODO: Query if save project when exit

public class Editor implements ApplicationListener {
	public static final String VERSION_STR = "0.1.0 (Beta)";
	public static final String COPYRIGHT_STR = "2013 - Rafael García\nhttp://bladecoder.blogspot.com";
	public static final String TITLE = "Adventure Composer";	
	
//	public static final String SKIN = "res/skin/HoloSkin/Holo-dark-ldpi.json";
	// public static final String SKIN = "res/skin/uiskin.json";
	public static final String SKIN = "res/skin/BladeSkin/BladeSkin-ldpi.json";

	Stage stage;
	ScnEditor scnEditor;

	@Override
	public void create() {
		Skin skin = new Skin(Gdx.files.internal(SKIN));

		EditorLogger.setDebug();
		EditorLogger.debug("CREATE");
		Ctx.project = new Project();
		Ctx.msg = new Message(skin);
		Ctx.assetManager = new EditorAssetManager();

		scnEditor = new ScnEditor(skin);

		/*** STAGE SETUP ***/
		stage = new Stage(new ScreenViewport());
		Gdx.input.setInputProcessor(stage);

		// RIGHT PANEL
		ScenePanel scenePanel = new ScenePanel(skin);
		ActorPanel actorPanel = new ActorPanel(skin);

		Table rightPanel = new Table();
		rightPanel.top().left();
		rightPanel.add(scenePanel).expand().fill();
		rightPanel.row();
		rightPanel.add(actorPanel).expand().fill();

		SplitPane splitPaneRight = new SplitPane(scnEditor, rightPanel,
				false, skin);

		// LEFT PANEL
		ProjectPanel projectPanel = new ProjectPanel(skin);
		AssetPanel assetPanel = new AssetPanel(skin);
		Image img = new Image(Ctx.assetManager.getIcon("title"));
		img.setScaling(Scaling.none);
		img.setAlign(Align.left);

		Table leftPanel = new Table();
		leftPanel.top().left().padLeft(10);
		leftPanel.add(img).expandX().fill().padBottom(20).padTop(20).padLeft(20);
		leftPanel.row();
		leftPanel.add(new ProjectToolbar(skin)).expandX().fill();
		leftPanel.row();
		leftPanel.add(projectPanel).expand().fill();
		leftPanel.row();
		leftPanel.add(assetPanel).expand().fill();

		SplitPane splitPaneLeft = new SplitPane(leftPanel, splitPaneRight,
				false, skin);
		splitPaneLeft.setFillParent(true);
		splitPaneLeft.setSplitAmount(0.3f);
		stage.addActor(splitPaneLeft);

		// LOAD LAST OPEN PROJECT
		String lastProject = Ctx.project.getConfig().getProperty(
				Project.LAST_PROJECT_PROP, "");

		if (!lastProject.isEmpty() && new File(lastProject).exists()) {
			try {
				EditorLogger.debug("Loading last project: " + lastProject);
				Ctx.project.loadProject(new File(lastProject));
			} catch (Exception e) {
				EditorLogger.debug("Error loading last project: "
						+ e.getMessage());
				Ctx.project.closeProject();
				e.printStackTrace();
			}
		}
		
		stage.setScrollFocus(scnEditor.getScnWidget());
	}

	@Override
	public void render() {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
		stage.draw();
		Table.drawDebug(stage);
	}

	@Override
	public void resize(int width, int height) {
		EditorLogger.debug("RESIZE - w:" + width + " h:" + height);

		stage.getViewport().update(width, height, true);
	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public void dispose() {
		scnEditor.dispose();
		stage.dispose();
		
		Ctx.project.saveConfig();
		
		try {
			Ctx.project.saveProject();
		} catch (Exception ex) {
			System.out.println("Something went wrong while saving the project.\n");
			ex.printStackTrace();
		}
	}
}
