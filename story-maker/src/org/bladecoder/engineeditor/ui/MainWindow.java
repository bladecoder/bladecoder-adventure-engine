package org.bladecoder.engineeditor.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.xml.parsers.ParserConfigurationException;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.glcanvas.ScnCanvas;
import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.model.Project;
import org.bladecoder.engineeditor.ui.components.Theme;
import org.bladecoder.engineeditor.utils.DesktopUtils;
import org.bladecoder.engineeditor.utils.EditorLogger;
import org.xml.sax.SAXException;

import com.badlogic.gdx.backends.lwjgl.LwjglAWTCanvas;


@SuppressWarnings("serial")
public class MainWindow extends JFrame implements PropertyChangeListener {
	public static final String VERSION_STR = "0.1.0 (Beta)";
	public static final String COPYRIGHT_STR = "<html><p align=\"left\">2013 - Rafael García<br/>http://bladecoder.blogspot.com</p></html>";
	public static final String TITLE = "Story Maker";
	

    private javax.swing.JPanel leftPanel;
    private javax.swing.JPanel rightPanel;
    
    private ProjectToolbar projectToolbar;
    private ProjectPanel projectPanel;
    private AssetPanel assetPanel;
    private ScenePanel scenePanel;
    private ActorPanel actorPanel;
    
    private LwjglAWTCanvas glCanvas;
    
    private javax.swing.JLabel labelLogo;
    private javax.swing.JPanel creditsPanel;
    private javax.swing.JLabel websiteLbl;
    private javax.swing.JLabel versionLabel;
    
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if(evt.getPropertyName().equals(BaseDocument.NOTIFY_DOCUMENT_SAVED))
			setTitle(TITLE);
		else
			setTitle(TITLE + " *");
	}
	
	public MainWindow() {
		setIconImage(Toolkit.getDefaultToolkit().getImage(MainWindow.class.getResource("/res/images/ic_app.png")));
		
		initComponents();
		
		Ctx.project.getWorld().addPropertyChangeListener(this);

		websiteLbl.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				DesktopUtils.browse(MainWindow.this, "http://bladecoder.blogspot.com");
			}
		});

		addWindowListener(new WindowAdapter() {
			@Override public void windowOpened(WindowEvent e) {
//				DO SOMETHING WHEN OPENS?
			}
			
			@Override public void windowClosed(WindowEvent e) {
					projectToolbar.exit();
				}
		});
		
		
		
		String lastProject = Ctx.project.getConfig().getProperty(Project.LAST_PROJECT_PROP, "");
		
		if(!lastProject.isEmpty()&& new File(lastProject).exists()) {
			try {
				Ctx.project.loadProject(new File(lastProject));
			} catch (IOException | ParserConfigurationException | SAXException e) {
				EditorLogger.debug("Last project not found: " + lastProject + " " + e.getMessage());
				Ctx.project.closeProyect();
			} catch (Exception e) {
				Ctx.project.closeProyect();
			}
		}
    }
	
	public ScnCanvas getScnCanvas() {
		return (ScnCanvas)glCanvas.getApplicationListener();
	}

    private void initComponents() {

        projectToolbar = new ProjectToolbar();
    	leftPanel = new JPanel();
        rightPanel = new JPanel();
        creditsPanel = new JPanel();
        labelLogo = new JLabel();
        websiteLbl = new JLabel();
        projectPanel = new ProjectPanel();
        assetPanel = new AssetPanel();
        scenePanel = new ScenePanel();
		versionLabel = new JLabel();
		glCanvas = new LwjglAWTCanvas(new ScnCanvas());
		actorPanel = new ActorPanel();
		

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(TITLE);
        
        labelLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
//        jLabelLogo.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        labelLogo.setIcon(new ImageIcon(MainWindow.class.getResource("/res/images/title.png")));
		versionLabel.setText(VERSION_STR);
        websiteLbl.setText(COPYRIGHT_STR);
        versionLabel.setFont(Theme.FONT_SMALL);
        websiteLbl.setFont(Theme.FONT_SMALL);

        creditsPanel.setOpaque(false);
        creditsPanel.setLayout(new BoxLayout(creditsPanel, BoxLayout.X_AXIS));
        creditsPanel.add(websiteLbl);
        creditsPanel.add(versionLabel);

        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        leftPanel.add(labelLogo);
        leftPanel.add(Box.createVerticalStrut(20));
        leftPanel.add(projectToolbar);
        leftPanel.add(Box.createVerticalStrut(20));        
        leftPanel.add(projectPanel);
        leftPanel.add(Box.createVerticalStrut(20));        
        leftPanel.add(assetPanel);
        
        leftPanel.add(creditsPanel);

        
        leftPanel.setPreferredSize(new Dimension(300, 200));
        leftPanel.setBorder(new EmptyBorder(0, 10, 10, 10));
        
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);
        
        rightPanel.add(scenePanel);
        rightPanel.add(Box.createVerticalStrut(20));
        rightPanel.add(actorPanel);  
        
        rightPanel.setPreferredSize(new Dimension(300, 200));
        rightPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        setLayout(new BorderLayout());
        add(leftPanel, BorderLayout.LINE_START);
        add(glCanvas.getCanvas(),BorderLayout.CENTER);
        add(rightPanel, BorderLayout.LINE_END);
        pack();
    }
}
