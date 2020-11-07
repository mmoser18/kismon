/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 05.10.2020
 */

package net.mmo.utils.monitoring_kiss.security;

import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import com.vaadin.flow.server.ServletHelper;
import com.vaadin.flow.shared.ApplicationConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@SuppressWarnings("deprecation")
@Slf4j
public final class SecurityUtils
{
	private SecurityUtils() {
		// static - util methods only
	}

	static boolean isFrameworkInternalRequest(HttpServletRequest request) {
		final String parameterValue = request.getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER);
		return parameterValue != null 
			&& Stream.of(ServletHelper.RequestType.values()).anyMatch(r -> r.getIdentifier().equals(parameterValue));
	}

	static boolean isUserLoggedIn() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		boolean userIsLoggedIn = 
		       authentication != null 
		       && !(authentication instanceof AnonymousAuthenticationToken)
			   && authentication.isAuthenticated();
		log.info("user successfully logged-in.");
		return userIsLoggedIn;
	}
}
