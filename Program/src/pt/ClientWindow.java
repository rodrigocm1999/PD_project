package pt;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ClientWindow extends Application {

	private Scene scene;
	private Stage stage;

	private static ClientWindow instance;

	public static ClientWindow getInstance() {
		return instance;
	}


	@Override
	public void start(Stage primaryStage) throws Exception {
		stage = primaryStage;
		primaryStage.setTitle("");
		Parent root = loadParent("LoginPage.fxml");
		scene = new Scene(root, 600, 460);
		primaryStage.setScene(scene);
		primaryStage.show();

		if (instance != null) {
			throw new Exception("Server Already Running");
		}
		instance = this;
	}
	
	
	public void setWindowRoot(String fileName) throws IOException {
		scene.setRoot(loadParent(fileName));
	}
	
	private Parent loadParent(String fileName) throws IOException {
		return FXMLLoader.load(getClass().getResource(fileName));
	}
	
	public static void main(String[] args) {

		if (args.length != 2) {
			System.out.println("Invalid arguments: server_address, server_UDP_port");
		}
		String serverAddress = args[0];
		int port = Integer.parseInt(args[1]);
		

		try {

			ClientMain client = new ClientMain(serverAddress, port);
			if (client.run() > 0) {
				launch(args);
			} else {
				System.out.println("CHOULD NOT ACCESS THE SERVER. TRY AGAIN LATER");
				return;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
