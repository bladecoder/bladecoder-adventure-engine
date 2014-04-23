package org.bladecoder.engineeditor.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.SceneDocument;
import org.bladecoder.engineeditor.ui.components.EditToolbar;
import org.bladecoder.engineeditor.ui.components.Theme;
import org.w3c.dom.Element;

import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.utils.Array;

@SuppressWarnings("serial")
public class FA3DListPanel extends JPanel {

	private SceneDocument scn;
	private Element actor;

	private EditToolbar editToolbar;

	private javax.swing.JScrollPane jScrollPane;
	private javax.swing.JList<Animation> list;

	private JButton initBtn;

	public FA3DListPanel() {
		editToolbar = new EditToolbar();
		editToolbar.hideCopyPaste();
		editToolbar.enableEdit(false);

		initBtn = new JButton();
		editToolbar.addToolBarButton(initBtn, "/res/images/ic_check.png",
				"Set init scene", "Set init scene");
		initBtn.setEnabled(false);

		jScrollPane = new javax.swing.JScrollPane();
		list = new javax.swing.JList<Animation>();

		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		setLayout(new java.awt.BorderLayout());

		add(editToolbar, BorderLayout.PAGE_START);
		add(jScrollPane, BorderLayout.CENTER);

		jScrollPane.setViewportView(list);
		jScrollPane.setBorder(BorderFactory.createLineBorder(Theme.HOLO_COLOR,
				1));

		list.setModel(new DefaultListModel<Animation>());
		list.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				int pos = list.getSelectedIndex();

				String id = null;

				if (pos != -1)
					id = list.getModel().getElementAt(pos).id;

				Ctx.project.setSelectedFA(id);

				// editToolbar.enableEdit(pos != -1);
				initBtn.setEnabled(pos != -1);
			}
		});

		list.setCellRenderer(listCellRenderer);

		initBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setDefault();
			}
		});
	}

	private void setDefault() {

		int pos = list.getSelectedIndex();

		if (pos == -1)
			return;

		DefaultListModel<Animation> lm = (DefaultListModel<Animation>) list
				.getModel();

		String id = lm.getElementAt(pos).id;

		scn.setRootAttr(actor, "init_frame_animation", id);

		list.repaint();
	}

	public void addAnimations(SceneDocument doc, Element a) {
		this.scn = doc;
		this.actor = a;

		DefaultListModel<Animation> lm = (DefaultListModel<Animation>) list
				.getModel();

		lm.clear();

		if (a != null
				&& a.getAttribute("type").equals(SceneDocument.SPRITE3D_ACTOR)) {

			ArrayList<Animation> tmp = new ArrayList<Animation>();

			Array<Animation> anims = doc.getAnimations3d(a.getAttribute("id"));

			if (anims != null) {
				for (Animation anim : anims) {
					tmp.add(anim);
				}
			}

			Collections.sort(tmp, new Comparator<Animation>() {
				@Override
				public int compare(Animation o1, Animation o2) {
					return o2.id.compareTo(o1.id);
				}
			});

			for (Animation s : tmp)
				lm.addElement(s);

			if (lm.size() > 0) {
				list.setSelectedIndex(0);
			}
		}

		editToolbar.enableCreate(false);
	}

	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private final ListCellRenderer<Animation> listCellRenderer = new ListCellRenderer<Animation>() {
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
				Animation value, int index, boolean isSelected,
				boolean cellHasFocus) {

			String name = value.id;

			String init = actor.getAttribute("init_frame_animation");

			if (init == null || init.isEmpty()) {

				init = scn.getAnimations3d(actor.getAttribute("id")).get(0).id;
			}

			if (init.equals(name))
				name += " <init>";

			nameLabel.setText(name);

			panel.setOpaque(isSelected);
			return panel;
		}
	};
}
