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

public class ClientMain extends Application {
	
	private Scene scene;
	private Stage stage;

	private String ipServer;
	private int port;
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		stage = primaryStage;
		primaryStage.setTitle("");
		Parent root = loadParent("sample.fxml");
		scene = new Scene(root, 300, 275);
		primaryStage.setScene(scene);
		primaryStage.show();
	}
	
	
	public void setWindowRoot(String fileName) throws IOException {
		scene.setRoot(loadParent(fileName));
	}
	
	private Parent loadParent(String fileName) throws IOException {
		return FXMLLoader.load(getClass().getResource(fileName));
	}

	public ClientMain(String ipServer, int port) {
		this.ipServer = ipServer;
		this.port = port;
	}

	public int run() throws IOException{
		DatagramSocket datagramSocket =  new DatagramSocket();

		byte[] buff =  ServerMain.ESTABLISH_CONNECTION.getBytes();
		InetAddress ip =InetAddress.getByName(ipServer);
		DatagramPacket datagramPacket =  new DatagramPacket(buff,buff.length,ip,port);

		datagramSocket.send(datagramPacket);

		datagramPacket =  new DatagramPacket(new byte[256],256);
		datagramSocket.receive(datagramPacket);

		String str = new String(datagramPacket.getData(),0,datagramPacket.getLength());
		System.out.println(str);


		if(str.equals(ServerMain.CONNECTION_ACCEPTED)){
			return 1;
		}else{

			return -1;
		}

 	}
	
	public static void main(String[] args) {

		ClientMain client =  new ClientMain("rodrigohost.ddns.net", 9321);
		try {
			if (client.run() > 0){
				launch(args);
			}else{
				System.out.println("CHOULD NOT ACCESS THE SERVER. TRY AGAIN LATER");
				return;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
