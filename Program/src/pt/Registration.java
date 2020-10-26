package pt;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class Registration {


    public Label errorLabel;
    public TextField idUsername;
    public PasswordField idPassword;
    public PasswordField idRepeatPassword;
    public TextField idName;
    public TextField idPhotoPath;


    public void onRegister(ActionEvent actionEvent) {
        String username = idUsername.getText();
        String password = idPassword.getText();
        String repeatPwd = idRepeatPassword.getText();
        String name = idName.getText();
        String photoPath = idPhotoPath.getText();
        errorLabel.setText("Funciona");
    }
}
