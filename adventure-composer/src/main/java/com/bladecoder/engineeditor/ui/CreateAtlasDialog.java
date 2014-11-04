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

import java.io.IOException;
import java.util.List;

import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.components.EditDialog;
import com.bladecoder.engineeditor.ui.components.FileInputPanel;
import com.bladecoder.engineeditor.ui.components.InputPanel;
import com.bladecoder.engineeditor.utils.EditorLogger;
import com.bladecoder.engineeditor.utils.ImageUtils;

public class CreateAtlasDialog extends EditDialog {

	private static final String INFO = "Package all the images in the selected dir to a new atlas";

	private static final String[] FILTERS = { "Linear", "Nearest", "MipMap",
			"MipMapLinearLinear", "MipMapLinearNearest", "MipMapNearestLinear",
			"MipMapNearestNearest" };
	
	private InputPanel name;
	private InputPanel dir;
	private InputPanel filterMin;
	private InputPanel filterMag;


	public CreateAtlasDialog(Skin skin) {
		super("CREATE ATLAS", skin);
		
		name = new InputPanel(skin, "Atlas Name",
				"The name of the sprite atlas", true);
		dir = new FileInputPanel(skin, "Input Image Directory",
				"Select the output directory with the images to create the Atlas",
				true);

		filterMin = new InputPanel(skin, "Min Filter",
				"The filter when the texture is scaled down", FILTERS);
		filterMag = new InputPanel(skin, "Mag Filter",
				"The filter when the texture is scaled up", FILTERS);

		addInputPanel(name);
		addInputPanel(dir);
		addInputPanel(filterMin);
		addInputPanel(filterMag);

		filterMin.setText(FILTERS[0]);
		filterMag.setText(FILTERS[1]);

		setInfo(INFO);
	}

	@Override
	protected void ok() {
		new Thread(new Runnable() {
			Stage stage = getStage();
			
			@Override
			public void run() {
				Ctx.msg.show(stage, "Generating atlas...", true);

				String msg = genAtlas();
				
				Ctx.msg.hide();
				
				if(msg != null)
					Ctx.msg.show(stage, msg, 3);
			}
		}).start();
	}

	@Override
	protected boolean validateFields() {
		boolean ok = true;

		if (!dir.validateField())
			ok = false;

		if (!name.validateField())
			ok = false;

		return ok;
	}

	private String genAtlas() {
		String outdir = Ctx.project.getProjectPath() + Project.ATLASES_PATH;
		List<String> res = Ctx.project.getResolutions();
		String name = this.name.getText();
		String fMin = filterMin.getText();
		String fMag = filterMag.getText();

		TextureFilter filterMin = null, filterMag = null;

		if (fMin.equals("Linear"))
			filterMin = TextureFilter.Linear;
		else if (fMin.equals("Nearest"))
			filterMin = TextureFilter.Nearest;
		else if (fMin.equals("MipMap"))
			filterMin = TextureFilter.MipMap;
		else if (fMin.equals("MipMapLinearLinear"))
			filterMin = TextureFilter.MipMapLinearLinear;
		else if (fMin.equals("MipMapLinearNearest"))
			filterMin = TextureFilter.MipMapLinearNearest;
		else if (fMin.equals("MipMapNearestLinear"))
			filterMin = TextureFilter.MipMapNearestLinear;
		else if (fMin.equals("MipMapNearestNearest"))
			filterMin = TextureFilter.MipMapNearestNearest;

		if (fMag.equals("Linear"))
			filterMag = TextureFilter.Linear;
		else if (fMag.equals("Nearest"))
			filterMag = TextureFilter.Nearest;
		else if (fMag.equals("MipMap"))
			filterMag = TextureFilter.MipMap;
		else if (fMag.equals("MipMapLinearLinear"))
			filterMag = TextureFilter.MipMapLinearLinear;
		else if (fMag.equals("MipMapLinearNearest"))
			filterMag = TextureFilter.MipMapLinearNearest;
		else if (fMag.equals("MipMapNearestLinear"))
			filterMag = TextureFilter.MipMapNearestLinear;
		else if (fMag.equals("MipMapNearestNearest"))
			filterMag = TextureFilter.MipMapNearestNearest;


		for (String r : res) {
			float scale = Float.parseFloat(r);
			
			try {				
				ImageUtils.createAtlas(dir.getText(), outdir + "/" + r, name + ".atlas", scale, filterMin, filterMag);
			} catch (IOException e) {
				EditorLogger.error(e.getMessage());
			}
		}

		return null;
	}
}
