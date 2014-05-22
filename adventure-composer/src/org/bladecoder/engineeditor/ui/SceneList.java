package org.bladecoder.engineeditor.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import javax.swing.JOptionPane;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.Project;
import org.bladecoder.engineeditor.model.WorldDocument;
import org.bladecoder.engineeditor.ui.components.CellRenderer;
import org.bladecoder.engineeditor.ui.components.EditElementDialog;
import org.bladecoder.engineeditor.ui.components.ElementList;
import org.bladecoder.engineeditor.utils.EditorLogger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

public class SceneList extends ElementList {

	private ImageButton initBtn;
	private SelectBox<String> chapters;

	public SceneList(Skin skin) {
		super(skin, true);

		chapters = new SelectBox<String>(skin);
		clearChildren();
		
		add(chapters).expandX().fillX();
		row().fill();
		add(toolbar).expandX().fillX();
		row().fill();
		add(container).expandY().fill();

//		Label lbl = new Label("CHAPTER", skin);
//		Font font = lbl.getFont();
//		lbl.setFont(new Font(font.getName(), Font.BOLD, font.getSize()));
//		lbl.setAlignmentX(LEFT_ALIGNMENT);
//		chapterPanel.add(lbl);
//		chapterPanel.add(Box.createHorizontalStrut(10));
//		chapterPanel.add(chapters);
//		chapterPanel.setAlignmentX(LEFT_ALIGNMENT);
//		add(chapterPanel, 0);

//		FontMetrics fm = chapters.getFontMetrics(chapters.getFont());
//		chapters.setMaximumSize(new Dimension(chapters.getMaximumSize().width,
//				fm.getHeight() + 10));

		initBtn = new ImageButton(skin);
		toolbar.addToolBarButton(initBtn, "res/images/ic_check.png",
				"Set init scene", "Set init scene");

		initBtn.setDisabled(true);

		list.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				int pos = list.getSelectedIndex();

				if (pos == -1) {
					Ctx.project.setSelectedScene(null);
				} else {
					Element a = list.getItems().get(pos);
					Ctx.project.setSelectedScene(a);
				}

				toolbar.disableEdit(pos == -1);
				initBtn.setDisabled(pos == -1);
			}
		});

		list.setCellRenderer(listCellRenderer);

		initBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				setDefault();
			}

		});

		Ctx.project.addPropertyChangeListener(Project.NOTIFY_PROJECT_LOADED,
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent arg0) {
						toolbar.disableCreate(Ctx.project.getProjectDir() == null);

						addChapters();
					}
				});

		chapters.addListener(chapterListener);

		Ctx.project.getWorld().addPropertyChangeListener(
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent evt) {
						EditorLogger.debug(evt.getPropertyName() + " NEW:"
								+ evt.getNewValue() + " OLD:"
								+ evt.getOldValue());

						if (evt.getPropertyName().equals("chapter")) {
							chapters.removeListener(chapterListener);
							addChapters();
							chapters.addListener(chapterListener);
						} else if (evt.getPropertyName().equals(
								"ELEMENT_DELETED")) {
							Element e = (Element) evt.getNewValue();

							if (e.getTagName().equals("chapter")) {
								chapters.removeListener(chapterListener);
								addChapters();
								chapters.addListener(chapterListener);
							}
						}
					}
				});
	}

	ChangeListener chapterListener = new ChangeListener() {
		@Override
		public void changed(ChangeEvent event, Actor actor) {
			WorldDocument w = Ctx.project.getWorld();
			String selChapter = (String) chapters.getSelected();

			if (selChapter != null && !selChapter.equals(Ctx.project.getSelectedChapter().getId())) {

				// Save the project when changing chapter
				try {
					Ctx.project.saveProject();
				} catch (IOException | TransformerException e1) {
					Ctx.msg.show(getStage(),
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
		chapters.getItems().clear();

		for (int i = 0; i < nl.getLength(); i++) {
			Element e = (Element) nl.item(i);

			chapters.getItems().add(e.getAttribute("id"));
		}

		chapters.setSelected(Ctx.project.getSelectedChapter().getId());
	}

	private void setDefault() {

		int pos = list.getSelectedIndex();

		if (pos == -1)
			return;

		String id = list.getItems().get(pos).getAttribute("id");

		doc.setRootAttr((Element) list.getItems().get(pos).getParentNode(),
				"init_scene", id);

	}

	@Override
	protected EditElementDialog getEditElementDialogInstance(
			Element e) {
		return new EditSceneDialog(skin, doc, parent, e);
	}

	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private final CellRenderer<Element> listCellRenderer = new CellRenderer<Element>() {

		@Override
		protected String getCellTitle(Element e) {
			String name = e.getAttribute("id");

			Element chapter = (Element) e.getParentNode();

			String init = chapter.getAttribute("init_scene");

			if (init.equals(name))
				name += " <init>";

			return name;
		}

		@Override
		protected String getCellSubTitle(Element e) {
			return e.getAttribute("background");
		}

		@Override
		public TextureRegion getCellImage(Element e) {
//			String bg = e.getAttribute("background");
//			String bgPath = Ctx.project.getProjectPath()
//					+ Project.BACKGROUNDS_PATH + "/" + Ctx.project.getResDir()
//					+ "/" + bg;
//
//			File f = new File(bgPath);
//
//			ImageIcon ic = null;
//
//			try {
//				ic = ImageUtils.getImageIcon(f.toURI().toURL(), 100);
//			} catch (IOException e1) {
//				ic = new ImageIcon(getClass().getResource(
//						"/res/images/ic_no_scene.png"));
//			}
//
//			if (ic == null)
//				ic = new ImageIcon(getClass().getResource(
//						"/res/images/ic_no_scene.png"));
//
//			return ic;
			
			return null;
		}
		
		@Override
		protected boolean hasSubtitle() {
			return true;
		}
	};
}
