package org.bladecoder.engineeditor.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.Project;
import org.bladecoder.engineeditor.ui.components.EditToolbar;
import org.bladecoder.engineeditor.ui.components.Theme;
import org.bladecoder.engineeditor.utils.ImageUtils;

import com.badlogic.gdx.assets.loaders.resolvers.ResolutionFileResolver.Resolution;

@SuppressWarnings("serial")
public class AssetsListPanel extends javax.swing.JPanel {
	private static final String[] ASSET_TYPES = { "3d models", "backgrounds", "bg maps",
			"atlases", "music", "sounds", "overlays" };

	private EditToolbar editToolbar;
	private JComboBox<String> assetTypes;

	private javax.swing.JScrollPane jScrollPane;
	private javax.swing.JList<String> list;

	private File lastDir;
	
	private void initComponents() {
		assetTypes = new JComboBox<String>(ASSET_TYPES);
		editToolbar = new EditToolbar();

		jScrollPane = new javax.swing.JScrollPane();
		list = new javax.swing.JList<String>();

		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		editToolbar.setAlignmentX(LEFT_ALIGNMENT);
		jScrollPane.setAlignmentX(LEFT_ALIGNMENT);
		assetTypes.setAlignmentX(LEFT_ALIGNMENT);

		add(assetTypes);
		add(editToolbar);
		add(jScrollPane);

		jScrollPane.setViewportView(list);
		jScrollPane.setBorder(BorderFactory.createLineBorder(Theme.HOLO_COLOR,
				1));

		FontMetrics fm = assetTypes.getFontMetrics(assetTypes.getFont());
		assetTypes.setMaximumSize(new Dimension(
				assetTypes.getMaximumSize().width, fm.getHeight()));
	}

