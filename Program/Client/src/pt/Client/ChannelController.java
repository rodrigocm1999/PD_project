package pt.Client;

import javafx.fxml.Initializable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import pt.Common.ChannelInfo;
import pt.Common.Command;
import pt.Common.Constants;
import pt.Common.UserInfo;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class ChannelController implements Initializable {

    public TextField nameField;
    public PasswordField passwordField;
    public TextArea descriptionField;
    public Button btn;
    public Label errorLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        errorLabel.setText("");
    }

    public void onCreate(ActionEvent actionEvent) throws IOException, ClassNotFoundException {
        String name = nameField.getText();
        ClientMain instance = ClientMain.getInstance();
        ArrayList<ChannelInfo> list = instance.getChannels();

        for (var item: list) {
            if (item.getName().equals(name)) {
                errorLabel.setText("This channel already exists");
                return;
            }
        }
    //TODO check if atualiza lista de canais
        Command command = (Command) instance.sendCommandToServer(Constants.CHANNEL_ADD, new ChannelInfo(name, passwordField.getText(), descriptionField.getText()));
        if (command.getProtocol().equals(Constants.SUCCESS)){
            errorLabel.setText("Created successfully");
            errorLabel.setTextFill(Color.web("green"));
            btn.setDisable(true);
            btn.setVisible(false);
        }else {
            errorLabel.setText((String) command.getExtras());
        }
    }

    public void onDelete(ActionEvent actionEvent) throws IOException, ClassNotFoundException {
        String name = nameField.getText();
        ClientMain instance = ClientMain.getInstance();
        ArrayList<ChannelInfo> list = instance.getChannels();
        ChannelInfo channel = null;

        for (var item: list) {
            if (!item.getName().equals(name)) {
                errorLabel.setText("This does not exists!");
            }else {
                channel = item;
            }
        }

        Command command = (Command) instance.sendCommandToServer(Constants.CHANNEL_REMOVE, channel.getId());
        if (command.getProtocol().equals(Constants.SUCCESS)){
            errorLabel.setText("Deleted Successfully");
            errorLabel.setTextFill(Color.web("green"));
            btn.setDisable(true);
            btn.setVisible(false);
        }else {
            errorLabel.setText((String) command.getExtras());
        }
    }
}
