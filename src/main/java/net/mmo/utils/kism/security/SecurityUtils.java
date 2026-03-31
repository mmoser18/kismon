/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.security;

import java.util.Collection;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.ui.CommonConstants;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
@SuppressWarnings({"nls", "javadoc"})
public final class SecurityUtils
{
	private SecurityUtils() {
		// static - util methods only
	}

	public static Authentication getAuthentication() {
		return SecurityContextHolder.getContext().getAuthentication();
	}

	public static boolean isUserLoggedIn() {
		Authentication authentication = getAuthentication();
		boolean userIsLoggedIn =
			authentication != null
			&& !(authentication instanceof AnonymousAuthenticationToken)
			&& authentication.isAuthenticated();

		if (!userIsLoggedIn) {
			log.info("user not logged-in.");
		}
		return userIsLoggedIn;
	}

	public static boolean isAdminUser() {
		if (isUserLoggedIn()) {
			Authentication authentication = getAuthentication();
			if (authentication != null) {
				Object obj = authentication.getDetails();
				Collection<String> auths = authentication.getAuthorities().stream()
					.map((auth) -> auth.getAuthority())
					.collect(Collectors.toList());
				log.debug("user details: {} / authorities: {}", obj, auths);
				return auths.contains("ROLE_" + CommonConstants.Role_ADMIN);
			}
		}
		return false;
	}

	public static String getUserName() {
		Authentication authentication = getAuthentication();
		return (authentication != null ? authentication.getName() : null);
	}
}
