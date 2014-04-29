package org.bladecoder.engineeditor.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BoxLayout;
import javax.swing.JTabbedPane;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.Project;
import org.bladecoder.engineeditor.model.SceneDocument;
import org.bladecoder.engineeditor.ui.components.HeaderPanel;
import org.w3c.dom.Element;

@SuppressWarnings("serial")
public class ActorPanel extends javax.swing.JPanel {

	private HeaderPanel headerPanel;
	private JTabbedPane tabPanel;
	private VerbListPanel verbList;
	private DialogListPanel dialogList;
	private FAListPanel faList;
	private SoundListPanel soundList;
	private ActorPropsPanel props;

	public ActorPanel() {
		headerPanel = new HeaderPanel("ACTOR");
		tabPanel = new JTabbedPane();
		verbList = new VerbListPanel();
		dialogList = new DialogListPanel();
		faList = new FAListPanel();
		props = new ActorPropsPanel();
		soundList = new SoundListPanel();
		props = new ActorPropsPanel();

		BoxLayout bl = new BoxLayout(this, BoxLayout.Y_AXIS);
		setLayout(bl);

		headerPanel.setContentPane(tabPanel);
		add(headerPanel);

		tabPanel.setOpaque(false);

		tabPanel.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

		Ctx.project.addPropertyChangeListener(Project.NOTIFY_ACTOR_SELECTED,
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent e) {
						Element a = (Element) e.getNewValue();
						SceneDocument doc = Ctx.project.getSelectedScene();

						faList.addElements(doc, a, "frame_animation");
						verbList.addElements(doc, a, "verb");
						dialogList.addElements(doc, a, "dialog");
						soundList.addElements(doc, a, "sound");
						props.setActorDocument(doc, a);

						String selTitle = tabPanel.getSelectedIndex() == -1? null: tabPanel.getTitleAt(tabPanel.getSelectedIndex());
						tabPanel.removeAll();

						if (a != null) {

							String type = doc.getType(a);

							if (!type.equals("background"))
								tabPanel.add("Sprites", faList);

							if (!type.equals("foreground")) {
								tabPanel.add("Verbs", verbList);
								tabPanel.add("Sounds", soundList);
							}

							tabPanel.add("Dialogs", dialogList);

							tabPanel.add("Properties", props);
							headerPanel.setTile("ACTOR " + doc.getId(a));

							// select previous tab
							if (selTitle != null) {
								for (int i = 0; i < tabPanel.getTabCount(); i++) {
									if (tabPanel.getTitleAt(i).equals(selTitle)) {
										tabPanel.setSelectedIndex(i);
									}
								}
							}

						} else
							headerPanel.setTile("ACTOR");
					}
				});
	}
}
