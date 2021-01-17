package pt.Server.RestAPI;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.bind.annotation.*;
import pt.Common.MessageInfo;
import pt.Common.UserInfo;
import pt.Common.Utils;
import pt.Server.Database.MessageManager;
import pt.Server.Database.UserManager;
import pt.Server.ServerMain;

import javax.servlet.http.HttpServletRequest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.*;

@SpringBootApplication
@RestController
public class HttpAPI {
	
	static class AuthUser {
		public UserInfo user;
		public long lastSeen;
		
		public AuthUser(UserInfo user) {
			this.user = user;
			this.lastSeen = System.currentTimeMillis();
		}
	}
	
	private static final ServerMain serverMain = ServerMain.getInstance();
	public static final Map<String, AuthUser> authenticatedUsers = Collections.synchronizedMap(new HashMap<>());
	
	static {
		new Thread(() -> { // Esta thread serve para limpar os "utilizadores" da API que já não se conectam á algum tempo
			while (true) {
				try {
					Thread.sleep(AuthorizationFilter.TIMEOUT);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				synchronized (authenticatedUsers) {
					long now = System.currentTimeMillis();
					List<String> toRemove = new ArrayList<>();
					authenticatedUsers.forEach((key, authUser) -> {
						if ((now - authUser.lastSeen) > AuthorizationFilter.TIMEOUT) {
							toRemove.add(key);
						}
					});
					toRemove.forEach(key -> authenticatedUsers.remove(key));
				}
			}
		}).start();
	}
	
	@EnableWebSecurity
	@Configuration
	public class SecurityConfig extends WebSecurityConfigurerAdapter {
		
		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http.csrf().disable()
					.addFilterAfter(new AuthorizationFilter(),
							UsernamePasswordAuthenticationFilter.class)
					.authorizeRequests()
					.antMatchers(HttpMethod.POST, "/login").permitAll()
					.anyRequest().authenticated().and()
					.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
					.and().exceptionHandling().authenticationEntryPoint(
					new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));
		}
	}
	
	@PostMapping("/login")
	public String login(@RequestBody User user) {
		//Check username and password
		//Generate token
		//Put it in the response body
		// add to authenticated Users
		try {
			String token = Utils.hashStringBase36(user.username + ":" + user.password);
			
			if (UserManager.doesPasswordMatchUsername(user.username, user.password)) {
				UserInfo userInfo = UserManager.getUserByName(user.username);
				if (userInfo != null) {
					authenticatedUsers.put(token, new AuthUser(userInfo));
					return "token : " + token;
				}
			}
			return "Password doesn't match username";
		} catch (SQLException | NoSuchAlgorithmException throwables) {
			throwables.printStackTrace();
		}
		return "Server error";
	}
	
	// Everything that requires login must return 401 (Unauthorized) if there is no token
	// no header de request para mensagens entre outros é necessário vir o token no campo "Authorization"
	
	@GetMapping("/getMessages")
	public List<MessageInfo> getLastNMessages(@RequestParam(value = "amount", defaultValue = "10") int amount) {
		//Obter asúltimas nmensagens  trocadas noservidor,  sendo  cada uma descrita  pelo remetente,
		// pelo destinatário(utilizador ou canal) e pela mensagem em si;
		//Limit the query to the N amount
		try {
			return MessageManager.getLastMessages(amount);
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}
		return null;
	}
	
	@PostMapping("/sendMessage")
	public String sendToConnected(HttpServletRequest request, @RequestBody String messageContent) {
		
		//Enviar  uma  mensagem  para  todos  os  utilizadores  que  estão  ligados  ao mesmo servidor.
		//for each connectedMachine -> change the destination
		
		String token = request.getHeader("Authorization");
		UserInfo user = authenticatedUsers.get(token).user;
		
		if (messageContent.isBlank()) {
			return "Message body cannot be empty";
		}
		
		MessageInfo message = new MessageInfo(MessageInfo.TYPE_TEXT, messageContent);
		message.setSenderUsername(user.getUsername());
		message.setSenderId(user.getUserId());
		message.setRecipientType(MessageInfo.Recipient.USER);
		try {
			int amount = serverMain.sendToAllConnected(message);
			return "Message sent to all connected: " + amount;
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}
		return "Something went wrong!";
	}
	
}
