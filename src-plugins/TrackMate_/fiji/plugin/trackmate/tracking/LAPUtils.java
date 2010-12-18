package fiji.plugin.trackmate.tracking;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

public class LAPUtils {

	private static final Border RED_BORDER = new LineBorder(Color.RED); 
	
	
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
	
	/**
	 * Display the cost matrix solved by the Hungarian algorithm in the LAP approach.
	 * @param costs  the cost matrix
	 * @param nSegments  the number of track segments found in the first step of the LAP tracking
	 * @param nSpots  the number of middle spots to consider 
	 * @param blockingValue  the blocking value for cost 
	 * @param solutions  the Hungarian assignment couple
	 */
	public static final void displayCostMatrix(final double[][] costs, final int nSegments, final int nSpots, final double blockingValue, final int[][]solutions) {
		int width = costs.length;
		int height = costs[0].length;
		double val;
		String txt;
		System.out.println(String.format("Displaying table with: Width = %d, Height = %d", width, height));
		
		// Set column header
		TableModel model = new DefaultTableModel(height, width) {
			private static final long serialVersionUID = 1L;

			@Override
			public String getColumnName(int i) {
				if (i < nSegments)
					return "Ts "+i;
				else if (i < nSegments + nSpots)
					return "Sp "+(i-nSegments);
				else 
					return "ø";
			}
		};
		
		// Create table with specific coloring
		JTable debugTable = new JTable(model) {
			private static final long serialVersionUID = 1L;

			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
				
				JLabel label = (JLabel) super.prepareRenderer(renderer, row, col);
				// Change font color according to matrix parts
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

				// Change border color according to Hungarian solution
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
	
		
		// Set values
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

		// Row headers
		TableModel rhm = new AbstractTableModel() {
			private static final long serialVersionUID = 1L;
			String headers[] = new String[2*(nSegments + nSpots)];
			{
				for (int i = 0; i < nSegments; i++)
					headers[i] = "Te "+i;
				for (int i = nSegments; i < nSegments + nSpots; i++)
					headers[i] = "Sp "+(i-nSegments);
				for (int i = nSegments + nSpots; i < headers.length; i++) 
					headers[i] = "ø";
			}
			
			public int getColumnCount() {return 1;	}
			public int getRowCount() { return headers.length; }
			public Object getValueAt(int rowIndex, int columnIndex) {
				return headers[rowIndex];
			}
		};
		JTable rowHeader = new JTable(rhm);
		Dimension d = rowHeader.getPreferredScrollableViewportSize();
		d.width = rowHeader.getPreferredSize().width;
		rowHeader.setPreferredScrollableViewportSize(d);
		
		// Set column width
		debugTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		for (int i = 0; i < debugTable.getColumnCount(); i++) {
			debugTable.getColumnModel().getColumn(i).setPreferredWidth(50);
		}
		
		// Embed table in scroll pane
		JScrollPane scrollPane = new JScrollPane(debugTable);
		debugTable.setFillsViewportHeight(true);
		scrollPane.setRowHeaderView(rowHeader);
		JFrame frame = new JFrame("Segment cost matrix");
		frame.getContentPane().add(scrollPane);
		frame.setSize(800, 600);
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
