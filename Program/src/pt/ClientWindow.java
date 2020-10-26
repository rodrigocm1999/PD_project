package pt;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

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
		Parent root = loadParent("sample.fxml");
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
		String serverAddress = "rodrigohost.ddns.net";
		if (args.length == 0) {
			System.out.println("No server address on arguments\nUsing default");
		} else {
			serverAddress = args[0];
		}
		

		try {
			ClientMain client = new ClientMain(serverAddress, Constants.SERVER_PORT);
			if (client.run() > 0) {
				System.out.println("gtnreiiger");
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
