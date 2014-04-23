package org.bladecoder.engineeditor.ui;

import java.io.File;
import java.io.IOException;

import org.bladecoder.engine.actions.Param;
import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.Project;
import org.bladecoder.engineeditor.ui.components.EditDialog;
import org.bladecoder.engineeditor.ui.components.InputPanel;
import org.bladecoder.engineeditor.utils.ImageUtils;

@SuppressWarnings("serial")
public class CreateResolutionDialog extends EditDialog {

	private static final String INFO = "Create a new resolution. Scale backgrounds and overlays from the world resolution.";

	private InputPanel scale = new InputPanel("Scale",
			"Scale relative to the world resolution", Param.Type.FLOAT, false);
	
	String atlasDir = Ctx.project.getProjectPath() + "/" + Project.ATLASES_PATH;
	String bgDir = Ctx.project.getProjectPath() + "/" + Project.BACKGROUNDS_PATH;
	String uiDir = Ctx.project.getProjectPath() + "/" + Project.UI_PATH;
	String overlaysDir = Ctx.project.getProjectPath() + "/" + Project.OVERLAYS_PATH;

	public CreateResolutionDialog(java.awt.Frame parent) {
		super(parent);

		centerPanel.add(scale);

		scale.setMandatory(true);

		setTitle("CREATE RESOLUTION");
		setInfo(INFO);

		init(parent);
	}

	@Override
	protected void ok() {
		dispose();

		Ctx.window.getScnCanvas().setMsg("Creating resolution...");
		
		createResolution();

		new Thread(new Runnable() {
			@Override
			public void run() {
				String msg = scaleImages();
				Ctx.window.getScnCanvas().setMsgWithTimer(msg, 2000);
			}
		}).start();
	}

	@Override
	protected boolean validateFields() {
		boolean ok = true;

		if (!scale.validateField())
			ok = false;

		return ok;
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
}
