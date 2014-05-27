package org.bladecoder.engineeditor.ui;

import java.io.File;
import java.io.IOException;

import org.bladecoder.engine.actions.Param;
import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.Project;
import org.bladecoder.engineeditor.ui.components.EditDialog;
import org.bladecoder.engineeditor.ui.components.InputPanel;
import org.bladecoder.engineeditor.utils.ImageUtils;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;

public class CreateResolutionDialog extends EditDialog {

	private static final String INFO = "Create a new resolution. Scale backgrounds and overlays from the world resolution.";

	private InputPanel scale;
	
	   protected ChangeListener listener;
	
	String atlasDir = Ctx.project.getProjectPath() + "/" + Project.ATLASES_PATH;
	String bgDir = Ctx.project.getProjectPath() + "/" + Project.BACKGROUNDS_PATH;
	String uiDir = Ctx.project.getProjectPath() + "/" + Project.UI_PATH;
	String overlaysDir = Ctx.project.getProjectPath() + "/" + Project.OVERLAYS_PATH;

	public CreateResolutionDialog(Skin skin) {
		super("CREATE RESOLUTION", skin);
		
		 scale = new InputPanel(skin, "Scale",
					"Scale relative to the world resolution", Param.Type.FLOAT, true);

		getCenterPanel().row().fill().expandX();
		getCenterPanel().add(scale);
		
		setInfo(INFO);
	}

	@Override
	protected void ok() {
		Ctx.msg.show(getStage(), "Creating resolution...");
		
		createResolution();

		String msg = scaleImages();
		Ctx.msg.show(getStage(), msg, 2);
		
		if(listener != null)
			listener.changed(new ChangeEvent(), this);
	}
	
	private void createResolution() {
		float s = Float.parseFloat(scale.getText());
		String prefix = (int)(Ctx.project.getWorld().getWidth() * s) + "_" +  (int)(Ctx.project.getWorld().getHeight() * s);
		
		new File(atlasDir + "/" + prefix).mkdir();
		new File(bgDir + "/" + prefix).mkdir();
		new File(uiDir + "/" + prefix).mkdir();
		new File(overlaysDir + "/" + prefix).mkdir();		
	}

	private String scaleImages() {
		
		float s = Float.parseFloat(scale.getText());
		String prefix = (int)(Ctx.project.getWorld().getWidth() * s) + "_" +  (int)(Ctx.project.getWorld().getHeight() * s);	
		
		// COPY ASSETS FROM WORLD RESOLUTION SCALED
		String wPrefix = Ctx.project.getResDir();
		
		try {
			ImageUtils.scaleDirFiles(new File(uiDir + "/" + wPrefix), new File(uiDir + "/" + prefix), s);
			ImageUtils.scaleDirFiles(new File(bgDir + "/" + wPrefix), new File(bgDir + "/" + prefix), s);
			ImageUtils.scaleDirFiles(new File(overlaysDir + "/" + wPrefix), new File(overlaysDir + "/" + prefix), s);
		} catch (IOException e) {
			return e.getMessage();
		}
		
		return null;
	}

	@Override
	protected boolean validateFields() {
		boolean ok = true;

		if (!scale.validateField())
			ok = false;

		return ok;
	}

	public void setListener(ChangeListener changeListener) {
		this.listener = changeListener;
	}
}
