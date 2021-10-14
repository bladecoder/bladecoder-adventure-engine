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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.DPIUtils;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.EditorLogger;
import com.bladecoder.engineeditor.common.ElementUtils;
import com.bladecoder.engineeditor.common.Message;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.panels.CellRenderer;
import com.bladecoder.engineeditor.ui.panels.EditModelDialog;
import com.bladecoder.engineeditor.ui.panels.ModelList;
import com.bladecoder.engineeditor.undo.UndoDeleteScene;
import com.bladecoder.engineeditor.undo.UndoOp;

public class SceneList extends ModelList<World, Scene> {

	private ImageButton initBtn;
	private ImageButton reloadBtn;
	private SelectBox<String> chapters;
	private HashMap<String, TextureRegion> bgIconCache = new HashMap<>();
	private boolean disposeBgCache = false;
	private String filterText;

	public SceneList(final Skin skin) {
		super(skin, true);

		HorizontalGroup chapterPanel = new HorizontalGroup();
		chapterPanel.padRight(DPIUtils.MARGIN_SIZE);

		chapters = new SelectBox<>(skin);
		// chapters.setFillParent(true);
		TextButton inkBtn = new TextButton("Ink", skin, "no-toggled");

		TextTooltip t = new TextTooltip("Sets the Ink Story file", skin);
		inkBtn.addListener(t);

		inkBtn.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				EditInkDialog dialog = new EditInkDialog(skin);
				dialog.show(getStage());
			}
		});

		chapterPanel.addActor(new Label("CHAPTER ", skin, "big"));
		chapterPanel.addActor(chapters);
		chapterPanel.addActor(inkBtn);

		clearChildren();

		add(chapterPanel).expandX().fillX();
		row();
		add(toolbar).expandX().fillX();
		row().fill();
		add(container).expandY().fill();

		initBtn = new ImageButton(skin);
		toolbar.addToolBarButton(initBtn, "ic_check", "Set init scene", "Set init scene");

		initBtn.setDisabled(true);

		reloadBtn = new ImageButton(skin);
		toolbar.addToolBarButton(reloadBtn, "ic_reload_small", "Reload Assets", "Reload current scene assets");

		reloadBtn.setDisabled(true);

		toolbar.addFilterBox(new EventListener() {

			@Override
			public boolean handle(Event e) {
				if (((TextField) e.getTarget()).getText() != filterText) {
					filterText = ((TextField) e.getTarget()).getText();

					addFilteredElements();
				}

				return false;
			}

		});

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
				reloadBtn.setDisabled(pos == -1);
			}
		});

		list.setCellRenderer(listCellRenderer);

		initBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				setDefault();
			}

		});

		reloadBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				reloadAssets();
			}

		});

		chapters.addListener(chapterListener);
		chapters.getSelection().setProgrammaticChangeEvents(false);

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
						addFilteredElements();
					}
				} else if (evt.getPropertyName().equals(Project.NOTIFY_PROJECT_LOADED)) {
					toolbar.disableCreate(!Ctx.project.isLoaded());

					disposeBgCache = true;
					addChapters();
				} else if (evt.getPropertyName().equals(Project.NOTIFY_ELEMENT_CREATED)) {
					if (evt.getNewValue() instanceof Scene && !(evt.getSource() instanceof EditSceneDialog)) {
						addFilteredElements();
					}
				}
			}
		});

	}

	ChangeListener chapterListener = new ChangeListener() {
		@Override
		public void changed(ChangeEvent event, Actor actor) {
			String selChapter = chapters.getSelected();

			if (selChapter != null) {

				if (selChapter.equals(Ctx.project.getChapter().getId()))
					return;

				// Save the project when changing chapter
				try {
					Ctx.project.saveProject();
				} catch (IOException e1) {
					Message.showMsgDialog(getStage(), "Error saving project", e1.getMessage());
					EditorLogger.error(e1.getMessage());
				}

				try {

					if (selChapter != null)
						Ctx.project.loadChapter(selChapter);

					String init = Ctx.project.getEditorConfig().getProperty("project.selectedScene",
							Ctx.project.getWorld().getInitScene());

					addFilteredElements();

					if (init != null) {
						Scene s = Ctx.project.getWorld().getScenes().get(init);

						if (s == null && Ctx.project.getWorld().getInitScene() != null)
							s = Ctx.project.getWorld().getScenes().get(Ctx.project.getWorld().getInitScene());

						if (s != null) {
							int indexOf = list.getItems().indexOf(s, true);
							list.setSelectedIndex(indexOf);
						}
					}
				} catch (IOException e1) {
					EditorLogger.printStackTrace(e1);
				}
			} else {
				addElements(null, null);
			}
		}
	};

	public void addChapters() {
		Array<String> array = new Array<>();

		if (Ctx.project.isLoaded()) {

			String[] nl = Ctx.project.getChapter().getChapters();

			for (int i = 0; i < nl.length; i++) {
				array.add(nl[i]);
			}

			chapters.setItems(array);

			String init = Ctx.project.getEditorConfig().getProperty("project.selectedChapter",
					Ctx.project.getWorld().getInitChapter());

			if (init != null) {
				if (array.contains(init, false)) {
					chapters.setSelected(init);
				} else if (array.size > 0) {
					chapters.setSelected(Ctx.project.getChapter().getInitChapter());
				}
			}
		} else {
			chapters.setItems(array);
		}

		chapterListener.changed(null, null);

		invalidate();
	}

	private void addFilteredElements() {

		List<Scene> filtered = new ArrayList<>();

		for (Scene s : Ctx.project.getWorld().getScenes().values()) {
			if (filterText == null || filterText.isEmpty() || s.getId().contains(filterText)) {
				filtered.add(s);
			}
		}

		addElements(Ctx.project.getWorld(), filtered);

	}

	private void setDefault() {

		int pos = list.getSelectedIndex();

		if (pos == -1)
			return;

		String id = list.getItems().get(pos).getId();
		Ctx.project.getWorld().setInitScene(id);
		Ctx.project.setModified();
	}

	private void reloadAssets() {
		Ctx.project.setSelectedScene(list.getSelected());
	}

	@Override
	protected void delete() {
		Scene s = removeSelected();

		parent.getScenes().remove(s.getId());

		// delete init_scene attr if the scene to delete is the chapter
		// init_scene
		if (parent.getInitScene() != null && parent.getInitScene().equals(s.getId())) {
			if (parent.getScenes().size() > 0)
				parent.setInitScene(parent.getScenes().values().iterator().next().getId());
			else
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
				Ctx.project.getWorld().getScenes().keySet().toArray(new String[0])));

		int pos = list.getSelectedIndex() + 1;

		list.getItems().insert(pos, newElement);

		Ctx.project.getWorld().addScene(newElement);
		Ctx.project.getI18N().extractStrings(newElement);

		if (parent.getInitScene() == null) {
			parent.setInitScene(newElement.getId());
		}

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

			try {
				icon = createBgIcon(atlas, region);
			} catch (Exception e) {
				EditorLogger.error("Error creating Background icon: " + atlas + "." + region);
			}

			if (icon == null) {
				EditorLogger.error("Error creating Background icon: " + atlas + "." + region);
				icon = Ctx.assetManager.getIcon("ic_no_scene");
			}

			bgIconCache.put(s, icon);

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
		TextureAtlas a = new TextureAtlas(
				Gdx.files.absolute(Ctx.project.getAssetPath() + Project.ATLASES_PATH + "/1/" + atlas + ".atlas"));
		AtlasRegion r = a.findRegion(region);

		if (r == null) {
			a.dispose();
			return null;
		}

		GLFrameBuffer.FrameBufferBuilder frameBufferBuilder = new GLFrameBuffer.FrameBufferBuilder(200,
				(int) (r.getRegionHeight() * 200f / r.getRegionWidth()));

		frameBufferBuilder.addColorTextureAttachment(GL30.GL_RGBA8, GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE);
		FrameBuffer fbo = frameBufferBuilder.build();

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
			String init = Ctx.project.getWorld().getInitScene();

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

			if (atlas != null && region != null && !atlas.isEmpty() && !region.isEmpty()) {
				r = getBgIcon(atlas, region);
			}

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
