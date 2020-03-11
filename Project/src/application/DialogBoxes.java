package application;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class DialogBoxes {

	public static void showMessageBox(String Title, String HeaderText, String ContextText) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle(Title);
				alert.setHeaderText(HeaderText);
				alert.setContentText(ContextText);
				alert.setGraphic(new ImageView(this.getClass().getResource("/Logo_icon_msg.png").toString()));
				Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
				stage.getIcons().add(new Image(this.getClass().getResource("/Logo_icon.png").toString()));
				alert.show();
			}
		});
	}

	public static void showErrorBox(String Title, String HeaderText, String ContextText) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle(Title);
				alert.setHeaderText(HeaderText);
				alert.setContentText(ContextText);
				alert.setGraphic(new ImageView(this.getClass().getResource("/Logo_icon_error.png").toString()));
				Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
				stage.getIcons().add(new Image(this.getClass().getResource("/Logo_icon.png").toString()));
				alert.show();
			}
		});
	}
}
