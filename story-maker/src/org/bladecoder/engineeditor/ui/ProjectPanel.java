package org.bladecoder.engineeditor.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BoxLayout;
import javax.swing.JTabbedPane;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.Project;
import org.bladecoder.engineeditor.model.WorldDocument;
import org.bladecoder.engineeditor.ui.components.HeaderPanel;

@SuppressWarnings("serial")
public class ProjectPanel extends javax.swing.JPanel {

	private HeaderPanel headerPanel;
	private JTabbedPane tabPanel;
	private SceneListPanel sceneList;
	private VerbListPanel verbList;
	private ChapterListPanel chapterList;
	
	
	public ProjectPanel() {
		headerPanel = new HeaderPanel("ADVENTURE");

		tabPanel = new JTabbedPane();
		sceneList = new SceneListPanel();
		verbList = new VerbListPanel();
		chapterList = new ChapterListPanel();
		
		BoxLayout bl = new BoxLayout(this, BoxLayout.Y_AXIS);
		setLayout(bl);

		
		headerPanel.setContentPane(tabPanel);
		add(headerPanel);
		
		tabPanel.setOpaque(false);
		tabPanel.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		add(tabPanel);
		
		tabPanel.add("Scenes", sceneList);
		tabPanel.add("Chapters", chapterList);
		tabPanel.add("Def. Verbs", verbList);
		tabPanel.add("Properties", new WorldPropsPanel());


		Ctx.project.addPropertyChangeListener(Project.NOTIFY_PROJECT_LOADED, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				WorldDocument w = Ctx.project.getWorld();
				
				verbList.addElements(w, w.getElement(), "verb");
				chapterList.addElements(w, w.getElement(), "chapter");
				headerPanel.setTile("ADVENTURE " + Ctx.project.getTitle());
			}
		});		
	}
}
