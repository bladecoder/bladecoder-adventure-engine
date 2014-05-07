package org.bladecoder.engineeditor.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.Project;
import org.bladecoder.engineeditor.model.WorldDocument;
import org.bladecoder.engineeditor.ui.components.CreateEditElementDialog;
import org.bladecoder.engineeditor.ui.components.ElementListCellRender;
import org.bladecoder.engineeditor.ui.components.ElementListModel;
import org.bladecoder.engineeditor.ui.components.ElementListPanel;
import org.bladecoder.engineeditor.utils.EditorLogger;
import org.bladecoder.engineeditor.utils.ImageUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@SuppressWarnings("serial")
public class SceneListPanel extends ElementListPanel {

	private JButton initBtn;
	private JComboBox<String> chapters;

	public SceneListPanel() {
		super(true);

		chapters = new JComboBox<String>();
		chapters.setAlignmentX(LEFT_ALIGNMENT);

		JPanel chapterPanel = new JPanel();
		chapterPanel.setLayout(new BoxLayout(chapterPanel, BoxLayout.X_AXIS));
		JLabel lbl = new JLabel("CHAPTER");
		Font font = lbl.getFont();
		lbl.setFont(new Font(font.getName(), Font.BOLD, font.getSize()));
		lbl.setAlignmentX(LEFT_ALIGNMENT);
		chapterPanel.add(lbl);
		chapterPanel.add(Box.createHorizontalStrut(10));
		chapterPanel.add(chapters);
		chapterPanel.setAlignmentX(LEFT_ALIGNMENT);
		add(chapterPanel, 0);

		FontMetrics fm = chapters.getFontMetrics(chapters.getFont());
		chapters.setMaximumSize(new Dimension(chapters.getMaximumSize().width,
				fm.getHeight() + 10));

		initBtn = new JButton();
		editToolbar.addToolBarButton(initBtn, "/res/images/ic_check.png",
				"Set init scene", "Set init scene");
		initBtn.setDisabledIcon(new javax.swing.ImageIcon(getClass()
				.getResource("/res/images/ic_check_disabled.png")));

		initBtn.setEnabled(false);

		list.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				int pos = list.getSelectedIndex();

				if (pos == -1) {
					Ctx.project.setSelectedScene(null);
				} else {
					Element a = list.getModel().getElementAt(pos);
					Ctx.project.setSelectedScene(a);
				}

				editToolbar.enableEdit(pos != -1);
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

		Ctx.project.addPropertyChangeListener(Project.NOTIFY_PROJECT_LOADED,
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent arg0) {
						editToolbar.enableCreate(Ctx.project.getProjectDir() != null);

						addChapters();
					}
				});

		chapters.addActionListener(actionListener);

		Ctx.project.getWorld().addPropertyChangeListener(
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent evt) {
						EditorLogger.debug(evt.getPropertyName() + " NEW:"
								+ evt.getNewValue() + " OLD:"
								+ evt.getOldValue());

						if (evt.getPropertyName().equals("chapter")) {
							chapters.removeActionListener(actionListener);
							addChapters();
							chapters.addActionListener(actionListener);
						} else if (evt.getPropertyName().equals(
								"ELEMENT_DELETED")) {
							Element e = (Element) evt.getNewValue();

							if (e.getTagName().equals("chapter")) {
								chapters.removeActionListener(actionListener);
								addChapters();
								chapters.addActionListener(actionListener);
							}
						}
					}
				});
	}

	ActionListener actionListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			WorldDocument w = Ctx.project.getWorld();
			String selChapter = (String) chapters.getSelectedItem();

			if (selChapter != null && !selChapter.equals(Ctx.project.getSelectedChapter().getId())) {

				// Save the project when changing chapter
				try {
					Ctx.project.saveProject();
				} catch (IOException | TransformerException e1) {
					JOptionPane.showMessageDialog(Ctx.window,
							"Error saving project");
					EditorLogger.error(e1.getMessage());
				}

				try {
					if(selChapter != null)
						Ctx.project.loadChapter(selChapter);
					
					addElements(w, null, "scene");
				} catch (ParserConfigurationException | SAXException
						| IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	};

	public void addChapters() {
		WorldDocument w = Ctx.project.getWorld();
		NodeList nl = w.getChapters();
		chapters.removeAllItems();

		for (int i = 0; i < nl.getLength(); i++) {
			Element e = (Element) nl.item(i);

			chapters.addItem(e.getAttribute("id"));
		}

		chapters.setSelectedItem(Ctx.project.getSelectedChapter().getId());
	}

	private void setDefault() {

		int pos = list.getSelectedIndex();

		if (pos == -1)
			return;

		ElementListModel lm = (ElementListModel) list.getModel();
		String id = lm.getElementAt(pos).getAttribute("id");

		doc.setRootAttr((Element) lm.getElementAt(pos).getParentNode(),
				"init_scene", id);

		list.repaint();
	}

	@Override
	protected CreateEditElementDialog getCreateEditElementDialogInstance(
			Element e) {
		return new CreateEditSceneDialog(Ctx.window, doc, parent, e);
	}

	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private final ElementListCellRender listCellRenderer = new ElementListCellRender(
			true) {

		@Override
		public String getName(Element e) {
			String name = e.getAttribute("id");

			Element chapter = (Element) e.getParentNode();

			String init = chapter.getAttribute("init_scene");

			if (init.equals(name))
				name += " <init>";

			return name;
		}

		@Override
		public String getInfo(Element e) {
			return e.getAttribute("background");
		}

		@Override
		public ImageIcon getImageIcon(Element e) {
			String bg = e.getAttribute("background");
			String bgPath = Ctx.project.getProjectPath()
					+ Project.BACKGROUNDS_PATH + "/" + Ctx.project.getResDir()
					+ "/" + bg;

			File f = new File(bgPath);

			ImageIcon ic = null;

			try {
				ic = ImageUtils.getImageIcon(f.toURI().toURL(), 100);
			} catch (IOException e1) {
				ic = new ImageIcon(getClass().getResource(
						"/res/images/ic_no_scene.png"));
			}

			if (ic == null)
				ic = new ImageIcon(getClass().getResource(
						"/res/images/ic_no_scene.png"));

			return ic;
		}
	};
}
