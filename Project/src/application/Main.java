package application;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;

public class Main extends Application {

	@Override
	public void start(Stage primaryStage) {

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/userInterface/WordWindow.fxml"));
		Parent root1 = null;
		try {
			root1 = (Parent) fxmlLoader.load();
		} catch (IOException e) {
			DialogBoxes.showErrorBox("Error detected!", e.getMessage(), e.getLocalizedMessage());
			e.printStackTrace();
		}
		Stage stage = new Stage();
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.initStyle(StageStyle.DECORATED);
		stage.setTitle("Synthekst");                
		stage.setMaximized(true);
		stage.setScene(new Scene(root1));
		stage.getIcons().add(new Image(getClass().getResourceAsStream("/Logo_icon.png")));
		
		stage.show();
	}

	public static void main(String[] args) {
		launch(args);
	}

}
