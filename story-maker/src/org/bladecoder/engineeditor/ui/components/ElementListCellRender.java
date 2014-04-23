package org.bladecoder.engineeditor.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;

import org.w3c.dom.Element;

@SuppressWarnings("rawtypes")
public abstract class ElementListCellRender implements ListCellRenderer<Element> {
	private final JPanel panel = new JPanel(new BorderLayout());
	private final JPanel txtPanel = new JPanel(new BorderLayout());
	private final JLabel nameLabel = new JLabel();
	private final JLabel infoLabel = new JLabel();
	private final JLabel imgPanel = new JLabel();
	
	private final boolean hasImg;

	public ElementListCellRender(boolean hasImg) {
		this.hasImg = hasImg;
		txtPanel.setOpaque(false);
		txtPanel.add(nameLabel, BorderLayout.CENTER);
		txtPanel.add(infoLabel, BorderLayout.SOUTH);

		panel.setBorder(new EmptyBorder(5, 5, 5, 10));
		panel.add(txtPanel, BorderLayout.CENTER);
		
		if(hasImg) {
			panel.add(imgPanel, BorderLayout.WEST);
//			imgPanel.setPreferredSize(new Dimension(35, 35));
			imgPanel.setBorder(new EmptyBorder(0, 0, 0, 5));
		}
		
		panel.setBackground(Color.DARK_GRAY);
		infoLabel.setForeground(Color.LIGHT_GRAY);

		Font font = nameLabel.getFont();
		nameLabel.setFont(new Font(font.getName(), Font.BOLD, font.getSize()));

		font = infoLabel.getFont();
		infoLabel.setFont(new Font(font.getName(), font.getStyle(), 10));
	}

	@Override
	public Component getListCellRendererComponent(JList list, Element value, int index,
			boolean isSelected, boolean cellHasFocus) {
		
		nameLabel.setText(getName(value));
		infoLabel.setText(getInfo(value));
		
		if(hasImg) {
			ImageIcon img = getImageIcon(value);
			
			imgPanel.setIcon(img);
		}
		
		panel.setOpaque(isSelected);
		return panel;
	}
	
	public abstract String getName(Element e);
	public abstract String getInfo(Element e);
	public abstract ImageIcon getImageIcon(Element e);
}
