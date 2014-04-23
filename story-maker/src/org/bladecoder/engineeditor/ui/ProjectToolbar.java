package org.bladecoder.engineeditor.ui;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.xml.transform.TransformerException;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.Project;
import org.bladecoder.engineeditor.utils.RunProccess;

@SuppressWarnings("serial")
public class ProjectToolbar extends JPanel {
	private JButton newBtn;
	private JButton loadBtn;
	private JButton saveBtn;
	private JButton packageBtn;
	private JButton exitBtn;
	private JButton playBtn;
	private JButton assetsBtn;
	private JButton atlasBtn;

	public ProjectToolbar() {
		super();
		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
//		setLayout(new FlowLayout());

		newBtn = new JButton();
		saveBtn = new JButton();
		loadBtn = new JButton();
		packageBtn = new JButton();
		exitBtn = new JButton();
		playBtn = new JButton();
		assetsBtn = new JButton();
		atlasBtn = new JButton();
		
		JToolBar toolbar1 = new JToolBar();
		JToolBar toolbar2 = new JToolBar();
		
		add(toolbar1);
		add(toolbar2);

		addToolBarButton(toolbar1, newBtn, "/res/images/ic_new.png", "New",
				"Create a new project");
		addToolBarButton(toolbar1, loadBtn, "/res/images/ic_load.png", "Load",
				"Load an existing project");
		addToolBarButton(toolbar1, saveBtn, "/res/images/ic_save.png", "Save",
				"Save the current project");
		addToolBarButton(toolbar1, exitBtn, "/res/images/ic_exit.png", "Exit",
				"Save changes and exits");
		
		addToolBarButton(toolbar2, playBtn, "/res/images/ic_play.png", "Play",
				"Play Adventure");
		addToolBarButton(toolbar2, packageBtn, "/res/images/ic_package.png", "Package",
				"Package the game for distribution");
		addToolBarButton(toolbar2, assetsBtn, "/res/images/ic_assets.png", "Assets",
				"Open assets folder");
		addToolBarButton(toolbar2, atlasBtn, "/res/images/ic_atlases.png", "Atlas",
				"Create Atlas");
		

		saveBtn.setDisabledIcon(new javax.swing.ImageIcon(getClass()
				.getResource("/res/images/ic_save_disabled.png")));
		saveBtn.setEnabled(false);

		playBtn.setDisabledIcon(new javax.swing.ImageIcon(getClass()
				.getResource("/res/images/ic_play_disabled.png")));

		packageBtn.setDisabledIcon(new	 javax.swing.ImageIcon(getClass().
				 getResource("/res/images/ic_package_disabled.png")));
	
		assetsBtn.setDisabledIcon(new	 javax.swing.ImageIcon(getClass().
				 getResource("/res/images/ic_assets_disabled.png")));
		
		packageBtn.setEnabled(false);
		playBtn.setEnabled(false);
		assetsBtn.setEnabled(false);
		atlasBtn.setEnabled(false);
			

		newBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				newProject();
			}
		});

		loadBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadProject();
			}
		});

		exitBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				exit();
			}
		});

		saveBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveProject();
			}
		});

		playBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				play();
			}
		});

		packageBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				packageProject();
			}
		});
		
		assetsBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {				
				openProjectFolder();
			}
		});
		
		atlasBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {				
				createAtlas();
			}
		});		

		Ctx.project.getWorld().addPropertyChangeListener(
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent e) {
						saveBtn.setEnabled(!e.getPropertyName().equals(
								"DOCUMENT_SAVED"));
					}
				});
		
		Ctx.project.addPropertyChangeListener(Project.NOTIFY_PROJECT_LOADED,
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent arg0) {
						packageBtn.setEnabled(Ctx.project.getProjectDir() != null);
						playBtn.setEnabled(Ctx.project.getProjectDir() != null);
						assetsBtn.setEnabled(Ctx.project.getProjectDir() != null);
						atlasBtn.setEnabled(Ctx.project.getProjectDir() != null);
					}
				});		
	}

	private void addToolBarButton(JToolBar panel, JButton button, String icon, String text,
			String tooltip) {
		button.setIcon(new javax.swing.ImageIcon(getClass().getResource(icon)));
		// button.setText(text);
		button.setToolTipText(tooltip);
		button.setFocusable(false);
		// button.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
		button.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
		panel.add(button);
		button.setEnabled(true);
	}

	private void newProject() {
		CreateProjectDialog dialog = new CreateProjectDialog(Ctx.window);
		dialog.setVisible(true);

		if (!dialog.isCancel()) {
			playBtn.setEnabled(true);
			packageBtn.setEnabled(true);
		}
	}

	private void loadProject() {
		JFileChooser chooser = new JFileChooser(
				Ctx.project.getProjectDir() != null ? Ctx.project
						.getProjectDir() : new File("."));
		chooser.setDialogTitle("Select the project to load");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setMultiSelectionEnabled(false);

		if (chooser.showOpenDialog(Ctx.window) == JFileChooser.APPROVE_OPTION) {
			try {
				Ctx.project.saveProject();
				Ctx.project.loadProject(chooser.getSelectedFile());
				playBtn.setEnabled(true);
				packageBtn.setEnabled(true);
			} catch (Exception ex) {
				String msg = "Something went wrong while loading the project.\n\n"
						+ ex.getClass().getSimpleName()
						+ " - "
						+ ex.getMessage();
				JOptionPane.showMessageDialog(Ctx.window, msg);
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
			JOptionPane.showMessageDialog(Ctx.window, msg);

			e1.printStackTrace();
		}

		Ctx.window.dispose();
		System.exit(0);
	}

	private void saveProject() {
		File file = Ctx.project.getProjectDir();

		if (file == null) {
			String msg = "Please create a new project first.";
			JOptionPane.showMessageDialog(Ctx.window, msg);
			return;
		}

		try {
			Ctx.project.saveProject();
			saveBtn.setEnabled(false);

		} catch (Exception ex) {
			String msg = "Something went wrong while saving the project.\n\n"
					+ ex.getClass().getSimpleName() + " - " + ex.getMessage();
			JOptionPane.showMessageDialog(Ctx.window, msg);
		}
	}

	private void packageProject() {
		saveProject();
		
		new PackageDialog(Ctx.window).setVisible(true);
	}

	private void play() {
		try {
			saveProject();
			
			RunProccess.runBladeEngine(Ctx.project.getProjectDir()
					.getAbsolutePath());
		} catch (IOException e) {
			String msg = "Something went wrong while playing the project.\n\n"
					+ e.getClass().getSimpleName() + " - " + e.getMessage();
			JOptionPane.showMessageDialog(Ctx.window, msg);
		}
	}
	
	private void openProjectFolder() {
		if (Desktop.isDesktopSupported()) {
		    try {
				Desktop.getDesktop().open(new File(Ctx.project.getProjectDir().getAbsoluteFile() + "/assets"));
			} catch (IOException e1) {
				String msg = "Something went wrong while opening assets folder.\n\n"
						+ e1.getClass().getSimpleName() + " - " + e1.getMessage();
				JOptionPane.showMessageDialog(Ctx.window, msg);
			}
		}
	}
	
	private void createAtlas() {
		new CreateAtlasDialog(Ctx.window).setVisible(true);
	}	

}
