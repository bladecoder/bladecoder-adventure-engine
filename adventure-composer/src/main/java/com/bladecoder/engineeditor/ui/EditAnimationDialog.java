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

import org.w3c.dom.Element;

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
import com.bladecoder.engine.loader.XMLConstants;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.BaseDocument;
import com.bladecoder.engineeditor.model.ChapterDocument;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.scneditor.AnimationWidget;
import com.bladecoder.engineeditor.ui.components.EditElementDialog;
import com.bladecoder.engineeditor.ui.components.InputPanel;
import com.bladecoder.engineeditor.ui.components.InputPanelFactory;
import com.bladecoder.engineeditor.utils.EditorLogger;

public class EditAnimationDialog extends EditElementDialog {
	public static final String INFO = "Define sprites and animations";
	
	private static final int SOURCE_INPUTPANEL = 0;
	private static final int ATLAS_INPUTPANEL = 1;
	private static final int ID_INPUTPANEL = 2;
	private static final int TYPE_INPUTPANEL = 3;
	private static final int SPEED_INPUTPANEL = 4;
	private static final int DELAY_INPUTPANEL = 5;
	private static final int COUNT_INPUTPANEL = 6;
	
	private static final String ATLAS_EXT = ".atlas";

	private InputPanel[] inputs = new InputPanel[12];
	InputPanel typePanel;

	String attrs[] = { XMLConstants.SOURCE_ATTR, XMLConstants.ATLAS_VALUE, XMLConstants.ID_ATTR, XMLConstants.ANIMATION_TYPE_ATTR, XMLConstants.SPEED_ATTR, XMLConstants.DELAY_ATTR,
			XMLConstants.COUNT_ATTR, XMLConstants.IND_ATTR, XMLConstants.OUTD_ATTR, XMLConstants.SOUND_ATTR, XMLConstants.PRELOAD_ATTR, XMLConstants.DISPOSE_WHEN_PLAYED_ATTR };

	AnimationWidget spriteWidget = new AnimationWidget(this);

