package pt.Server;

import pt.Common.UserInfo;

import java.util.HashMap;
import java.util.Map;

public class HttpAPI { // TODO
	
	private final ServerMain serverMain;
	private final Map<String, UserInfo> authenticatedUsers;
	
	public HttpAPI(ServerMain serverMain) {
		this.serverMain = serverMain;
		authenticatedUsers = new HashMap<>();
		// probably need to create some sort of socket
		// might give exception
	}
	
	//GetMapping("/login")
	public String login(String username, String password) {
		//Check username and password
		//Generate token
		//Put it in the response body
		// add to authenticated Users
		// authenticatedUsers.put(token,user);
		// return token;
		return null;
	}
	
	// Everything that requires login must return 401 (Unauthorized) if there is no token
	// no header de request para mensagens entre outros é necessário vir o token no campo "Authorization"
	
	//GetMapping("/getMessages")
	public void getLastNMessages(int amount) {
		if (checkAuthorized()) {
			//Obter asúltimas nmensagens  trocadas noservidor,  sendo  cada uma descrita  pelo remetente,
			// pelo destinatário(utilizador ou canal) e pela mensagem em si;
			
			//Get all messages that are for this user, limit the query to the N amount
			//Return JSON containing sender,receiver and content
		}
	}
	
	//GetMapping("/send")
	public void sendToConnected(String messageContent) {
		if (checkAuthorized()) {
			//Enviar  uma  mensagem  para  todos  os  utilizadores  que  estão  ligados  ao mesmo servidor.
			//for each connectedMachine -> change the destination
			//MessageManager.insertMessage();
			//serverMain.propagateNewMessage();
		}
	}
	
	public boolean checkAuthorized() {
		// Check if token is valid
		// if not respond with code 401
		//UserInfo user = authenticatedUsers.get(token);
		// if(user == null) { //Send unauthorized  }
		return false;
	}
}
