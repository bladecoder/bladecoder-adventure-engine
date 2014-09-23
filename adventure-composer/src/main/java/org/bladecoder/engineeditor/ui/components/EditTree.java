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
package org.bladecoder.engineeditor.ui.components;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.ui.Tree.Node;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Selection;

public abstract class EditTree extends Table {
	
	protected EditToolbar toolbar;
    protected Tree tree;
    protected Skin skin;
    protected Container container;
    
	private ImageButton upBtn;
	private ImageButton downBtn;
	private ImageButton leftBtn;
	private ImageButton rightBtn;
	
	public EditTree(Skin skin) {
		super(skin);
		
		this.skin = skin;
		upBtn = new ImageButton(skin);
		downBtn = new ImageButton(skin);
		leftBtn = new ImageButton(skin);
		rightBtn = new ImageButton(skin);
		
		tree = new Tree(skin);
			
		ScrollPane scrollPane = new ScrollPane(tree, skin);
		container = new Container(scrollPane);
		container.fill();
		container.prefHeight(100);
		
		toolbar = new EditToolbar(skin);
//		debug();
		add(toolbar).expandX().fillX();
		row().fill();
		add(container).expandY().fill();
		
		toolbar.addToolBarButton(upBtn, "ic_up", "Move up", "Move up");
		toolbar.addToolBarButton(downBtn, "ic_down", "Move down", "Move down");
		toolbar.addToolBarButton(leftBtn, "ic_left", "Child",
				"Move to child option");
		toolbar.addToolBarButton(rightBtn, "ic_right", "Parent",
				"Move to parent option");
		toolbar.addCreateListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				create();
			}
		});
		
		toolbar.addEditListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				edit();
			}
		});
		
		toolbar.addDeleteListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				delete();
			}
		});
		
		toolbar.addCopyListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				copy();
			}
		});
		
		toolbar.addPasteListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				paste();
			}
		});
		
		upBtn.addListener(new ChangeListener() {
			
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				upNode();
			}
		});
		
		downBtn.addListener(new ChangeListener() {
			
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				downNode();
			}
		});
		
		leftBtn.addListener(new ChangeListener() {
			
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				leftNode();
			}
		});
		
		rightBtn.addListener(new ChangeListener() {
			
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				rightNode();
			}
		});
		
		tree.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {				
				Selection<Node> selection = tree.getSelection();

				if (selection.size() == 0) {
					upBtn.setDisabled(true);
					downBtn.setDisabled(true);

					leftBtn.setDisabled(true);
					rightBtn.setDisabled(true);
				} else {

					Node nodeSel = selection.getLastSelected();

					upBtn.setDisabled(nodeSel.getParent() == null || nodeSel.getParent().getChildren().get(0) == nodeSel);
					downBtn.setDisabled(nodeSel.getParent() == null || nodeSel.getParent().getChildren().get(
							nodeSel.getParent().getChildren().size - 1) == nodeSel);

					leftBtn.setDisabled(nodeSel.getParent() != null);
					rightBtn.setDisabled(nodeSel.getParent() == null || nodeSel.getParent().getChildren().get(0) == nodeSel);
				}
				
				toolbar.disableEdit(selection == null);
			}

		});
    }
	
	abstract protected void create();
	abstract protected void edit();
	abstract protected void delete();
	abstract protected void copy();
	abstract protected void paste();
	abstract protected void upNode();
	abstract protected void downNode();
	abstract protected void leftNode();
	abstract protected void rightNode();
}