	@SuppressWarnings("unchecked")
	public EditAnimationDialog(Skin skin, BaseDocument doc, Element p, Element e) {
		super(skin);

		setInfo(INFO);

		inputs[0] = InputPanelFactory.createInputPanel(skin, "Source",
				"Select the source where the sprite or animation is defined",
				new String[0], true);
		inputs[1] = InputPanelFactory.createInputPanel(skin, "Atlas",
				"Select the atlas for the selected Spine skeleton",
				getAtlases(), true);
		inputs[2] = InputPanelFactory.createInputPanel(skin, "ID",
				"Select the id of the animation", new String[0], true);
		inputs[3] = InputPanelFactory.createInputPanel(skin, "Animation type",
				"Select the type of the animation",
				ChapterDocument.ANIMATION_TYPES, true);
		inputs[4] = InputPanelFactory.createInputPanel(skin, "Speed",
				"Select the speed of the animation in secods",
				Param.Type.FLOAT, true, "1.0");
		inputs[5] = InputPanelFactory.createInputPanel(skin, "Delay",
				"Select the delay between repeats in seconds",
				Param.Type.FLOAT, false);
		inputs[6] = InputPanelFactory.createInputPanel(skin, "Count", "Select the repeat times",
				Param.Type.INTEGER, false);
		inputs[7] = InputPanelFactory.createInputPanel(
				skin,
				"In Dist",
				"Select the distance in pixels to add to the actor position when the sprite is displayed",
				Param.Type.VECTOR2, false);
		inputs[8] = InputPanelFactory.createInputPanel(
				skin,
				"Out Dist",
				"Select the distance in pixels to add to the actor position when the sprite is changed",
				Param.Type.VECTOR2, false);
		inputs[9] = InputPanelFactory.createInputPanel(skin, "Sound",
				"Select the sound ID that will be play when displayed");
		inputs[10] = InputPanelFactory.createInputPanel(skin, "Preload",
				"Preload the animation when the scene is loaded",
				Param.Type.BOOLEAN, true, "true", null);
		inputs[11] = InputPanelFactory.createInputPanel(skin, "Dispose When Played",
				"Dispose de animation when the animation is played",
				Param.Type.BOOLEAN, true, "false", null);

		typePanel = inputs[TYPE_INPUTPANEL];

		((SelectBox<String>) typePanel.getField())
				.addListener(new ChangeListener() {

					@Override
					public void changed(ChangeEvent event, Actor actor) {
						String type = typePanel.getText();

						if (type.equals(XMLConstants.REPEAT_VALUE) || type.equals(XMLConstants.YOYO_VALUE)) {
							setVisible(inputs[DELAY_INPUTPANEL],true);
							setVisible(inputs[COUNT_INPUTPANEL],true);
						} else {
							setVisible(inputs[DELAY_INPUTPANEL],false);
							setVisible(inputs[COUNT_INPUTPANEL],false);
						}
					}
				});

		((SelectBox<String>) inputs[SOURCE_INPUTPANEL].getField())
				.addListener(new ChangeListener() {
					@Override
					public void changed(ChangeEvent event, Actor actor) {
						EditorLogger.debug("EditAnimationDialog.setSource():"
								+ inputs[SOURCE_INPUTPANEL].getText());

						setSource();
						fillAnimations();
					}
				});

		((SelectBox<String>) inputs[ID_INPUTPANEL].getField())
				.addListener(new ChangeListener() {
					@Override
					public void changed(ChangeEvent event, Actor actor) {
						setAnimation();
					}
				});

		((TextField) inputs[SPEED_INPUTPANEL].getField()).addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				setAnimation();
			}
		});	

		setInfoWidget(spriteWidget);
		init(inputs, attrs, doc, p, XMLConstants.ANIMATION_TAG, e);
		
		setVisible(inputs[DELAY_INPUTPANEL],false);
		setVisible(inputs[COUNT_INPUTPANEL],false);
		
		setVisible(inputs[ATLAS_INPUTPANEL],false);

		addSources();
		if(e !=  null) {
			inputs[SOURCE_INPUTPANEL].setText(e.getAttribute(attrs[SOURCE_INPUTPANEL]));
		}

		if (inputs[SOURCE_INPUTPANEL].getText() != null && !inputs[SOURCE_INPUTPANEL].getText().isEmpty()) {
			setSource();

			fillAnimations();
			
			if(e !=  null) {		
				inputs[ID_INPUTPANEL].setText(e.getAttribute(attrs[ID_INPUTPANEL]));
			}
		}
	}
	
	private void setSource() {
		AnimationDesc anim = null;
		
		String type = parent.getAttribute(XMLConstants.TYPE_ATTR);
		String source = inputs[SOURCE_INPUTPANEL].getText();
		
		if (type.equals(XMLConstants.SPINE_VALUE)) {
			anim = new SpineAnimationDesc();
		} else if (type.equals(XMLConstants.ATLAS_VALUE)) {
			anim = new AtlasAnimationDesc();
		} else {
			anim = new AnimationDesc();
		}
		
		anim.source = source;
		anim.count = Tween.INFINITY;
		anim.preload = true;
		anim.disposeWhenPlayed = false;	
		
		spriteWidget.setSource(type, anim);
	}

	private void setAnimation() {
		String id = inputs[ID_INPUTPANEL].getText();
		String type = typePanel.getText();
		String speed = inputs[SPEED_INPUTPANEL].getText();

		@SuppressWarnings("unchecked")
		SelectBox<String> cb = (SelectBox<String>) inputs[ID_INPUTPANEL].getField();

		if (e != null || cb.getSelectedIndex() != 0)
			spriteWidget.setAnimation(id, speed, type);
	}

	private void fillAnimations() {
		@SuppressWarnings("unchecked")
		SelectBox<String> cb = (SelectBox<String>) inputs[ID_INPUTPANEL].getField();
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
		SelectBox<String> cb = (SelectBox<String>) inputs[SOURCE_INPUTPANEL].getField();
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
		String type = parent.getAttribute(XMLConstants.TYPE_ATTR);

		if (type.equals(XMLConstants.ATLAS_VALUE)) {
			path = Ctx.project.getProjectPath() + Project.ATLASES_PATH + "/"
					+ Ctx.project.getResDir();
			ext = ".atlas";
		} else if (type.equals(XMLConstants.S3D_VALUE)) {
			path = Ctx.project.getProjectPath() + Project.SPRITE3D_PATH;
			ext = ".g3db";
		} else if (type.equals(XMLConstants.SPINE_VALUE)) {
			path = Ctx.project.getProjectPath() + Project.SPINE_PATH;
			ext = ".skel";
		} else if (type.equals(XMLConstants.IMAGE_VALUE)) {
			path = Ctx.project.getProjectPath() + Project.IMAGE_PATH + "/"
					+ Ctx.project.getResDir();
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
				sources[i] = sources[i].substring(0,
						sources[i].length() - ext.length());
		} else {
			sources = new String[0];
		}

		return sources;
	}
	
	private String[] getAtlases() {
		String path = Ctx.project.getProjectPath() + Project.ATLASES_PATH + "/"
				+ Ctx.project.getResDir();

		File f = new File(path);

		String atlases[] = f.list(new FilenameFilter() {

			@Override
			public boolean accept(File arg0, String arg1) {
				if (arg1.endsWith(ATLAS_EXT))
					return true;

				return false;
			}
		});

		if (atlases != null) {
			Arrays.sort(atlases);

			for (int i = 0; i < atlases.length; i++)
				atlases[i] = atlases[i].substring(0,
						atlases[i].length() - ATLAS_EXT.length());
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
		SelectBox<String> cb = (SelectBox<String>) inputs[ID_INPUTPANEL].getField();

		if (e == null && cb.getSelectedIndex() == 0) {
			for (int i = 1; i < cb.getItems().size; i++) {
				cb.setSelectedIndex(i);
				create();
				fill();
//				doc.setId(e, cb.getItems().get(i));
				
				if (listener != null)
					listener.changed(new ChangeEvent(), this);
			}

			
		} else {
			super.ok();
		}
	}

}
