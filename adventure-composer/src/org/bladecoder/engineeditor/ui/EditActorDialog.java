package org.bladecoder.engineeditor.ui;

import org.bladecoder.engine.actions.Param;
import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.model.ChapterDocument;
import org.bladecoder.engineeditor.ui.components.EditElementDialog;
import org.bladecoder.engineeditor.ui.components.InputPanel;
import org.w3c.dom.Element;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

public class EditActorDialog extends EditElementDialog {

	public static final String TYPES_INFO[] = {
			"Background actors define an interactive area in the background",
			"Atlas actors allows 2d image and animations",
			"3d actors allows 3d models and animations",
			"Spine actors allows Spine 2d skeletal animations",
			"Actors always in the foreground. No interactive" };

	private InputPanel[] inputs = new InputPanel[11];
	InputPanel typePanel;

	String attrs[] = { "type", "id", "desc", "state", "interaction", "visible", "walking_speed", "depth_type" , "sprite_size", "camera", "fov"};

	@SuppressWarnings("unchecked")
	public EditActorDialog(Skin skin, BaseDocument doc, Element parent,
			Element e) {
		super(skin);

		
		inputs[0] =	new InputPanel(skin, "Actor Type",
						"Actors can be from different types", ChapterDocument.ACTOR_TYPES);
		
		inputs[1] =	new InputPanel(skin, "Actor ID",
							"IDs can not contain '.' or '_' characters.", true);
		inputs[2] =	new InputPanel(skin, "Description",
							"The text showed when the cursor is over the actor.");
		inputs[3] =	new InputPanel(skin, "State",
							"Initial state of the actor. Actors can be in differentes states during the game.");
		inputs[4] =	new InputPanel(skin, "Interaction",
							"True when the actor reacts to the user input.",
							Param.Type.BOOLEAN, false);
		inputs[5] =	new InputPanel(skin, "Visible", "The actor visibility.", Param.Type.BOOLEAN, false);
		inputs[6] =	new InputPanel(skin, "Walking Speed", "The walking speed in pix/sec. Default 700.", Param.Type.FLOAT, false);
		inputs[7] =	new InputPanel(skin, "Depth Type", "Scene fake depth for scaling", new String[] {"none", "vector", "map"});
		inputs[8] =	new InputPanel(skin, "Sprite Dimensions", "The size of the 3d sprite", Param.Type.DIMENSION, true);
		inputs[9] =	new InputPanel(skin, "Camera Name", "The name of the camera in the model", Param.Type.STRING, true, "Camera", null);
		inputs[10] =new InputPanel(skin, "Camera FOV", "The camera field of view", Param.Type.FLOAT, true, "49.3", null);

		setInfo(TYPES_INFO[0]);
		
		typePanel = inputs[0];

		((SelectBox<String>) typePanel.getField()).addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				typeChanged();
			}
		});

		init(inputs, attrs, doc, parent, "actor", e);
		
		typeChanged();
		
	}
	
	private void typeChanged() {
		int i = typePanel.getSelectedIndex();

		setInfo(TYPES_INFO[i]);
		
		inputs[8].setVisible(false);
		inputs[9].setVisible(false);
		inputs[10].setVisible(false);	
		
		if(ChapterDocument.ACTOR_TYPES[i].equals(ChapterDocument.SPRITE3D_ACTOR_TYPE)) {
			inputs[8].setVisible(true);
			inputs[9].setVisible(true);
			inputs[10].setVisible(true);
		}
		
		pack();
	}
	
	@Override
	protected void fill(){
		String type = typePanel.getText();
		
		if (type.equals(ChapterDocument.BACKGROUND_ACTOR_TYPE) && ((ChapterDocument)doc).getBBox(e) == null)
			((ChapterDocument)doc).setBbox(e, new Rectangle(0, 0, 100, 100));
		
		if (!type.equals(ChapterDocument.BACKGROUND_ACTOR_TYPE) && ((ChapterDocument)doc).getPos(e) == null)
			((ChapterDocument)doc).setPos(e, new Vector2(0, 0));		
		
		super.fill();
	}
}
