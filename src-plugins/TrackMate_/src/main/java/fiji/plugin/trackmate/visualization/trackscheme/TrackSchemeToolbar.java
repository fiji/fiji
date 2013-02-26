package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

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
	//	private static final ImageIcon BRANCH_FOLDING_ON_ICON 	= new ImageIcon(TrackSchemeFrame.class.getResource("resources/shape_square.png"));
	//	private static final ImageIcon BRANCH_FOLDING_OFF_ICON 	= new ImageIcon(TrackSchemeFrame.class.getResource("resources/shape_square-forbid.png"));
	//	private static final ImageIcon FOLD_ALL_BRANCHES_ICON	= new ImageIcon(TrackSchemeFrame.class.getResource("resources/shape_group.png"));
	//	private static final ImageIcon UNFOLD_ALL_BRANCHES_ICON	= new ImageIcon(TrackSchemeFrame.class.getResource("resources/shape_ungroup.png"));
	//	private static final ImageIcon DISPLAY_COST_ON_ICON		= new ImageIcon(TrackSchemeFrame.class.getResource("resources/Label-icons.png"));
	//	private static final ImageIcon DISPLAY_COST_OFF_ICON	= new ImageIcon(TrackSchemeFrame.class.getResource("resources/Label-icons-disabled.png"));
	private static final ImageIcon DISPLAY_DECORATIONS_ON_ICON	= new ImageIcon(TrackSchemeFrame.class.getResource("resources/application_view_columns.png"));
	private static final ImageIcon DISPLAY_DECORATIONS_OFF_ICON	= new ImageIcon(TrackSchemeFrame.class.getResource("resources/application.png"));
	private static final ImageIcon SELECT_STYLE_ICON = new ImageIcon(TrackSchemeFrame.class.getResource("resources/style.png"));

	private final TrackScheme trackScheme;


	public TrackSchemeToolbar(final TrackScheme trackScheme) {
		super("Track Scheme toolbar", JToolBar.HORIZONTAL);
		this.trackScheme = trackScheme;
		init();
	}

	@SuppressWarnings("serial")
	private void init() {

		setFloatable(false);

		/*
		 *  Toggle Connect Mode
		 */

		boolean defaultLinkingEnabled = TrackScheme.DEFAULT_LINKING_ENABLED;
		final Action toggleLinkingAction = new AbstractAction(null, defaultLinkingEnabled ? LINKING_ON_ICON : LINKING_OFF_ICON) {
			public void actionPerformed(ActionEvent e) {
				boolean enabled = trackScheme.toggleLinking();
				ImageIcon connectIcon;
				if (!enabled) {
					connectIcon = LINKING_OFF_ICON;
				} else {
					connectIcon = LINKING_ON_ICON;
				}
				putValue(SMALL_ICON, connectIcon);

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
				trackScheme.zoomIn();
			}
		};
		zoomOutAction = new AbstractAction(null, ZOOM_OUT_ICON) {
			public void actionPerformed(ActionEvent e) {
				trackScheme.zoomOut();
			}
		};
		resetZoomAction  = new AbstractAction(null, RESET_ZOOM_ICON) {
			public void actionPerformed(ActionEvent e) {
				trackScheme.resetZoom();
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
				trackScheme.doTrackLayout();
				trackScheme.refresh();
			}
		};
		final JButton redoLayoutButton = new JButton(redoLayoutAction);
		redoLayoutButton.setToolTipText("Redo layout");

		/* 
		 * Folding
		 */

		//		boolean defaultEnabled = frame.getGraphLayout().isBranchGroupingEnabled();
		//		final JButton foldAllButton = new JButton(null, FOLD_ALL_BRANCHES_ICON);
		//		foldAllButton.addActionListener(new ActionListener() {
		//			@Override
		//			public void actionPerformed(ActionEvent e) {
		//				frame.getGraphLayout().setAllFolded(true);
		//			}
		//		});
		//		final JButton unFoldAllButton = new JButton(null, UNFOLD_ALL_BRANCHES_ICON);
		//		unFoldAllButton.addActionListener(new ActionListener() {
		//			@Override
		//			public void actionPerformed(ActionEvent e) {
		//				frame.getGraphLayout().setAllFolded(false);
		//			}
		//		});
		//		final JButton toggleEnableFoldingButton = new JButton(null, 
		//						defaultEnabled ? 
		//						BRANCH_FOLDING_ON_ICON : BRANCH_FOLDING_OFF_ICON);
		//		toggleEnableFoldingButton.addActionListener(new ActionListener() {
		//			@Override
		//			public void actionPerformed(ActionEvent e) {
		//				boolean enabled = frame.getGraphLayout().isBranchGroupingEnabled();
		//				frame.getGraphLayout().setBranchGrouping(!enabled);
		//				
		//				if (enabled) {
		//					toggleEnableFoldingButton.setIcon(BRANCH_FOLDING_OFF_ICON);	
		//					foldAllButton.setEnabled(false);
		//					unFoldAllButton.setEnabled(false);
		//				} else {
		//					toggleEnableFoldingButton.setIcon(BRANCH_FOLDING_ON_ICON);
		//					foldAllButton.setEnabled(true);
		//					unFoldAllButton.setEnabled(true);
		//				}
		//			}
		//		});
		//		toggleEnableFoldingButton.setToolTipText("Toggle folding (redo layout)");
		//		foldAllButton.setToolTipText("Fold all branches");
		//		unFoldAllButton.setToolTipText("Unfold all branches");
		//		if (!defaultEnabled) {
		//			foldAllButton.setEnabled(false);
		//			unFoldAllButton.setEnabled(false);
		//		}


		// Capture 
		final Action captureUndecoratedAction = new AbstractAction(null, CAPTURE_UNDECORATED_ICON) {			
			@Override
			public void actionPerformed(ActionEvent e) {
				trackScheme.captureUndecorated();
			}
		};
		final Action captureDecoratedAction = new AbstractAction(null, CAPTURE_DECORATED_ICON) {			
			@Override
			public void actionPerformed(ActionEvent e) {
				trackScheme.captureDecorated();
			}
		};
		final Action saveAction = new SaveAction(trackScheme);
		final JButton captureUndecoratedButton = new JButton(captureUndecoratedAction);
		final JButton captureDecoratedButton = new JButton(captureDecoratedAction);
		final JButton saveButton = new JButton(saveAction);
		captureUndecoratedButton.setToolTipText("Capture undecorated track scheme");
		captureDecoratedButton.setToolTipText("Capture decorated track scheme");
		saveButton.setToolTipText("Export to..");


		/*
		 * display labels on edges
		 */

		//		JButton toggleDisplayCostsButton;
		//		{
		//			boolean defaultDisplayCosts= TrackScheme.DEFAULT_DO_DISPLAY_COSTS_ON_EDGES;
		//			final Action toggleDisplayCostsAction = new AbstractAction(null, defaultDisplayCosts ? DISPLAY_COST_ON_ICON : DISPLAY_COST_OFF_ICON) {
		//				public void actionPerformed(ActionEvent e) {
		//					boolean enabled = trackScheme.toggleDisplayCosts();
		//					ImageIcon displayIcon;
		//					if (enabled)
		//						displayIcon = DISPLAY_COST_OFF_ICON;
		//					else
		//						displayIcon = DISPLAY_COST_ON_ICON;
		//					putValue(SMALL_ICON, displayIcon);
		//
		//				}
		//			};
		//			toggleDisplayCostsButton = new JButton(toggleDisplayCostsAction);
		//			toggleDisplayCostsButton.setToolTipText("Toggle costs display (redo layout)");
		//		}

		/*
		 * display background decorations
		 */
		JButton toggleDisplayDecorationsButton; 
		{
			boolean defaultDisplayDecorations= TrackScheme.DEFAULT_DO_PAINT_DECORATIONS;
			final Action toggleDisplayDecorations = new AbstractAction(null, defaultDisplayDecorations ? DISPLAY_DECORATIONS_ON_ICON : DISPLAY_DECORATIONS_OFF_ICON) {
				public void actionPerformed(ActionEvent e) {
					boolean enabled = trackScheme.toggleDisplayDecoration();
					ImageIcon displayIcon;
					if (enabled)
						displayIcon = DISPLAY_DECORATIONS_OFF_ICON;
					else
						displayIcon = DISPLAY_DECORATIONS_ON_ICON;
					putValue(SMALL_ICON, displayIcon);
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
			Set<String> styleNames = new HashSet<String>(TrackSchemeStylist.VERTEX_STYLES.keySet());
			selectStyleBox = new JComboBox(styleNames.toArray());
			selectStyleBox.setSelectedItem(TrackSchemeStylist.DEFAULT_STYLE_NAME);
			selectStyleBox.setMaximumSize(new Dimension(100, 20));
			selectStyleBox.setFont(FONT);
			selectStyleBox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					final String selectedStyle = (String) selectStyleBox.getSelectedItem();
					new Thread("TrackScheme changing style thread") {
						public void run() {
							trackScheme.stylist.setStyle(selectedStyle);
							trackScheme.doTrackStyle();
							trackScheme.refresh();
						}
					}.start();
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
		//		add(toggleDisplayCostsButton);
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
