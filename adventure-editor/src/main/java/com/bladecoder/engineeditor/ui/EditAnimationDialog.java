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

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashMap;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.anim.AnimationDesc;
import com.bladecoder.engine.anim.AtlasAnimationDesc;
import com.bladecoder.engine.anim.SpineAnimationDesc;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.anim.Tween.Type;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.model.ActorRenderer;
import com.bladecoder.engine.model.AnimationRenderer;
import com.bladecoder.engine.model.AtlasRenderer;
import com.bladecoder.engine.model.ImageRenderer;
import com.bladecoder.engine.model.Sprite3DRenderer;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.spine.SpineRenderer;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.EditorLogger;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.scneditor.AnimationWidget;
import com.bladecoder.engineeditor.ui.panels.EditModelDialog;
import com.bladecoder.engineeditor.ui.panels.FilteredSelectBox;
import com.bladecoder.engineeditor.ui.panels.InputPanel;
import com.bladecoder.engineeditor.ui.panels.InputPanelFactory;

public class EditAnimationDialog extends EditModelDialog<SpriteActor, AnimationDesc> {

	public static final String ANIMATION_TYPES[] = { Tween.Type.NO_REPEAT.toString(), Tween.Type.REPEAT.toString(),
			Tween.Type.YOYO.toString(), Tween.Type.REVERSE.toString() };

	public static final String INFO = "Define sprites and animations";

	InputPanel source;
	InputPanel atlas;
	InputPanel id;
	InputPanel localizable;
	InputPanel repeat;
	InputPanel speed;
	InputPanel count;
	InputPanel in;
	InputPanel out;
	InputPanel sound;
	InputPanel preload;
	InputPanel dispose;

	AnimationWidget spriteWidget = new AnimationWidget(this);

