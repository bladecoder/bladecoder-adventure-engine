package org.bladecoder.engineeditor.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.Project;
import org.bladecoder.engineeditor.ui.components.EditToolbar;
import org.bladecoder.engineeditor.ui.components.Theme;
import org.bladecoder.engineeditor.utils.DesktopUtils;

import com.badlogic.gdx.assets.loaders.resolvers.ResolutionFileResolver.Resolution;

@SuppressWarnings("serial")
public class ResolutionListPanel extends javax.swing.JPanel {
	Resolution clipboard;

	private EditToolbar editToolbar;

	private javax.swing.JScrollPane jScrollPane;
	private javax.swing.JList<Resolution> list;

	private void initComponents() {
		editToolbar = new EditToolbar();
		editToolbar.hideCopyPaste();

		jScrollPane = new javax.swing.JScrollPane();
		list = new javax.swing.JList<Resolution>();

		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		setLayout(new java.awt.BorderLayout());

		add(editToolbar, BorderLayout.PAGE_START);
		add(jScrollPane, BorderLayout.CENTER);

		jScrollPane.setViewportView(list);
		jScrollPane.setBorder(BorderFactory.createLineBorder(Theme.HOLO_COLOR,
				1));
	}

	public ResolutionListPanel() {
		initComponents();

		list.setModel(new DefaultListModel<Resolution>());
		list.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				int pos = list.getSelectedIndex();

				editToolbar.enableEdit(pos != -1);
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
						addResolutions();
					}
				});
	}

	private void addResolutions() {
		if (Ctx.project.getProjectDir() != null) {

			DefaultListModel<Resolution> lm = (DefaultListModel<Resolution>) list
					.getModel();

			lm.clear();

			ArrayList<Resolution> tmp = new ArrayList<Resolution>();

			for (Resolution scn : Ctx.project.getResolutions()) {
				tmp.add(scn);
			}

			Collections.sort(tmp, new Comparator<Resolution>() {
				@Override
				public int compare(Resolution o1, Resolution o2) {
					return o2.portraitWidth - o1.portraitWidth;
				}
			});

			for (Resolution s : tmp)
				lm.addElement(s);

			if (lm.size() > 0) {
				list.setSelectedIndex(0);
			}
		}

		editToolbar.enableCreate(Ctx.project.getProjectDir() != null);
	}

	private void create() {
		new CreateResolutionDialog(Ctx.window).setVisible(true);
		addResolutions();
	}

	private void edit() {

	}

	private void delete() {
		DefaultListModel<Resolution> lm = (DefaultListModel<Resolution>) list
				.getModel();
		int index = list.getSelectedIndex();
		Resolution r = lm.get(index);

		removeDir(Ctx.project.getProjectDir() + "/" + Project.BACKGROUNDS_PATH
				+ "/" + r.suffix);
		removeDir(Ctx.project.getProjectDir() + "/" + Project.OVERLAYS_PATH
				+ "/" + r.suffix);
		removeDir(Ctx.project.getProjectDir() + "/" + Project.UI_PATH + "/"
				+ r.suffix);
		removeDir(Ctx.project.getProjectDir() + "/" + Project.ATLASES_PATH
				+ "/" + r.suffix);

		addResolutions();
	}

	private void removeDir(String dir) {
		try {
			DesktopUtils.removeDir(dir);
		} catch (IOException e) {
			String msg = "Something went wrong while deleting the resolution.\n\n"
					+ e.getClass().getSimpleName() + " - " + e.getMessage();
			JOptionPane.showMessageDialog(Ctx.window, msg);
			e.printStackTrace();
		}
	}

	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private final ListCellRenderer<Resolution> listCellRenderer = new ListCellRenderer<Resolution>() {
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
		public Component getListCellRendererComponent(JList list,
				Resolution value, int index, boolean isSelected,
				boolean cellHasFocus) {
			nameLabel.setText(value.suffix);

			panel.setOpaque(isSelected);
			return panel;
		}
	};
}
