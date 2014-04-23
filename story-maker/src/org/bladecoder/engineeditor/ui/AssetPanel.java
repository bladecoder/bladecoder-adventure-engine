package org.bladecoder.engineeditor.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BoxLayout;
import javax.swing.JTabbedPane;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.Project;
import org.bladecoder.engineeditor.ui.components.HeaderPanel;

@SuppressWarnings("serial")
public class AssetPanel extends javax.swing.JPanel {
	
	public AssetPanel() {
		initComponents();

		Ctx.project.addPropertyChangeListener(Project.NOTIFY_PROJECT_LOADED, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				
				headerPanel.setTile("ASSETS " + Ctx.project.getProjectDir().getName());
			}
		});		
	}

	private void initComponents() {

		headerPanel = new HeaderPanel("ASSETS");

		tabPanel = new JTabbedPane();
		
		BoxLayout bl = new BoxLayout(this, BoxLayout.Y_AXIS);
		setLayout(bl);

		
		headerPanel.setContentPane(tabPanel);
		add(headerPanel);
		
		tabPanel.setOpaque(false);
		tabPanel.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		add(tabPanel);
		
		tabPanel.add("Assets", new AssetsListPanel());
		tabPanel.add("Resolutions", new ResolutionListPanel());
	}

	private HeaderPanel headerPanel;
	private JTabbedPane tabPanel;
}
