/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 05.10.2020
 */

package net.mmo.utils.kism.security;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.spring.security.VaadinWebSecurityConfigurerAdapter;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.ui.views.login.LoginView;
import net.mmo.utils.kism.utils.AppProperties;
import net.mmo.utils.kism.utils.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@SuppressWarnings({"nls", "javadoc"})
@EnableWebSecurity
@Configuration
@Slf4j
public class SecurityConfiguration extends VaadinWebSecurityConfigurerAdapter
{
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		log.info("configure: http={}", http);
		// Delegating the responsibility of general configurations of http security to the super class.
		// It is configuring the followings:
		// Vaadin's CSRF protection by ignoring framework's internal requests,
		// default request cache,
		// ignoring public views annotated with @AnonymousAllowed,
		// restricting access to other views/endpoints, and
		// enabling ViewAccessChecker authorization.
		super.configure(http);

		// This is important to register your login view to the view access checker mechanism:
		setLoginView(http, LoginView.class);

		// You can add any possible extra configurations of your own here
	}

	@SuppressWarnings("deprecation")
	@Bean
	@Override
	public UserDetailsService userDetailsService() {
		log.info("userDetailsService");
		String userNames = AppProperties.getProperties().getProperty("user.names");
		if (StringUtils.isEmpty(userNames)) {
			throw new IllegalArgumentException("No entry 'user.names' found in application.properties");
		}
		List<UserDetails> userDetails = new ArrayList<>();
		for (String userNameEntry: userNames.split(",")) {
			String[] uidAndRole = userNameEntry.split(":");
			if (uidAndRole == null || uidAndRole.length != 2) {
				throw new IllegalArgumentException("User-entry '" + userNameEntry + "' does not match format <user>:<role>");
			}
			String uid  = uidAndRole[0];
			String role = uidAndRole[1];
			String pwdPropertyName = "user.pwd." + uid;
			String pwd  = AppProperties.getProperties().getProperty(pwdPropertyName);
			if (StringUtils.isEmpty(pwd)) {
				throw new IllegalArgumentException("No value for '" + pwdPropertyName + "' found in application.properties");
			}
			userDetails.add(User.withUsername(uid)
			                    .password("{noop}" + pwd)
			                    .roles(role)
			                    .build());
		}
		return new InMemoryUserDetailsManager(userDetails);
	}

	/* exclude Vaadin-framework communication and static assets from Spring Security. */
	@Override
	public void configure(WebSecurity web) throws Exception {
		log.info("configure: web={}", web);
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
		super.configure(web);
	}
}
