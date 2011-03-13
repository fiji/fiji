package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.gui.TrackMateFrame.FONT;
import static fiji.plugin.trackmate.gui.TrackMateFrame.SMALL_FONT;
import ij.ImagePlus;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;


import org.jgrapht.Graph;
import org.jgrapht.event.GraphEdgeChangeEvent;
import org.jgrapht.event.GraphListener;
import org.jgrapht.event.GraphVertexChangeEvent;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.ext.JGraphModelAdapter.CellFactory;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.ListenableUndirectedWeightedGraph;
import org.jgrapht.graph.SimpleWeightedGraph;

import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.layout.mxEdgeLabelLayout;
import com.mxgraph.layout.mxGraphLayout;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.layout.orthogonal.mxOrthogonalLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.view.mxInteractiveCanvas;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxGraphView;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.visualization.SpotCollectionEditEvent;
import fiji.plugin.trackmate.visualization.SpotCollectionEditListener;
import fiji.plugin.trackmate.visualization.trackscheme.SpotCellViewFactory.SpotCell;
import fiji.plugin.trackmate.visualization.trackscheme.SpotCellViewFactory.TrackEdgeCell;

public class TrackSchemeFrame extends JFrame implements SpotCollectionEditListener {

	{
		//Set Look & Feel
		try {
			javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * CONSTANTS
	 */

	static final int Y_COLUMN_SIZE = 96;
	static final int X_COLUMN_SIZE = 160;

	static final int DEFAULT_CELL_WIDTH = 128;
	static final int DEFAULT_CELL_HEIGHT = 80;

	public static final ImageIcon TRACK_SCHEME_ICON = new ImageIcon(TrackSchemeFrame.class.getResource("resources/track_scheme.png"));

	private static final long serialVersionUID = 1L;
	private static final Dimension DEFAULT_SIZE = new Dimension(800, 600);
	private static final int TABLE_CELL_WIDTH 		= 40;
	private static final int TABLE_ROW_HEADER_WIDTH = 50;
	private static final Color GRID_COLOR = Color.GRAY;

	
	/*
	 * FIELDS
	 */

	SimpleWeightedGraph<Spot, DefaultWeightedEdge> trackGraph;
	ListenableUndirectedWeightedGraph<Spot, DefaultWeightedEdge> lGraph;
	private JGraphXAdapter<Spot, DefaultWeightedEdge> graph;
	private InfoPane infoPane;
	private ArrayList<GraphListener<Spot, DefaultWeightedEdge>> graphListeners = new ArrayList<GraphListener<Spot,DefaultWeightedEdge>>();
	/** The spots currently selected. */
	private HashSet<Spot> spotSelection = new HashSet<Spot>();
	Settings settings;
	mxTrackGraphComponent graphComponent;

	/*
	 * CONSTRUCTORS
	 */

	public TrackSchemeFrame(final SimpleWeightedGraph<Spot, DefaultWeightedEdge> trackGraph, final Settings settings) {
		this.trackGraph = trackGraph;
		this.lGraph = new ListenableUndirectedWeightedGraph<Spot, DefaultWeightedEdge>(trackGraph);
		this.graph = createGraph();
		this.settings = settings;
		init();
		setSize(DEFAULT_SIZE);
	}


	/*
	 * PUBLIC METHODS
	 */

	@Override
	public void collectionChanged(SpotCollectionEditEvent event) {
		//		if (event.getFlag() == SpotCollectionEditEvent.SPOT_CREATED) {
		//			final JGraphFacade facade = new JGraphFacade(jGraph);
		//			
		//			int targetColumn = 0;
		//			for (int i = 0; i < backPane.columnWidths.length; i++)
		//				targetColumn += backPane.columnWidths[i];
		//			
		//			SpotCell cell = null;
		//			for (Spot spot : event.getSpots()) {
		//				float instant = spot.getFeature(Feature.POSITION_T);
		//				cell = new SpotCell(spot);
		//				graph.getGraphLayoutCache().insert(cell);
		//				facade.setLocation(cell,  (targetColumn-2) * X_COLUMN_SIZE - DEFAULT_CELL_WIDTH/2, (0.5 + backPane.rows.get(instant)) * Y_COLUMN_SIZE - DEFAULT_CELL_HEIGHT/2);
		//				int height = Math.min(DEFAULT_CELL_WIDTH, spot.getIcon().getIconHeight());
		//				height = Math.max(height, 12);
		//				facade.setSize(cell, DEFAULT_CELL_WIDTH, height);
		//			}
		//			@SuppressWarnings("rawtypes")
		//			Map nested = facade.createNestedMap(false, false); // Obtain a map of the resulting attribute changes from the facade 
		//			jGraph.getGraphLayoutCache().edit(nested); // Apply the results to the actual graph 
		//			centerViewOn(cell);
		//		}

	}

	public void addGraphListener(GraphListener<Spot, DefaultWeightedEdge> listener) {
		graphListeners.add(listener);
	}

	public boolean removeGraphListener(GraphListener<Spot, DefaultWeightedEdge> listener) {
		return graphListeners.remove(listener);
	}

	public List<GraphListener<Spot, DefaultWeightedEdge>> getGraphListeners() {
		return graphListeners;
	}

	/**
	 * Return an updated reference of the {@link Graph} that acts as a model for tracks. This graph will
	 * have his edges and vertices updated by the manual interaction occuring in this view.
	 */
	public SimpleWeightedGraph<Spot, DefaultWeightedEdge> getTrackModel() {
		return trackGraph;
	}

	/**
	 * Return a reference to the {@link mxGraph} view in charge of rendering the track scheme.
	 */
	public JGraphXAdapter<Spot, DefaultWeightedEdge> getGraph() {
		return graph;
	}

	public void centerViewOn(mxCell cell) {
		mxRectangle bounds = graph.getCellBounds(cell);
		if (null == bounds)
			return;
		double scale = graphComponent.getZoomFactor();
		Point2D center = new Point2D.Double(bounds.getCenterX()*scale, bounds.getCenterY()*scale);
		graphComponent.getHorizontalScrollBar().setValue((int) center.getX() - graphComponent.getWidth()/2);
		graphComponent.getVerticalScrollBar().setValue((int) center.getY() - graphComponent.getHeight()/2);
	}

	public void doTrackLayout() {
		mxTrackGraphLayout graphLayout = new mxTrackGraphLayout(lGraph, graph);
		graphLayout.execute(graph.getDefaultParent());

//		 Forward painting info to graph component
		graphComponent.setColumnWidths(graphLayout.getTrackColumnWidths());
		graphComponent.setRowForInstant(graphLayout.getRowForInstant());
		graphComponent.setColumnColor(graphLayout.getTrackColors());

	}
	
	public void plotSelectionData() {
		Feature xFeature = infoPane.featureSelectionPanel.getXKey();
		Set<Feature> yFeatures = infoPane.featureSelectionPanel.getYKeys();
		if (yFeatures.isEmpty())
			return;

		Object[] selectedCells = graph.getSelectionCells();
		if (selectedCells == null || selectedCells.length == 0)
			return;

		List<Spot> spots = new ArrayList<Spot>();
		for(Object cell : selectedCells)
			if (cell instanceof SpotCell)
				spots.add(((SpotCell)cell).getSpot());
		if (spots.isEmpty())
			return;

		SpotFeatureGrapher grapher = new SpotFeatureGrapher(xFeature, yFeatures, spots, trackGraph, settings);
		grapher.setVisible(true);

	}

	
	/*
	 * PROTECTED METHODS
	 */
	
	/**
	 * Used to instantiate and configure the {@link JGraphXAdapter} that will be used for display.
	 */
	protected JGraphXAdapter<Spot, DefaultWeightedEdge> createGraph() {
		JGraphXAdapter<Spot, DefaultWeightedEdge> graph = new JGraphXAdapter<Spot, DefaultWeightedEdge>(lGraph, new SpotCellViewFactory(lGraph));
		graph.setAllowLoops(false);
		graph.setAllowDanglingEdges(false);
		graph.setCellsCloneable(false);
		graph.setGridEnabled(false);
		graph.setLabelsVisible(false);
		graph.setDropEnabled(false);
		graph.setSwimlaneNesting(true);
		return graph;
	}

	protected mxTrackGraphComponent createGraphComponent() {
		mxTrackGraphComponent gc = new mxTrackGraphComponent(this);
		gc.getVerticalScrollBar().setUnitIncrement(16);
		gc.getHorizontalScrollBar().setUnitIncrement(16);
		gc.setExportEnabled(false);
		gc.setImportEnabled(false);
		return gc;
		
	}
	
	
	/**
	 * Instantiate the toolbar of the track scheme. Hook for subclassers.
	 */
	protected JToolBar createToolBar() {
		return new TrackSchemeToolbar(this);		
	}

	/*
	 * PRIVATE METHODS
	 */

	private void remove(Object[] cells) {
		for (Object cell : cells) {
			if (cell instanceof TrackEdgeCell) {
				TrackEdgeCell trackEdge = (TrackEdgeCell) cell;
				DefaultWeightedEdge edge = trackEdge.getEdge();
				lGraph.removeEdge(edge);
				trackGraph.removeEdge(edge);
			} else if (cell instanceof SpotCell) {
				SpotCell spotCell = (SpotCell) cell;
				lGraph.removeVertex(spotCell.getSpot());
				trackGraph.removeVertex(spotCell.getSpot());
			}
		}
	}

	

	private void init() {
		// Frame look
		setIconImage(TRACK_SCHEME_ICON.getImage());
		setTitle("Track scheme");

		getContentPane().setLayout(new BorderLayout());
		// Add a ToolBar
		getContentPane().add(createToolBar(), BorderLayout.NORTH);

		// GraphComponent
		graphComponent = createGraphComponent();
		

		// Arrange graph layout
		doTrackLayout();

		// Add the info pane
		infoPane = new InfoPane();

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, infoPane, graphComponent);
		splitPane.setDividerLocation(170);
		getContentPane().add(splitPane, BorderLayout.CENTER);

		// Listeners
		//		graph.addListener(null, new mxIEventListener() {
		//			
		//			@Override
		//			public void invoke(Object sender, mxEventObject evt) {
		//				System.out.println("Received event: "+evt+" from: "+sender);// DEBUG
		//				
		//			}
		//		});
		//		addGraphSelectionListener(new GraphSelectionListener() {
		//
		//			@Override
		//			public void valueChanged(GraphSelectionEvent e) {
		//				Object[] cells = e.getCells();
		//				for(Object cell : cells) {
		//					if (cell instanceof SpotCell) {
		//						SpotCell spotCell = (SpotCell) cell;
		//						if (e.isAddedCell(cell))
		//							spotSelection.add(spotCell.getSpot());
		//						else 
		//							spotSelection.remove(spotCell.getSpot());
		//					}
		//				}
		//				infoPane.echo(spotSelection);
		//				if (spotSelection.isEmpty())
		//					infoPane.scrollTable.setVisible(false);
		//				else
		//					infoPane.scrollTable.setVisible(true);
		//			}
		//		});

		// Forward graph change events to the listeners registered with this frame 
		//		lGraph.addGraphListener(new MyGraphListener());
	}

	

	


