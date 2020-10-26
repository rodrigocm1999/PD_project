package pt;

import javafx.event.ActionEvent;

import java.io.IOException;

public class Controller {
	
	public void onClickLogin(ActionEvent actionEvent) {
	
	}
	
	
	public void onClickRegister(ActionEvent actionEvent) {
		ClientWindow instance = ClientWindow.getInstance();
		try {
			instance.setWindowRoot("Registration.fxml");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