	public AssetsListPanel() {
		initComponents();

		editToolbar.hideCopyPaste();

		list.setModel(new DefaultListModel<String>());
		list.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
//				int pos = list.getSelectedIndex();

//				editToolbar.enableEdit(pos != -1);
				editToolbar.enableEdit(true);
			}
		});

		list.setCellRenderer(listCellRenderer);

		editToolbar.addCreateActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				create();
			}
		});
		editToolbar.addEditActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				edit();
			}
		});
		editToolbar.addDeleteActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				delete();
			}
		});

		Ctx.project.addPropertyChangeListener(Project.NOTIFY_PROJECT_LOADED,
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent arg0) {
						editToolbar.enableCreate(Ctx.project.getProjectDir() != null);
						addAssets();
					}
				});

		assetTypes.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				addAssets();
			}
		});
	}

	private void addAssets() {
		DefaultListModel<String> lm = (DefaultListModel<String>) list
				.getModel();
		lm.clear();

		if (Ctx.project.getProjectDir() != null) {
			String type = (String) assetTypes.getSelectedItem();
			String dir = getAssetDir(type);

			if (type.equals("backgrounds") || type.equals("overlays") || type.equals("atlases"))
				dir += "/" + Ctx.project.getResolutions().get(0).suffix;

			String[] files = new File(dir).list(new FilenameFilter() {
				@Override
				public boolean accept(File arg0, String arg1) {
					String type = (String) assetTypes.getSelectedItem();

					if (type.equals("atlases") && !arg1.endsWith(".atlas"))
						return false;

					if (type.equals("bg maps") && !arg1.endsWith(".map.png"))
						return false;

					return true;
				}
			});

			if (files != null)
				for (String f : files)
					lm.addElement(f);

			if (lm.size() > 0) {
				list.setSelectedIndex(0);
			}
		}

		editToolbar.enableCreate(Ctx.project.getProjectDir() != null);
	}

	private String getAssetDir(String type) {
		String dir;

		if (type.equals("backgrounds")) {
			dir = Ctx.project.getProjectPath() + "/" + Project.BACKGROUNDS_PATH;
		} else if (type.equals("bg maps")) {
			dir = Ctx.project.getProjectPath() + "/" + Project.BACKGROUNDS_PATH;
		} else if (type.equals("atlases")) {
			dir = Ctx.project.getProjectPath() + "/" + Project.ATLASES_PATH;
		} else if (type.equals("music")) {
			dir = Ctx.project.getProjectPath() + "/" + Project.MUSIC_PATH;
		} else if (type.equals("sounds")) {
			dir = Ctx.project.getProjectPath() + "/" + Project.SOUND_PATH;
		} else if (type.equals("overlays")) {
			dir = Ctx.project.getProjectPath() + "/" + Project.OVERLAYS_PATH;
		} else if (type.equals("3d models")) {
			dir = Ctx.project.getProjectPath() + "/" + Project.SPRITE3D_PATH;			
		} else {
			dir = Ctx.project.getProjectPath() + Project.ASSETS_PATH;
		}

		return dir;
	}

	private void create() {
		String type = (String) assetTypes.getSelectedItem();

		if (type.equals("atlases")) {
			new CreateAtlasDialog(Ctx.window).setVisible(true);
		} else {

			JFileChooser chooser = new JFileChooser(lastDir);
			chooser.setDialogTitle("Select the '" + type + "' asset files");
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooser.setMultiSelectionEnabled(true);

			FileNameExtensionFilter filter = null;

			if (type.equals("backgrounds") || type.equals("overlays"))
				filter = new FileNameExtensionFilter("Images", "jpg", "png",
						"etc1");
			else if (type.equals("music") || type.equals("sounds"))
				filter = new FileNameExtensionFilter("Music", "wav", "mp3",
						"ogg");
			else if (type.equals("bg maps"))
				filter = new FileNameExtensionFilter("Map", "png");
			else if (type.equals("3d models"))
				filter = new FileNameExtensionFilter("3D Models", "g3db", "png");			

			chooser.removeChoosableFileFilter(chooser.getChoosableFileFilters()[0]);
			chooser.addChoosableFileFilter(filter);

			if (chooser.showOpenDialog(Ctx.window) == JFileChooser.APPROVE_OPTION) {
				try {
					File[] files = chooser.getSelectedFiles();
					String dir = getAssetDir(type);
					lastDir = chooser.getSelectedFile();

					for (File f : files) {
						if (type.equals("backgrounds")
								|| type.equals("overlays")) {
							List<Resolution> res = Ctx.project.getResolutions();
							int wWidth = Ctx.project.getWorld().getWidth();

							for (Resolution r : res) {
								File destFile = new File(dir + "/" + r.suffix
										+ "/" + f.getName());

								if (r.portraitWidth != wWidth) {
									float scale = r.portraitWidth / (float)wWidth;

									ImageUtils.scaleImageFile(f, destFile,
											scale);
								} else {
									Files.copy(f.toPath(), destFile.toPath());
								}
							}
						} else {
							File destFile = new File(dir + "/" + f.getName());
							Files.copy(f.toPath(), destFile.toPath());
						}
						
					}

				} catch (Exception ex) {
					String msg = "Something went wrong while getting the assets.\n\n"
							+ ex.getClass().getSimpleName()
							+ " - "
							+ ex.getMessage();
					JOptionPane.showMessageDialog(Ctx.window, msg);
					ex.printStackTrace();
				}
			}
		}
			
		addAssets();
	}

	private void edit() {
		if (Desktop.isDesktopSupported()) {
			try {
				Desktop.getDesktop().open(
						new File(Ctx.project.getProjectDir().getAbsoluteFile()
								+ "/assets"));
			} catch (IOException e1) {
				String msg = "Something went wrong while opening assets folder.\n\n"
						+ e1.getClass().getSimpleName()
						+ " - "
						+ e1.getMessage();
				JOptionPane.showMessageDialog(Ctx.window, msg);
			}
		}
	}

	private void delete() {
		String type = (String) assetTypes.getSelectedItem();
		String dir = getAssetDir(type);

		String name = list.getSelectedValue();
		try {
			if (type.equals("backgrounds") || type.equals("overlays")
					|| type.equals("atlases")) {
				List<Resolution> res = Ctx.project.getResolutions();

				for (Resolution r : res) {
					File file = new File(dir + "/" + r.suffix + "/" + name);

					file.delete();

					// delete pages on atlases
					if (type.equals("atlases")) {
						File atlasDir = new File(dir + "/" + r.suffix);

						File[] files = atlasDir.listFiles();

						if (files != null)
							for (File f : files) {
								String destName = f.getName();
								String nameWithoutExt = name.substring(0,
										name.lastIndexOf('.'));
								String destNameWithoutExt = destName.substring(0,
										destName.lastIndexOf('.'));
								
								if(destNameWithoutExt.length() < nameWithoutExt.length())
									continue;
								
								String suffix = destNameWithoutExt.substring( nameWithoutExt.length());
								
								if(!suffix.isEmpty() && !suffix.matches("[0-9]+"))
									continue;

								if (destName.startsWith(nameWithoutExt)
										&& destName.toLowerCase().endsWith(".png"))
									Files.delete(f.toPath());
							}
					}
				}
			} else {
				File file = new File(dir + "/" + name);
				file.delete();
			}

			addAssets();
		} catch (Exception ex) {
			String msg = "Something went wrong while deleting the asset.\n\n"
					+ ex.getClass().getSimpleName() + " - " + ex.getMessage();
			JOptionPane.showMessageDialog(Ctx.window, msg);
			ex.printStackTrace();
		}
	}

	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private final ListCellRenderer<String> listCellRenderer = new ListCellRenderer<String>() {
		private final JPanel panel = new JPanel(new BorderLayout());
		private final JPanel txtPanel = new JPanel(new BorderLayout());
		private final JLabel nameLabel = new JLabel();
		private final JLabel infoLabel = new JLabel();

		{
			txtPanel.setOpaque(false);
			txtPanel.add(nameLabel, BorderLayout.CENTER);
			txtPanel.add(infoLabel, BorderLayout.SOUTH);

			panel.setBorder(new EmptyBorder(5, 10, 5, 10));
			panel.add(txtPanel, BorderLayout.CENTER);
			panel.setBackground(Color.DARK_GRAY);

			Font font = nameLabel.getFont();
			nameLabel.setFont(new Font(font.getName(), Font.BOLD, font
					.getSize()));
		}

		@SuppressWarnings("rawtypes")
		@Override
		public Component getListCellRendererComponent(JList list, String value,
				int index, boolean isSelected, boolean cellHasFocus) {
			nameLabel.setText(value);

			panel.setOpaque(isSelected);
			return panel;
		}
	};
}
