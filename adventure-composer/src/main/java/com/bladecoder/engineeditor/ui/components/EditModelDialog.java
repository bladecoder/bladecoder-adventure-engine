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
package com.bladecoder.engineeditor.ui.components;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;

public abstract class EditModelDialog<PARENT, T> extends EditDialog {

	protected T e;
	protected PARENT parent;
	private InputPanel[] i;

	protected ChangeListener listener;

	public EditModelDialog(Skin skin) {
		super("", skin);
	}

	protected void init(PARENT parent, T e, InputPanel[] inputs) {
		this.e = e;
		this.parent = parent;
		this.i = inputs;

		getCenterPanel().clear();

		for (InputPanel i : inputs) {
			addInputPanel(i);
		}

		if (e == null) {
			setTitle("CREATE OBJECT");
		} else {		
			setTitle("EDITING " + e.getClass().getSimpleName());
			
			modelToInputs();
		}

		// TODO Set focus to the 1st element
		// if(inputs.length > 0) {
		// getStage().setKeyboardFocus(inputs[0].getField());
		// }
	}

	@Override
	protected void ok() {

		if (e == null) {
			e = create();
		}

		inputsToModel();

		if (listener != null)
			listener.changed(new ChangeEvent(), this);
	}

	public void setListener(ChangeListener l) {
		listener = l;
	}

	protected abstract T create();

	protected abstract void inputsToModel();

	protected abstract void modelToInputs();

	public T getElement() {
		return e;
	}

	@Override
	protected boolean validateFields() {
		for (InputPanel p : i) {
			if (p.isVisible() && !p.validateField())
				return false;
		}

		return true;
	}
}
