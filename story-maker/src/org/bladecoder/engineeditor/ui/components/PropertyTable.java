package org.bladecoder.engineeditor.ui.components;

import java.util.ArrayList;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

@SuppressWarnings("serial")
public class PropertyTable extends JTable {
	
	public static class Property {
		String name;
		String value;
		Types type;
	}
	
	public enum Types {INTEGER, BOOLEAN, FLOAT, STRING};	

	public PropertyTable() {
		// Set the data model for this table
		setModel(new PropertyTableModel());

		// Tweak the appearance of the table by manipulating its column model
		TableColumnModel colmodel = getColumnModel();

		// Set column widths
		colmodel.getColumn(0).setPreferredWidth(200);
		colmodel.getColumn(1).setPreferredWidth(125);

		// Right justify the text in the second column
		TableColumn namecol = colmodel.getColumn(1);
		DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
		renderer.setHorizontalAlignment(SwingConstants.RIGHT);
		namecol.setCellRenderer(renderer);
	}

	public static class PropertyTableModel extends AbstractTableModel {
		ArrayList<Property> properties; // The properties to display

		public PropertyTableModel() {
			properties = new ArrayList<Property>();

		}
		
		public void addProperty(String name, String value, Types type) {
			Property p = new Property();
			p.name= name;
			p.value = value;
			p.type = type;
			
			properties.add(p);
			
//			fireTableDataChanged();
		}
		
		public void addProperty(String name, int value) {
			addProperty(name, Integer.toString(value), Types.INTEGER);
		}
		
		public void addProperty(String name, float value) {
			addProperty(name, Float.toString(value), Types.FLOAT);
		}
		
		public void addProperty(String name, String value) {
			addProperty(name,value, Types.STRING);
		}
		
		public void addProperty(String name, boolean value) {
			addProperty(name, Boolean.toString(value), Types.BOOLEAN);		
		}
		
		public void sort() {
//			Arrays.sort(properties, new Comparator() {
//				public int compare(Object p, Object q) {
//					PropertyDescriptor a = (PropertyDescriptor) p;
//					PropertyDescriptor b = (PropertyDescriptor) q;
//					return a.getName().compareToIgnoreCase(b.getName());
//				}
//
//				public boolean equals(Object o) {
//					return o == this;
//				}
//			});			
		}

		// These are the names of the columns represented by this TableModel
		static final String[] columnNames = new String[] { "Name", "Value"};

		// These are the types of the columns represented by this TableModel
		static final Class<?>[] columnTypes = new Class[] { String.class,
				String.class};

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public int getRowCount() {
			return properties.size();
		}

		@Override
		public String getColumnName(int column) {
			return columnNames[column];
		}

		@Override
		public Class<?> getColumnClass(int column) {
			return columnTypes[column];
		}
		
		@Override
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

		@Override
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

		@Override
		public void setValueAt(Object aValue,
                int rowIndex,
                int columnIndex) {
			Property prop = properties.get(rowIndex);
			
			switch (columnIndex) {
			case 0:
				break;
			case 1:
				String value =  aValue.toString();
				
				// INPUT VALUE VALIDATION				
				try {
				switch(prop.type) {
				case BOOLEAN:
					if(!value.equalsIgnoreCase("true") &&!value.equalsIgnoreCase("false")) return;
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
				} catch(Exception e) {
					return;
				}
								
				prop.value = value;
				fireTableCellUpdated(rowIndex, columnIndex);
				break;
			default:
			}						
		}

		public void clear() {
			properties.clear();
		}
		
	}	
}
