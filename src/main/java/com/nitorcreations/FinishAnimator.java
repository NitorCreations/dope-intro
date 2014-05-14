package com.nitorcreations;

import javafx.animation.Interpolator;
import javafx.animation.RotateTransitionBuilder;
import javafx.animation.ScaleTransitionBuilder;
import javafx.animation.TranslateTransitionBuilder;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

public class FinishAnimator implements EventHandler<ActionEvent> {

	private final ImageView slide;
	private final EventHandler<ActionEvent> onFinished;
	public FinishAnimator(ImageView slide, EventHandler<ActionEvent> onFinished) {
		this.slide = slide;
		this.onFinished = onFinished;
	}

	public void handle(ActionEvent t) {
		double parentRot = slide.getParent().getParent().getRotate();
		double parentScale = slide.getParent().getParent().getScaleX(); 
		if (parentScale != BumpAndFadeController.SCALE) {
			ScaleTransitionBuilder.create()
			        .node(slide.getParent().getParent())
			        .toX(BumpAndFadeController.SCALE)
			        .toY(BumpAndFadeController.SCALE)
			        .duration(Duration.millis(600))
			        .onFinished(this)
			        .build().play();
		} else if (parentRot != BumpAndFadeController.getRotation(slide.getRotate())) {
			RotateTransitionBuilder.create()
					.toAngle(BumpAndFadeController.getRotation(slide.getRotate()))
					.node(slide.getParent().getParent())
					.onFinished(this)
					.duration(Duration.millis(600)).build().play();
		} else {
			Bounds point = slide.localToScene(slide.getBoundsInLocal());
			double nowX = point.getMinX();   
			double nowY = point.getMinY();
			TranslateTransitionBuilder.create()
					.node(slide.getParent().getParent())
					.byX(-nowX)
					.byY(-nowY)
					.interpolator(Interpolator.EASE_OUT)
					.duration(Duration.millis(600))
					.onFinished(new EventHandler<ActionEvent>() {

						@Override
						public void handle(ActionEvent arg0) {
							slide.toFront();
							onFinished.handle(arg0);
						}
					})
					.build().play();
		}		

	}
}
