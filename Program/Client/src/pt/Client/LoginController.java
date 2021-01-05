package pt.Client;

import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import pt.Common.Command;
import pt.Common.Constants;
import pt.Common.UserInfo;

import java.io.IOException;

public class LoginController {

    public TextField idUsername;
    public PasswordField idPassword;
    public Label errorLabel;

    public void onClickLogin(ActionEvent actionEvent) {
        UserInfo user = new UserInfo(idUsername.getText(), idPassword.getText());

        if (user.getUsername().isBlank()) {
            errorLabel.setText("Username cannot be empty");
            return;
        }
        if (user.getPassword().isBlank()) {
            errorLabel.setText("Password cannot be empty");
            return;
        }
        onLoginUser(user);
    }

    public void onClickLoginTestUser(ActionEvent actionEvent) {
        UserInfo user = new UserInfo("testuser", "Testuser12345");
        onLoginUser(user);
    }

    public void onLoginUser(UserInfo user) {
        try {
            ClientMain instance = ClientMain.getInstance();
            Command command = (Command) instance.sendCommandToServer(Constants.LOGIN, user);
            if (command.getProtocol().equals(Constants.LOGIN_SUCCESS)) {
                UserInfo thisMan = (UserInfo) command.getExtras();
                thisMan.setPassword(user.getPassword());
                instance.setUserInfo(thisMan);
                ClientWindow.getInstance().setWindowRoot("Application.fxml");
            } else {
                errorLabel.setText((String) command.getExtras());
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void onClickRegister(ActionEvent actionEvent) throws IOException {
        ClientWindow.getInstance().setWindowRoot("Registration.fxml");
    }

    public void keyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            onClickLogin(null);
        }
    }
}
