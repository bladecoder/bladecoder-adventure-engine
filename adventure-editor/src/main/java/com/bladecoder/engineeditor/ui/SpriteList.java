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
import java.util.Arrays;
import java.util.HashMap;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engine.anim.AnimationDesc;
import com.bladecoder.engine.anim.Tween.Type;
import com.bladecoder.engine.model.AnimationRenderer;
import com.bladecoder.engine.model.Dialog;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.ElementUtils;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.panels.CellRenderer;
import com.bladecoder.engineeditor.ui.panels.EditModelDialog;
import com.bladecoder.engineeditor.ui.panels.ModelList;
import com.bladecoder.engineeditor.undo.UndoDeleteAnimation;

public class SpriteList extends ModelList<SpriteActor, AnimationDesc> {

	private ImageButton initBtn;
	private ImageButton flipInitBtn;

	public SpriteList(Skin skin) {
		super(skin, true);

		initBtn = new ImageButton(skin);
		toolbar.addToolBarButton(initBtn, "ic_check", "Set init animation", "Set init animation");
		initBtn.setDisabled(true);

		flipInitBtn = new ImageButton(skin);
		toolbar.addToolBarButton(flipInitBtn, "ic_flip", "Set init animation flipped", "Set init animation flipped");
		flipInitBtn.setDisabled(true);

		setCellRenderer(listCellRenderer);

		list.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				int pos = list.getSelectedIndex();

				String id = null;

				if (pos != -1)
					id =  list.getItems().get(pos).id;

				Ctx.project.setSelectedFA(id);

				toolbar.disableEdit(pos == -1);
				initBtn.setDisabled(pos == -1);
				flipInitBtn.setDisabled(pos == -1);
			}
		});

		initBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				setDefault();
			}
		});

		flipInitBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				flipInit();
			}
		});
		
		Ctx.project.addPropertyChangeListener(Project.NOTIFY_ELEMENT_CREATED, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getNewValue() instanceof Dialog && !(evt.getSource() instanceof EditDialogDialog) && parent instanceof SpriteActor) {
					HashMap<String, AnimationDesc> animations = ((AnimationRenderer)parent.getRenderer()).getAnimations();
					addElements(parent, Arrays.asList(animations.values().toArray(new AnimationDesc[0])));
				}
			}
		});
	}

	private void setDefault() {
		int pos = list.getSelectedIndex();

		if (pos == -1)
			return;
		
		AnimationRenderer renderer = (AnimationRenderer)((SpriteActor) Ctx.project.getSelectedActor()).getRenderer();

		String id = list.getItems().get(pos).id;
		String oldId = renderer.getInitAnimation();
				
		renderer.setInitAnimation(id);
		
		Ctx.project.setModified(this, "init_animation", oldId, id);
	}

	private void flipInit() {
		int pos = list.getSelectedIndex();

		if (pos == -1)
			return;
		
		AnimationRenderer renderer = (AnimationRenderer)((SpriteActor) Ctx.project.getSelectedActor()).getRenderer();

		String id = list.getItems().get(pos).id;

		String newValue = AnimationRenderer.getFlipId(id);

		renderer.setInitAnimation(newValue);
		
		Ctx.project.setModified(this, "init_animation", id, newValue);
	}

	@Override
	protected void delete() {
		AnimationDesc d = removeSelected();
		
		AnimationRenderer renderer = (AnimationRenderer)parent.getRenderer();
		
		renderer.getAnimations().remove(d.id);
			
		//  UNDO
		Ctx.project.getUndoStack().add(new UndoDeleteAnimation(parent, d));

		// delete init_animation attr if the animation to delete is the chapter
		// init_animation
		if (renderer.getInitAnimation().equals(d.id)) {
			HashMap<String, AnimationDesc> animations = renderer.getAnimations();
			String newValue = null;

			if(animations.size() > 0)
				newValue = animations.keySet().iterator().next();
			
			renderer.setInitAnimation(newValue);
			
			Ctx.project.setModified(this, "init_animation", d.id, newValue);
		}
		
		Ctx.project.setModified();
	}
	
	@Override
	protected void copy() {
		AnimationDesc e = list.getSelected();

		if (e == null)
			return;

		clipboard = (AnimationDesc)ElementUtils.cloneElement(e);
		toolbar.disablePaste(false);
	}

	@Override
	protected void paste() {
		AnimationDesc newElement = (AnimationDesc)ElementUtils.cloneElement(clipboard);
		
		int pos = list.getSelectedIndex() + 1;

		list.getItems().insert(pos, newElement);

		((AnimationRenderer)parent.getRenderer()).addAnimation(newElement);

		list.setSelectedIndex(pos);
		list.invalidateHierarchy();
		
		Ctx.project.setModified();
	}		

	@Override
	protected EditModelDialog<SpriteActor, AnimationDesc> getEditElementDialogInstance(AnimationDesc e) {
		 return new EditAnimationDialog(skin, parent, e);
	}

	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private final CellRenderer<AnimationDesc> listCellRenderer = new CellRenderer<AnimationDesc>() {

		@Override
		protected String getCellTitle(AnimationDesc e) {
			String name = e.id;
			SpriteActor actor = (SpriteActor) Ctx.project.getSelectedActor();
			AnimationRenderer renderer = (AnimationRenderer)actor.getRenderer();

			String init = renderer.getInitAnimation();

			if (init == null || init.isEmpty()) {
				if (renderer.getAnimations().values().size() > 0)
					init = renderer.getAnimations().values().iterator().next().id;
				else
					init = "";
			}

			if (init.equals(name) || AnimationRenderer.getFlipId(init).equals(name))
				name += " <init>";

			return name;
		}

		@Override
		protected String getCellSubTitle(AnimationDesc e) {
			StringBuilder sb = new StringBuilder();

			if (e.source != null && !e.source.isEmpty())
				sb.append("source: ").append(e.source);

			sb.append(" speed: ").append(e.duration);
			sb.append(" count: ").append(e.count);

			return sb.toString();
		}

		@Override
		public TextureRegion getCellImage(AnimationDesc a) {
			String u = null;

			if (a.animationType == Type.REPEAT) {
				u = "ic_repeat";
			} else if (a.animationType == Type.YOYO) {
				u = "ic_yoyo";
			} else {
				u = "ic_sprite_actor";
			}

			return Ctx.assetManager.getIcon(u);
		}

		@Override
		protected boolean hasSubtitle() {
			return true;
		}

		@Override
		protected boolean hasImage() {
			return true;
		}
	};
}
