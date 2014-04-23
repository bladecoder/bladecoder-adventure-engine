package org.bladecoder.engineeditor.ui.components;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

@SuppressWarnings("serial")
public class HeaderPanel extends JPanel {
    private JComponent center;
    private JPanel north;
    private JLabel titleLbl;
    private boolean collapsable = true;
   
    public HeaderPanel() {
    	this("TITLE");
    }    
    
    public HeaderPanel(String title) {
    	titleLbl = new JLabel(title);
    	north = new JPanel();
    	
    	setLayout(new BorderLayout());
    	north.setLayout(new BoxLayout(north, BoxLayout.PAGE_AXIS));
    	
    	setOpaque(false);
    	
    	titleLbl.setAlignmentX(LEFT_ALIGNMENT);    	
    	titleLbl.setOpaque(false);
    	titleLbl.setFont(Theme.BOLD_FONT);
    	titleLbl.setForeground(Theme.FG_LABEL);
    	titleLbl.setBackground(Theme.BACKGROUND);
    	titleLbl.setBorder(new EmptyBorder(3, 3, 3, 3));
    	
    	setCollapsable(true);   	   	
   	
    	Border compound;
    	Border holo = BorderFactory.createMatteBorder(
                0, 0, 2, 0, Theme.HOLO_COLOR);

    	compound = BorderFactory.createCompoundBorder(new EmptyBorder(3, 0, 3, 0), holo);    	
    	
//    	titleLbl.setBorder(compound);
    	
    	north.setBorder(compound);
    	north.add(titleLbl);
    	add(north, java.awt.BorderLayout.NORTH);
    	
    	if(collapsable)
    		titleLbl.addMouseListener(ml);
    } 
    
    public void fillTitle() {
    	north.setBackground(Theme.HOLO_COLOR);
    	north.setOpaque(true);
    }


	public void setTile(String title) {
    	titleLbl.setText(title);
    }
    
    public void setContentPane(JComponent center) {
    	this.center = center;
    	
    	add(center, java.awt.BorderLayout.CENTER);
    }
    
    public void setCollapsable(boolean c) {
    	collapsable = c;
    	
    	if(c)
    		titleLbl.setIcon(new javax.swing.ImageIcon(getClass().getResource(
				"/res/images/ic_open.png")));
    	else
    		titleLbl.setIcon(null);
    }
    
    
    MouseListener ml = new MouseListener() {
		
		@Override
		public void mouseReleased(MouseEvent arg0) {
			
		}
		
		@Override
		public void mousePressed(MouseEvent arg0) {
			
		}
		
		@Override
		public void mouseExited(MouseEvent arg0) {
			
		}
		
		@Override
		public void mouseEntered(MouseEvent arg0) {
			
		}
		
		@Override
		public void mouseClicked(MouseEvent arg0) {
			if(center!=null && collapsable) {
				if(center.isVisible()) {
					center.setVisible(false);
				
					titleLbl.setIcon(new javax.swing.ImageIcon(getClass().getResource(
						"/res/images/ic_closed.png")));
				} else {
					center.setVisible(true);
					
					titleLbl.setIcon(new javax.swing.ImageIcon(getClass().getResource(
						"/res/images/ic_open.png")));						
				}
			}
		}
	};
}
