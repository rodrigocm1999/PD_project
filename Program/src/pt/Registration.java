package pt;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

import java.io.IOException;

public class Registration {


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

        String[] answer = instance.userRegistration(user);

        if (answer[0].equals(Constants.REGISTER_SUCCESS)) {
            errorLabel.setText(answer[0]);
            errorLabel.setTextFill(Color.web("green"));
            errorLabel.setDisable(false);

            try {
                ClientWindow instanceWindow = ClientWindow.getInstance();
                instanceWindow.setWindowRoot("sample.fxml");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (answer[0].equals(Constants.REGISTER_ERROR)) {
            errorLabel.setText(answer[1]);
            errorLabel.setDisable(false);
        }
    }
}