	/**
	 *  PopupMenu
	 */
	@SuppressWarnings("serial")
	private JPopupMenu createPopupMenu(final Point pt, final Object cell) {
		JPopupMenu menu = new JPopupMenu();

		if (cell != null) {
			// Edit
			menu.add(new AbstractAction("Edit spot name") {
				public void actionPerformed(ActionEvent e) {
					//					graph.startEditingAtCell(cell);
				}
			});

		} else if (spotSelection.size() > 0) {

			// Multi edit

			//			menu.add(new AbstractAction("Edit " + spotSelection.size() +" spot names") {
			//				public void actionPerformed(ActionEvent e) {
			//					
			//					final SpotView[] cellViews = new SpotView[spotSelection.size()];
			//					final JGraphFacade facade = new JGraphFacade(jGraph);
			//					Iterator<Spot> it = spotSelection.iterator();
			//					for (int i = 0; i < spotSelection.size(); i++) {
			//						Object facadeTarget = jGMAdapter.getVertexCell(it.next());
			//						SpotView vView = (SpotView) facade.getCellView(facadeTarget);
			//						cellViews[i] = vView;
			//					}
			//					
			//					final JTextField editField = new JTextField(20);
			//					editField.setFont(FONT);
			//					editField.setBounds(pt.x, pt.y, 100, 20);
			//					jGraph.add(editField);
			//					editField.setVisible(true);
			//					editField.revalidate();
			//					jGraph.repaint();
			//					editField.requestFocusInWindow();
			//					editField.addActionListener(new ActionListener() {
			//						
			//						@Override
			//						public void actionPerformed(ActionEvent e) {
			//							for(Spot spot : spotSelection)
			//								spot.setName(editField.getText());
			//							jGraph.remove(editField);
			//							jGraph.refresh();
			//						}
			//					});
			//				}
			//			});

		}

		// Link
		if (spotSelection.size() > 1) {
			Action linkAction = new AbstractAction("Link spots") {

				@Override
				public void actionPerformed(ActionEvent e) {
					// Sort spots by time
					TreeMap<Float, Spot> spotsInTime = new TreeMap<Float, Spot>();
					for(Spot spot : spotSelection) 
						spotsInTime.put(spot.getFeature(Feature.POSITION_T), spot);
					// Then link them in this order
					Iterator<Float> it = spotsInTime.keySet().iterator();
					Float previousTime = it.next();
					Spot previousSpot = spotsInTime.get(previousTime);
					Float currentTime;
					Spot currentSpot;
					while(it.hasNext()) {
						currentTime = it.next();
						currentSpot = spotsInTime.get(currentTime);
						// Link if not linked already
						if (trackGraph.containsEdge(previousSpot, currentSpot))
							continue;
						DefaultWeightedEdge edge = lGraph.addEdge(previousSpot, currentSpot);
						if (null == edge)
							infoPane.textPane.setText("Invalid edge.");
						lGraph.setEdgeWeight(edge, -1); // Default Weight
						// Update the MODEL graph as well
						trackGraph.addEdge(previousSpot, currentSpot, edge);
						previousSpot = currentSpot;
					}
				}
			};
			menu.add(linkAction);
		}

		// Remove
		if (!graph.isSelectionEmpty()) {
			Action removeAction = new AbstractAction("Remove spots and links") {
				public void actionPerformed(ActionEvent e) {
					remove(graph.getSelectionCells());
				}
			};
			menu.add(removeAction);
		}

		return menu;
	}



