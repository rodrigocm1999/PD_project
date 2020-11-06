package pt.Client;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pt.Common.ChannelInfo;
import pt.Common.Command;
import pt.Common.Constants;
import pt.Common.MessageInfo;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class ApplicationController implements Initializable {

    int wHeight;
    int wWidth;

    @FXML
    public VBox window;
    public SplitPane bigSPane;
    public SplitPane smallSPane;
    public ListView channelsListView;
    public ListView usersListView;
    public ListView msgListView;
    public GridPane msgPane;
    public Button btnSend;
    public TextField msgTextField;

    private ObservableList<String> channels = FXCollections.observableArrayList();
    private ObservableList<String> users = FXCollections.observableArrayList();
    private ObservableList<String> msgs = FXCollections.observableArrayList();


    @Override
    public void initialize(URL location, ResourceBundle resources) {


        ClientWindow instance = ClientWindow.getInstance();
        Stage stage = instance.getStage();
        stage.setMaximized(true);
        wWidth = (int) stage.getWidth();
        wHeight = (int) stage.getHeight();


        ClientMain client = ClientMain.getInstance();
        updateChannelsListView(client);
        channelsListView.setItems(channels);
        msgListView.setItems(msgs);

    }

    public void updateChannelsListView(ClientMain client) {
        channels.removeAll();
        try {
            Command command = (Command) client.sendCommandToServer(Constants.CHANNEL_GET_ALL, null);
            ArrayList<ChannelInfo> list = (ArrayList<ChannelInfo>) command.getExtras();
            client.setChannels(list);
            for (var item : list) {
                channels.add(item.getName());
            }
            channelsListView.setOnMouseClicked(event -> {
                String selectedItem = (String) channelsListView.getSelectionModel().getSelectedItem();
                ChannelInfo channel = client.getChannelByName(selectedItem);
                if (!channel.isPartOf()) {
                    String pwd = openPasswordDialog(channel.getName());
                    try {
                        Command feedback = (Command) client.sendCommandToServer(Constants.CHANNEL_REGISTER, new ChannelInfo(channel.getId(), pwd));
                        if (!feedback.getProtocol().equals(Constants.SUCCESS)) {
                            alertError("Channel Passsword incorret, try again");
                            return;
                        }
                        channel.setPartOf(true);
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    ArrayList<MessageInfo> msgForChannel = client.getMessagesFromChannel(channel.getId());
                    updateMessageListView(msgForChannel);
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            });

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    private String openPasswordDialog(String channelName) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(channelName);
        dialog.setHeaderText(null);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        grid.add(new Label("Channel Password:"), 0, 1);
        grid.add(password, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.showAndWait();
        return password.getText();
    }

    public void alertError(String error) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(" ERROR ");
        alert.setHeaderText(null);
        alert.setContentText(error);

        alert.showAndWait();
    }

    public void updateMessageListView(ArrayList<MessageInfo> msgList) {
        msgs.clear();
        System.out.println(msgList.size());
        for (MessageInfo item : msgList) {
            msgs.add(item.getContent());
        }
    }


    public void onClickSend(ActionEvent actionEvent) {
        msgTextField.getText();


    }
}
