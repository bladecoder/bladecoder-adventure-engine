package org.bladecoder.engineeditor.ui.components;

import java.util.ArrayList;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

public class PropertyTable extends Table {
	ArrayList<Property> properties; // The properties to display
	Skin skin;
	
	// These are the names of the columns represented by this TableModel
	static final String[] columnNames = new String[] { "Name", "Value" };

	public static class Property {
		String name;
		String value;
		Types type;
	}

	public enum Types {
		INTEGER, BOOLEAN, FLOAT, STRING
	}

	public PropertyTable(Skin skin) {
		super(skin);
		this.skin = skin;
		properties = new ArrayList<Property>();
		top().left();
		
		add(new Label(columnNames[0], skin));
		add(new Label( columnNames[1], skin));
	}

	public void addProperty(String name, String value, Types type) {
		Property p = new Property();
		p.name = name;
		p.value = value;
		p.type = type;

		properties.add(p);
		
		row();
		add(new Label(p.name, skin)).expandX().left();
		add(new TextField( p.value, skin)).expandX().left();
	}

	public void addProperty(String name, int value) {
		addProperty(name, Integer.toString(value), Types.INTEGER);
	}

	public void addProperty(String name, float value) {
		addProperty(name, Float.toString(value), Types.FLOAT);
	}

	public void addProperty(String name, String value) {
		addProperty(name, value, Types.STRING);
	}

	public void addProperty(String name, boolean value) {
		addProperty(name, Boolean.toString(value), Types.BOOLEAN);
	}

	public void sort() {
		// Arrays.sort(properties, new Comparator() {
		// public int compare(Object p, Object q) {
		// PropertyDescriptor a = (PropertyDescriptor) p;
		// PropertyDescriptor b = (PropertyDescriptor) q;
		// return a.getName().compareToIgnoreCase(b.getName());
		// }
		//
		// public boolean equals(Object o) {
		// return o == this;
		// }
		// });
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public int getRowCount() {
		return properties.size();
	}

	public String getColumnName(int column) {
		return columnNames[column];
	}

	public boolean isCellEditable(int row, int col) {
		switch (col) {
		case 0:
			return false;
		case 1:
			return true;
		default:
			return false;
		}
	}

	public Object getValueAt(int row, int column) {
			Property prop = properties.get(row);
			
			switch (column) {
			case 0:
				return prop.name;
			case 1:
				return prop.value;
			default:
				return null;
			}
	}

	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		Property prop = properties.get(rowIndex);

		switch (columnIndex) {
		case 0:
			break;
		case 1:
			String value = aValue.toString();

			// INPUT VALUE VALIDATION
			try {
				switch (prop.type) {
				case BOOLEAN:
					if (!value.equalsIgnoreCase("true")
							&& !value.equalsIgnoreCase("false"))
						return;
					break;
				case FLOAT:
					Float.parseFloat(value);
					break;
				case INTEGER:
					Integer.parseInt(value);
					break;
				case STRING:
					break;
				default:
					break;
				}
			} catch (Exception e) {
				return;
			}

			prop.value = value;
//			fireTableCellUpdated(rowIndex, columnIndex);
			break;
		default:
		}
	}

	public void clear() {
		properties.clear();
		super.clear();
	}

}
