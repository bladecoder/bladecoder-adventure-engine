package org.bladecoder.engineeditor.ui;

import java.awt.Desktop;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.xml.transform.TransformerException;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.glcanvas.Assets;
import org.bladecoder.engineeditor.model.Project;
import org.bladecoder.engineeditor.utils.RunProccess;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Button.ButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton.ImageButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class ProjectToolbar extends Table {
	private ImageButton newBtn;
	private ImageButton loadBtn;
	private ImageButton saveBtn;
	private ImageButton packageBtn;
	private ImageButton exitBtn;
	private ImageButton playBtn;
	private ImageButton assetsBtn;
	private ImageButton atlasBtn;
	
	private Skin skin;

	public ProjectToolbar(Skin skin) {
		super(skin);
		this.skin = skin;
		left();
		newBtn = new ImageButton(skin);
		saveBtn = new ImageButton(skin);
		loadBtn = new ImageButton(skin);
		packageBtn = new ImageButton(skin);
		exitBtn = new ImageButton(skin);
		playBtn = new ImageButton(skin);
		assetsBtn = new ImageButton(skin);
		atlasBtn = new ImageButton(skin);		

		addToolBarButton(skin, newBtn, "res/images/ic_new.png", "New",
				"Create a new project");
		addToolBarButton(skin, loadBtn, "res/images/ic_load.png", "Load",
				"Load an existing project");
		addToolBarButton(skin, saveBtn, "res/images/ic_save.png", "Save",
				"Save the current project");
		addToolBarButton(skin, exitBtn, "res/images/ic_exit.png", "Exit",
				"Save changes and exits");
		row();
		
		addToolBarButton(skin, playBtn, "res/images/ic_play.png", "Play",
				"Play Adventure");
		addToolBarButton(skin, packageBtn, "res/images/ic_package.png", "Package",
				"Package the game for distribution");
		addToolBarButton(skin, assetsBtn, "res/images/ic_assets.png", "Assets",
				"Open assets folder");
		addToolBarButton(skin, atlasBtn, "res/images/ic_atlases.png", "Atlas",
				"Create Atlas");
		
		newBtn.setDisabled(false);
		loadBtn.setDisabled(false);
		exitBtn.setDisabled(false);			

		newBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				newProject();
			}
		});

		loadBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				loadProject();
			}
		});

		exitBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				exit();
			}
		});

		saveBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				saveProject();
			}
		});

		playBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				play();
			}
		});

		packageBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				packageProject();
			}
		});
		
		assetsBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {		
				openProjectFolder();
			}
		});
		
		atlasBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {		
				createAtlas();
			}
		});		

		Ctx.project.getWorld().addPropertyChangeListener(
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent e) {
						saveBtn.setDisabled(e.getPropertyName().equals(
								"DOCUMENT_SAVED"));
					}
				});
		
		Ctx.project.addPropertyChangeListener(Project.NOTIFY_PROJECT_LOADED,
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent arg0) {
						packageBtn.setDisabled(Ctx.project.getProjectDir() == null);
						playBtn.setDisabled(Ctx.project.getProjectDir() == null);
						assetsBtn.setDisabled(Ctx.project.getProjectDir() == null);
						atlasBtn.setDisabled(Ctx.project.getProjectDir() == null);
					}
				});		
	}

	private void addToolBarButton(Skin skin, ImageButton button, String icon, String text,
			String tooltip) {
		ImageButtonStyle style = new ImageButtonStyle(skin.get(ButtonStyle.class));
		Texture image = Assets.inst().get(icon, Texture.class);
		style.imageUp = new TextureRegionDrawable(new TextureRegion(image));
		
		try {
			Texture imageDisabled = Assets.inst().get(icon.substring(0,icon.indexOf(".")) + "_disabled.png", Texture.class);
			style.imageDisabled = new TextureRegionDrawable(new TextureRegion(imageDisabled));
		} catch(Exception e) {
			
		}
				
		button.setStyle(style);
				
        add(button);
        button.setDisabled(true);
	}

	private void newProject() {
		CreateProjectDialog dialog = new CreateProjectDialog(skin);
		dialog.show(getStage());
// TODO
//		if (!dialog.isCancel()) {
//			playBtn.setDisabled(false);
//			packageBtn.setDisabled(false);
//		}
	}

	private void loadProject() {
		JFileChooser chooser = new JFileChooser(
				Ctx.project.getProjectDir() != null ? Ctx.project
						.getProjectDir() : new File("."));
		chooser.setDialogTitle("Select the project to load");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setMultiSelectionEnabled(false);

		if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			try {
				Ctx.project.saveProject();
				Ctx.project.loadProject(chooser.getSelectedFile());
				playBtn.setDisabled(false);
				packageBtn.setDisabled(false);
			} catch (Exception ex) {
				String msg = "Something went wrong while loading the project.\n\n"
						+ ex.getClass().getSimpleName()
						+ " - "
						+ ex.getMessage();
				Ctx.msg.show(getStage(), msg, 2000);
				ex.printStackTrace();
			}
		}
	}

	public void exit() {
		try {
			Ctx.project.saveProject();
			Ctx.project.saveConfig();
		} catch (TransformerException | IOException e1) {
			String msg = "Something went wrong while saving the actor.\n\n"
					+ e1.getClass().getSimpleName() + " - " + e1.getMessage();
			Ctx.msg.show(getStage(),msg, 2000);

			e1.printStackTrace();
		}

		Gdx.app.exit();
	}

	private void saveProject() {
		File file = Ctx.project.getProjectDir();

		if (file == null) {
			String msg = "Please create a new project first.";
			Ctx.msg.show(getStage(),msg, 2000);
			return;
		}

		try {
			Ctx.project.saveProject();
		} catch (Exception ex) {
			String msg = "Something went wrong while saving the project.\n\n"
					+ ex.getClass().getSimpleName() + " - " + ex.getMessage();
			Ctx.msg.show(getStage(),msg, 2000);
		}
	}

	private void packageProject() {
		saveProject();
		
		new PackageDialog(skin).show(getStage());
	}

	private void play() {
		try {
			saveProject();
			
			RunProccess.runBladeEngine(Ctx.project.getProjectDir()
					.getAbsolutePath(), null);
		} catch (IOException e) {
			String msg = "Something went wrong while playing the project.\n\n"
					+ e.getClass().getSimpleName() + " - " + e.getMessage();
			Ctx.msg.show(getStage(),msg, 2000);
		}
	}
	
	private void openProjectFolder() {
		if (Desktop.isDesktopSupported()) {
		    try {
				Desktop.getDesktop().open(new File(Ctx.project.getProjectDir().getAbsoluteFile() + "/assets"));
			} catch (IOException e1) {
				String msg = "Something went wrong while opening assets folder.\n\n"
						+ e1.getClass().getSimpleName() + " - " + e1.getMessage();
				Ctx.msg.show(getStage(),msg, 2000);
			}
		}
	}
	
	private void createAtlas() {
		new CreateAtlasDialog(skin).show(getStage());
	}	

}
