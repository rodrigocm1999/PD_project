package pt.Server;

import pt.Common.UserInfo;

import java.util.ArrayList;
import java.util.List;

public class HttpAPI { // TODO
	
	private class User {
		public final String token;
		public final UserInfo user;
		
		public User(String token, UserInfo user) {
			this.token = token;
			this.user = user;
		}
	}
	
	private final ServerMain serverMain;
	private final List<User> authenticatedUsers;
	
	public HttpAPI(ServerMain serverMain) {
		this.serverMain = serverMain;
		authenticatedUsers = new ArrayList<>();
		// probably need to create some sort of socket
		// might give exception
	}
	
	private void login() {
		//Check username and password
		//Generate token
		//Put it in the response body
		// add to authenticated Users
	}
	
	// Everything that requires login must return 401 (Unauthorized) if there is no token
	// no header de request para mensagens entre outros é necessário vir o token no campo "Authorization"
	
	private void getLastNMessages() {
		if (checkAuthorized()) {
			//Obter asúltimas nmensagens  trocadas noservidor,  sendo  cada uma descrita  pelo remetente,
			// pelo destinatário(utilizador ou canal) e pela mensagem em si;
			
			//Get all messages that are for this user, limit the query to the N amount
			//Return JSON containing sender,receiver and content
		}
	}
	
	private void sendToConnected() {
		if (checkAuthorized()) {
			//Enviar  uma  mensagem  para  todos  os  utilizadores  que  estão  ligados  ao mesmo servidor.
			//for each connectedMachine -> change the destination
			//MessageManager.insertMessage();
			//serverMain.propagateNewMessage();
		}
	}
	
	private boolean checkAuthorized() {
		// Check if token is valid
		// if not respond with code 401
		return false;
	}
}
