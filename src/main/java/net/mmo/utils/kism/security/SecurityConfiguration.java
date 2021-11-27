/**
 * @author Michael Moser (michael.moser@elca.ch)
 * @since 05.10.2020
 */

package net.mmo.utils.kism.security;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.spring.security.VaadinDefaultRequestCache;
import com.vaadin.flow.spring.security.VaadinWebSecurityConfigurerAdapter;
import net.mmo.utils.kism.ui.CommonConstants;
import net.mmo.utils.kism.utils.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@EnableWebSecurity
@Configuration
@SuppressWarnings("nls")
public class SecurityConfiguration extends VaadinWebSecurityConfigurerAdapter
{
	private static final String LOGIN_PROCESSING_URL = "/login";
	private static final String LOGIN_FAILURE_URL = "/login?error";
	private static final String LOGIN_URL = "/login";
	private static final String LOGOUT_SUCCESS_URL = "/logout";

	@Autowired
	VaadinDefaultRequestCache requestCache;

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.csrf().disable()  // Disables cross-site httpRequest forgery (CSRF) protection, as Vaadin already has CSRF protection
			.requestCache()
			.requestCache(this.requestCache) // to track unauthorized requests so that users are redirected appropriately after login.
			.and().authorizeRequests() // Turns on authorization
			.requestMatchers(SecurityUtils::isFrameworkInternalRequest).permitAll() // Allows all internal traffic from the Vaadin framework.
			.anyRequest().authenticated() // Allows all authenticated traffic.
			.and().formLogin().loginPage(LOGIN_URL).permitAll() // Enables form-based login and permits unauthenticated access to it.
			.loginProcessingUrl(LOGIN_PROCESSING_URL) // Configures the login page URLs.
			.failureUrl(LOGIN_FAILURE_URL)
			.and().logout()
			// commented since these are the defaults:
			//.invalidateHttpSession(true)
			.logoutUrl(CommonConstants.LogoutURLSuffix)
			.logoutSuccessUrl(LOGOUT_SUCCESS_URL);
	}

	@Bean
	@Override
	public UserDetailsService userDetailsService() {
		String userNames = AppProperties.getProperties().getProperty("user.names");
		if (userNames == null || userNames.isEmpty()) {
			throw new IllegalArgumentException("user.names entry not found in application.properties");
		}
		List<UserDetails> userDetails = new ArrayList<>();
		for (String userNameEntry: userNames.split(",")) {
			String[] uidAndRole = userNameEntry.split(":");
			if (uidAndRole == null || uidAndRole.length != 2) {
				throw new IllegalArgumentException("User-entry '" + userNameEntry + "' does not contain user:role");
			}
			String uid  = uidAndRole[0];
			String role = uidAndRole[1];
			String pwdPropertyName = "user.pwd." + uid;
			String pwd  = AppProperties.getProperties().getProperty(pwdPropertyName);
			if (pwd == null || pwd.isEmpty()) {
				throw new IllegalArgumentException("No entry for '" + pwdPropertyName + "' found in application.properties");
			}
			userDetails.add(User
			                .withUsername(uid)
			                .password("{noop}" + pwd)
			                .roles(role)
			                .build());
		}
		return new InMemoryUserDetailsManager(userDetails);
	}

	/* exclude Vaadin-framework communication and static assets from Spring Security. */
	@Override
	public void configure(WebSecurity web) {
		web.ignoring()
			.antMatchers("/VAADIN/**",
			             "/favicon.ico",
			             "/robots.txt",
			             "/manifest.webmanifest",
			             "/sw.js",
			             "/offline.html",
			             "/icons/**",
			             "/images/**",
			             "/styles/**",
			             "/h2-console/**");
	}
}
