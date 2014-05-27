package org.bladecoder.engineeditor.ui;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

import org.bladecoder.engine.actions.Param;
import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.model.ChapterDocument;
import org.bladecoder.engineeditor.model.Project;
import org.bladecoder.engineeditor.scn.SpriteWidget;
import org.bladecoder.engineeditor.ui.components.EditElementDialog;
import org.bladecoder.engineeditor.ui.components.InputPanel;
import org.bladecoder.engineeditor.utils.EditorLogger;
import org.w3c.dom.Element;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;

public class EditFADialog extends EditElementDialog {
	public static final String INFO = "Define sprites and frame animations";

	private InputPanel[] inputs = new InputPanel[11];
	InputPanel typePanel;

	String attrs[] = { "source", "id", "animation_type", "speed", "delay",
			"count", "inD", "outD", "sound", "preload", "disposed_when_played" };

	SpriteWidget spriteWidget = new SpriteWidget(this);

	@SuppressWarnings("unchecked")
	public EditFADialog(Skin skin, BaseDocument doc, Element p, Element e) {
		super(skin);

		setInfo(INFO);

		inputs[0] = new InputPanel(skin, "Source",
				"Select the source where the sprite or animation is defined",
				new String[0]);
		inputs[1] = new InputPanel(skin, "ID",
				"Select the id of the animation", new String[0]);
		inputs[2] = new InputPanel(skin, "Animation type",
				"Select the type of the animation",
				ChapterDocument.ANIMATION_TYPES);
		inputs[3] = new InputPanel(skin, "Speed",
				"Select the speed of the animation in secods",
				Param.Type.FLOAT, true);
		inputs[4] = new InputPanel(skin, "Delay",
				"Select the delay between repeats in seconds",
				Param.Type.FLOAT, false);
		inputs[5] = new InputPanel(skin, "Count", "Select the repeat times",
				Param.Type.INTEGER, false);
		inputs[6] = new InputPanel(
				skin,
				"In Dist",
				"Select the distance in pixels to add to the actor position when the sprite is displayed",
				Param.Type.VECTOR2, false);
		inputs[7] = new InputPanel(
				skin,
				"Out Dist",
				"Select the distance in pixels to add to the actor position when the sprite is changed",
				Param.Type.VECTOR2, false);
		inputs[8] = new InputPanel(skin, "Sound",
				"Select the sound ID that will be play when displayed");
		inputs[9] = new InputPanel(skin, "Preload",
				"Preload the animation when the scene is loaded",
				Param.Type.BOOLEAN, true, "true", null);
		inputs[10] = new InputPanel(skin, "Dispose When Played",
				"Dispose de animation when the animation is played",
				Param.Type.BOOLEAN, true, "false", null);

		typePanel = inputs[2];

		((SelectBox<String>) typePanel.getField())
				.addListener(new ChangeListener() {

					@Override
					public void changed(ChangeEvent event, Actor actor) {
						String type = typePanel.getText();

						if (type.equals("repeat") || type.equals("yoyo")) {
							inputs[4].setVisible(true);
							inputs[5].setVisible(true);
						} else {
							inputs[4].setVisible(false);
							inputs[5].setVisible(false);
						}
					}
				});

		((SelectBox<String>) inputs[0].getField())
				.addListener(new ChangeListener() {
					@Override
					public void changed(ChangeEvent event, Actor actor) {
						EditorLogger.debug("CreateEditFADialog.setSource():"
								+ inputs[0].getText());

						spriteWidget.setSource(parent.getAttribute("type"),
								inputs[0].getText());

						fillAnimations();
					}
				});

		((SelectBox<String>) inputs[1].getField())
				.addListener(new ChangeListener() {
					@Override
					public void changed(ChangeEvent event, Actor actor) {
						setSprite();
					}
				});

		((TextField) inputs[3].getField()).addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				setSprite();
			}
		});

		inputs[4].setVisible(false);
		inputs[5].setVisible(false);

		init(inputs, attrs, doc, p, "frame_animation", e);

		setInfoWidget(spriteWidget);

		addSources();

		if (inputs[0].getText() != null && !inputs[0].getText().isEmpty()) {
			spriteWidget.setSource(parent.getAttribute("type"),
					inputs[0].getText());

			fillAnimations();
		}
	}

	private void setSprite() {
		String id = inputs[1].getText();
		String type = typePanel.getText();
		String speed = inputs[3].getText();

		@SuppressWarnings("unchecked")
		SelectBox<String> cb = (SelectBox<String>) inputs[1].getField();

		if (e != null || cb.getSelectedIndex() != 0)
			spriteWidget.setFrameAnimation(id, speed, type);
	}

	private void fillAnimations() {
		EditorLogger.debug("CreateEditFADialog.fillAnimations()");

		@SuppressWarnings("unchecked")
		SelectBox<String> cb = (SelectBox<String>) inputs[1].getField();
		cb.getItems().clear();

		// When creating, give option to add all elements
		if (e == null)
			cb.getItems().add("<ADD ALL>");

		String ids[] = spriteWidget.getAnimations();
		for (String s : ids)
			cb.getItems().add(s);

		cb.getList().setItems(cb.getItems());

		cb.invalidateHierarchy();

		setSprite();
	}

	String ext;

	private void addSources() {
		@SuppressWarnings("unchecked")
		SelectBox<String> cb = (SelectBox<String>) inputs[0].getField();
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
		String type = parent.getAttribute("type");

		if (type.equals(ChapterDocument.FOREGROUND_ACTOR_TYPE)
				|| type.equals(ChapterDocument.ATLAS_ACTOR_TYPE)) {
			path = Ctx.project.getProjectPath() + Project.ATLASES_PATH + "/"
					+ Ctx.project.getResDir();
			ext = ".atlas";
		} else if (type.equals(ChapterDocument.SPRITE3D_ACTOR_TYPE)) {
			path = Ctx.project.getProjectPath() + Project.SPRITE3D_PATH;
			ext = ".g3db";
		} else if (type.equals(ChapterDocument.SPINE_ACTOR_TYPE)) {
			path = Ctx.project.getProjectPath() + Project.SPINE_PATH;
			ext = ".json";
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
		}

		return sources;
	}

	/**
	 * Override to append all animations if selected.
	 */
	@Override
	protected void ok() {
		@SuppressWarnings("unchecked")
		SelectBox<String> cb = (SelectBox<String>) inputs[1].getField();

		if (e == null && cb.getSelectedIndex() == 0) {
			for (int i = 1; i < cb.getItems().size; i++) {
				create();
				fill();
				doc.setId(e, cb.getItems().get(i));
			}

			if (listener != null)
				listener.changed(new ChangeEvent(), this);
		} else {
			super.ok();
		}
	}

}
