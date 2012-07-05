package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import ij.ImagePlus;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.SwingConstants;

import com.mxgraph.util.mxCellRenderer;

public class TrackSchemeToolbar extends JToolBar {
	
	private static final long serialVersionUID = 3442140463984241266L;
	private static final ImageIcon LINKING_ON_ICON 	= new ImageIcon(TrackSchemeFrame.class.getResource("resources/connect.png")); 
	private static final ImageIcon LINKING_OFF_ICON = new ImageIcon(TrackSchemeFrame.class.getResource("resources/forbid_connect.png")); 
	private static final ImageIcon RESET_ZOOM_ICON 	= new ImageIcon(TrackSchemeFrame.class.getResource("resources/zoom.png")); 
	private static final ImageIcon ZOOM_IN_ICON 	= new ImageIcon(TrackSchemeFrame.class.getResource("resources/zoom_in.png")); 
	private static final ImageIcon ZOOM_OUT_ICON 	= new ImageIcon(TrackSchemeFrame.class.getResource("resources/zoom_out.png")); 
	private static final ImageIcon REFRESH_ICON		= new ImageIcon(TrackSchemeFrame.class.getResource("resources/refresh.png"));
	private static final ImageIcon CAPTURE_UNDECORATED_ICON = new ImageIcon(TrackSchemeFrame.class.getResource("resources/camera_go.png"));
	private static final ImageIcon CAPTURE_DECORATED_ICON = new ImageIcon(TrackSchemeFrame.class.getResource("resources/camera_edit.png"));

	private static final ImageIcon BRANCH_FOLDING_ON_ICON 	= new ImageIcon(TrackSchemeFrame.class.getResource("resources/shape_square.png"));
	private static final ImageIcon BRANCH_FOLDING_OFF_ICON 	= new ImageIcon(TrackSchemeFrame.class.getResource("resources/shape_square-forbid.png"));
	private static final ImageIcon FOLD_ALL_BRANCHES_ICON	= new ImageIcon(TrackSchemeFrame.class.getResource("resources/shape_group.png"));
	private static final ImageIcon UNFOLD_ALL_BRANCHES_ICON	= new ImageIcon(TrackSchemeFrame.class.getResource("resources/shape_ungroup.png"));

	private static final ImageIcon DISPLAY_COST_ON_ICON		= new ImageIcon(TrackSchemeFrame.class.getResource("resources/Label-icons.png"));
	private static final ImageIcon DISPLAY_COST_OFF_ICON	= new ImageIcon(TrackSchemeFrame.class.getResource("resources/Label-icons-disabled.png"));
	
	private static final ImageIcon DISPLAY_DECORATIONS_ON_ICON	= new ImageIcon(TrackSchemeFrame.class.getResource("resources/application_view_columns.png"));
	private static final ImageIcon DISPLAY_DECORATIONS_OFF_ICON	= new ImageIcon(TrackSchemeFrame.class.getResource("resources/application.png"));
	
	private static final ImageIcon SELECT_STYLE_ICON = new ImageIcon(TrackSchemeFrame.class.getResource("resources/style.png"));
	
	private TrackSchemeFrame frame;

	public TrackSchemeToolbar(TrackSchemeFrame frame) {
		super("Track Scheme toolbat", JToolBar.HORIZONTAL);
		this.frame = frame;
		init();
	}
	
