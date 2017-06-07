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

	@Override
	public boolean run(VerbRunner cb) {

		float scale = EngineAssetManager.getInstance().getScale();

		SceneCamera camera = World.getInstance().getSceneCamera();
		
		if(zoom == null || zoom < 0)
			zoom = camera.getZoom();
		
		if(pos == null && target == null) {
			pos = camera.getPosition();
			pos.x /= scale;
			pos.y /= scale;
		}
		
		if (target != null) {
			BaseActor target = World.getInstance().getCurrentScene().getActor(this.target, false);
			
			float x = target.getX();
			float y = target.getY();
			
			if(target instanceof InteractiveActor) {
				Vector2 refPoint = ((InteractiveActor) target).getRefPoint();
				x+= refPoint.x;
				y+= refPoint.y;
			}
			
			if(pos != null){
				pos.x += x;
				pos.y += y;
			} else {
				pos = new Vector2(x,y);
			}
		} 

		if (followActor != null) {
			if (followActor.equals("none"))
				World.getInstance().getCurrentScene().setCameraFollowActor(null);
			else {
				World.getInstance().getCurrentScene().setCameraFollowActor((SpriteActor) World.getInstance().getCurrentScene()
						.getActor(followActor, false));
			}
		}

		if (duration == null || duration == 0) {
			camera.setZoom(zoom);
			camera.setPosition(pos.x * scale, pos.y * scale);
			return false;
		} else {
			camera.startAnimation(pos.x * scale, pos.y * scale, zoom, duration, interpolation, wait?cb:null);
		}
		
		return wait;
	}

}
