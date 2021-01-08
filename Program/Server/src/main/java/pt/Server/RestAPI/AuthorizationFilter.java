package pt.Server.RestAPI;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import pt.Common.UserInfo;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthorizationFilter extends OncePerRequestFilter {
	
	public static final long TIMEOUT = 5 * 60 * 1000; // 5 minutos
	
	@Override
	protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
		
		String token = httpServletRequest.getHeader("Authorization");
		if (token == null || token.isBlank()) {
			filterChain.doFilter(httpServletRequest, httpServletResponse);
			return;
		}
		
		HttpAPI.AuthUser authUser = HttpAPI.authenticatedUsers.get(token);
		
		if (authUser != null) {
			
			long now = System.currentTimeMillis();
			if ((now - authUser.lastSeen) > TIMEOUT) { // Timeout passou
				System.out.println("timeout");
				HttpAPI.authenticatedUsers.remove(token);
				filterChain.doFilter(httpServletRequest, httpServletResponse);
				return;
			}
			
			authUser.lastSeen = now;
			
			UsernamePasswordAuthenticationToken uPAT =
					new UsernamePasswordAuthenticationToken("USER", null, null);
			SecurityContextHolder.getContext().setAuthentication(uPAT);
		}
		
		filterChain.doFilter(httpServletRequest, httpServletResponse);
	}
}
