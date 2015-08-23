package com.bladecoder.engineeditor.ui.components;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.utils.OptionsInputPanelUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class SoundInputPanel extends EditableOptionsInputPanel<String> implements LinkableInputPanel {
	SoundInputPanel(Skin skin, String title, String desc, boolean mandatory, String defaultValue) {
		super(skin, title, desc, mandatory, defaultValue, getValues(mandatory));
	}

	private static String[] getValues(boolean mandatory) {
		return !mandatory ? new String[]{""} : new String[0];
	}

	@Override
	public void linkChanged(String id) {
		setText("");

		if (id == null || id.isEmpty()) {
			input.setItems();
			return;
		}

		final Element actor = Ctx.project.getSelectedChapter().getActor(Ctx.project.getSelectedScene(), id);
		final NodeList sounds = actor.getElementsByTagName("sound");
		input.setItems(OptionsInputPanelUtils.getIdFromNodeList(isMandatory(), sounds));
	}
}
