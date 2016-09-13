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
import java.util.Locale;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.Config;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.components.PropertyTable;

public class WorldProps extends PropertyTable {
	public WorldProps(Skin skin) {
		super(skin);

		Ctx.project.addPropertyChangeListener(Project.NOTIFY_PROJECT_LOADED, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent arg0) {
				setProject();
			}
		});
	}

	@Override
	protected void updateModel(String property, String value) {
		if (property.equals(Project.WIDTH_PROPERTY)) {
			World.getInstance().setWidth(Integer.parseInt(value));
		} else if (property.equals(Config.TITLE_PROP)) {
			Ctx.project.getProjectConfig().setProperty(Config.TITLE_PROP, value);
		} else if (property.equals(Project.HEIGHT_PROPERTY)) {
			World.getInstance().setHeight(Integer.parseInt(value));
		} else if (property.equals(Config.INVENTORY_POS_PROP)) {
			Ctx.project.getProjectConfig().setProperty(Config.INVENTORY_POS_PROP, value);
		} else if (property.equals(Config.INVENTORY_AUTOSIZE_PROP)) {
			Ctx.project.getProjectConfig().setProperty(Config.INVENTORY_AUTOSIZE_PROP, value);
		} else if (property.equals(Config.UI_MODE)) {
			Ctx.project.getProjectConfig().setProperty(Config.UI_MODE, value);
		} else if (property.equals(Config.SINGLE_ACTION_INVENTORY)) {
			Ctx.project.getProjectConfig().setProperty(Config.SINGLE_ACTION_INVENTORY, value);
		} else if (property.equals(Config.FAST_LEAVE)) {
			Ctx.project.getProjectConfig().setProperty(Config.FAST_LEAVE, value);			
		} else if (property.equals(Config.DEBUG_PROP)) {
			Ctx.project.getProjectConfig().setProperty(Config.DEBUG_PROP, value);
		} else if (property.equals(Config.CHARACTER_ICON_ATLAS)) {
			Ctx.project.getProjectConfig().setProperty(Config.CHARACTER_ICON_ATLAS, value);
		} else if (property.equals(Config.SHOW_DESC_PROP)) {
			Ctx.project.getProjectConfig().setProperty(Config.SHOW_DESC_PROP, value);
		} else if (property.equals(Config.EXTEND_VIEWPORT_PROP)) {
			Ctx.project.getProjectConfig().setProperty(Config.EXTEND_VIEWPORT_PROP, value);
		}

		Ctx.project.setModified(); // TODO Add propertychange to Config
	}

	private void setProject() {
		clearProps();

		if (Ctx.project.getProjectDir() != null) {
			addProperty(Project.WIDTH_PROPERTY, World.getInstance().getWidth());
			addProperty(Project.HEIGHT_PROPERTY, World.getInstance().getHeight());
			addProperty(Config.TITLE_PROP, Ctx.project.getTitle());
			addProperty(Config.INVENTORY_POS_PROP,
					Ctx.project.getProjectConfig().getProperty(Config.INVENTORY_POS_PROP, "DOWN").toUpperCase(Locale.ENGLISH),  new String[] {"TOP", "DOWN", "LEFT", "RIGHT", "CENTER"});
			addProperty(Config.INVENTORY_AUTOSIZE_PROP, Boolean
					.parseBoolean(Ctx.project.getProjectConfig().getProperty(Config.INVENTORY_AUTOSIZE_PROP, "true")));
			addProperty(Config.UI_MODE, Ctx.project.getProjectConfig().getProperty(Config.UI_MODE, "TWO_BUTTONS").toUpperCase(Locale.ENGLISH), new String[] {"TWO_BUTTONS", "PIE", "SINGLE_CLICK"});
			addProperty(Config.SINGLE_ACTION_INVENTORY, Boolean
					.parseBoolean(Ctx.project.getProjectConfig().getProperty(Config.SINGLE_ACTION_INVENTORY, "false")));
			addProperty(Config.FAST_LEAVE, Boolean
					.parseBoolean(Ctx.project.getProjectConfig().getProperty(Config.FAST_LEAVE, "false")));			
			addProperty(Config.DEBUG_PROP,
					Boolean.parseBoolean(Ctx.project.getProjectConfig().getProperty(Config.DEBUG_PROP, "false")));
			addProperty(Config.SHOW_DESC_PROP,
					Boolean.parseBoolean(Ctx.project.getProjectConfig().getProperty(Config.SHOW_DESC_PROP, "true")));
			addProperty(Config.CHARACTER_ICON_ATLAS,
					Ctx.project.getProjectConfig().getProperty(Config.CHARACTER_ICON_ATLAS, ""));
			addProperty(Config.EXTEND_VIEWPORT_PROP, Boolean
					.parseBoolean(Ctx.project.getProjectConfig().getProperty(Config.EXTEND_VIEWPORT_PROP, "true")));
		}

		invalidateHierarchy();
	}
}
