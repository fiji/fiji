package fiji.plugin.trackmate.tracking;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Map;

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

import fiji.plugin.trackmate.Spot;

public class LAPUtils {

	private static final Border RED_BORDER = new LineBorder(Color.RED); 
	
	
	/*
	 * STATIC METHODS - UTILS
	 */
	
	/**
	 * Compute the cost to link two spots, in the default way for the TrackMate plugin.
	 * <p>
	 * This cost is calculated as follow:
	 * <ul>
	 * 	<li> The distance between the two spots <code>D</code> is calculated
	 * 	<li> If the spots are separated by more than the distance cutoff, the cost is
	 * set to be the blocking value. If not,
	 * 	<li> For each feature in the map, a penalty <code>p</code> is calculated as 
	 * <code>p = 3 × α × |f1-f2| / (f1+f2)</code>, where <code>α</code> is the factor 
	 * associated to the feature in the map. This expression is such that:
	 * 	<ul> 
	 * 	<li> there is no penalty if the 2 feature values <code>f1</code> and <code>f2</code> 
	 * are the same;
	 * 	<li> that, with a factor of 1, the penalty if 1 is one value is the double of the other;
	 * 	<li>the penalty is 2 if one is 5 times the other one.
	 * 	</ul>
	 * 	<li> All penalties are summed, to form <code>P = (1 + ∑ p )</code>
	 *  <li> The cost is set to the square of the product: <code>C = ( D × P )²</code>
	 * </ul>
	 * For instance: if 2 spots differ by twice the value in a feature which is 
	 * in the penalty map with a factor of 1, they will <i>look</i> as if they were 
	 * twice as far.
	 */
	public static final double computeLinkingCostFor(final Spot s0, final Spot s1, 
			final double distanceCutOff, final double blockingValue, final Map<String, Double> featurePenalties) {
		double d2 = s0.squareDistanceTo(s1);

		// Distance threshold
		if (d2 > distanceCutOff * distanceCutOff) {
			return blockingValue;
		}

		double penalty = 1;
		for (String feature : featurePenalties.keySet()) {
			double ndiff = s0.normalizeDiffTo(s1, feature);
			if (Double.isNaN(ndiff))
				continue;
			double factor = featurePenalties.get(feature);
			penalty += factor * 1.5 * ndiff;
		}
		
		// Set score
		return d2 * penalty * penalty;
	}
	
	
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
					return "Ã¸";
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
					headers[i] = "Ã¸";
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
