package com.nitorcreations;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.Screen;

import org.wiigee.control.WiimoteWiigee;
import org.wiigee.device.Wiimote;
import org.wiigee.event.ButtonListener;
import org.wiigee.event.ButtonPressedEvent;
import org.wiigee.event.ButtonReleasedEvent;
import org.wiigee.event.GestureEvent;
import org.wiigee.event.GestureListener;
import org.wiigee.event.InfraredEvent;
import org.wiigee.event.InfraredListener;

public abstract class BaseController implements EventHandler<KeyEvent>, ButtonListener, GestureListener, InfraredListener{
	static Map<String, Set<String>> jarEntryCache = new HashMap<String, Set<String>>();

	ImageView[] slides;
	protected int index=0;
	public static double SCALE=8;
	protected double screenWidth;
	protected double screenHeight;
	public static final double SLIDE_WIDTH=200; 
	public static final double SLIDE_HEIGHT=150; 

	protected ImageView prevSlide=null;
	protected AnchorPane rootPane;
	protected Group slideGroup;
	protected boolean videoPlayed = false;
	protected PresentationHttpServer server;
	protected AtomicBoolean a_pressed = new AtomicBoolean(false);
	protected Path wiiTrail = new Path();
	protected Wiimote wiimote = null;
	protected boolean hasTitle = false;
	protected int[][] coordinates;

	protected double[] middle = new double[] {0, 0};

	protected double[] pointer = new double[] {0, 0};

	protected double lastdeltaX=0;

	protected double lastdeltaY=0;

