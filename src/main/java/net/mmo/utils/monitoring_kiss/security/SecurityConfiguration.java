/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 05.10.2020
 */

package net.mmo.utils.monitoring_kiss.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@EnableWebSecurity
@Configuration
@SuppressWarnings("nls")
public class SecurityConfiguration extends WebSecurityConfigurerAdapter
{
	private static final String LOGIN_PROCESSING_URL = "/login";
	private static final String LOGIN_FAILURE_URL = "/login?error";
	private static final String LOGIN_URL = "/login";
	private static final String LOGOUT_SUCCESS_URL = "/login";

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.csrf().disable()  // Disables cross-site request forgery (CSRF) protection, as Vaadin already has CSRF protection
			.requestCache()
			.requestCache(new CustomRequestCache()) // Uses CustomRequestCache to track unauthorized requests so that users are redirected appropriately after login.
			.and().authorizeRequests() // Turns on authorization
			.requestMatchers(SecurityUtils::isFrameworkInternalRequest).permitAll() // Allows all internal traffic from the Vaadin framework.
			.anyRequest().authenticated() // Allows all authenticated traffic.
			.and().formLogin().loginPage(LOGIN_URL).permitAll() // Enables form-based login and permits unauthenticated access to it.
			.loginProcessingUrl(LOGIN_PROCESSING_URL) // Configures the login page URLs.
			.failureUrl(LOGIN_FAILURE_URL)
			.and().logout() // Configures the logout URL
			.logoutSuccessUrl(LOGOUT_SUCCESS_URL);
	}

	@Bean
	@Override
	public UserDetailsService userDetailsService() {
		UserDetails user = User.withUsername("user")
		                       .password("{noop}password")
		                       .roles("USER")
		                       .build();
		return new InMemoryUserDetailsManager(user);
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
