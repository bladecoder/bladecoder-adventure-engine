package org.bladecoder.engineeditor.ui;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.Project;
import org.bladecoder.engineeditor.ui.components.PropertyTable;
import org.bladecoder.engineeditor.ui.components.PropertyTable.PropertyTableModel;

@SuppressWarnings("serial")
public class WorldPropsPanel extends JPanel {

	public static final String WIDTH_PROP = "width";
	public static final String HEIGHT_PROP = "height";
	
	public static final String TITLE_PROP = "title";
	
	TableModelListener tableModelListener = new TableModelListener() {
		@Override
		public void tableChanged(TableModelEvent e) {
			if (e.getType() == TableModelEvent.UPDATE) {
				int row = e.getFirstRow();
				updateModel((String) propertyTable.getModel().getValueAt(row, 0),
						(String) propertyTable.getModel().getValueAt(row, 1));
			}
		}
	};

	public WorldPropsPanel() {

		initComponents();

		Ctx.project.addPropertyChangeListener(Project.NOTIFY_PROJECT_LOADED,
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent arg0) {
						setProject();
					}
				});
	}

	private void updateModel(String property, String value) {
		if (property.equals(WIDTH_PROP)) {
			Ctx.project.getWorld().setWidth(value);
		} else if (property.equals(TITLE_PROP)) {
			Ctx.project.setProjectProperty(TITLE_PROP, value);
		} else if (property.equals(HEIGHT_PROP)) {
			Ctx.project.getWorld().setHeight(value);
		}
	}

	private void setProject() {
		PropertyTableModel model = (PropertyTableModel) propertyTable.getModel();
		model.removeTableModelListener(tableModelListener);
		model.clear();
		model.addProperty(WIDTH_PROP, Ctx.project.getWorld().getWidth());
		model.addProperty(HEIGHT_PROP, Ctx.project.getWorld().getHeight());
		
		model.addProperty(TITLE_PROP, Ctx.project.getProjectProperty(TITLE_PROP));
		
		model.addTableModelListener(tableModelListener);	
	}

	private void initComponents() {
		propertyTable = new PropertyTable();
		setLayout(new BorderLayout(0, 0));
		add(propertyTable, java.awt.BorderLayout.CENTER);
	}

	private PropertyTable propertyTable;
}
