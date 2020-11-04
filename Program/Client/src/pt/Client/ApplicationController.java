package pt.Client;

import javafx.application.Application;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pt.Common.ChannelInfo;
import pt.Common.Command;
import pt.Common.Constants;

import java.io.IOException;
import java.lang.reflect.Array;
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
    public ListView channelsList;
    public ListView usersList;
    public ListView msgList;

    private ObservableList<String> channels = FXCollections.observableArrayList();
    private ObservableList<String> users = FXCollections.observableArrayList();
    private ObservableList<String> msgs = FXCollections.observableArrayList();


    @Override
    public void initialize(URL location, ResourceBundle resources) {


        ClientWindow instance = ClientWindow.getInstance();
        Stage stage = instance.getStage();
        stage.setMaximized(true);
        wWidth = (int)stage.getWidth();
        wHeight = (int)stage.getHeight();

        window.setPrefSize(wWidth,wHeight);

        bigSPane.setPrefSize(wWidth,wHeight-10);
        smallSPane.setMaxSize(wWidth*0.2,wHeight-10);

        channelsList.setItems(channels);
        ClientMain client = ClientMain.getInstance();
        try {
            Command command = (Command) client.sendCommandToServer(Constants.CHANNEL_GET_ALL,null);
            ArrayList<ChannelInfo> list = (ArrayList<ChannelInfo>) command.getExtras();
            for (var item:list) {
                channels.add(item.name);
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }



    }
}
