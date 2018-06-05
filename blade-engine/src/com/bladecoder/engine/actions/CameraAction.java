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

import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.SceneCamera;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.InterpolationMode;

@ActionDescription("Set/Animates the camera position and zoom. Also can stablish the follow character parameter")
public class CameraAction implements Action {
	@ActionPropertyDescription("Sets the camera position relative to this actor.")
	@ActionProperty(type = Type.ACTOR)
	private String target;
	
	@ActionProperty
	@ActionPropertyDescription("The target position")
	private Vector2 pos;

	@ActionProperty
	@ActionPropertyDescription("The target 'zoom'. If not set, the current zoom is used.")
	private Float zoom;

	@ActionProperty
	@ActionPropertyDescription("Duration of the animation in seconds. If not '0' and animation is triggered")
	private Float duration;

	@ActionPropertyDescription("Sets the actor to follow. 'none' puts no actor to follow")
	@ActionProperty(type = Type.ACTOR)
	private String followActor;
	
	@ActionProperty
	@ActionPropertyDescription("The interpolation mode")
	private InterpolationMode interpolation;

	@ActionProperty(defaultValue = "true", required = true)
	@ActionPropertyDescription("If this param is 'false' the text is showed and the action continues inmediatly")
	private boolean wait = true;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		
		Vector2 pos2 = null;
		
		Float zoom2 = zoom;
		
		if(pos != null)
			pos2 = new Vector2(pos);

		float scale = EngineAssetManager.getInstance().getScale();

		SceneCamera camera = w.getSceneCamera();
		
		if(zoom2 == null || zoom2 < 0)
			zoom2 = camera.getZoom();
		
		if(pos == null && target == null) {
			pos2 = new Vector2(camera.getPosition());
			pos2.x /= scale;
			pos2.y /= scale;
		}
		
		if (target != null) {
			BaseActor target = w.getCurrentScene().getActor(this.target, false);
			
			float x = target.getX();
			float y = target.getY();
			
			if(target instanceof InteractiveActor) {
				Vector2 refPoint = ((InteractiveActor) target).getRefPoint();
				x+= refPoint.x;
				y+= refPoint.y;
			}
			
			if(pos2 != null){
				pos2.x += x;
				pos2.y += y;
			} else {
				pos2 = new Vector2(x,y);
			}
		} 

		if (followActor != null) {
			if (followActor.equals("none"))
				w.getCurrentScene().setCameraFollowActor(null);
			else {
				w.getCurrentScene().setCameraFollowActor((SpriteActor) w.getCurrentScene()
						.getActor(followActor, false));
			}
		}

		if (duration == null || duration == 0) {
			camera.setZoom(zoom2);
			camera.setPosition(pos2.x * scale, pos2.y * scale);
			return false;
		} else {
			camera.startAnimation(pos2.x * scale, pos2.y * scale, zoom2, duration, interpolation, wait?cb:null);
		}
		
		return wait;
	}

}
