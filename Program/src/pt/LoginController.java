package pt;

import javafx.event.ActionEvent;
import javafx.scene.control.*;

import java.io.IOException;

public class LoginController {

	public TextField idUsername;
	public PasswordField idPassword;
	
	public void onClickLogin(ActionEvent actionEvent) throws IOException, ClassNotFoundException {

		ClientMain instance = ClientMain.getInstance();
		UserInfo user = new UserInfo(idUsername.getText(),idPassword.getText());

		Command command = instance.sendCommandToServer(Constants.LOGIN,user);
		if(command.getProtocol().equals(Constants.LOGIN_SUCCESS)){
			//TODO ABRIR UMA NOVA JANELA
			System.out.println("Login feito");
		}else if(command.getProtocol().equals(Constants.LOGIN_ERROR)){
			//TODO CRIRAR errorLabel and show the error
			System.out.println(command.getExtras());
		}
	
	}
	
	
	public void onClickRegister(ActionEvent actionEvent) {
		ClientWindow instance = ClientWindow.getInstance();
		try {
			instance.setWindowRoot("RegistrationController.fxml");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