	@SuppressWarnings("serial")
	private void init() {
		
		setFloatable(false);

		/*
		 *  Toggle Connect Mode
		 */
		
		boolean defaultLinkingEnabled = frame.defaultLinkingEnabled;
		final Action toggleLinkingAction = new AbstractAction(null, defaultLinkingEnabled ? LINKING_ON_ICON : LINKING_OFF_ICON) {
			public void actionPerformed(ActionEvent e) {
				boolean enabled = frame.getGraphComponent().getConnectionHandler().isEnabled();
				ImageIcon connectIcon;
				if (enabled)
					connectIcon = LINKING_OFF_ICON;
				else
					connectIcon = LINKING_ON_ICON;
				putValue(SMALL_ICON, connectIcon);
				frame.getGraphComponent().getConnectionHandler().setEnabled(!enabled);
			}
			
		};
		final JButton toggleLinkingButton = new JButton(toggleLinkingAction);
		toggleLinkingButton.setToolTipText("Toggle linking");
		
		/*
		 *  Zoom 
		 */
		
		final Action zoomInAction;
		final Action zoomOutAction;
		final Action resetZoomAction;
		final JButton zoomInButton = new JButton();
		final JButton zoomOutButton = new JButton();
		final JButton  resetZoomButton = new JButton();
		zoomInAction = new AbstractAction(null, ZOOM_IN_ICON) {
			public void actionPerformed(ActionEvent e) {
				frame.getGraphComponent().zoomIn();
			}
		};
		zoomOutAction = new AbstractAction(null, ZOOM_OUT_ICON) {
			public void actionPerformed(ActionEvent e) {
				frame.getGraphComponent().zoomOut();
			}
		};
		resetZoomAction  = new AbstractAction(null, RESET_ZOOM_ICON) {
			public void actionPerformed(ActionEvent e) {
				frame.getGraphComponent().zoomTo(1.0, false);
			}
		};
		zoomInButton.setAction(zoomInAction);
		zoomOutButton.setAction(zoomOutAction);
		resetZoomButton.setAction(resetZoomAction);
		zoomInButton.setToolTipText("Zoom in 2x");
		zoomOutButton.setToolTipText("Zoom out 2x");
		resetZoomButton.setToolTipText("Reset zoom");
		
		// Redo layout
		final Action redoLayoutAction = new AbstractAction(null, REFRESH_ICON) {
			public void actionPerformed(ActionEvent e) {
				frame.doTrackLayout();
			}
		};
		final JButton redoLayoutButton = new JButton(redoLayoutAction);
		redoLayoutButton.setToolTipText("Redo layout");
		
		/* 
		 * Folding
		 */
		
		boolean defaultEnabled = frame.getGraphLayout().isBranchGroupingEnabled();
		final JButton foldAllButton = new JButton(null, FOLD_ALL_BRANCHES_ICON);
		foldAllButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.getGraphLayout().setAllFolded(true);
			}
		});
		final JButton unFoldAllButton = new JButton(null, UNFOLD_ALL_BRANCHES_ICON);
		unFoldAllButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.getGraphLayout().setAllFolded(false);
			}
		});
		final JButton toggleEnableFoldingButton = new JButton(null, 
						defaultEnabled ? 
						BRANCH_FOLDING_ON_ICON : BRANCH_FOLDING_OFF_ICON);
		toggleEnableFoldingButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean enabled = frame.getGraphLayout().isBranchGroupingEnabled();
				frame.getGraphLayout().setBranchGrouping(!enabled);
				
				if (enabled) {
					toggleEnableFoldingButton.setIcon(BRANCH_FOLDING_OFF_ICON);	
					foldAllButton.setEnabled(false);
					unFoldAllButton.setEnabled(false);
				} else {
					toggleEnableFoldingButton.setIcon(BRANCH_FOLDING_ON_ICON);
					foldAllButton.setEnabled(true);
					unFoldAllButton.setEnabled(true);
				}
			}
		});
		toggleEnableFoldingButton.setToolTipText("Toggle folding (redo layout)");
		foldAllButton.setToolTipText("Fold all branches");
		unFoldAllButton.setToolTipText("Unfold all branches");
		if (!defaultEnabled) {
			foldAllButton.setEnabled(false);
			unFoldAllButton.setEnabled(false);
		}
		
		
		// Capture 
		final Action captureUndecoratedAction = new AbstractAction(null, CAPTURE_UNDECORATED_ICON) {			
			@Override
			public void actionPerformed(ActionEvent e) {
				BufferedImage image = mxCellRenderer.createBufferedImage(frame.getGraph(), null, 1, Color.WHITE, true, null, 
						frame.getGraphComponent().getCanvas());
				ImagePlus imp = new ImagePlus("Track scheme capture", image);
				imp.show();
			}
		};
		final Action captureDecoratedAction = new AbstractAction(null, CAPTURE_DECORATED_ICON) {			
			@Override
			public void actionPerformed(ActionEvent e) {
				JViewport view = frame.getGraphComponent().getViewport();
				Point currentPos = view.getViewPosition();
				view.setViewPosition(new Point(0, 0)); // We have to do that otherwise, top left is not painted
				Dimension size = view.getViewSize();
				BufferedImage image =  (BufferedImage) view.createImage(size.width, size.height);
				Graphics2D captureG = image.createGraphics();
				view.paintComponents(captureG);
				view.setViewPosition(currentPos);
				ImagePlus imp = new ImagePlus("Track scheme capture", image);
				imp.show();
			}
		};
		final Action saveAction = new SaveAction(frame);
		final JButton captureUndecoratedButton = new JButton(captureUndecoratedAction);
		final JButton captureDecoratedButton = new JButton(captureDecoratedAction);
		final JButton saveButton = new JButton(saveAction);
		captureUndecoratedButton.setToolTipText("Capture undecorated track scheme");
		captureDecoratedButton.setToolTipText("Capture decorated track scheme");
		saveButton.setToolTipText("Export to..");
		
		
		/*
		 * display labels on edges
		 */

		JButton toggleDisplayCostsButton;
		{
			boolean defaultDisplayCosts= TrackSchemeFrame.DEFAULT_DO_DISPLAY_COSTS_ON_EDGES;
			final Action toggleDisplayCostsAction = new AbstractAction(null, defaultDisplayCosts ? DISPLAY_COST_ON_ICON : DISPLAY_COST_OFF_ICON) {
				public void actionPerformed(ActionEvent e) {
					boolean enabled = frame.getGraphLayout().isDoDisplayCosts();
					ImageIcon displayIcon;
					if (enabled)
						displayIcon = DISPLAY_COST_OFF_ICON;
					else
						displayIcon = DISPLAY_COST_ON_ICON;
					putValue(SMALL_ICON, displayIcon);
					frame.getGraphLayout().setDoDisplayCosts(!enabled);
				}

			};
			toggleDisplayCostsButton = new JButton(toggleDisplayCostsAction);
			toggleDisplayCostsButton.setToolTipText("Toggle costs display (redo layout)");
		}
		
		/*
		 * display background decorations
		 */
		JButton toggleDisplayDecorationsButton; 
		{
			boolean defaultDisplayDecorations= TrackSchemeFrame.DEFAULT_DO_PAINT_DECORATIONS;
			final Action toggleDisplayDecorations = new AbstractAction(null, defaultDisplayDecorations ? DISPLAY_DECORATIONS_ON_ICON : DISPLAY_DECORATIONS_OFF_ICON) {
				public void actionPerformed(ActionEvent e) {
					boolean enabled = frame.getGraphComponent().isDoPaintDecorations();
					ImageIcon displayIcon;
					if (enabled)
						displayIcon = DISPLAY_DECORATIONS_OFF_ICON;
					else
						displayIcon = DISPLAY_DECORATIONS_ON_ICON;
					putValue(SMALL_ICON, displayIcon);
					frame.getGraphComponent().setDoPaintDecorations(!enabled);
					frame.getGraphComponent().repaint();
				}

			};
			toggleDisplayDecorationsButton = new JButton(toggleDisplayDecorations);
			toggleDisplayDecorationsButton.setToolTipText("Toggle display decorations");
		}
		
		
		/*
		 * styles
		 */
		final JLabel selectStyleLabel;
		{
			selectStyleLabel = new JLabel("Style:", SELECT_STYLE_ICON, SwingConstants.RIGHT);
			selectStyleLabel.setFont(FONT);
		}
		
		final JComboBox selectStyleBox;
		{
			Map<String, Map<String, Object>> styles = frame.getGraph().getStylesheet().getStyles();
			Set<String> styleNames = new HashSet<String>(styles.keySet());
			styleNames.remove("defaultEdge");
			styleNames.remove("defaultVertex");
			selectStyleBox = new JComboBox(styleNames.toArray());
			selectStyleBox.setSelectedItem(TrackSchemeFrame.DEFAULT_STYLE_NAME);
			selectStyleBox.setMaximumSize(new Dimension(100, 20));
			selectStyleBox.setFont(FONT);
			selectStyleBox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					String selectedStyle = (String) selectStyleBox.getSelectedItem();
					mxTrackGraphLayout layout = frame.getGraphLayout();
					if (!selectedStyle.equals(layout.selectedStyle)) {
						layout.selectedStyle = selectedStyle;
						layout.execute(frame);
					}
				}
			});

		}
		
		/*
		 * ADD TO TOOLBAR
		 */
		
		// Layout
		add(redoLayoutButton);
		// Separator
		addSeparator();
		// Linking
		add(toggleLinkingButton);
		// Separator
		addSeparator();
		// Folding  - DISABLED until further notice
//		add(toggleEnableFoldingButton);
//		add(foldAllButton);
//		add(unFoldAllButton);
//		// Separator
//		addSeparator();
		// Zoom
		add(zoomInButton);
		add(zoomOutButton);
		add(resetZoomButton);	
		// Separator
		addSeparator();
		// Capture / Export
		add(captureUndecoratedButton);
		add(captureDecoratedButton);
		add(saveButton);
		// Separator
		addSeparator();
		// Display costs along edges
		add(toggleDisplayCostsButton);
		// Display background decorations
		add(toggleDisplayDecorationsButton);
		// Separator
		addSeparator();
		// Set display style
		add(selectStyleLabel);
		add(selectStyleBox);
		add(Box.createHorizontalGlue());
				
	}
}
