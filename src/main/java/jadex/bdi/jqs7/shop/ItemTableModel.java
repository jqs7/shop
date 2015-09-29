package jadex.bdi.jqs7.shop;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import jadex.bdi.jqs7.shop.ItemInfo;

class ItemTableModel extends AbstractTableModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2439360678004884372L;
	protected List list;

	public ItemTableModel(List list) {
		this.list = list;
	}

	public int getRowCount() {
		return list.size();
	}

	public int getColumnCount() {
		return 3;
	}

	public String getColumnName(int column) {
		switch (column) {
		case 0:
			return "Name";
		case 1:
			return "Price";
		case 2:
			return "Quantity";
		default:
			return "";
		}
	}

	public boolean isCellEditable(int row, int column) {
		return false;
	}

	public Object getValueAt(int row, int column) {
		Object value = null;
		ItemInfo ii = (ItemInfo) list.get(row);
		if (column == 0) {
			value = ii.getName();
		} else if (column == 1) {
			value = Double.valueOf(ii.getPrice());
		} else if (column == 2) {
			value = Integer.valueOf(ii.getQuantity());
		}
		return value;
	}
};