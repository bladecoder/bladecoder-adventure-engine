package org.bladecoder.engineeditor.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;

import org.bladecoder.engine.actions.Param;
import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.model.SceneDocument;
import org.bladecoder.engineeditor.ui.components.CreateEditElementDialog;
import org.bladecoder.engineeditor.ui.components.InputPanel;
import org.w3c.dom.Element;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

@SuppressWarnings("serial")
public class CreateEditActorDialog extends CreateEditElementDialog {

	public static final String TYPES_INFO[] = {
			"<html><center>Background actors define an interactive area in the background</center></html>",
			"<html><center>Atlas actors allows 2d image and animations</center></html>",
			"<html><center>3d actors allows 3d models and animations</center></html>",
			"<html><center>Spine actors allows Spine 2d skeletal animations</center></html>",
			"<html><center>Actors always in the foreground. No interactive</center></html>" };

	private InputPanel[] inputs = {
			new InputPanel("Actor Type",
					"<html>Actors can be from different types</html>", SceneDocument.ACTOR_TYPES),
			new InputPanel("Actor ID",
					"<html>IDs can not contain '.' or '_' characters.</html>", true),
			new InputPanel("Description",
					"<html>The text showed when the cursor is over the actor.</html>"),
			new InputPanel("State",
					"<html>Initial state of the actor. Actors can be in differentes states during the game.</html>"),
			new InputPanel("Interaction",
					"<html>True when the actor reacts to the user input.</html>",
					Param.Type.BOOLEAN, false),
			new InputPanel("Visible", "<html>The actor visibility.</html>", Param.Type.BOOLEAN, false),
			new InputPanel("Walking Speed", "<html>The walking speed in pix/sec. Default 700.</html>", Param.Type.FLOAT, false),
			new InputPanel("Depth Type", "<html>Scene fake depth for scaling</html>", new String[] {"none", "vector", "map"}),
			new InputPanel("Sprite Dimensions", "<html>The size of the 3d sprite</html>", Param.Type.DIMENSION, true),
			new InputPanel("Camera Name", "<html>The name of the camera in the model</html>", Param.Type.STRING, true, "Camera", null),
			new InputPanel("Camera FOV", "<html>The camera field of view</html>", Param.Type.FLOAT, true, "49.3", null)
			};

	String attrs[] = { "type", "id", "desc", "state", "interaction", "visible", "walking_speed", "depth_type" , "sprite_size", "camera", "fov"};
	InputPanel typePanel = inputs[0];

	@SuppressWarnings("unchecked")
	public CreateEditActorDialog(java.awt.Frame parentWindow, BaseDocument doc, Element parent,
			Element e) {
		super(parentWindow);

		setInfo(TYPES_INFO[0]);

		((JComboBox<String>) typePanel.getField()).addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
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
		
		if(SceneDocument.ACTOR_TYPES[i].equals(SceneDocument.SPRITE3D_ACTOR_TYPE)) {
			inputs[8].setVisible(true);
			inputs[9].setVisible(true);
			inputs[10].setVisible(true);
		}
		
		pack();
	}
	
	@Override
	protected void fill(){
		String type = typePanel.getText();
		
		if (type.equals(SceneDocument.BACKGROUND_ACTOR_TYPE) && ((SceneDocument)doc).getBBox(e) == null)
			((SceneDocument)doc).setBbox(e, new Rectangle(0, 0, 100, 100));
		
		if (!type.equals(SceneDocument.BACKGROUND_ACTOR_TYPE) && ((SceneDocument)doc).getPos(e) == null)
			((SceneDocument)doc).setPos(e, new Vector2(0, 0));		
		
		super.fill();
	}
}
