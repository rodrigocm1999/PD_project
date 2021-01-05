package pt.Client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import pt.Common.*;
import pt.Common.MessageInfo.Recipient;

import javax.swing.text.html.ImageView;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class ApplicationController implements Initializable {

    @FXML
    public VBox window;
    public SplitPane smallSPane;
    public ListView channelsListView;
    public ListView usersListView;
    public Button btnSend;
    public TextField msgTextField;
    public TextField searchTextField;
    public VBox vBoxMessage;
    public ScrollPane scrollPane;
    public Label titleLabel;
    //public ImageView userImage;


    private final ObservableList<String> channelsObsList = FXCollections.observableArrayList();
    private final ObservableList<String> usersObsList = FXCollections.observableArrayList();


    private static ApplicationController instance;
    private ClientMain client;
    private Stage stage;
    private int lastSelectedItem;

    public static ApplicationController get() {
        return instance;
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance = this;
        ClientWindow window = ClientWindow.getInstance();
        client = ClientMain.getInstance();

        window.getStage().setMaximized(true);
        String title = "Welcome back ";
        title += client.getUserInfo().getName();
        window.getStage().setTitle(title);


        updateChannelsListView();
        channelsListView.setItems(channelsObsList);
        //msgListView.setItems(msgsObsList);
        usersListView.setItems(usersObsList);

        searchUsersByUsername();

        channelsListView.setOnMouseClicked(event -> {
            channelsListViewOnClick();
        });
        usersListView.setOnMouseClicked(event -> {
            usersListViewOnClick();
        });

        getUpdates();
        window.getStage().widthProperty().addListener((observable, oldValue, newValue) -> {
            vBoxMessage.setPrefWidth(scrollPane.getWidth());
        });
        window.getStage().heightProperty().addListener((observable, oldValue, newValue) -> {
            vBoxMessage.setPrefHeight(scrollPane.getHeight());
        });


        vBoxMessage.heightProperty().addListener((observable, oldValue, newValue) -> {
            scrollPane.setVvalue(1.0d);
        });


    }


    public void updateChannelsListView() {
        channelsObsList.clear();
        try {
            ArrayList<ChannelInfo> list = client.getChannelsFromServer();
            client.setChannels(list);
            for (var item : list) {
                channelsObsList.add(item.getName());
            }
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void channelsListViewOnClick() {
        try {
            String selectedItem = (String) channelsListView.getSelectionModel().getSelectedItem();
            if (selectedItem == null) return;
            ChannelInfo channel = client.getChannelByName(selectedItem);

            if (channel != null && !channel.isPartOf()) {
                String pwd = openPasswordDialog(channel.getName());
                if (pwd.isBlank()) {
                    channelsListView.getSelectionModel().select(lastSelectedItem);
                    return;
                }
                Command feedback = (Command) client.sendCommandToServer(Constants.CHANNEL_REGISTER, new ChannelInfo(channel.getId(), pwd));

                if (!feedback.getProtocol().equals(Constants.SUCCESS)) {
                    alertError("Channel Password incorrect, try again");
                    channelsListView.getSelectionModel().select(lastSelectedItem);
                    return;
                }
                channel.setPartOf(true);
            }
            ArrayList<MessageInfo> messages = client.getMessagesFromChannel(channel.getId());
            client.defineMessageTemplate(Recipient.CHANNEL, channel.getId());
            titleLabel.setText(channel.getDescription());
            titleLabel.setPrefWidth(window.getWidth() - 10);
            client.setMessages(messages);
            updateMessageVBox();

            lastSelectedItem = channelsListView.getSelectionModel().getSelectedIndex();
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
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

    private String openExitDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Exit Channel");
        dialog.setHeaderText(null);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField name = new TextField();
        name.setPromptText("Password");
        grid.add(new Label("Channel Name:"), 0, 1);
        grid.add(name, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.showAndWait();
        return name.getText();
    }

    public void alertError(String error) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(" ERROR ");
        alert.setHeaderText(null);
        alert.setContentText(error);

        alert.showAndWait();
    }

    public void updateMessageVBox() {
        client.getMessagesRecipientType();

        vBoxMessage.getChildren().clear();
        vBoxMessage.setPrefWidth(scrollPane.getWidth());
        vBoxMessage.setPrefHeight(scrollPane.getHeight());
        for (MessageInfo item : client.getMessages()) {

            vBoxMessage.getChildren().add(insertLine(item));
        }

    }

    public HBox insertLine(MessageInfo message) {
        HBox box = new HBox();
        box.setPrefWidth(Region.USE_COMPUTED_SIZE);
        box.setFillHeight(true);

        Label label = new Label(message.getContent());
        if (client.getUserInfo().getUserId() != message.getSenderId()) {
            if (message.getRecipientType().equals(Recipient.CHANNEL)) {
                Label usernameLabel = new Label(message.getSenderUsername() + ": ");
                usernameLabel.setTextFill(Color.web("#7D82B8"));
                box.getChildren().add(usernameLabel);
            }
            box.setAlignment(Pos.BASELINE_LEFT);
        } else {
            box.setAlignment(Pos.BASELINE_RIGHT);
        }

        if (message.getType().equals(MessageInfo.TYPE_FILE)) {
            label.setTextFill(Color.color(0, 0, 1));
            label.setOnMouseClicked(event -> {
                DirectoryChooser directoryChooser = new DirectoryChooser();
                File fileDirectory = directoryChooser.showDialog(ClientWindow.getInstance().getStage());
                if (fileDirectory == null) {
                    return;
                }
                try {
                    client.downloadFile(message, fileDirectory.getAbsolutePath());
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }


        box.getChildren().add(label);
        return box;
    }

    public void addMessageToScreen(MessageInfo message) {
        vBoxMessage.getChildren().add(insertLine(message));
    }

    public void onMessageSend(ActionEvent actionEvent) {
        String messageText = msgTextField.getText();
        msgTextField.clear();
        if (client.getMessages() == null || messageText.isBlank()) {
            return;
        }
        int recipientId = client.getMessagesRecipientId();
        Recipient recipientType = client.getMessagesRecipientType();
        MessageInfo message = new MessageInfo(recipientType, recipientId, MessageInfo.TYPE_TEXT, messageText);
        message.setSenderId(client.getUserInfo().getUserId());
        System.out.println(message);

        Thread thread = new Thread(() -> {
            try {
                Command command = (Command) client.sendCommandToServer(Constants.ADD_MESSAGE, message);

                if (command.getProtocol().equals(Constants.SUCCESS)) {

                    Platform.runLater(() -> addMessageToScreen(message));
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread.start();

    }

    public void getUpdates() {
        new Thread(() -> {
            while (true) {
                try {

                    Command command = (Command) client.receiveCommand();
                    System.out.println("update thread received : " + command);
                    switch (command.getProtocol()) {
                        case Constants.NEW_MESSAGE -> {
                            MessageInfo message = (MessageInfo) command.getExtras();

                            Platform.runLater(() -> addMessageToScreen(message));
                        }
                        case Constants.NEW_USER -> {
                            UserInfo user = (UserInfo) command.getExtras();

                            Platform.runLater(() -> {
                                usersObsList.add(user.getUsername());
                            });
                        }
                        case Constants.NEW_CHANNEL -> {
                            Platform.runLater(() -> {
                                updateChannelsListView();
                            });
                        }

                        case Constants.SERVERS_LIST -> {
                            ArrayList<ServerAddress> newServerList = (ArrayList<ServerAddress>) command.getExtras();
                            System.out.println("Recived server list: " + newServerList);
                            client.setServersList(newServerList);
                        }
                    }
                } catch (InterruptedException e) {
                    try {
                        System.out.println("Interruped connecting again");
                        client.connectToServer();
                        String connectedServer = client.getServerIPAddress() + ":" + client.getPortUDPServer();
                        Platform.runLater(() -> ClientWindow.getInstance().getStage().setTitle(connectedServer));
                        client.sendCommandToServer(Constants.LOGIN, client.getUserInfo());
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void onSendFile(ActionEvent actionEvent) throws IOException, InterruptedException {
        if (client.getMessagesRecipientType() == null)
            return;
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(ClientWindow.getInstance().getStage());
        if (file != null) {
            MessageInfo message = new MessageInfo(MessageInfo.TYPE_TEXT, "A enviar Ficheiro: " + file.getName());
            message.setRecipientType(client.getMessagesRecipientType());
            message.setSenderId(client.getUserInfo().getUserId());
            addMessageToScreen(message);


            ClientMain.getInstance().sendFile(file);

        }
    }

    public void onClickCreateChannel(ActionEvent actionEvent) {
        ClientWindow instance = ClientWindow.getInstance();
        try {
            Stage stage = new Stage();
            Parent parent = instance.loadParent("CreateChannel.fxml");
            stage.setScene(new Scene(parent, 500, 400));
            stage.setTitle("Channel Creation");
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
        updateChannelsListView();
    }

    public void onClickDeleteChannel(ActionEvent actionEvent) {
        ClientWindow instance = ClientWindow.getInstance();
        try {
            Stage stage = new Stage();
            Parent parent = instance.loadParent("DeleteChannel.fxml");
            stage.setScene(new Scene(parent, 500, 400));
            stage.setTitle("Channel Delete");
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
        updateChannelsListView();
    }

    public void updateUsersListView(ArrayList<UserInfo> users) {
        usersObsList.clear();
        for (var item : users) {
            usersObsList.add(item.getUsername());
        }
    }

    private void usersListViewOnClick() {
        String selectedItem = (String) usersListView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) return;
        UserInfo user = client.getUserByUsername(selectedItem);
        titleLabel.setText(user.getName());
        try {
            Command command = (Command) client.sendCommandToServer(Constants.USER_GET_MESSAGES, new Ids(user.getUserId(), 0, 0));
            client.defineMessageTemplate(Recipient.USER, user.getUserId());
            client.setMessages((ArrayList<MessageInfo>) command.getExtras());
            updateMessageVBox();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void searchUserByKey(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            searchUsersByUsername();
        }
    }

    private void searchUsersByUsername() {
        try {
            Command command = (Command) client.sendCommandToServer(Constants.USER_GET_LIKE, searchTextField.getText());
            ArrayList<UserInfo> usersFound = (ArrayList<UserInfo>) command.getExtras();
            client.setUsers(usersFound);
            System.out.println(client.getUsers());
            updateUsersListView(usersFound);
        } catch (IOException | InterruptedException e) {
        }
    }

    public void sendMessageByKey(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            onMessageSend(null);
        }
    }

    public void logout(ActionEvent actionEvent) throws InterruptedException, IOException, ClassNotFoundException {
        if (client.logout()) {
            ClientWindow.getInstance().setWindowRoot("LoginPage.fxml");
        }
    }

    public void onClickEditChannel(ActionEvent actionEvent) {
        ClientWindow instance = ClientWindow.getInstance();
        try {
            Stage stage = new Stage();
            Parent parent = instance.loadParent("EditChannel.fxml");
            stage.setScene(new Scene(parent, 500, 400));
            stage.setTitle("Edit Channel");
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onClickExitChannel(ActionEvent actionEvent) throws IOException, InterruptedException {
        String channelName = openExitDialog();
        ChannelInfo channel = null;
        for (var item : client.getChannels()) {
            if (channelName.equals(item.getName())) {
                channel = item;
            }
        }
        if (channel == null) {
            alertError("This channel doesn't exit!");
            return;
        }
        Command command = (Command) client.sendCommandToServer(Constants.CHANNEL_LEAVE, channel.getId());
        if (!command.getProtocol().equals(Constants.SUCCESS)) {
            alertError((String) command.getExtras());
        } else {
            channel.setPartOf(false);
        }


    }
}