	public void initialize(AnchorPane root) throws NumberFormatException, IOException, NoSuchAlgorithmException {
		rootPane = root;
		String slideDir="slides/";
		if (System.getProperty("slides") != null) {
			slideDir = System.getProperty("slides");
			if (!slideDir.endsWith("/")) {
				slideDir = slideDir + "/";
			}
		}
		List<String> slideNames = new ArrayList<String>(Arrays.asList(Utils.getResourceListing(slideDir)));
		Collections.sort(slideNames);
		if (slideNames.indexOf("title.png") > -1) {
			slideNames.remove(slideNames.indexOf("title.png"));
			hasTitle = true;
		}
		ArrayList<ImageView> slideList = new ArrayList<>();
		Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
		screenWidth = primaryScreenBounds.getMaxX();
		screenHeight = primaryScreenBounds.getMaxY();
		for (String next : slideNames) {
			if (next.isEmpty() || next.equals("/")) continue;
			if (next.endsWith(".video")) {
				ImageView nextView = slideList.get(slideList.size() -1);
				try (BufferedReader in = 
						new BufferedReader(new InputStreamReader(Utils.getResource(slideDir + next)))) {
					String video=in.readLine();
					in.readLine();
					String left = in.readLine();
					String top = in.readLine();
					String width = in.readLine();
					String heigth = in.readLine();
					File tmp = File.createTempFile(video, null);
					tmp.deleteOnExit();
					InputStream videoStream = Utils.getResource("html/" + video);
					Files.copy(videoStream, Paths.get(tmp.toURI()), StandardCopyOption.REPLACE_EXISTING);
					nextView.getProperties().put("video", tmp);
					nextView.getProperties().put("left", left);
					nextView.getProperties().put("top", top);
					nextView.getProperties().put("width", width);
					nextView.getProperties().put("height", heigth);
				} catch (IOException e) {
					e.printStackTrace();
				} 
			} else {
				ImageView nextView = new ImageView();
				nextView.setFitWidth(SLIDE_WIDTH);
				nextView.setFitHeight(SLIDE_HEIGHT);
				nextView.setPreserveRatio(true);
				nextView.setFocusTraversable(false);
				nextView.setLayoutX(screenWidth/2);
				nextView.setLayoutX(screenHeight/2);
				nextView.setImage(new Image(slideDir + next));
				slideList.add(nextView);
			}
		}
		if (hasTitle) {
			ImageView nextView = new ImageView();
			nextView.setFitWidth(SLIDE_WIDTH);
			nextView.setFitHeight(SLIDE_HEIGHT);
			nextView.setPreserveRatio(true);
			nextView.setFocusTraversable(false);
			nextView.setLayoutX(screenWidth/2);
			nextView.setLayoutX(screenHeight/2);
			nextView.setImage(new Image(slideDir + "title.png"));
			slideList.add(0, nextView);
		}
		slides = (ImageView[]) slideList.toArray(new ImageView[slideList.size()]);
		slideGroup = new Group(slides);
		slideGroup.getChildren().get(0).toFront();
		rootPane.setLayoutX(0);
		rootPane.setLayoutY(0);
		rootPane.setVisible(true);
		rootPane.setFocusTraversable(true);
		rootPane.getChildren().add(slideGroup);
		rootPane.setOnMouseMoved(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				double x = event.getX();
				double y = event.getY();
				if (!a_pressed.get()) {
					wiiTrail.getElements().clear();
					wiiTrail.getElements().add(new MoveTo(x, y));
					wiiTrail.getElements().add(new LineTo(x+1, y));
				} else {
					wiiTrail.getElements().add(new LineTo(x, y));
				}
				wiiTrail.toFront();
			}
		});

		SCALE = screenWidth / SLIDE_WIDTH;
		rootPane.setStyle("-fx-background-color: #ffffff;");
		for (ImageView next : slides) {
			next.setLayoutX(screenWidth/2 - SLIDE_WIDTH/2);
			next.setLayoutY(screenHeight/2 -SLIDE_HEIGHT/2);
		}

		if (System.getProperty("wiimote") != null) {
			initWiimote();
		}
		if (System.getProperty("httpport") != null) {
			server = new PresentationHttpServer(Integer.parseInt(System.getProperty("httpport")), this);
		}
	}

	public synchronized void handle(KeyEvent event) {
		handle(event.getCode());
	}

	public void handleLater(final KeyCode code) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				handle(code);
			}
		});
	}

	public synchronized void handle(KeyCode code) {
		boolean quick=false;
		switch (code) {
		case LEFT:
			index--;
			break;
		case RIGHT:
			index++;
			break;
		case UP:
			index++;
			quick=true;
			break;
		case DOWN:
			index--;
			quick=true;
			break;
		case A:
			index=0;
			break;
		case E:
			index = slides.length -1;
			break;
		default:
			return;
		}
		if (index < 0) {
			index = 0;
			tryVibrate(1000);
			return;
		} else if (index >= slides.length) {
			index = slides.length - 1;
			tryVibrate(1500);
			return;
		} else {
			tryVibrate(150);
		}
		showSlide(slides[index], quick);
	}
	public synchronized int slideCount() {
		return slides.length;
	}
	public synchronized int curentSlide() {
		return index;
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


	public abstract void showSlide(final ImageView slide, boolean quick);

	public static double getRotation(double angle) {
		if (angle > 180) {
			return 360 - angle;
		} else {
			return -angle;
		}
	}


	private void initWiimote() {
		try {
			WiimoteWiigee wiigee = new WiimoteWiigee();
			wiimote = wiigee.getDevice();
			wiigee.setTrainButton(Wiimote.BUTTON_1); 
			wiigee.setRecognitionButton(Wiimote.BUTTON_B);
			wiigee.setCloseGestureButton(Wiimote.BUTTON_HOME); 
			for (String nextGesture : new String[] {"right", "left", "up", "down", "circleCW", "circleCCW" }) {
				File tmp = File.createTempFile("gesture_" + nextGesture, ".txt");
				tmp.deleteOnExit();
				InputStream gestureStream = Utils.getResource("gestureset/" + nextGesture + ".txt") ;
				Files.copy(gestureStream , Paths.get(tmp.toURI()), StandardCopyOption.REPLACE_EXISTING);
				String gestureName = tmp.getAbsolutePath().substring(0, tmp.getAbsolutePath().length() - 4);
				wiimote.loadGesture(gestureName);
			}
			wiimote.fireButtonPressedEvent(Wiimote.BUTTON_HOME);
			wiimote.addButtonListener(this);
			wiimote.addGestureListener(this);
			wiimote.addInfraredListener(this);
			wiimote.setWiiMotionPlusEnabled(true);
			wiimote.setInfraredCameraEnabled(true);
			wiiTrail.setStroke(Color.KHAKI);
			wiiTrail.setOpacity(0.6);
			wiiTrail.setStrokeWidth(10);
			wiiTrail.setStrokeLineCap(StrokeLineCap.ROUND);
			tryVibrate(100);
			rootPane.getChildren().add(wiiTrail);
		} catch (Throwable e) {
			//Wiimote init failing is not fatal, but I'd like to know about it
			e.printStackTrace();
		}
	}

	@Override
	public void buttonPressReceived(ButtonPressedEvent event) {
		System.out.printf("Button pressed %d\n", event.getButton());
		switch (event.getButton()) {
		case 256:
			handleLater(KeyCode.LEFT);
			break;
		case 512:
			handleLater(KeyCode.RIGHT);
			break;
		case 2048:
			handleLater(KeyCode.UP);
			break;
		case 1024:
			handleLater(KeyCode.DOWN);
			break;
		case 16:
			handleLater(KeyCode.A);
			break;
		case 1:
			handleLater(KeyCode.E);
		case 8:
			a_pressed.set(true);
			break;
		default:
			break;
		}
	}

	@Override
	public void buttonReleaseReceived(ButtonReleasedEvent event) {
		System.out.printf("Button released %d\n", event.getButton());
		switch (event.getButton()) {
		case 8:
			a_pressed.set(false);
			break;
		default:
			break;
		}

	}

	@Override
	public void gestureReceived(GestureEvent event) {
		switch (event.getId()) {
		case 0:
			handleLater(KeyCode.RIGHT);
			break;
		case 1:
			handleLater(KeyCode.LEFT);
			break;
		case 2:
			handleLater(KeyCode.UP);
			break;
		case 3:
			handleLater(KeyCode.DOWN);
			break;
		case 4:
			handleLater(KeyCode.A);
			break;
		case 5:
			handleLater(KeyCode.E);
			break;
		}
	}

	private long eventCount=0;
	@Override
	public void infraredReceived(InfraredEvent event) {
		eventCount++;
		if ((eventCount % 5) != 0) {
			return;
		}
		this.coordinates = event.getCoordinates();

		int x1 = this.coordinates[0][0];
		int y1 = this.coordinates[0][1];
		int x2 = this.coordinates[1][0];
		int y2 = this.coordinates[1][1];


		// calculate pointing direction
		if(x1<1023 && x2<1023) {
			// middle in view, used for pointer calculation
			double dx = x2-x1;
			double dy = y2-y1;
			this.middle[0] = x1+(dx/2);
			this.middle[1] = y1+(dy/2);
			this.pointer[0] = 1024-this.middle[0];
			this.pointer[1] = 768-this.middle[1];

			this.lastdeltaX = dx;
			this.lastdeltaY = dy;
		} else if(x1<1023 && x2>=1023) {
			// middle not in view, P1 in view
			this.pointer[0] = 1024-x1-(int)(this.lastdeltaX*0.5);
			this.pointer[1] = 768-y1-(int)(this.lastdeltaY*0.5);
		} else if(x1>=1023 && x2<1023) {
			// middle not in view, P2 in view
			this.pointer[0] = 1024-x2+(int)(this.lastdeltaX*0.5);
			this.pointer[1] = 768-y2+(int)(this.lastdeltaY*0.5);
		}
		updateRobotMouse();
	}

	private void updateRobotMouse() {
		try {
			double x = pointer[0]* screenWidth/1024;
			double y = (768-pointer[1])*screenHeight/768;
			@SuppressWarnings("deprecation")
			final MouseEvent e = MouseEvent.impl_mouseEvent(x, y, x, y, MouseButton.NONE, 0, false, false, false, false, false, false, false, false, false, MouseEvent.MOUSE_MOVED);
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					MouseEvent.fireEvent(rootPane, e);
				}
			});
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	protected void tryVibrate(long millis) {
		if (wiimote != null) {
			try {
				wiimote.vibrateForTime(millis);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}

	}

	public void quit() {
		if (server != null) {
			server.quit();
		}
	}

}
