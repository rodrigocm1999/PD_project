package pt.Client;

import javafx.event.ActionEvent;
import javafx.scene.control.*;
import pt.Common.Command;
import pt.Common.Constants;
import pt.Common.UserInfo;

import java.io.IOException;

public class LoginController {
	
	public TextField idUsername;
	public PasswordField idPassword;
	
	public void onClickLogin(ActionEvent actionEvent) throws IOException, ClassNotFoundException {
		ClientMain instance = ClientMain.getInstance();
		UserInfo user = new UserInfo(idUsername.getText(), idPassword.getText());
		
		Command command = (Command) instance.sendCommandToServer(Constants.LOGIN, user);
		if (command.getProtocol().equals(Constants.LOGIN_SUCCESS)) {
			instance.setUserInfo((UserInfo) command.getExtras());
			ClientWindow.getInstance().setWindowRoot("Application.fxml");
		} else if (command.getProtocol().equals(Constants.LOGIN_ERROR)) {
			//TODO CRIRAR errorLabel and show the error
			System.out.println(command.getExtras());
		}
	}
	
	//TODO add go back to login page
	
	public void onClickRegister(ActionEvent actionEvent) throws IOException {
		ClientWindow.getInstance().setWindowRoot("Registration.fxml");
	}
}
