package pt.Client;

import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import pt.Common.Command;
import pt.Common.Constants;
import pt.Common.UserInfo;

import java.io.IOException;

import static java.lang.Thread.sleep;

public class RegistrationController {


    public Label errorLabel;
    public TextField idUsername;
    public PasswordField idPassword;
    public PasswordField idRepeatPassword;
    public TextField idName;
    public TextField idPhotoPath;


    public void onRegister(ActionEvent actionEvent) {

        errorLabel.setDisable(true);
        String username = idUsername.getText();
        String password = idPassword.getText();
        String repeatPwd = idRepeatPassword.getText();
        String name = idName.getText();
        String photoPath = idPhotoPath.getText();


        if (!password.equals(repeatPwd)) {
            errorLabel.setText("PASSWORD MUST BE THE SAME");
            return;
        }

        UserInfo user = new UserInfo(name, username, password, photoPath);
        ClientMain instance = ClientMain.getInstance();
        Command command;

        try {
            command = (Command) instance.sendCommandToServer(Constants.REGISTER,user);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }


        if (command.getProtocol().equals(Constants.REGISTER_SUCCESS)) {
            errorLabel.setText((String) command.getExtras());
            errorLabel.setTextFill(Color.web("green"));
            errorLabel.setDisable(false);

            try {
                sleep(1000);
                ClientWindow instanceWindow = ClientWindow.getInstance();
                instanceWindow.setWindowRoot("LoginPage.fxml");
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        } else if (command.getProtocol().equals(Constants.REGISTER_ERROR)) {
            errorLabel.setText((String) command.getExtras());
            errorLabel.setDisable(false);
        }
    }
}
