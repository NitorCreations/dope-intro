package com.nitorcreations;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ParallelTransitionBuilder;
import javafx.animation.RotateTransition;
import javafx.animation.RotateTransitionBuilder;
import javafx.animation.ScaleTransition;
import javafx.animation.ScaleTransitionBuilder;
import javafx.animation.SequentialTransition;
import javafx.animation.SequentialTransitionBuilder;
import javafx.animation.Transition;
import javafx.animation.TranslateTransition;
import javafx.animation.TranslateTransitionBuilder;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;

import org.wiigee.event.ButtonListener;
import org.wiigee.event.GestureListener;
import org.wiigee.event.InfraredListener;

public class SpinController extends BaseController implements EventHandler<KeyEvent>, ButtonListener, GestureListener, InfraredListener{
	static Map<String, Set<String>> jarEntryCache = new HashMap<String, Set<String>>();

	private Interpolator SLOW = new SlowingInterpolator();
	private Interpolator ACCELERATE = new AccelerateAndBumpInterpolator();
	private double OUT_SCALE=1;
	private final Duration outDuration=Duration.seconds(2);
	private final Duration inDuration=Duration.seconds(2);
	private final Duration moveDuration=Duration.seconds(0.7);

	private boolean started = false;
	private SequentialTransition start=null;
	private int startSlide=0;

