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
package com.bladecoder.engineeditor.ui.panels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engine.actions.ActorAnimationRef;
import com.bladecoder.engine.anim.AnimationDesc;
import com.bladecoder.engine.model.AnimationRenderer;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engineeditor.Ctx;

public class ActorAnimationInputPanel extends InputPanel {
	EditableSelectBox<String> animation;
	EditableSelectBox<String> actor;
	Table panel;

	ActorAnimationInputPanel(Skin skin, String title, String desc,
			boolean mandatory, String defaultValue) {
		panel = new Table(skin);
		animation = new EditableSelectBox<>(skin);
		actor = new EditableSelectBox<>(skin);

		panel.add(new Label(" Actor ", skin));
		panel.add(actor);
		panel.add(new Label("  Animation ", skin));
		panel.add(animation);
		

		ArrayList<String> values = new ArrayList<String>();

		// values.add("");

		for (BaseActor a: Ctx.project.getSelectedScene().getActors().values()) {

			if (a instanceof SpriteActor) {
				values.add(a.getId());
			}
		}
		
		values.add(Scene.VAR_PLAYER);

		actor.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				actorSelected();
			}
		});

		init(skin, title, desc, panel, mandatory, defaultValue);
		String[] array = values.toArray(new String[values.size()]);
		Arrays.sort(array);
		actor.setItems(array);

		if (values.size() > 0) {
			if (defaultValue != null)
				setText(defaultValue);
			else
				actor.setSelected("");
		}

	}

	private void actorSelected() {
		String s = actor.getSelected();
		SpriteActor a = null;
		
		if(Ctx.project.getSelectedActor() instanceof SpriteActor)
			a = (SpriteActor) Ctx.project.getSelectedActor();
		
		ArrayList<String> values = new ArrayList<String>();

		if (s != null && !s.isEmpty()) {
			a = (SpriteActor)Ctx.project.getSelectedScene().getActor(s, false);
		}

		if (a != null && a.getRenderer() instanceof AnimationRenderer) {

			 HashMap<String, AnimationDesc> animations = ((AnimationRenderer)a.getRenderer()).getAnimations();

			if (!isMandatory()) {
				values.add("");
			}

			for (AnimationDesc anim:animations.values()) {
				values.add(anim.id);

				String flipped = AnimationRenderer.getFlipId(anim.id);

				if (!flipped.isEmpty()) {
					values.add(flipped);
				}
			}
		}
		
		String[] array = values.toArray(new String[values.size()]);
		Arrays.sort(array);
		animation.setItems(array);

		if (values.size() > 0)
			animation.setSelected("");

	}

	public String getText() {
		
		String selectedActor = !actor.getSelected().isEmpty() ? actor.getSelected():Ctx.project.getSelectedActor().getId();
		
		return (new ActorAnimationRef(selectedActor, animation.getSelected())).toString();
	}

	public void setText(String s) {
		ActorAnimationRef aa = new ActorAnimationRef(s);
			
		actor.setSelected(aa.getActorId() == null?"":aa.getActorId());
		actorSelected();
		animation.setSelected(aa.getAnimationId());
	}
	
	@Override
	public boolean validateField() {
		
		ActorAnimationRef a = new ActorAnimationRef(getText());
		
		if(isMandatory()) {
			if(a.getActorId() == null || a.getActorId().trim().isEmpty() || 
					a.getAnimationId() == null || a.getAnimationId().trim().isEmpty()) {
				setError(true);
				return false;
			}		
		}
		
		setError(false);	
		return true;
	}
}
