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
package com.bladecoder.engine.actions;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.bladecoder.engine.BladeEngine;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.ui.SceneScreen;
import com.bladecoder.engine.ui.UI.Screens;

@ActionDescription("Sets actor position in screen coordinates. This is used to show an actor in the same screen position when the scene has scrolled.")
public class ScreenPositionAction implements Action {
	public enum Anchor {
		NONE, CENTER, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, TOP, BOTTOM, LEFT, RIGHT
	}
	
	
	@ActionProperty( required = true)
	@ActionPropertyDescription("The target actor")	
	private SceneActorRef actor;

	@ActionProperty( required = true)
	@ActionPropertyDescription("The position to set")
	private Vector2 position;
	
	@ActionProperty(defaultValue = "NONE")
	@ActionPropertyDescription("The position can be relative to an anchor.")
	private Anchor anchor = Anchor.NONE;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		Scene s = actor.getScene(w);

		BaseActor a = s.getActor(actor.getActorId(), true);

		if (position != null) {
			float scale = EngineAssetManager.getInstance().getScale();
			
			Viewport viewport = ((SceneScreen)BladeEngine.getAppUI().getScreen(Screens.SCENE_SCREEN)).getViewport();
			
			Vector3 v = new Vector3(position.x * scale, position.y * scale, 0);
			
			if(anchor == Anchor.CENTER) {
				v.x += viewport.getWorldWidth() / 2;
				v.y += viewport.getWorldHeight() / 2;
			} else if(anchor == Anchor.TOP_LEFT) {
				v.x += 0;
				v.y += viewport.getWorldHeight();		
			} else if(anchor == Anchor.TOP_RIGHT) {
				v.x += viewport.getWorldWidth();
				v.y += viewport.getWorldHeight();				
			} else if(anchor == Anchor.BOTTOM_RIGHT) {
				v.x += viewport.getWorldWidth();
				v.y += 0;
			} else if(anchor == Anchor.BOTTOM_LEFT) {
				v.x += 0;
				v.y += 0;
			} else if(anchor == Anchor.TOP) {
				v.x += viewport.getWorldWidth() / 2;
				v.y += viewport.getWorldHeight();
			} else if(anchor == Anchor.BOTTOM) {
				v.x += viewport.getWorldWidth() / 2;
				v.y += 0;
			} else if(anchor == Anchor.LEFT) {
				v.x += 0;
				v.y += viewport.getWorldHeight() / 2;
			} else if(anchor == Anchor.RIGHT) {
				v.x += viewport.getWorldWidth();
				v.y += viewport.getWorldHeight() / 2;
			}
			
//			viewport.project(v);
			
			v.x *= viewport.getScreenWidth() / viewport.getWorldWidth();
			v.y *= viewport.getScreenHeight() /  viewport.getWorldHeight();
			
//			v.y = viewport.getScreenHeight() - v.y;
			v.y = Gdx.graphics.getHeight() - v.y;		
			
			w.getCurrentScene().getCamera().
				unproject(v, 0, 0, 
					viewport.getScreenWidth(), viewport.getScreenHeight());	

			a.setPosition(v.x, v.y);
		}

		return false;
	}

}
