package pt.Client;

import javafx.fxml.Initializable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
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
    public TextField channelNameTextField;
    public Button editBtn;
    public PasswordField repeatPasswordField;
    public TextField newChannelNameField;


    public ChannelInfo channel = null;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        errorLabel.setText("");
    }

    public ChannelInfo getChannel() {
        return channel;
    }

    public void setChannel(ChannelInfo channel) {
        this.channel = channel;
    }

    public void onCreate(ActionEvent actionEvent) throws IOException, InterruptedException {
        String name = nameField.getText();
        ClientMain client = ClientMain.getInstance();
        ArrayList<ChannelInfo> list = client.getChannels();

        for (var item : list) {
            if (item.getName().equals(name)) {
                errorLabel.setText("This channel already exists");
                return;
            }
        }

        Command command = (Command) client.sendCommandToServer(Constants.CHANNEL_ADD, new ChannelInfo(name, passwordField.getText(), descriptionField.getText()));
        if (command.getProtocol().equals(Constants.SUCCESS)) {
            errorLabel.setText("Created successfully");
            errorLabel.setTextFill(Color.web("green"));
            btn.setDisable(true);
            btn.setVisible(false);
        } else {
            errorLabel.setText((String) command.getExtras());
        }
    }

    public void onDelete(ActionEvent actionEvent) throws IOException, InterruptedException {
        errorLabel.setText("");
        String name = nameField.getText();
        ClientMain client = ClientMain.getInstance();
        ArrayList<ChannelInfo> list = client.getChannels();
        String pwd = passwordField.getText();
        setChannel(null);

        for (var item : list) {
            if (item.getName().equals(name)) {
                setChannel(item);
            }
        }
        if (channel != null) {
            if (channel.getCreatorId() != client.getUserInfo().getUserId()) {
                errorLabel.setText("You are not the owner of this channel!");
                return;
            } else if (!channel.getPassword().equals(pwd)) {
                errorLabel.setText("Wrong Password!");
                return;
            }
        } else {
            errorLabel.setText("This does not exists!");
            return;
        }


        Command command = (Command) client.sendCommandToServer(Constants.CHANNEL_REMOVE, getChannel().getId());
        if (command.getProtocol().equals(Constants.SUCCESS)) {
            errorLabel.setText("Deleted Successfully");
            errorLabel.setTextFill(Color.web("green"));
            btn.setDisable(true);
            btn.setVisible(false);
        } else {
            errorLabel.setText((String) command.getExtras());
        }
    }

    public void findChannelOnClcik(ActionEvent actionEvent) {
        errorLabel.setText("");
        String channelName = channelNameTextField.getText();
        ClientMain client = ClientMain.getInstance();
        ArrayList<ChannelInfo> list = client.getChannels();
        channel = null;

        for (var item : list) {
            if (item.getName().equals(channelName)) {
                setChannel(item);
            }
        }
        if (channel != null) {
            if (channel.getCreatorId() != client.getUserInfo().getUserId()) {
                errorLabel.setText("You are not the owner of this channel!");
                return;
            } else {
                descriptionField.setText(channel.getDescription());
                newChannelNameField.setText(channel.getName());
            }
        } else {
            errorLabel.setText("This channel does not exist!");
            return;
        }
    }


    public void onEdit(ActionEvent actionEvent) throws IOException, InterruptedException {
        if (channel == null) {
            return;
        }
        String newDescrition = descriptionField.getText();
        String pwd = passwordField.getText();
        String repeatPwd = repeatPasswordField.getText();
        String newName = newChannelNameField.getText();


        if (!pwd.equals(repeatPwd)) {
            errorLabel.setText("Password needs to be the same");
            return;
        }
        ClientMain client = ClientMain.getInstance();

        channel.setDescription(newDescrition);
        channel.setPassword(pwd);
        channel.setName(newName);

        Command command = (Command) client.sendCommandToServer(Constants.CHANNEL_EDIT, channel);
        if (command.getProtocol().equals(Constants.SUCCESS)) {
            errorLabel.setText("Edited Successfully");
            errorLabel.setTextFill(Color.web("green"));
            editBtn.setDisable(true);
            editBtn.setVisible(false);
        } else {
            errorLabel.setText((String) command.getExtras());
        }


    }
}
