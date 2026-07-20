package com.bladecoder.engineeditor.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import com.bladecoder.engineeditor.common.EditorLogger;


public class PropertyChange {
	public final static String DOCUMENT_CHANGED = "DOCUMENT_CHANGED";
	
	protected PropertyChangeSupport propertyChangeSupport;

	public PropertyChange() {
		propertyChangeSupport = new PropertyChangeSupport(this);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
		propertyChangeSupport.addPropertyChangeListener(listener);
	}
	
	public void addPropertyChangeListener(String property, PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(property, listener);
		propertyChangeSupport.addPropertyChangeListener(property, listener);
	}	

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}

	protected void firePropertyChange(String propertyName, Object oldValue,
			Object newValue) {
		
		EditorLogger.debug(propertyName + "-> NEW: "+ newValue + " OLD:" + oldValue);
		propertyChangeSupport.firePropertyChange(propertyName, oldValue,
				newValue);
	}
	
	protected void firePropertyChange(PropertyChangeEvent evt) {
		
		//EditorLogger.debug(propertyName + "-> NEW: "+ newValue + " OLD:" + oldValue);
		propertyChangeSupport.firePropertyChange(evt);
	}
	
	protected void firePropertyChange() {
		propertyChangeSupport.firePropertyChange(DOCUMENT_CHANGED, 1, 0);
	}
	
	protected void firePropertyChange(String property) {
		propertyChangeSupport.firePropertyChange(property, 1, 0);
	}
	
	protected void firePropertyChange(String property, Object value) {
		propertyChangeSupport.firePropertyChange(property, null, value);
	}
}
