/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 05.10.2020
 */

package net.mmo.utils.kism.security;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.shared.ApplicationConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
@SuppressWarnings({"nls"})
public final class SecurityUtils
{
	private SecurityUtils() {
		// static - util methods only
	}

	static boolean isFrameworkInternalRequest(HttpServletRequest request) {
		final String parameterValue = request.getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER);
		return parameterValue != null
			&& Stream.of(HandlerHelper.RequestType.values()).anyMatch(r -> r.getIdentifier().equals(parameterValue));
	}

	static Authentication isAuthenticated() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		boolean userIsLoggedIn =
			authentication != null
			&& !(authentication instanceof AnonymousAuthenticationToken)
			&& authentication.isAuthenticated();

//		if (userIsLoggedIn) {
//			log.info("user successfully logged-in");
//		} else {
//			log.info("user not logged-in.");
//		}
		return (userIsLoggedIn ? authentication : null);
	}

	static boolean isUserLoggedIn() {
		return isAuthenticated() != null;
	}

	static boolean isAsminUser() {
		Authentication authentication = isAuthenticated();
		if (authentication != null) {
			Object obj = authentication.getDetails();
			Collection<String> auths = authentication.getAuthorities().stream()
				.map((auth) -> auth.getAuthority())
				.collect(Collectors.toList());
			log.info("user details: {} / authorities: {}", obj, auths);
			return auths.contains("ADMIN");
		}
		return false;
	}
}
