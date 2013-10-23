package com.nitorcreations;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

@SuppressWarnings("restriction")
public class Presentation extends Application{
    
    @Override
    public void start(Stage stage) throws Exception {
    	AnchorPane root = new AnchorPane();
    	Scene scene = new Scene(root);
    	final PresentationController controller = new PresentationController();
    	scene.setOnKeyPressed(controller);
    	controller.initialize(root);
        scene.setFill(Color.web("#a0a0a0"));
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			
			@Override
			public void handle(WindowEvent arg0) {
				controller.quit();
			}
		});
        stage.show();
    }
    

    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
