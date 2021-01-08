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
	
	@Override
	protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
		
		String token = httpServletRequest.getHeader("Authorization");
		if(token == null){
			filterChain.doFilter(httpServletRequest, httpServletResponse);
			return;
		}
		
		UserInfo userInfo = HttpAPI.authenticatedUsers.get(token);
		
		if (userInfo != null) {
			UsernamePasswordAuthenticationToken uPAT =
					new UsernamePasswordAuthenticationToken("USER", null, null);
			SecurityContextHolder.getContext().setAuthentication(uPAT);
		}
		
		filterChain.doFilter(httpServletRequest, httpServletResponse);
	}
}
