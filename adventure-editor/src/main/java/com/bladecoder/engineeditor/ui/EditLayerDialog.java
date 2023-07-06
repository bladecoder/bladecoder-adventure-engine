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

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.SceneLayer;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.ui.panels.EditModelDialog;
import com.bladecoder.engineeditor.ui.panels.InputPanel;
import com.bladecoder.engineeditor.ui.panels.InputPanelFactory;

public class EditLayerDialog extends EditModelDialog<Scene, SceneLayer> {

    private InputPanel name;
    private InputPanel visible;
    private InputPanel dynamic;
    private InputPanel parallax;

    public EditLayerDialog(Skin skin, Scene parent, SceneLayer e) {
        super(skin);

        name = InputPanelFactory.createInputPanel(skin, "Layer Name", "The name of the layer", true);
        visible = InputPanelFactory.createInputPanel(skin, "Visible", "Layer Visibility", Param.Type.BOOLEAN, true, "true");
        dynamic = InputPanelFactory.createInputPanel(skin, "Dynamic", "True for actor reordering based in y position", Param.Type.BOOLEAN, true, "false");
        parallax = InputPanelFactory.createInputPanel(skin, "Parallax Factor", "The multiplier factor for parallax effect", Param.Type.FLOAT, true, "1.0");

        setInfo("Scenes can have a list of layers. Actors are added to a specific layer to control the draw order");

        init(parent, e, new InputPanel[]{name, visible, dynamic, parallax});
    }

    @Override
    protected void inputsToModel(boolean create) {

        if (create) {
            e = new SceneLayer();
        }

        // if the name is changed. Change all actor layer name.
        if (!create && !e.getName().equals(name.getText())) {
            for (SceneLayer l : parent.getLayers()) {
                for (int i = 0; i < l.getActors().size(); i++) {
                    if (l.getActors().get(i).getLayer().equals(e.getName())) {
                        l.getActors().get(i).setLayer(name.getText());
                    }
                }
            }
        }

        e.setName(name.getText());
        e.setVisible(Boolean.parseBoolean(visible.getText()));
        e.setDynamic(Boolean.parseBoolean(dynamic.getText()));
        e.setParallaxMultiplier(Float.parseFloat(parallax.getText()));

        if (create) {
            parent.getLayers().add(e);
        }

        // TODO UNDO OP
//		UndoOp undoOp = new UndoAddElement(doc, e);
//		Ctx.project.getUndoStack().add(undoOp);

        Ctx.project.setModified();
    }

    @Override
    protected void modelToInputs() {
        name.setText(e.getName());
        visible.setText(Boolean.toString(e.isVisible()));
        dynamic.setText(Boolean.toString(e.isDynamic()));
        parallax.setText(Float.toString(e.getParallaxMultiplier()));
    }
}
