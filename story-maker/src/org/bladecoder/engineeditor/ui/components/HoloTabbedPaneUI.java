package org.bladecoder.engineeditor.ui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.text.View;

public class HoloTabbedPaneUI extends BasicTabbedPaneUI {
	    private int anchoFocoH = 15;
//	    private int anchoCarpetas = 18;

	    public static ComponentUI createUI(JComponent c) {
	        return new HoloTabbedPaneUI();
	    }

	    @Override
	    protected void installDefaults() {
	        super.installDefaults();
//	        tabAreaInsets.right = anchoCarpetas;
	    }

	    @Override
	    protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
	        super.paintTabArea(g, tabPlacement, selectedIndex);
	    }

	    @Override
	    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
	    
	    }

	    @Override
	    protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics, int tabIndex, String title, Rectangle textRect, boolean isSelected) {
	        super.paintText(g, tabPlacement, font, metrics, tabIndex, title, textRect, isSelected);
	        g.setFont(font);
	        View v = getTextViewForTab(tabIndex);
	        
	        if (v != null) {
	            // html
	            v.paint(g, textRect);
	        } else {
	            // plain text
	            int mnemIndex = tabPane.getDisplayedMnemonicIndexAt(tabIndex);
	            if (tabPane.isEnabled() && tabPane.isEnabledAt(tabIndex)) {
	                if(isSelected) {           	
	                	g.setColor(Color.WHITE);
	                } else {
	                	g.setColor(tabPane.getForegroundAt(tabIndex));
	                }
	                
	                BasicGraphicsUtils.drawStringUnderlineCharAt(g, title, mnemIndex, textRect.x, textRect.y + metrics.getAscent());
	            } else { // tab disabled
	                g.setColor(Color.BLACK);
	                BasicGraphicsUtils.drawStringUnderlineCharAt(g, title, mnemIndex, textRect.x, textRect.y + metrics.getAscent());
	                g.setColor(tabPane.getBackgroundAt(tabIndex).darker());
	                BasicGraphicsUtils.drawStringUnderlineCharAt(g, title, mnemIndex, textRect.x - 1, textRect.y + metrics.getAscent() - 1);
	            }
	        }
	    }

	    @Override
	    protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
	        return  super.calculateTabWidth(tabPlacement, tabIndex, metrics);
	    }

	    @Override
	    protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
	        if (tabPlacement == LEFT || tabPlacement == RIGHT) {
	            return super.calculateTabHeight(tabPlacement, tabIndex, fontHeight);
	        } else {
	            return anchoFocoH + super.calculateTabHeight(tabPlacement, tabIndex, fontHeight);
	        }
	    }

	    @Override
	    protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
		    
		    if(isSelected) {
		    	Graphics2D g2D = (Graphics2D) g;
		    	g2D.setColor(Theme.HOLO_COLOR);
		    	g2D.setStroke(new BasicStroke(2));
		    	g2D.drawLine(x, y + h - 6, x + w, y + h - 6);
		    }
	    }

	    @Override
	    protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {
	        if (tabPane.hasFocus() && isSelected) {
//	            g.setColor(UIManager.getColor("ScrollBar.thumbShadow"));
//	            g.drawPolygon(shape);
	        }
	    }
}
