package fiji.plugin.trackmate.tracking;

import java.awt.Color;
import java.awt.Component;

import javax.swing.AbstractListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

public class LAPUtils {

	private static final Border RED_BORDER = new LineBorder(Color.RED); 
	
	
	private static class RowHeaderRenderer extends JLabel implements ListCellRenderer {

		private static final long serialVersionUID = 1L;

		RowHeaderRenderer(JTable table) {
			JTableHeader header = table.getTableHeader();
			setOpaque(true);
			setHorizontalAlignment(CENTER);
			setForeground(header.getForeground());
			setBackground(header.getBackground());
			setFont(header.getFont());
		}

		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			setText((value == null) ? "" : value.toString());
			return this;
		}
	}



	/*
	 * STATIC METHODS - UTILS
	 */
	
	
	public static final void echoMatrix(final double[][] m) {
		int nlines = m.length;
		if (nlines == 0) {
			System.out.println("0x0 empty matrix");
			return;
		}
		int nrows = m[0].length;
		double val;
		System.out.print("L\\C\t");
		for (int j = 0; j < nrows; j++) {
			System.out.print(String.format("%7d: ", j));
		}
		System.out.println();
		for (int i = 0; i < nlines; i++) {
			System.out.print(i+":\t");
			for (int j = 0; j < nrows; j++) {
				val = m[i][j];
				if (val > Double.MAX_VALUE/2)
					System.out.print("     B   ");
				else
					System.out.print(String.format("%7.1f  ", val));
			}
			System.out.println();
		}
	}	
	
	public static final void displayCostMatrix(final double[][] costs, final int nSegments, final int nSpots, final double blockingValue, final int[][]solutions) {
		int width = costs.length;
		int height = costs[0].length;
		double val;
		String txt;
		System.out.println(String.format("Displaying table with: Width = %d, Height = %d", width, height));
		TableModel model = new DefaultTableModel(height, width) {
			private static final long serialVersionUID = 1L;

			@Override
			public String getColumnName(int i) {
				if (i < nSegments)
					return "T start "+i;
				else if (i < nSegments + nSpots)
					return "Spot "+(i-nSegments);
				else 
					return "ø";
			}
		};
		
		JTable debugTable = new JTable(model) {
			private static final long serialVersionUID = 1L;

			@Override
			public Component prepareRenderer(TableCellRenderer renderer,
					int row, int col) {
				JLabel label = (JLabel) super.prepareRenderer(renderer, row, col);
				if (col < nSegments) {
					
					if (row < nSegments)
						label.setForeground(Color.BLUE);  // Gap closing
					else if (row < nSegments + nSpots)
						label.setForeground(Color.GREEN.darker()); // Splitting
					else 
						label.setForeground(Color.BLACK); // Initiating
					
				} else if (col < nSegments + nSpots) {
					
					if (row < nSegments)
						label.setForeground(Color.CYAN.darker());  // Merging
					else if (row < nSegments + nSpots)
						label.setForeground(Color.RED.darker()); // Middle block
					else 
						label.setForeground(Color.BLACK); // Initiating
					
				} else {
					if (row < nSegments + nSpots) 
						label.setForeground(Color.BLACK); // Terminating
					else
						label.setForeground(Color.GRAY); // Bottom right block
				}
				label.setHorizontalAlignment(SwingConstants.CENTER);

				label.setBorder(null);
				for (int i = 0; i < solutions.length; i++) {
					int srow = solutions[i][0];
					int scol = solutions[i][1];
					if (row == srow && col == scol)
						label.setBorder(RED_BORDER);
				}
				
				return label;
			}
		};
			
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				val = costs[row][col];
				if (val == blockingValue)
					txt = "B";
				else 
					txt = String.format("%.1f", val);
				model.setValueAt(txt, row, col);
			}
		}

		ListModel lm = new AbstractListModel() {
			private static final long serialVersionUID = 1L;
			String headers[] = new String[2*(nSegments + nSpots)];
			{
				for (int i = 0; i < nSegments; i++)
					headers[i] = "T end "+i;
				for (int i = nSegments; i < nSegments + nSpots; i++)
					headers[i] = "Spot "+(i-nSegments);
				for (int i = nSegments + nSpots; i < headers.length; i++) 
					headers[i] = "ø";
			}
			public int getSize() { return headers.length; }
			public Object getElementAt(int index) {
				return headers[index];
			}
		};
		JList rowHeader = new JList(lm);    
		rowHeader.setFixedCellWidth(50);

		rowHeader.setFixedCellHeight(debugTable.getRowHeight()
				+ debugTable.getRowMargin()
				+ debugTable.getIntercellSpacing().height);
	    rowHeader.setCellRenderer(new RowHeaderRenderer(debugTable));
		
		
		JScrollPane scrollPane = new JScrollPane(debugTable);
		debugTable.setFillsViewportHeight(true);
		scrollPane.setRowHeaderView(rowHeader);
		JFrame frame = new JFrame("Segment cost matrix");
		frame.getContentPane().add(scrollPane);
		frame.setVisible(true);
	}
	
	
	public static void echoSolutions(final int[][] solutions) {
		for (int i = 0; i < solutions.length; i++)
			System.out.println(String.format("%3d: %3d -> %3d", i, solutions[i][0], solutions[i][1]));
	}

	public static void displayLAPresults(int[][] array) {
		Object[][] data = new Object[array.length][array[0].length];
		Object[] headers = new Object[array[0].length];
		for (int i = 0; i < data.length; i++) {
			for (int j = 0; j < data[0].length; j++) {
				data[i][j] = ""+array[i][j];
			}
		}
		for (int i = 0; i < headers.length; i++) {
			headers[i] = ""+i;
		}
		JTable table = new JTable(data, headers);

		JScrollPane scrollPane = new JScrollPane(table);
		table.setFillsViewportHeight(true);
		JFrame frame = new JFrame("Hungarian solution");
		frame.getContentPane().add(scrollPane);
		frame.setVisible(true);
	}

}
