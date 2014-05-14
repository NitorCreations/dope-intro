package com.nitorcreations;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;

public class BumpAndFadeController extends BaseController{
	@Override
	public void initialize(AnchorPane root) throws NumberFormatException, IOException, NoSuchAlgorithmException {
		super.initialize(root);
		rootPane.setScaleX(SCALE);
		rootPane.setScaleY(SCALE);		
		for (ImageView nextView : slides) {
			nextView.setOpacity(0);
		}
		showSlide(0);
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
			TranslateTransition move = new TranslateTransition();
			move.setNode(rootPane);
			move.setByX(moveX);
			move.setByY(moveY);
			move.setDuration(Duration.seconds(1));
			ScaleTransition zoom = new ScaleTransition();
			zoom.setNode(rootPane);
			zoom.setToX(zoomTo);
			zoom.setToY(zoomTo);
			zoom.setDuration(Duration.seconds(1));
			SequentialTransition seq = new SequentialTransition(move, zoom);
			seq.setOnFinished(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					Utils.runVideo((File)prevSlide.getProperties().get("video"));
				}
			});
			seq.play();
			index--;
		} else {
			Duration DURATION_OUT = Duration.millis(500);
			slide.toFront();
			FadeTransition out = new FadeTransition();
			out.setDuration(DURATION_OUT);
			out.setFromValue(1);
			out.setToValue(0);
			out.setNode(prevSlide);
			out.setInterpolator(Interpolator.EASE_OUT);
			FadeTransition in = new FadeTransition();
			in.setDelay(DURATION_OUT);
			in.setDuration(Duration.millis(1200));
			in.setFromValue(0);
			in.setToValue(1);
			in.setNode(slide);
			in.setInterpolator(Interpolator.EASE_IN);
			ParallelTransition seq = new ParallelTransition(out, in);
			ScaleTransition scaleOut = new ScaleTransition();
			scaleOut.setNode(rootPane);
			scaleOut.setFromX(SCALE);
			scaleOut.setFromY(SCALE);
			scaleOut.setToX(25);
			scaleOut.setToY(25);
			scaleOut.setDuration(DURATION_OUT);
			scaleOut.setInterpolator(Interpolator.EASE_IN);
			ScaleTransition scaleIn = new ScaleTransition();
			scaleIn.setNode(rootPane);
			scaleIn.setFromX(25);
			scaleIn.setFromY(25);
			scaleIn.setToX(SCALE);
			scaleIn.setToY(SCALE);
			scaleIn.setDuration(Duration.millis(500));
			scaleIn.setInterpolator(Interpolator.EASE_OUT);
			SequentialTransition seq2 = new SequentialTransition(scaleOut, scaleIn);
			ParallelTransition par = new ParallelTransition(seq, seq2);
			par.play();

			new FinishAnimator(slide, new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					tryVibrate(200);
				}
			}).handle(null);
			prevSlide = slide;
			videoPlayed = false;
		} 
	}

}
