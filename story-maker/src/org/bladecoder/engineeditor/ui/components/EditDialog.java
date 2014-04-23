package org.bladecoder.engineeditor.ui.components;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import org.bladecoder.engineeditor.utils.ImageUtils;


@SuppressWarnings("serial")
public abstract class EditDialog extends JDialog {
	
    private JButton createBtn;
    private JButton cancelBtn;
    
    private javax.swing.JLabel infoLbl;
    
    protected javax.swing.JPanel centerPanel;
    protected ToolbarHeaderPanel headerPanel;
    
    boolean cancelled = false;

    public EditDialog(java.awt.Frame parent) {
        super(parent, true);
        
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        setUndecorated(true);
        

        //        setLocation(parent.getLocation());
         
        infoLbl = new javax.swing.JLabel();
        infoLbl.setForeground(Theme.FG_LABEL);
        infoLbl.setFont(Theme.FONT);
        infoLbl.setBorder(new EmptyBorder(10, 10, 10, 10));
        infoLbl.setPreferredSize(new Dimension(300,100));
        
        headerPanel = new ToolbarHeaderPanel();
        headerPanel.add(infoLbl, java.awt.BorderLayout.WEST);
        headerPanel.setCollapsable(false);
        headerPanel.fillTitle();
        
        centerPanel = new javax.swing.JPanel();      
        centerPanel.setOpaque(true);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));       
        centerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        createBtn = new JButton();
        cancelBtn = new JButton();
        createBtn.setText("OK");
        cancelBtn.setText("Cancel");
        createBtn.setOpaque(false);
        cancelBtn.setOpaque(false);
        
        headerPanel.add(new JScrollPane(centerPanel), java.awt.BorderLayout.CENTER);
        headerPanel.addButton(cancelBtn);
        headerPanel.addButton(createBtn);
        
		createBtn.addActionListener(new ActionListener() {@Override public void actionPerformed(ActionEvent e) {if(validateFields()) ok();}});
		cancelBtn.addActionListener(new ActionListener() {@Override public void actionPerformed(ActionEvent e) {cancelled = true; dispose();}});      
        
		getContentPane().add(headerPanel);
		
//		okcancelPanel.setBorder(BorderFactory.createLineBorder(Theme.HOLO_COLOR, 2));
		((JComponent) getContentPane()).setBorder(BorderFactory.createLineBorder(Theme.HOLO_COLOR, 2));
		
		
		// CLOSE THE DIALOG WHEN PRESSING ENTER
		getRootPane().setDefaultButton(createBtn);
		
    }
    
    protected void init(java.awt.Frame parent) {
    	int height = (int)getContentPane().getPreferredSize().getHeight();
    	
    	if(height > parent.getHeight()) height = parent.getHeight() - 50 ;
    	
    	getContentPane().setPreferredSize(new Dimension(600,height));
    	infoLbl.setPreferredSize(new Dimension(300,(int)infoLbl.getPreferredSize().getHeight()));
//        centerPanel.setPreferredSize(new Dimension(250,(int)centerPanel.getPreferredSize().getHeight()));
    	pack();
		center(parent);    	
    }
    
    protected void center(java.awt.Frame parent) {
    	if(parent == null) return;
    	
    	Point l = parent.getLocationOnScreen();
    	
    	int w = getWidth();
		int h = getHeight();
    	
    	setLocation(l.x + (parent.getWidth() - w)/ 2, l.y + (parent.getHeight() - h)/ 2);
    }
    
    public void setInfo(String text) {
    	if(text.startsWith("<html")) {
        	infoLbl.setText(text);
    	} else {
    		infoLbl.setText("<html>" + text + "</html>");
    	}
    }
    
    public void setInfoIcon(URL u) {
    	ImageIcon icon = null;
    	
    	try {
			icon = ImageUtils.getImageIcon(u, 300);
		} catch (IOException e) {
		}
    	
    	if(icon != null)
    		infoLbl.setIcon(icon);
    }
    
    public void setInfoComponent(Component c) {
    	headerPanel.remove(infoLbl);
    	headerPanel.add(c, java.awt.BorderLayout.WEST);
    	c.setPreferredSize(new Dimension(300,(int)getContentPane().getPreferredSize().getHeight()));
    }
    
    public void setTitle(String title) {
        super.setTitle(title);
        headerPanel.setTile(title);
    }
    
    public boolean isCancel() {
    	return cancelled;
    }
    
    abstract protected boolean validateFields();

	abstract protected void ok();
}
