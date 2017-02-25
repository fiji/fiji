/*
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2007 - 2015 Fiji
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package sc.fiji;

import java.awt.BorderLayout;
import java.awt.Window;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JPanel;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.realtransform.AffineTransform3D;

import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.viewer.ViewerFrame;
import bdv.viewer.ViewerPanel;
import javafx.application.Application;
import javafx.embed.swing.SwingNode;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.RotateEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.SwipeEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 * Launches Fiji.
 * 
 * @author Curtis Rueden
 * @see net.imagej.ImageJ
 */
@SuppressWarnings("restriction")
public final class Main {

	private Main() {
		// Prevent instantiation of utility class.
	}

	// -- Main method --

	public static void main(final String[] args) throws IOException {
		// NB: If you set the plugins.dir system property to a valid Fiji
		// installation, you will have access to that installation's scripts
		// and ImageJ 1.x plugins.
		//
		// However, ImageJ1 will prioritize the plugin JARs in the ImageJ
		// installation's plugins folder over the JARs on the classpath!
		System.setProperty("plugins.dir", "/path/to/your/Fiji.app");

		ImageJ ij = new ImageJ();
		ij.launch(args);
		final Dataset t1Head = //
			ij.scifio().datasetIO().open("/Users/curtis/data/t1-head.tif");
		BdvFunctions.show(t1Head, "T1-Head").setDisplayRange(0, 742);
		for (Window w : Window.getWindows()) {
			if (w instanceof ViewerFrame) {
				// Convert it to JavaFX!
				convertToJavaFX((ViewerFrame) w);
			}
		}
	}

	public static ViewerPanel viewer;
	public static class MyApplication extends Application {

		@Override
		public void start(Stage primaryStage) throws Exception {
			AnchorPane root = new AnchorPane();

			final SwingNode swingNode = new SwingNode();
			swingNode.setContent(viewer);
			addEventHandlers(swingNode);
			root.getChildren().add(swingNode);
			Scene scene = new Scene(root, 500, 500);

			primaryStage.setTitle("BDV");
			primaryStage.setScene(scene);
			primaryStage.show();
		}
	}

	private static void convertToJavaFX(final ViewerFrame viewerFrame) {
		viewerFrame.setVisible(false);
		viewer = viewerFrame.getViewerPanel(); // AAAAARGH
		viewerFrame.remove(viewer);
		Application.launch(MyApplication.class);
	}
	
	public static void addEventHandlers(final Node node) {
		// add event handlers
		node.setOnScroll(new EventHandler<ScrollEvent>() {

			@Override
			public void handle(final ScrollEvent event) {
				System.err.println(event);
			}

		});

		node.setOnZoom(new EventHandler<ZoomEvent>() {

			@Override
			public void handle(ZoomEvent event) {
				double x = event.getX();
				double y = event.getY();
//				viewer.setCurrentViewerTransform(hb);
//				AffineTransform3D affine = viewer.getDisplay().getTransformEventHandler().getTransform();
//				affine.translate(translationVector);
//				affine.scale(s);
				System.err.println(event);
			}
		});

		node.setOnRotate(new EventHandler<RotateEvent>() {

			@Override
			public void handle(RotateEvent event) {
				System.err.println(event);
			}
		});

		node.setOnScrollStarted(new EventHandler<ScrollEvent>() {

			@Override
			public void handle(ScrollEvent event) {
				System.err.println(event);
			}
		});

		node.setOnScrollFinished(new EventHandler<ScrollEvent>() {

			@Override
			public void handle(ScrollEvent event) {
				System.err.println(event);
			}
		});

		node.setOnZoomStarted(new EventHandler<ZoomEvent>() {

			@Override
			public void handle(ZoomEvent event) {
				System.err.println(event);
			}
		});

		node.setOnZoomFinished(new EventHandler<ZoomEvent>() {

			@Override
			public void handle(ZoomEvent event) {
				System.err.println(event);
			}
		});

		node.setOnRotationStarted(new EventHandler<RotateEvent>() {

			@Override
			public void handle(RotateEvent event) {
				System.err.println(event);
			}
		});

		node.setOnRotationFinished(new EventHandler<RotateEvent>() {

			@Override
			public void handle(RotateEvent event) {
				System.err.println(event);
			}
		});

		node.setOnMousePressed(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				System.err.println(event);
			}
		});

		node.setOnMouseReleased(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				System.err.println(event);
			}
		});

		node.setOnTouchPressed(new EventHandler<TouchEvent>() {

			@Override
			public void handle(TouchEvent event) {
				System.err.println("Touch pressed event");
			}
		});

		node.setOnTouchReleased(new EventHandler<TouchEvent>() {

			@Override
			public void handle(TouchEvent event) {
				System.err.println("Touch released event");
			}
		});

		node.setOnSwipeRight(new EventHandler<SwipeEvent>() {

			@Override
			public void handle(SwipeEvent event) {
				System.err.println("Swipe right event");
			}
		});

		node.setOnSwipeLeft(new EventHandler<SwipeEvent>() {

			@Override
			public void handle(SwipeEvent event) {
				System.err.println("Swipe left event");
			}
		});
	}

}
