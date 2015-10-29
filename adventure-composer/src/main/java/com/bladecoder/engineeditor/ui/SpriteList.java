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

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engine.anim.AnimationDesc;
import com.bladecoder.engine.anim.Tween.Type;
import com.bladecoder.engine.model.ActorRenderer;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.ui.components.CellRenderer;
import com.bladecoder.engineeditor.ui.components.EditModelDialog;
import com.bladecoder.engineeditor.ui.components.ModelList;

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
	}

	private void setDefault() {
		int pos = list.getSelectedIndex();

		if (pos == -1)
			return;
		
		ActorRenderer renderer = ((SpriteActor) Ctx.project.getSelectedActor()).getRenderer();

		String id = list.getItems().get(pos).id;
		
		renderer.setInitAnimation(id);
	}

	private void flipInit() {
		int pos = list.getSelectedIndex();

		if (pos == -1)
			return;
		
		ActorRenderer renderer = ((SpriteActor) Ctx.project.getSelectedActor()).getRenderer();

		String id = list.getItems().get(pos).id;
		// String prev = w.getRootAttr("init_scene");

		id = AnimationDesc.getFlipId(id);

		renderer.setInitAnimation(id);
	}

	@Override
	protected void delete() {
		AnimationDesc d = removeSelected();
		
		ActorRenderer renderer = parent.getRenderer();
		
		renderer.getAnimations().remove(d.id);
			
	// TODO UNDO
//			UndoOp undoOp = new UndoDeleteElement(doc, e);
//			Ctx.project.getUndoStack().add(undoOp);
//			doc.deleteElement(e);

	// TODO TRANSLATIONS
//			I18NUtils.putTranslationsInElement(doc, clipboard);


		// delete init_animation attr if the animation to delete is the chapter
		// init_animation
		if (renderer.getInitAnimation().equals(d.id)) {
			// TODO Set next animation as init
			renderer.setInitAnimation(null);
		}
		
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

			String init = actor.getRenderer().getInitAnimation();

			if (init == null || init.isEmpty()) {
				if (actor.getRenderer().getAnimations().values().size() > 0)
					init = actor.getRenderer().getAnimations().values().iterator().next().id;
				else
					init = "";
			}

			if (init.equals(name) || AnimationDesc.getFlipId(init).equals(name))
				name += " <init>";

			return name;
		}

		@Override
		protected String getCellSubTitle(AnimationDesc e) {
			StringBuilder sb = new StringBuilder();

			if (e.source != null && !e.source.isEmpty())
				sb.append("source: ").append(e.source);

			sb.append(" speed: ").append(e.duration);
			sb.append(" delay: ").append(e.delay);
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
