package pt;

import javafx.event.ActionEvent;
import javafx.scene.control.*;

import java.io.IOException;

public class Controller {

	public TextField idUsername;
	public PasswordField idPassword;
	
	public void onClickLogin(ActionEvent actionEvent) {

		ClientMain instance = ClientMain.getInstance();
		UserInfo user = new UserInfo(idUsername.getText(),idPassword.getText());

		String[] answer = instance.userLogin(user);
		if(answer[0].equals(Constants.LOGIN_SUCCESS)){
			//TODO ABRIR UMA NOVA JANELA
			System.out.println("Login feito");
		}else if(answer[0].equals(Constants.LOGIN_ERROR)){
			//TODO CRIRAR errorLabel and show the error
			System.out.println(answer[1]);
		}
	
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