	@SuppressWarnings("unchecked")
	public EditAnimationDialog(Skin skin, SpriteActor p, AnimationDesc e) {
		super(skin);

		setInfo(INFO);

		source = InputPanelFactory.createInputPanel(skin, "Source",
				"Select the source where the sprite or animation is defined", new String[0], true);
		atlas = InputPanelFactory.createInputPanel(skin, "Atlas", "Select the atlas for the selected Spine skeleton",
				getAtlases(), true);
		localizable = InputPanelFactory.createInputPanel(skin, "Localizable",
				"True if the image is customizable per language.", Param.Type.BOOLEAN, true, "false");
		id = InputPanelFactory.createInputPanel(skin, "ID", "Select the id of the animation", new String[0], true);
		repeat = InputPanelFactory.createInputPanel(skin, "Animation type", "Select the type of the animation",
				Param.Type.OPTION, true, Tween.Type.NO_REPEAT.toString(), Tween.Type.class.getEnumConstants());

		speed = InputPanelFactory.createInputPanel(skin, "Speed", "Select the speed of the animation in secods",
				Param.Type.FLOAT, true, "1.0");
		count = InputPanelFactory.createInputPanel(skin, "Count", "Select the repeat times. -1 for infinity",
				Param.Type.INTEGER, true, "-1");
		in = InputPanelFactory.createInputPanel(skin, "In Dist",
				"Select the distance in pixels to add to the actor position when the sprite is displayed",
				Param.Type.VECTOR2, false);
		out = InputPanelFactory.createInputPanel(skin, "Out Dist",
				"Select the distance in pixels to add to the actor position when the sprite is changed",
				Param.Type.VECTOR2, false);
		sound = InputPanelFactory.createInputPanel(skin, "Sound",
				"Select the sound ID that will be played when showing", Param.Type.SOUND, false);
		preload = InputPanelFactory.createInputPanel(skin, "Preload", "Preload the animation when the scene is loaded",
				Param.Type.BOOLEAN, true, "true");
		dispose = InputPanelFactory.createInputPanel(skin, "Dispose When Played", "Dispose de animation after playing",
				Param.Type.BOOLEAN, true, "false");

		((SelectBox<String>) repeat.getField()).addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				showHideFieldsDelayCountFields();
			}
		});

		((FilteredSelectBox<String>) source.getField()).addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				EditorLogger.debug("EditAnimationDialog.setSource():" + source.getText());

				setSource();
				fillAnimations();
			}
		});

		((FilteredSelectBox<String>) id.getField()).addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				setAnimation();
			}
		});

		((FilteredSelectBox<String>) atlas.getField()).addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				setSource();
				fillAnimations();
			}
		});

		((TextField) speed.getField()).addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				setAnimation();
			}
		});

		setInfoWidget(spriteWidget);

		init(p, e, new InputPanel[] { source, localizable, atlas, id, repeat, speed, count, in, out, sound, preload,
				dispose });

		addSources();

		// call modelToInputs again to set the correct source
		if (e != null)
			modelToInputs();

		setVisible(count, false);
		setVisible(atlas, false);
		setVisible(localizable, false);

		ActorRenderer renderer = parent.getRenderer();
		if (renderer instanceof ImageRenderer) {
			setVisible(localizable, true);
			setVisible(speed, false);
			setVisible(repeat, false);
			setVisible(id, false);
		}

		if (e != null) {
			source.setText(e.source);
		} else {
			// If the actor has some animation, set the same source.
			HashMap<String, AnimationDesc> animations = ((AnimationRenderer) p.getRenderer()).getAnimations();
			if (animations.size() > 0) {
				source.setText(animations.values().iterator().next().source);
			} else {
				// set the background source it atlas type
				if (p.getRenderer() instanceof AtlasRenderer && p.getScene().getBackgroundAtlas() != null)
					source.setText(p.getScene().getBackgroundAtlas());
			}
		}

		if (source.getText() != null && !source.getText().isEmpty()) {
			setSource();

			fillAnimations();

			if (e != null) {
				id.setText(e.id);
			}
		}
	}

	private void showHideFieldsDelayCountFields() {
		String type = repeat.getText();

		if (type.equals(Tween.Type.REPEAT.toString()) || type.equals(Tween.Type.YOYO.toString())) {
			setVisible(count, true);
		} else {
			setVisible(count, false);
		}
	}

	private void setSource() {
		AnimationDesc anim = null;

		ActorRenderer renderer = parent.getRenderer();
		String sourceStr = source.getText();

		if (renderer instanceof SpineRenderer) {
			anim = new SpineAnimationDesc();

			if (spineAtlasExists(sourceStr)) {
				((SpineAnimationDesc) anim).atlas = null;
				setVisible(atlas, false);
			} else {
				if (!atlas.isVisible()) {
					setVisible(atlas, true);
				}

				((SpineAnimationDesc) anim).atlas = atlas.getText();
			}

		} else if (renderer instanceof AtlasRenderer) {
			anim = new AtlasAnimationDesc();
		} else {
			anim = new AnimationDesc();
		}

		anim.source = sourceStr;
		anim.count = Tween.INFINITY;
		anim.preload = true;
		anim.disposeWhenPlayed = false;

		if (renderer instanceof SpineRenderer) {
			spriteWidget.setSource(Project.SPINE_RENDERER_STRING, anim);
		} else if (renderer instanceof AtlasRenderer) {
			spriteWidget.setSource(Project.ATLAS_RENDERER_STRING, anim);
		} else if (renderer instanceof ImageRenderer) {
			spriteWidget.setSource(Project.IMAGE_RENDERER_STRING, anim);
		} else if (renderer instanceof Sprite3DRenderer) {
			spriteWidget.setSource(Project.S3D_RENDERER_STRING, anim);
		}
	}

	public boolean spineAtlasExists(String source) {
		return EngineAssetManager.getInstance()
				.assetExists(Ctx.project.getAssetPath() + Project.ATLASES_PATH + "/" + source + ".atlas");
	}

	private void setAnimation() {
		String ids = id.getText();
		String type = repeat.getText();
		String speedStr = speed.getText();

		@SuppressWarnings("unchecked")
		FilteredSelectBox<String> cb = (FilteredSelectBox<String>) id.getField();

		if (e != null || cb.getSelectedIndex() != 0)
			spriteWidget.setAnimation(ids, speedStr, Tween.Type.valueOf(type));
	}

	private void fillAnimations() {
		@SuppressWarnings("unchecked")
		FilteredSelectBox<String> cb = (FilteredSelectBox<String>) id.getField();
		cb.clearItems();

		// When creating, give option to add all elements
		if (e == null)
			cb.getItems().add("<ADD ALL>");

		String ids[] = spriteWidget.getAnimations();
		for (String s : ids)
			cb.getItems().add(s);

		cb.getList().setItems(cb.getItems());
		if (cb.getItems().size > 0)
			cb.setSelectedIndex(0);

		cb.invalidateHierarchy();

		setAnimation();
	}

	String ext;

	private void addSources() {
		@SuppressWarnings("unchecked")
		FilteredSelectBox<String> cb = (FilteredSelectBox<String>) source.getField();
		String[] src = getSources();
		cb.getItems().clear();

		for (String s : src)
			cb.getItems().add(s);

		cb.getList().setItems(cb.getItems());
		if (cb.getItems().size > 0)
			cb.setSelectedIndex(0);
		cb.invalidateHierarchy();
	}

	private String[] getSources() {
		String path = null;
		ActorRenderer renderer = parent.getRenderer();

		if (renderer instanceof AtlasRenderer) {
			path = Ctx.project.getAssetPath() + Project.ATLASES_PATH + "/" + Ctx.project.getResDir();
			ext = EngineAssetManager.ATLAS_EXT;
		} else if (renderer instanceof Sprite3DRenderer) {
			path = Ctx.project.getAssetPath() + Project.SPRITE3D_PATH;
			ext = EngineAssetManager.MODEL3D_EXT;
		} else if (renderer instanceof SpineRenderer) {
			path = Ctx.project.getAssetPath() + Project.SPINE_PATH;
			ext = EngineAssetManager.SPINE_EXT;
		} else if (renderer instanceof ImageRenderer) {
			path = Ctx.project.getAssetPath() + Project.IMAGE_PATH + "/" + Ctx.project.getResDir();
			ext = "";
		}

		File f = new File(path);

		String sources[] = f.list(new FilenameFilter() {

			@Override
			public boolean accept(File arg0, String arg1) {
				if (arg1.endsWith(ext))
					return true;

				return false;
			}
		});

		if (sources != null) {
			Arrays.sort(sources);

			for (int i = 0; i < sources.length; i++)
				sources[i] = sources[i].substring(0, sources[i].length() - ext.length());
		} else {
			sources = new String[0];
		}

		return sources;
	}

	private String[] getAtlases() {
		String path = Ctx.project.getAssetPath() + Project.ATLASES_PATH + "/" + Ctx.project.getResDir();

		File f = new File(path);

		String atlases[] = f.list(new FilenameFilter() {

			@Override
			public boolean accept(File arg0, String arg1) {
				if (arg1.endsWith(EngineAssetManager.ATLAS_EXT))
					return true;

				return false;
			}
		});

		if (atlases != null) {
			Arrays.sort(atlases);

			for (int i = 0; i < atlases.length; i++)
				atlases[i] = atlases[i].substring(0, atlases[i].length() - EngineAssetManager.ATLAS_EXT.length());
		} else {
			atlases = new String[0];
		}

		return atlases;
	}

	/**
	 * Override to append all animations if selected.
	 */
	@Override
	protected void ok() {
		@SuppressWarnings("unchecked")
		FilteredSelectBox<String> cb = (FilteredSelectBox<String>) id.getField();

		if (e == null && cb.getSelectedIndex() == 0) {
			for (int i = 1; i < cb.getItems().size; i++) {
				cb.setSelectedIndex(i);
				inputsToModel(true);
				// doc.setId(e, cb.getItems().get(i));

				if (listener != null)
					listener.changed(new ChangeEvent(), this);
			}

		} else {
			super.ok();
		}
	}

	@Override
	protected void inputsToModel(boolean create) {

		String sourceStr = source.getText();
		AnimationRenderer renderer = (AnimationRenderer) parent.getRenderer();

		if (create) {

			if (renderer instanceof SpineRenderer) {
				e = new SpineAnimationDesc();

				if (spineAtlasExists(sourceStr)) {
					((SpineAnimationDesc) e).atlas = null;
					setVisible(atlas, false);
				} else {
					if (!atlas.isVisible()) {
						setVisible(atlas, true);
					}

					((SpineAnimationDesc) e).atlas = atlas.getText();
				}

			} else if (renderer instanceof AtlasRenderer) {
				e = new AtlasAnimationDesc();
			} else {
				e = new AnimationDesc();
			}
		} else {
			HashMap<String, AnimationDesc> animations = renderer.getAnimations();
			animations.remove(e.id);

			if (e.id.equals(renderer.getInitAnimation()))
				renderer.setInitAnimation(null);
		}

		e.id = id.getText();
		e.sound = sound.getText();
		e.source = sourceStr;
		e.count = Integer.parseInt(count.getText());
		e.preload = Boolean.parseBoolean(preload.getText());
		e.disposeWhenPlayed = Boolean.parseBoolean(dispose.getText());
		e.animationType = Type.valueOf(repeat.getText());
		e.inD = Param.parseVector2(in.getText());
		e.outD = Param.parseVector2(out.getText());
		e.duration = Float.parseFloat(speed.getText());

		((AnimationRenderer) parent.getRenderer()).addAnimation(e);

		if (renderer instanceof ImageRenderer && Boolean.parseBoolean(localizable.getText()) && e.source != null
				&& e.source.length() > 0) {
			e.source = I18N.PREFIX + e.source;
		}

		if (renderer.getInitAnimation() == null)
			renderer.setInitAnimation(e.id);

		// TODO UNDO OP
		// UndoOp undoOp = new UndoAddElement(doc, e);
		// Ctx.project.getUndoStack().add(undoOp);

		Ctx.project.setModified();
	}

	@Override
	protected void modelToInputs() {
		source.setText(e.source);

		if (atlas.isVisible() && e instanceof SpineAnimationDesc)
			atlas.setText(((SpineAnimationDesc) e).atlas);

		id.setText(e.id);
		repeat.setText(e.animationType.toString());
		speed.setText(Float.toString(e.duration));
		count.setText(Integer.toString(e.count));
		in.setText(Param.toStringParam(e.inD));
		out.setText(Param.toStringParam(e.outD));
		sound.setText(e.sound);
		preload.setText(Boolean.toString(e.preload));
		dispose.setText(Boolean.toString(e.disposeWhenPlayed));

		showHideFieldsDelayCountFields();

		ActorRenderer renderer = parent.getRenderer();
		if (renderer instanceof ImageRenderer && e.source != null && e.source.length() > 1
				&& e.source.charAt(0) == I18N.PREFIX) {
			localizable.setText("true");
			source.setText(e.source.substring(1));
		}
	}

}
