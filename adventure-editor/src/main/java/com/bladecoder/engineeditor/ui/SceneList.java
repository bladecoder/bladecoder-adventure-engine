/*******************************************************************************
 * Copyright 2014 Rafael Garcia Moreno.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.bladecoder.engineeditor.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.components.CellRenderer;
import com.bladecoder.engineeditor.ui.components.EditModelDialog;
import com.bladecoder.engineeditor.ui.components.ModelList;
import com.bladecoder.engineeditor.undo.UndoDeleteScene;
import com.bladecoder.engineeditor.undo.UndoOp;
import com.bladecoder.engineeditor.utils.EditorLogger;
import com.bladecoder.engineeditor.utils.ElementUtils;

public class SceneList extends ModelList<World, Scene> {

	private ImageButton initBtn;
	private SelectBox<String> chapters;
	private HashMap<String, TextureRegion> bgIconCache = new HashMap<String, TextureRegion>();
	private boolean disposeBgCache = false;

	public SceneList(Skin skin) {
		super(skin, true);

		HorizontalGroup chapterPanel = new HorizontalGroup();
		chapters = new SelectBox<String>(skin);
		chapters.setFillParent(true);

		chapterPanel.addActor(new Label("CHAPTER ", skin, "big"));
		chapterPanel.addActor(chapters);

		clearChildren();

		add(chapterPanel).expandX().fillX();
		row();
		add(toolbar).expandX().fillX();
		row().fill();
		add(container).expandY().fill();

		initBtn = new ImageButton(skin);
		toolbar.addToolBarButton(initBtn, "ic_check", "Set init scene", "Set init scene");

		initBtn.setDisabled(true);

		list.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				int pos = list.getSelectedIndex();

				if (pos == -1) {
					Ctx.project.setSelectedScene(null);
				} else {
					Scene s = list.getItems().get(pos);

					Ctx.project.setSelectedScene(s);
				}

				toolbar.disableEdit(pos == -1);
				initBtn.setDisabled(pos == -1);
			}
		});

		list.setCellRenderer(listCellRenderer);

		initBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				setDefault();
			}

		});

		chapters.addListener(chapterListener);

		Ctx.project.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {

				EditorLogger.debug(evt.getPropertyName() + " NEW:" + evt.getNewValue() + " OLD:" + evt.getOldValue());

				if (evt.getPropertyName().equals(Project.CHAPTER_PROPERTY)) {
					addChapters();
				} else if (evt.getPropertyName().equals(Project.NOTIFY_ELEMENT_DELETED)) {
					if (evt.getNewValue() instanceof World) {
						addChapters();
					} else if (evt.getNewValue() instanceof Scene) {
						addElements(World.getInstance(),
								Arrays.asList(World.getInstance().getScenes().values().toArray(new Scene[0])));
					}
				} else if (evt.getPropertyName().equals(Project.NOTIFY_PROJECT_LOADED)) {
					toolbar.disableCreate(Ctx.project.getProjectDir() == null);

					disposeBgCache = true;
					addChapters();
				} else if (evt.getPropertyName().equals(Project.NOTIFY_ELEMENT_CREATED)) {
					if (evt.getNewValue() instanceof Scene && !(evt.getSource() instanceof EditSceneDialog)) {
						addElements(World.getInstance(),
								Arrays.asList(World.getInstance().getScenes().values().toArray(new Scene[0])));
					}
				}
			}
		});

	}

	ChangeListener chapterListener = new ChangeListener() {
		@Override
		public void changed(ChangeEvent event, Actor actor) {
			String selChapter = (String) chapters.getSelected();

			if (selChapter != null && !selChapter.equals(Ctx.project.getChapter().getId())) {

				// Save the project when changing chapter
				try {
					Ctx.project.saveProject();
				} catch (IOException e1) {
					Ctx.msg.show(getStage(), "Error saving project", 3);
					EditorLogger.error(e1.getMessage());
				}

				try {

					if (selChapter != null)
						Ctx.project.loadChapter(selChapter);

					addElements(World.getInstance(),
							Arrays.asList(World.getInstance().getScenes().values().toArray(new Scene[0])));
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	};

	public void addChapters() {
		String[] nl = Ctx.project.getChapter().getChapters();
		Array<String> array = new Array<String>();

		for (int i = 0; i < nl.length; i++) {
			array.add(nl[i]);
		}

		chapters.setItems(array);
		chapters.setSelected(Ctx.project.getChapter().getId());
		invalidate();
	}

	private void setDefault() {

		int pos = list.getSelectedIndex();

		if (pos == -1)
			return;

		String id = list.getItems().get(pos).getId();
		World.getInstance().setInitScene(id);
	}

	@Override
	protected void delete() {
		Scene s = removeSelected();

		parent.getScenes().remove(s.getId());

		// delete init_scene attr if the scene to delete is the chapter
		// init_scene
		if (parent.getInitScene().equals(s.getId())) {
			parent.setInitScene(null);
		}

		// TRANSLATIONS
		Ctx.project.getI18N().putTranslationsInElement(s);

		// UNDO
		UndoOp undoOp = new UndoDeleteScene(s);
		Ctx.project.getUndoStack().add(undoOp);

		Ctx.project.setModified();
	}

	@Override
	protected void copy() {
		Scene e = list.getSelected();

		if (e == null)
			return;

		clipboard = (Scene) ElementUtils.cloneElement(e);
		toolbar.disablePaste(false);

		// TRANSLATIONS
		Ctx.project.getI18N().putTranslationsInElement(clipboard);
	}

	@Override
	protected void paste() {
		Scene newElement = (Scene) ElementUtils.cloneElement(clipboard);

		newElement.setId(ElementUtils.getCheckedId(newElement.getId(),
				World.getInstance().getScenes().keySet().toArray(new String[0])));

		int pos = list.getSelectedIndex() + 1;

		list.getItems().insert(pos, newElement);

		World.getInstance().addScene(newElement);
		Ctx.project.getI18N().extractStrings(newElement);

		list.setSelectedIndex(pos);
		list.invalidateHierarchy();

		Ctx.project.setModified();
	}

	@Override
	protected EditModelDialog<World, Scene> getEditElementDialogInstance(Scene e) {

		EditSceneDialog dialog = new EditSceneDialog(skin, parent, e);

		if (e != null) {
			dialog.setListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					Ctx.project.setSelectedScene(list.getSelected());
				}
			});
		}

		return dialog;
	}

	public TextureRegion getBgIcon(String atlas, String region) {

		// check here for dispose instead in project loading because the opengl
		// context lost in new project thread
		if (disposeBgCache) {
			dispose();
			disposeBgCache = false;
		}

		String s = atlas + "#" + region;
		TextureRegion icon = bgIconCache.get(s);

		if (icon == null) {
			Batch batch = getStage().getBatch();
			batch.end();

			icon = createBgIcon(atlas, region);

			if (icon != null) {
				bgIconCache.put(s, icon);
			} else {
				EngineLogger.error("Error creating Background icon");
			}

			batch.begin();

		}

		return icon;
	}

	public void dispose() {
		for (TextureRegion r : bgIconCache.values())
			r.getTexture().dispose();

		bgIconCache.clear();
	}

	private TextureRegion createBgIcon(String atlas, String region) {
		TextureAtlas a = new TextureAtlas(Gdx.files
				.absolute(Ctx.project.getProjectPath() + "/" + Project.ATLASES_PATH + "/1/" + atlas + ".atlas"));
		AtlasRegion r = a.findRegion(region);

		if (r == null) {
			a.dispose();
			return null;
		}

		FrameBuffer fbo = new FrameBuffer(Format.RGBA8888, 200, (int) (r.getRegionHeight() * 200f / r.getRegionWidth()),
				false);

		SpriteBatch fboBatch = new SpriteBatch();
		fboBatch.setColor(Color.WHITE);
		OrthographicCamera camera = new OrthographicCamera();
		camera.setToOrtho(false, fbo.getWidth(), fbo.getHeight());
		fboBatch.setProjectionMatrix(camera.combined);

		Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
		fbo.begin();
		fboBatch.begin();
		fboBatch.draw(r, 0, 0, fbo.getWidth(), fbo.getHeight());
		fboBatch.end();

		TextureRegion tex = ScreenUtils.getFrameBufferTexture(0, 0, fbo.getWidth(), fbo.getHeight());
		// tex.flip(false, true);

		fbo.end();
		Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);

		fbo.dispose();
		a.dispose();
		fboBatch.dispose();

		return tex;
	}

	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private final CellRenderer<Scene> listCellRenderer = new CellRenderer<Scene>() {

		@Override
		protected String getCellTitle(Scene e) {
			String name = e.getId();

			// TODO SET INIT SCENE
			String init = World.getInstance().getInitScene();

			if (name.equals(init))
				name += " <init>";

			return name;
		}

		@Override
		protected String getCellSubTitle(Scene e) {
			return e.getBackgroundAtlas();
		}

		@Override
		public TextureRegion getCellImage(Scene e) {
			String atlas = e.getBackgroundAtlas();
			String region = e.getBackgroundRegionId();

			TextureRegion r = null;

			if (atlas != null && region != null && !atlas.isEmpty() && !region.isEmpty())
				r = getBgIcon(atlas, region);

			if (r == null)
				r = Ctx.assetManager.getIcon("ic_no_scene");

			return r;
		}

		@Override
		protected boolean hasSubtitle() {
			return true;
		}

		@Override
		protected boolean hasImage() {
			return true;
		}
	};
}
