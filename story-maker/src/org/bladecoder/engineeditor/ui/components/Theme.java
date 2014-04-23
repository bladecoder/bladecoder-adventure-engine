package org.bladecoder.engineeditor.ui.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.util.ArrayList;

import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;


public class Theme {
	
	public static final Color BACKGROUND = Color.BLACK;
	public static final Color LIST_BACKGROUND = Color.BLACK;
	public static final Color FG_LABEL = Color.WHITE;
	public static final Color FG_DISABLED_LABEL = Color.GRAY;
	public static final Color BUTTON_COLOR = Color.WHITE;
	public static final Font FONT = new Font(Font.DIALOG, Font.PLAIN, 12);
	public static final Font FONT_SMALL = new Font(Font.DIALOG, Font.PLAIN, 8);
    public static final Font BOLD_FONT = new Font(Font.DIALOG, Font.BOLD, 12);
	public static final Font BOLD_FONT_BIG = new Font(Font.DIALOG, Font.BOLD, 14);
//	public static final Color TITLE_BACKGROUND = new Color(0xef,0x8a,0x00);
	public static final Color HOLO_COLOR = new Color(51, 181, 229);
    
    public static void setTheme() {
    	Border line = new LineBorder(Theme.BUTTON_COLOR);
    	Border margin = new EmptyBorder(5, 5, 5, 5);
    	Border zeroBorder = new EmptyBorder(0, 0, 0, 0);
    	Border compound = new CompoundBorder(line, margin);
    	
    	UIManager.put("Label.foreground", FG_LABEL);
    	UIManager.put("Label.background", BACKGROUND);
//    	UIManager.put("Label.border", compound);
    	UIManager.put("Label.font", FONT);
    	
    	
    	UIManager.put("Button.foreground", BUTTON_COLOR);
    	UIManager.put("Button.background", BACKGROUND);
    	UIManager.put("Button.border", margin);
    	UIManager.put("Button.font", BOLD_FONT);
    	UIManager.put("Button.select", HOLO_COLOR);
    	
    	UIManager.put("TextField.font", FONT);
    	UIManager.put("TextField.foreground", FG_LABEL);
    	UIManager.put("TextField.background", BACKGROUND);
    	UIManager.put("TextField.border", compound);
    	UIManager.put("TextField.caretForeground", FG_LABEL);
    	
    	UIManager.put("ComboBox.font", FONT);
    	UIManager.put("ComboBox.foreground", FG_LABEL);
    	UIManager.put("ComboBox.background", BACKGROUND);
//    	UIManager.put("ComboBox.border", compound);
    	UIManager.put("ComboBox.caretForeground", FG_LABEL);
    	
    	UIManager.put("List.font", FONT);
    	UIManager.put("List.foreground", FG_LABEL);
    	UIManager.put("List.background", LIST_BACKGROUND);
//    	UIManager.put("List.selectionBackground", Color.GRAY);
//    	UIManager.put("List.selectionForeground", Color.GRAY);
    	UIManager.put("List.border", zeroBorder);
    	
    	UIManager.put("TabbedPane.font", FONT);
    	UIManager.put("TabbedPane.foreground", FG_DISABLED_LABEL);
    	UIManager.put("TabbedPane.background", LIST_BACKGROUND);
//    	UIManager.put("TabbedPane.selected", LIST_BACKGROUND);
//    	UIManager.put("TabbedPane.selectHighlight", FG_LABEL);
//    	UIManager.put("TabbedPane.highlight", FG_LABEL);
    	UIManager.put("TabbedPane.contentOpaque", false);
//    	UIManager.put("TabbedPane.tabsOpaque", false);
//    	UIManager.put("TabbedPane.borderHightlightColor", BACKGROUND);
//    	UIManager.put("TabbedPane.shadow", BACKGROUND);
    	UIManager.put("TabbedPane.contentAreaColor", BACKGROUND);
    	UIManager.put("TabbedPane.contentBorderInsets", new Insets(0, 0, 0, 0));
//    	UIManager.put("TabbedPane.shadow", BACKGROUND);
    	UIManager.put("TabbedPaneUI", HoloTabbedPaneUI.class.getName());    	
//    	UIManager.put("TabbedPane.border", compound);    
    	
    	UIManager.put("ToolBar.font", FONT);
    	UIManager.put("ToolBar.foreground", FG_LABEL);
    	UIManager.put("ToolBar.background", LIST_BACKGROUND);
    	UIManager.put("ToolBar.border", margin);
    	
    	UIManager.put("ScrollPane.border", zeroBorder);
    	UIManager.put("ScrollPane.background", BACKGROUND);
    	
    	UIManager.put("ScrollBar.background", BACKGROUND);
    	UIManager.put("ScrollBar.foreground", HOLO_COLOR);
    	
    	UIManager.put("OptionPane.font", FONT);
    	UIManager.put("OptionPane.foreground", FG_LABEL);
    	UIManager.put("OptionPane.messageForeground", FG_LABEL);
    	UIManager.put("OptionPane.background", LIST_BACKGROUND);	
    	
    	ArrayList<Object> gradients = new ArrayList<Object>();
    	gradients.add(0.0);
    	gradients.add(0.0);
        gradients.add(Color.DARK_GRAY);
        gradients.add(Color.DARK_GRAY);
        gradients.add(Color.DARK_GRAY);
        
    	UIManager.put("ScrollBar.gradient", gradients);
    	UIManager.put("ScrollBar.shadow", BACKGROUND);
    	UIManager.put("ScrollBar.hightlight", BACKGROUND);
//    	UIManager.put("ScrollBar.track", Color.GREEN);
//    	UIManager.put("ScrollBar.trackHighlight", Color.RED);
//    	UIManager.put("ScrollBar.thumb", Color.ORANGE);
    	UIManager.put("ScrollBar.thumbHighlight", Color.DARK_GRAY);
    	UIManager.put("ScrollBar.thumbShadow", Color.DARK_GRAY);
    	UIManager.put("ScrollBar.thumbDarkShadow", Color.DARK_GRAY);
    	
    	UIManager.put("Tree.background", BACKGROUND);
    	UIManager.put("Tree.foreground", FG_LABEL);
    	
    	UIManager.put("Table.background", BACKGROUND);
    	UIManager.put("Table.foreground", FG_LABEL);
    	
    	UIManager.put("Panel.background", BACKGROUND);
    }

}
