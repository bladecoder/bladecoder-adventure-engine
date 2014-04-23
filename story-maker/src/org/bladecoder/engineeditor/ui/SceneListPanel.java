package org.bladecoder.engineeditor.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
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
import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.model.Project;
import org.bladecoder.engineeditor.model.SceneDocument;
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
		chapters.setMaximumSize(new Dimension(chapters.getMaximumSize().width, fm.getHeight() + 10));

		initBtn = new JButton();
		editToolbar.addToolBarButton(initBtn, "/res/images/ic_check.png", "Set init scene",
				"Set init scene");
		initBtn.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource(
				"/res/images/ic_check_disabled.png")));

		initBtn.setEnabled(false);

		list.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
			
				int pos = list.getSelectedIndex();
				EditorLogger.debug("SCENE SELECTION LISTENER " + pos);

				if (pos == -1) {
					Ctx.project.setSelectedScene((SceneDocument) null);
				} else {
					Element scn = list.getModel().getElementAt(pos);
					if(Ctx.project.getSelectedScene() == null)
						Ctx.project.setSelectedScene(scn.getAttribute("id"));
					else if(Ctx.project.getSelectedScene() != null && !Ctx.project.getSelectedScene().getId().equals(scn.getAttribute("id")))
						Ctx.project.setSelectedScene(scn.getAttribute("id"));
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

		Ctx.project.getWorld().addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				EditorLogger.debug(evt.getPropertyName() + " NEW:" + evt.getNewValue() + " OLD:"
						+ evt.getOldValue());
				
				if (evt.getPropertyName().equals("chapter")) {
					chapters.removeActionListener(actionListener);
					addChapters();
					chapters.addActionListener(actionListener);
				} else if (evt.getPropertyName().equals("ELEMENT_DELETED")) {
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
			
			// Save the project when changing chapter
			try{
				Ctx.project.saveProject();
			} catch (IOException | TransformerException e1) {
				JOptionPane.showMessageDialog(Ctx.window, "Error saving project");
				EditorLogger.error(e1.getMessage());
			}
			
			try {
				String selChapter = (String) chapters.getSelectedItem();
				w.setCurrentChapter(selChapter);
				addElements(w, null, "scene");
			} catch (ParserConfigurationException | SAXException | IOException e1) {
				e1.printStackTrace();
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

		chapters.setSelectedItem(w.getCurrentChapter().getAttribute("id"));
	}

	@Override
	public void addElements(BaseDocument doc, Element parent, String tag) {
		super.addElements(doc, parent, tag);

		if (doc != null) {

			ElementListModel lm = (ElementListModel) list.getModel();

			for (SceneDocument scn : ((WorldDocument) doc).getSceneMap().values()) {
				lm.addElement(scn.getElement());
			}

			if (lm.getSize() > 0) {
				list.setSelectedIndex(0);
			} else {
				list.clearSelection();
			}
		}

		editToolbar.enableCreate(doc != null);

	}

	@Override
	protected void delete() {

		int pos = list.getSelectedIndex();

		if (pos == -1)
			return;

		ElementListModel lm = (ElementListModel) list.getModel();
		SceneDocument scn = ((WorldDocument) doc).getScene(lm.getElementAt(pos).getAttribute("id"));

		try {
			Ctx.project.getWorld().removeScene(scn);
		} catch (Exception e) {
			String msg = "Something went wrong while deleting the scene.\n\n"
					+ e.getClass().getSimpleName() + " - " + e.getMessage();
			JOptionPane.showMessageDialog(Ctx.window, msg);

			e.printStackTrace();
		}

		super.delete();
	}

	@Override
	protected void paste() {
		WorldDocument w = (WorldDocument) doc;

		SceneDocument scn;
		try {
			scn = w.createScene(clipboard.getAttribute("id"));
			String id = scn.getId();

			Element newElement = scn.cloneNode(clipboard);

			scn.getDocument().replaceChild(newElement, scn.getElement());
			// scn.getDocument().appendChild(newElement);
			scn.setModified(true);

			newElement.setAttribute("id", id);

			ElementListModel lm = (ElementListModel) list.getModel();
			lm.addElement(newElement);
			list.setSelectedValue(newElement, true);
			doc.setModified(newElement);
		} catch (FileNotFoundException | TransformerException | ParserConfigurationException e) {
			String msg = "Something went wrong while pasting the scene.\n\n"
					+ e.getClass().getSimpleName() + " - " + e.getMessage();
			JOptionPane.showMessageDialog(Ctx.window, msg);

			e.printStackTrace();
		}

	}

	private void setDefault() {
		WorldDocument w = (WorldDocument) doc;

		int pos = list.getSelectedIndex();

		if (pos == -1)
			return;

		ElementListModel lm = (ElementListModel) list.getModel();
		String id = lm.getElementAt(pos).getAttribute("id");

		w.setInitScene(id);

		list.repaint();
	}

	@Override
	protected CreateEditElementDialog getCreateEditElementDialogInstance(Element e) {
		SceneDocument scn = null;

		if (e != null)
			scn = ((WorldDocument) doc).getScene(e.getAttribute("id"));

		return new CreateEditSceneDialog(Ctx.window, (WorldDocument) doc, scn, parent, e);
	}

	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private final ElementListCellRender listCellRenderer = new ElementListCellRender(true) {

		@Override
		public String getName(Element e) {
			String name = e.getAttribute("id");

			String init = ((WorldDocument) doc).getInitScene();

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
			String bgPath = Ctx.project.getProjectPath() + Project.BACKGROUNDS_PATH + "/"
					+ Ctx.project.getResDir() + "/" + bg;

			File f = new File(bgPath);

			ImageIcon ic = null;

			try {
				ic = ImageUtils.getImageIcon(f.toURI().toURL(), 100);
			} catch (IOException e1) {
				ic = new ImageIcon(getClass().getResource("/res/images/ic_no_scene.png"));
			}

			if (ic == null)
				ic = new ImageIcon(getClass().getResource("/res/images/ic_no_scene.png"));

			return ic;
		}
	};
}
