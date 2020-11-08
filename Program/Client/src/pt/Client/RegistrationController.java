package pt.Client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import pt.Common.Command;
import pt.Common.Constants;
import pt.Common.UserInfo;
import pt.Common.Utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static java.lang.Thread.sleep;

public class RegistrationController {

    @FXML
    public Label errorLabel;
    public TextField idUsername;
    public PasswordField idPassword;
    public PasswordField idRepeatPassword;
    public TextField idName;
    public TextField idPhotoPath;
    public Button btnChoseImage;


    public void onRegister(ActionEvent actionEvent) throws IOException {

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

        ClientMain instance = ClientMain.getInstance();
        File file = instance.getUserPhoto();
        BufferedImage buffImg = ImageIO.read(file);
        BufferedImage compressedImage = Utils.getCompressedImage(buffImg, Constants.USER_IMAGE_SIZE, Constants.USER_IMAGE_SIZE);
        byte[] imageBytes = Utils.getImageBytes(compressedImage);
        UserInfo user = new UserInfo(name, username, password,imageBytes);
        Command command;
        try {
            command = (Command) instance.sendCommandToServer(Constants.REGISTER, user);
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

    public void onChoseImg(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        var filters = new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg");
        fileChooser.getExtensionFilters().add(filters);
        File file = fileChooser.showOpenDialog(ClientWindow.getInstance().getStage());
        ClientMain.getInstance().setUserPhoto(file);
        idPhotoPath.setText(file.getAbsolutePath());
    }
}
