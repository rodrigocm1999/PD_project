package pt.Client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import pt.Common.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
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
	
	private final ObservableList<String> channelsObsList = FXCollections.observableArrayList();
	private final ObservableList<String> usersObsList = FXCollections.observableArrayList();
	private final ObservableList<String> msgsObsList = FXCollections.observableArrayList();
	
	private static ApplicationController instance;
	
	public static ApplicationController get() {
		return instance;
	}
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		instance = this;
		ClientWindow window = ClientWindow.getInstance();
		window.getStage().setMaximized(true);
		
		ClientMain client = ClientMain.getInstance();
		updateChannelsListView(client);
		channelsListView.setItems(channelsObsList);
		msgListView.setItems(msgsObsList);
		usersListView.setItems(usersObsList);
	}
	
	public void updateChannelsListView(ClientMain client) {
		channelsObsList.removeAll();
		try {
			Command command = (Command) client.sendCommandToServer(Constants.CHANNEL_GET_ALL, null);
			ArrayList<ChannelInfo> list = (ArrayList<ChannelInfo>) command.getExtras();
			client.setChannels(list);
			for (var item : list) {
				channelsObsList.add(item.getName());
			}
			channelsListView.setOnMouseClicked(event -> {
				try {
					channelsListViewOnClick();
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
				}
			});
			
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		
	}
	
	private void channelsListViewOnClick() throws IOException, ClassNotFoundException {
		ClientMain client = ClientMain.getInstance();
		String selectedItem = (String) channelsListView.getSelectionModel().getSelectedItem();
		ChannelInfo channel = client.getChannelByName(selectedItem);
		
		if (!channel.isPartOf()) {
			String pwd = openPasswordDialog(channel.getName());
			Command feedback = (Command) client.sendCommandToServer(Constants.CHANNEL_REGISTER, new ChannelInfo(channel.getId(), pwd));
			
			if (!feedback.getProtocol().equals(Constants.SUCCESS)) {
				alertError("Channel Password incorrect, try again");
				return;
			}
			channel.setPartOf(true);
		}
		ArrayList<MessageInfo> msgForChannel = client.getMessagesFromChannel(channel.getId());
		channel.setMessages(msgForChannel);
		updateMessageListView(msgForChannel);
		setCurrent(channel);
	}
	
	private void setCurrent(Object info) {
		ClientMain client = ClientMain.getInstance();
		if (info instanceof ChannelInfo) {
			client.setCurrentChannel((ChannelInfo) info);
			client.setCurrentUser(null);
		}
		if (info instanceof UserInfo) {
			client.setCurrentChannel(null);
			client.setCurrentUser((UserInfo) info);
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
		msgsObsList.clear();
		for (MessageInfo item : msgList) {
			msgsObsList.add(item.getContent());
		}
	}
	
	public void addMessage(MessageInfo msg, ChannelInfo channel) {
		ArrayList<MessageInfo> messages = channel.getMessages();
		messages.add(msg);
		msgsObsList.add(msg.getContent());
		updateMessageListView(messages);
	}
	
	public void onClickSend(ActionEvent actionEvent) {
		String messageText = msgTextField.getText();
		msgTextField.clear();
		if (!messageText.isBlank()) {
			ClientMain instance = ClientMain.getInstance();
			ChannelInfo currentChannel = instance.getCurrentChannel();
			MessageInfo messageInfo = new MessageInfo(MessageInfo.Recipient.CHANNEL, currentChannel.getId(), MessageInfo.TYPE_TEXT, messageText);
			Thread thread = new Thread(() -> {
				Command command;
				try {
					command = (Command) instance.sendCommandToServer(Constants.ADD_MESSAGE, messageInfo);
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
					return;
				}
				if (command.getProtocol().equals(Constants.SUCCESS)) {
					Platform.runLater(() -> addMessage(messageInfo, currentChannel));
				}
			});
			thread.start();
		}
	}
	
	public void onSendFile(ActionEvent actionEvent) throws IOException, ClassNotFoundException {
		FileChooser fileChooser = new FileChooser();
		File file = fileChooser.showOpenDialog(ClientWindow.getInstance().getStage());
		ClientMain.getInstance().sendFile(file);
	}
}