	/*
	 * INNER CLASSES
	 */

	private class InfoPane extends JPanel {

		private class RowHeaderRenderer extends JLabel implements ListCellRenderer, Serializable {

			RowHeaderRenderer(JTable table) {
				JTableHeader header = table.getTableHeader();
				setOpaque(false);
				setBorder(UIManager.getBorder("TableHeader.cellBorder"));
				setForeground(header.getForeground());
				setBackground(header.getBackground());
				setFont(SMALL_FONT.deriveFont(9.0f));
				setHorizontalAlignment(SwingConstants.LEFT);				
			}

			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				setText((value == null) ? "" : value.toString());
				return this;
			}
		}

		private JTextPane textPane;
		private JTable table;
		private JScrollPane scrollTable;
		private FeaturePlotSelectionPanel<Feature> featureSelectionPanel;

		public InfoPane() {
			init();
		}

		public void echo(Set<Spot> spots) {
			// Fill feature table
			DefaultTableModel dm = new DefaultTableModel() { // Un-editable model
				@Override
				public boolean isCellEditable(int row, int column) { return false; }
			};
			for (Spot spot : spots) {
				Object[] columnData = new Object[Feature.values().length];
				for (int i = 0; i < columnData.length; i++) 
					columnData[i] = String.format("%.1f", spot.getFeature(Feature.values()[i]));
				dm.addColumn(spot.getName(), columnData);
			}
			table.setModel(dm);
			// Tune look

			DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
				public boolean isOpaque() { return false; };
				@Override
				public Color getBackground() {
					return Color.BLUE;
				}
			};
			headerRenderer.setBackground(Color.RED);


			DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
			renderer.setOpaque(false);
			renderer.setHorizontalAlignment(SwingConstants.RIGHT);
			renderer.setFont(SMALL_FONT);			
			for(int i=0; i<table.getColumnCount(); i++) {
				table.setDefaultRenderer(table.getColumnClass(i), renderer);
				table.getColumnModel().getColumn(i).setPreferredWidth(TABLE_CELL_WIDTH);
			}
			for (Component c : scrollTable.getColumnHeader().getComponents())
				c.setBackground(getBackground());
			scrollTable.getColumnHeader().setOpaque(false);

			// Set text
			textPane.setText("Selection:");
		}

		private void init() {

			AbstractListModel lm = new AbstractListModel() {
				String headers[] = new String[Feature.values().length];
				{
					for(int i=0; i<headers.length; i++)
						headers[i] = Feature.values()[i].shortName();			    	  
				}

				public int getSize() {
					return headers.length;
				}

				public Object getElementAt(int index) {
					return headers[index];
				}
			};

			table = new JTable();
			table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			table.setOpaque(false);
			table.setFont(SMALL_FONT);
			table.setPreferredScrollableViewportSize(new Dimension(120, 400));
			table.getTableHeader().setOpaque(false);
			table.setSelectionForeground(Color.YELLOW.darker());
			table.setGridColor(GRID_COLOR);

			JList rowHeader = new JList(lm);
			rowHeader.setFixedCellWidth(TABLE_ROW_HEADER_WIDTH);
			rowHeader.setFixedCellHeight(table.getRowHeight());
			rowHeader.setCellRenderer(new RowHeaderRenderer(table));
			rowHeader.setBackground(getBackground());

			scrollTable = new JScrollPane(table);
			scrollTable.setRowHeaderView(rowHeader);
			scrollTable.getRowHeader().setOpaque(false);
			scrollTable.setOpaque(false);
			scrollTable.getViewport().setOpaque(false);
			scrollTable.setVisible(false); // for now

			textPane = new JTextPane();
			textPane.setCaretPosition(0);
			//			StyledDocument styledDoc = textPane.getStyledDocument();
			textPane.setEditable(false);
			textPane.setOpaque(false);
			textPane.setFont(SMALL_FONT);

			featureSelectionPanel = new FeaturePlotSelectionPanel<Feature>(Feature.POSITION_T);

			setLayout(new BorderLayout());
			add(textPane, BorderLayout.NORTH);
			add(scrollTable, BorderLayout.CENTER);
			add(featureSelectionPanel, BorderLayout.SOUTH);

		}

	}


	//	@SuppressWarnings("serial")
	//	private static class MyGraphCellEditor extends DefaultGraphCellEditor {
	//		private Object target;
	//		
	//		public MyGraphCellEditor() {
	//			addCellEditorListener(new CellEditorListener() {
	//
	//				@Override
	//				public void editingStopped(ChangeEvent e) {
	//					if (target instanceof SpotCell) {
	//						SpotCell spotCell = (SpotCell) target;
	//						spotCell.getSpot().setName(""+getCellEditorValue());
	//					}
	//				}
	//
	//				@Override
	//				public void editingCanceled(ChangeEvent e) {}
	//			});
	//		}
	//		
	//		@Override
	//		public Component getGraphCellEditorComponent(JGraph graph, Object cell, boolean isSelected) {
	//			target = cell;
	//			return super.getGraphCellEditorComponent(graph, cell, isSelected);
	//		};
	//		
	//		
	//	};


	/**
	 * Used to forwad listenable graph model changes to the listener of this frame.
	 */
	private class MyGraphListener implements GraphListener<Spot, DefaultWeightedEdge>, Serializable {

		private static final long serialVersionUID = -1054534879013143084L;

		@Override
		public void vertexAdded(GraphVertexChangeEvent<Spot> e) {
			for (GraphListener<Spot, DefaultWeightedEdge> graphListener : graphListeners)
				graphListener.vertexAdded(e);
		}
		@Override
		public void vertexRemoved(GraphVertexChangeEvent<Spot> e) {
			for (GraphListener<Spot, DefaultWeightedEdge> graphListener : graphListeners)
				graphListener.vertexRemoved(e);
		}
		@Override
		public void edgeAdded(GraphEdgeChangeEvent<Spot, DefaultWeightedEdge> e) {
			for (GraphListener<Spot, DefaultWeightedEdge> graphListener : graphListeners)
				graphListener.edgeAdded(e);
		}
		@Override
		public void edgeRemoved(GraphEdgeChangeEvent<Spot, DefaultWeightedEdge> e) {
			for (GraphListener<Spot, DefaultWeightedEdge> graphListener : graphListeners)
				graphListener.edgeRemoved(e);
		}
	}




}
