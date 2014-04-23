package org.bladecoder.engineeditor.ui.components;

import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JToolBar;

@SuppressWarnings("serial")
public class EditToolbar extends JToolBar {
	
    private JButton createBtn;
    private JButton deleteBtn;
    private JButton editBtn;
    private JButton copyBtn;	
    private JButton pasteBtn;		
	
	public EditToolbar() {
		super();
		
        setFloatable(false);
        setRollover(true);
		
        createBtn = new JButton();
        editBtn = new JButton();
        deleteBtn = new JButton();
        copyBtn = new JButton();
        pasteBtn = new JButton();
		
        addToolBarButton(createBtn, "/res/images/ic_add.png","New", "Create a new Element");
        addToolBarButton(editBtn, "/res/images/ic_edit.png","Edit", "Edit the selected Element");
        addToolBarButton(deleteBtn, "/res/images/ic_delete.png","Delete", "Delete and put in the clipboard"); 
        addToolBarButton(copyBtn, "/res/images/ic_copy.png","Copy", "Copy to the clipboard");
        addToolBarButton(pasteBtn, "/res/images/ic_paste.png","Paste", "Paste from the clipboard");
    }
	
	public void hideCopyPaste() {
		copyBtn.setVisible(false);
		pasteBtn.setVisible(false);
	}
    
	public void enableCreate(boolean enable) {
		createBtn.setEnabled(enable);
	}
	
	public void enableEdit(boolean enable) {
		deleteBtn.setEnabled(enable);
		editBtn.setEnabled(enable);
		copyBtn.setEnabled(enable);
	}
	
	public void enablePaste(boolean enable) {
		pasteBtn.setEnabled(enable);
	}
	
	public void addToolBarButton(JButton button, String icon, String text, String tooltip) {
		String disabledIcon = icon.substring(0,icon.indexOf(".")) + "_disabled.png";
		
		button.setIcon(new javax.swing.ImageIcon(getClass().getResource(icon)));
		
		URL disURL = getClass().getResource(disabledIcon);
		if(disURL != null)
			button.setDisabledIcon(new javax.swing.ImageIcon(disURL));
		//button.setText(text);
		button.setToolTipText(tooltip);
		button.setFocusable(false);
		//button.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
		button.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        add(button);
        button.setEnabled(false);
	}  	
	
	public void addCreateActionListener(ActionListener actionListener) {
		createBtn.addActionListener(actionListener);
	}
	
	public void addEditActionListener(ActionListener actionListener) {
		editBtn.addActionListener(actionListener);
	}
	
	public void addDeleteActionListener(ActionListener actionListener) {
		deleteBtn.addActionListener(actionListener);
	}
	
	public void addCopyActionListener(ActionListener actionListener) {
		copyBtn.addActionListener(actionListener);
	}
	
	public void addPasteActionListener(ActionListener actionListener) {
		pasteBtn.addActionListener(actionListener);
	}
}
