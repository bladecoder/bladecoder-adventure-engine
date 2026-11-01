package com.bladecoder.engineeditor.ui.panels;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engine.model.SceneLayer;
import com.bladecoder.engineeditor.Ctx;
import java.util.List;

public class LayerInputPanel extends EditableOptionsInputPanel<String> {
  LayerInputPanel(Skin skin, String title, String desc, boolean mandatory, String defaultValue) {
    super(skin, title, desc, mandatory, defaultValue, getValues(mandatory));
  }

  private static String[] getValues(boolean mandatory) {
    List<SceneLayer> layers = Ctx.project.getSelectedScene().getLayers();

    String[] result = new String[layers.size()];

    for (int i = 0; i < layers.size(); i++) {
      result[i] = layers.get(i).getName();
    }

    return result;
  }
}
