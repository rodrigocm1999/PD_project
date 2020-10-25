package pt;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    
    private Scene scene;
    private Stage stage;
    
    @Override
    public void start(Stage primaryStage) throws Exception{
        stage = primaryStage;
        primaryStage.setTitle("Unicord");
        Parent root = loadParent("sample.fxml");
        scene  = new Scene(root, 300, 275);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    
    public void setWindowRoot(String fileName) throws IOException{
        scene.setRoot(loadParent(fileName));
    }
    
    private Parent loadParent(String fileName) throws IOException {
        return FXMLLoader.load(getClass().getResource(fileName));
    }


    public static void main(String[] args) {
        launch(args);
    }
}
