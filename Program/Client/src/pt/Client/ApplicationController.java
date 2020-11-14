package pt.Client;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import pt.Common.*;
import pt.Common.MessageInfo.Recipient;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ApplicationController implements Initializable {

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
	public TextField searchTextField;


	private final ObservableList<String> channelsObsList = FXCollections.observableArrayList();
	private final ObservableList<String> usersObsList = FXCollections.observableArrayList();
	private final ObservableList<String> msgsObsList = FXCollections.observableArrayList();

	private static ApplicationController instance;
	private ClientMain client;
	private Stage stage;

	public static ApplicationController get() {
		return instance;
	}


	@Override
	public void initialize(URL location, ResourceBundle resources) {
		instance = this;
		ClientWindow window = ClientWindow.getInstance();
		client = ClientMain.getInstance();

		window.getStage().setMaximized(true);
		String title =  "Welcome back ";
		title += client.getUserInfo().getName();
		window.getStage().setTitle(title);


		updateChannelsListView();
		channelsListView.setItems(channelsObsList);
		msgListView.setItems(msgsObsList);
		usersListView.setItems(usersObsList);

		searchUsersByUsername();

		channelsListView.setOnMouseClicked(event -> {
			channelsListViewOnClick();
		});
		usersListView.setOnMouseClicked(event -> {
			usersListViewOnClick();
		});

		receiveMessage();


		usersListView.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
			@Override
			public ListCell<String> call(ListView<String> param) {
				return new ListCell<String>() {
					@Override
					protected void updateItem(String item, boolean empty) {
						super.updateItem(item, empty);

						if (item == null || empty) {
							setText(null);
							setStyle("-fx-background-color: white;");
							setStyle("-fx-color: black;");
						} else {
							setText(item);
							if(getIndex() % 2 == 1){
								setStyle("-fx-background-color: white;");
								setStyle("-fx-color: black;");
							}
							else
								setStyle("-fx-background-color: white;");
								setStyle("-fx-color: black;");
						}
					}
				};
			}
		});
		Callback cellFactory = usersListView.getCellFactory();
		channelsListView.setCellFactory(cellFactory);
		channelsListView.setCellFactory(cellFactory);
	}


	public void updateChannelsListView() {
		channelsObsList.clear();
		try {
			ArrayList<ChannelInfo> list = client.getChannelsFromServer();
			client.setChannels(list);
			for (var item : list) {
				channelsObsList.add(item.getName());
			}
		} catch (IOException | ClassNotFoundException e) {
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
				if (pwd.isBlank()){
					return;
				}
				Command feedback = (Command) client.sendCommandToServer(Constants.CHANNEL_REGISTER, new ChannelInfo(channel.getId(), pwd));

				if (!feedback.getProtocol().equals(Constants.SUCCESS)) {
					alertError("Channel Password incorrect, try again");
					return;
				}
				channel.setPartOf(true);
			}
			ArrayList<MessageInfo> messages = client.getMessagesFromChannel(channel.getId());
			client.defineMessageTemplate(Recipient.CHANNEL, channel.getId());
			client.setMessages(messages);
			updateMessageListView();
		} catch (IOException | ClassNotFoundException e) {
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

	public void updateMessageListView() {
		msgsObsList.clear();
		for (MessageInfo item : client.getMessages()) {
			msgsObsList.add(item.getContent());
		}
	}

	public void addMessageToScreen(MessageInfo message) {
		msgsObsList.add(message.getContent());
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
		System.out.println(message);

		Thread thread = new Thread(() -> {
			try {
				Command command = (Command) client.sendCommandToServer(Constants.ADD_MESSAGE, message);

				if (command.getProtocol().equals(Constants.SUCCESS)) {

					Platform.runLater(() -> addMessageToScreen(message));
				}
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		});
		thread.start();

	}

	public void receiveMessage(){

		Thread thread = new Thread(() -> {
			while (true){
				try {

					Command command = (Command) client.receiveCommand();
					System.out.println(command);
					if (command.getProtocol().equals(Constants.NEW_MESSAGE)){
						MessageInfo message = (MessageInfo) command.getExtras();

						Platform.runLater(() -> addMessageToScreen(message) );
					}
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
					try {
						Thread.sleep(1000);
					} catch (InterruptedException interruptedException) {
						interruptedException.printStackTrace();
					}
				}
			}
		});

		thread.start();

	}

	public void onSendFile(ActionEvent actionEvent) throws IOException, ClassNotFoundException {
		FileChooser fileChooser = new FileChooser();
		File file = fileChooser.showOpenDialog(ClientWindow.getInstance().getStage());
		ClientMain.getInstance().sendFile(file);
	}

	public void onClickCreateChannel(ActionEvent actionEvent) {
		ClientWindow instance = ClientWindow.getInstance();
		try {
			Stage stage = new Stage();
			Parent parent = instance.loadParent("CreateChannel.fxml");
			stage.setScene(new Scene(parent, 500, 400));
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
		System.out.println(selectedItem);
		try {
			Command command = (Command) client.sendCommandToServer(Constants.USER_GET_MESSAGES, new Ids(user.getUserId(), 0, 0));
			client.defineMessageTemplate(Recipient.USER, user.getUserId());
			client.setMessages((ArrayList<MessageInfo>) command.getExtras());
			updateMessageListView();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void searchUserByKey(KeyEvent keyEvent) throws IOException, ClassNotFoundException {
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
		} catch (IOException | ClassNotFoundException e) {
		}
	}

	public void sendMessageByKey(KeyEvent keyEvent) {
		if (keyEvent.getCode() == KeyCode.ENTER) {
			onMessageSend(null);
		}
	}


}
