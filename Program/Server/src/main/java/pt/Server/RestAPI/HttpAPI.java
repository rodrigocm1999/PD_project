package pt.Server.RestAPI;

import org.apache.logging.log4j.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.bind.annotation.*;
import pt.Common.MessageInfo;
import pt.Common.UserInfo;
import pt.Common.Utils;
import pt.Server.Database.MessageManager;
import pt.Server.Database.UserManager;
import pt.Server.ServerMain;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@RestController
public class HttpAPI {
	
	private static final ServerMain serverMain = ServerMain.getInstance();
	public static final Map<String, UserInfo> authenticatedUsers = new HashMap<>();
	
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
					authenticatedUsers.put(token, userInfo);
					return token;
				}
			}
			return "";
		} catch (SQLException | NoSuchAlgorithmException throwables) {
			throwables.printStackTrace();
		}
		return "";
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
	
	@PutMapping("/sendMessage")
	public void sendToConnected(@RequestParam(value = "content", defaultValue = "") String messageContent) {
		
		//Enviar  uma  mensagem  para  todos  os  utilizadores  que  estão  ligados  ao mesmo servidor.
		//for each connectedMachine -> change the destination
		//MessageManager.insertMessage();
		//serverMain.propagateNewMessage();

		//TODO send message
		
	}
	
}