	@Override
	public void initialize(AnchorPane root) throws NumberFormatException, IOException, NoSuchAlgorithmException {
		index=-1;
		super.initialize(root);
		rootPane.setStyle("-fx-background-color: #a0a0a0;");
		int paneSections = new Double(Math.ceil(Math.sqrt(slides.length))).intValue();
		rootPane.setPrefHeight(SlideLocation.SLIDE_SLOT_SIZE * paneSections);
		rootPane.setPrefWidth(SlideLocation.SLIDE_SLOT_SIZE * paneSections);
		rootPane.setMinHeight(SlideLocation.SLIDE_SLOT_SIZE * paneSections);
		rootPane.setMinWidth(SlideLocation.SLIDE_SLOT_SIZE * paneSections);

		OUT_SCALE = screenWidth / rootPane.getPrefWidth();
	     ArrayList<SlideLocation> locations = new ArrayList<>();
	     for (int i=0; i<paneSections;i++) {
	             for (int j=0; j<paneSections; j++) {
	                     locations.add(new SlideLocation(i, j));
	             }
	     }
		ArrayList<Transition> spreadTransitions = new ArrayList<>();
		for (ImageView next : slides) {
			next.setVisible(true);
			next.setRotate(Math.random() * 360);
			SlideLocation loc = locations.remove((int)(Math.random() * (double)locations.size()));
			double speedCoeff = 0.9 + (Math.random()/5);
			double duration = Math.sqrt(loc.coordX * loc.coordX + loc.coordY * loc.coordY) / (350 * speedCoeff);
			TranslateTransition slideMove = TranslateTransitionBuilder.create()
					.node(next).byX(loc.coordX-rootPane.getPrefWidth()/2)
					.byY(loc.coordY-rootPane.getPrefWidth()/2)
					.duration(Duration.seconds(duration))
					.interpolator(SLOW).build();
			RotateTransition slideRotate = RotateTransitionBuilder.create()
					.node(next).byAngle((Math.random() * 360) - 180).interpolator(SLOW)
					.duration(Duration.seconds(duration)).build();
			spreadTransitions.add(ParallelTransitionBuilder.create().children(slideMove, slideRotate).build());
		}
		rootPane.setScaleX(10);
		rootPane.setScaleY(10);
        if (hasTitle) {
        	startSlide = 1;
        	slides[0].setRotate(0);
        }

		ParallelTransition cardsSpread = ParallelTransitionBuilder.create().children(spreadTransitions).build();
		ScaleTransition cardsDrop = ScaleTransitionBuilder.create().node(rootPane)
				.toX(OUT_SCALE).toY(OUT_SCALE).duration(outDuration).interpolator(ACCELERATE).build();
		start = SequentialTransitionBuilder.create().children(cardsDrop, cardsSpread)
				.onFinished(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent arg0) {
						index=startSlide;
						showSlide(slides[index], false);
					}
				}).build();
		if (hasTitle) {
			showSlide(0, true);
		}
	}

	@Override
	public synchronized void handle(KeyCode code) {
		if (!started) {
			started = true;
			tryVibrate(150);
			start.play();
		} else {
			super.handle(code);
		}
	}

	
	public synchronized void showSlide(int index) {
		showSlide(index, false);
	}
	
	public synchronized void showSlide(final int index, final boolean quick) {
		if (index >= slides.length || index < 0) throw new ArrayIndexOutOfBoundsException(index);
		this.index = index;
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				showSlide(slides[index], quick);
			}
		});
	}
	
	@Override
	public void showSlide(final ImageView slide, boolean quick) {
		if (prevSlide != null && prevSlide.getProperties().get("video") != null && !videoPlayed) {
			videoPlayed = true;
			double left = Double.valueOf((String)prevSlide.getProperties().get("left"));
			double top = Double.valueOf((String)prevSlide.getProperties().get("top"));
			double width = Double.valueOf((String)prevSlide.getProperties().get("width"));
			double height = Double.valueOf((String)prevSlide.getProperties().get("height"));
			double zoomTo = screenWidth / (SLIDE_WIDTH * (width) / screenWidth);
			double moveX = ((screenWidth/2) - (left + (width/2))) * zoomTo / SCALE;
			double moveY = ((screenHeight/2) - (top + (height/2))) * zoomTo / SCALE;
			TranslateTransition move = TranslateTransitionBuilder.create()
					.node(rootPane)
					.byX(moveX)
					.byY(moveY)
					.duration(Duration.seconds(1))
					.build();
			ScaleTransition zoom = ScaleTransitionBuilder.create()
					.node(rootPane)
					.toX(zoomTo)
					.toY(zoomTo)
					.duration(Duration.seconds(1))
					.build();
			SequentialTransitionBuilder.create().children(move, zoom).onFinished(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					Utils.runVideo((File)prevSlide.getProperties().get("video"));
				}
			}).build().play();
			index--;
		} else {
			if (!quick || slide.getProperties().get("video") != null) {
				Duration outD;
				if (rootPane.getScaleX() > OUT_SCALE) {
					outD = outDuration;
				} else {
					outD = Duration.millis(100);
				}
				ScaleTransition scaleOut = ScaleTransitionBuilder.create().node(rootPane)
						.toX(OUT_SCALE).toY(OUT_SCALE)
						.interpolator(Interpolator.EASE_IN)
						.duration(outD).build();
				TranslateTransition reset = TranslateTransitionBuilder.create()
						.node(rootPane)
						.duration(outD)
						.interpolator(Interpolator.EASE_IN)
						.toX(0)
						.toY(0)
						.build();
				Transition out;
				if (prevSlide != null) {
					RotateTransition outRot = RotateTransitionBuilder.create()
							.node(rootPane)
							.toAngle(0)
							.duration(outD)
							.interpolator(Interpolator.EASE_BOTH)
							.build();
					out = ParallelTransitionBuilder.create().children(scaleOut, outRot, reset)
							.build();
				} else {
					out = ParallelTransitionBuilder.create().children(scaleOut, reset)
							.build();
				}

				out.setOnFinished(new EventHandler<ActionEvent>() {

					public void handle(ActionEvent t) {
						Bounds point = slide.localToScene(slide.getBoundsInLocal());
						double slideCenterX = (point.getMinX() + point.getMaxX())/2;   
						double slideCenterY = (point.getMinY() + point.getMaxY())/2;

						TranslateTransition move = TranslateTransitionBuilder.create()
								.node(slideGroup)
								.byX(((screenWidth / 2) - slideCenterX) / OUT_SCALE)
								.byY(((screenHeight / 2) - slideCenterY) / OUT_SCALE)
								.duration(moveDuration)
								.interpolator(Interpolator.EASE_BOTH)
								.build();

						ScaleTransition scale = ScaleTransitionBuilder.create()
								.node(rootPane)
								.toX(SCALE).toY(SCALE)
								.duration(inDuration)
								.interpolator(Interpolator.EASE_OUT)
								.build();

						RotateTransition inRot = RotateTransitionBuilder.create()
								.node(rootPane)
								.toAngle(getRotation(slide.getRotate()))
								.duration(inDuration)
								.interpolator(Interpolator.EASE_OUT)
								.build();

						ParallelTransition scaleIn = ParallelTransitionBuilder.create()
								.children(scale, inRot).build();
						SequentialTransition in = SequentialTransitionBuilder.create()
								.children(move, scaleIn)
								.onFinished(new FinishAnimator(slide, new EventHandler<ActionEvent>() {
									@Override
									public void handle(ActionEvent arg0) {
										tryVibrate(200);
									}
								})).build();
						in.play();
					}
				});
				out.play();
			} else {
				new FinishAnimator(slide, new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent arg0) {
						tryVibrate(200);
					}
				}).handle(null);
			}
			prevSlide = slide;
			videoPlayed = false;
		} 
	}

	public static double getRotation(double angle) {
		if (angle > 180) {
			return 360 - angle;
		} else {
			return -angle;
		}
	}

	private class SlowingInterpolator extends Interpolator {
		@Override
		protected double curve(double d) {
			return 1-((1-d)*(1-d));
		}

	}
	private class AccelerateAndBumpInterpolator extends Interpolator  {
		@Override
		protected double curve(double d) {
			return d<0.9 ? (d * d)+(0.21111*d) : 10 * (d-0.95) * (d-0.95) + 0.975;
		}
	}
}
