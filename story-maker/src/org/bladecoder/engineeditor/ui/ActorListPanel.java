package org.bladecoder.engineeditor.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.Project;
import org.bladecoder.engineeditor.model.SceneDocument;
import org.bladecoder.engineeditor.ui.components.CreateEditElementDialog;
import org.bladecoder.engineeditor.ui.components.ElementListCellRender;
import org.bladecoder.engineeditor.ui.components.ElementListModel;
import org.bladecoder.engineeditor.ui.components.ElementListPanel;
import org.w3c.dom.Element;

@SuppressWarnings("serial")
public class ActorListPanel extends ElementListPanel {

	private JButton playerBtn;

	public ActorListPanel() {
		super(true);

		playerBtn = new JButton();
		editToolbar.addToolBarButton(playerBtn, "/res/images/ic_check.png",
				"Set player", "Set player");
		playerBtn.setEnabled(false);

		list.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
//				EditorLogger.debug("ACTOR LIST ELEMENT SELECTED");
				int pos = list.getSelectedIndex();

				if (pos == -1) {
					Ctx.project.setSelectedActor(null);
				} else {
					Element a = list.getModel().getElementAt(pos);
					Ctx.project.setSelectedActor(a);
				}

				editToolbar.enableEdit(pos != -1);
				playerBtn.setEnabled(pos!= -1);
			}
		});

		playerBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setPlayer();
			}
		});

		list.setCellRenderer(listCellRenderer);

		Ctx.project.addPropertyChangeListener(Project.NOTIFY_ACTOR_SELECTED,
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent e) {
						int pos = list.getSelectedIndex();

						// Element newActor = (Element) e.getNewValue();
						Element newActor = Ctx.project.getSelectedActor();

						if (newActor == null)
							return;

						if (pos != -1) {
							Element oldActor = list.getModel()
									.getElementAt(pos);

							if (oldActor == newActor) {
								return;
							}
						}

						list.setSelectedValue(newActor, true);
					}
				});
	}

	@Override
	protected CreateEditElementDialog getCreateEditElementDialogInstance(
			Element e) {
		return new CreateEditActorDialog(Ctx.window, doc, parent, e);
	}

	private void setPlayer() {
		SceneDocument scn = (SceneDocument) doc;

		int pos = list.getSelectedIndex();

		if (pos == -1)
			return;

		ElementListModel lm = (ElementListModel) list.getModel();
		Element e = lm.getElementAt(pos);

		if (e.getAttribute("type").equals(SceneDocument.SPRITE3D_ACTOR_TYPE)
				|| e.getAttribute("type").equals(SceneDocument.ATLAS_ACTOR_TYPE)
				|| e.getAttribute("type").equals(SceneDocument.SPINE_ACTOR_TYPE)
				) {
			String id = e.getAttribute("id");

			scn.setRootAttr("player", id);

			list.repaint();
		}
	}

	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private final ElementListCellRender listCellRenderer = new ElementListCellRender(
			true) {

		@Override
		public String getName(Element e) {
			return e.getAttribute("id");
		}

		@Override
		public String getInfo(Element e) {
			return doc.getTranslation(e.getAttribute("desc"));
		}

		@Override
		public ImageIcon getImageIcon(Element e) {
			String type = e.getAttribute("type");
			URL u = null;

			boolean isPlayer = doc.getRootAttr("player").equals(
					e.getAttribute("id"));

			if (isPlayer) {
				u = getClass().getResource("/res/images/ic_player.png");
			} else if (type.equals(SceneDocument.FOREGROUND_ACTOR_TYPE)) {
				u = getClass().getResource("/res/images/ic_fg_actor.png");
			} else if (type.equals(SceneDocument.ATLAS_ACTOR_TYPE)) {
				u = getClass().getResource("/res/images/ic_sprite_actor.png");
			} else if (type.equals(SceneDocument.BACKGROUND_ACTOR_TYPE)) {
				u = getClass().getResource("/res/images/ic_base_actor.png");
			} else if (type.equals(SceneDocument.SPINE_ACTOR_TYPE)) {
				u = getClass()
						.getResource("/res/images/ic_character_actor.png");			
			} else if (type.equals(SceneDocument.SPRITE3D_ACTOR_TYPE)) {
				u = getClass()
						.getResource("/res/images/ic_character_actor.png");
			}

			return new ImageIcon(u);
		}
	};
}
