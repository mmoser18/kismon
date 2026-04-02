/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.ui.views.login;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.ui.CommonConstants;
import net.mmo.utils.kism.ui.utils.ConfirmDialog;

@Route(CommonConstants.LoginURL) // Note: this also acts as @Component annotation!
@PageTitle("Login | " + CommonConstants.ApplicationFullName)
@AnonymousAllowed
@Slf4j
@SuppressWarnings({"nls", "javadoc"})
public class LoginView extends VerticalLayout implements BeforeEnterObserver
{
	static {
		log.info("{} static c'tor begin:", LoginView.class.getName()); //$NON-NLS-1$;
	}
	{
		log.debug("Creating {}:", this.getClass().getSimpleName());
	}
	private static final long serialVersionUID = 2530016773527276009L;

	// public to allow access by tests classes
	public static final String ViewClassName = "login-view";

	private LoginForm login = new LoginForm();

	@SuppressWarnings("unused")
	public LoginView() {
		try {
			addClassName(ViewClassName);
			setSizeFull();
			setAlignItems(Alignment.CENTER);
			setJustifyContentMode(JustifyContentMode.CENTER);
			add(new H1(CommonConstants.ApplicationFullName), this.login);
			// According to some startup warning the login-view should not define BOTH,
			// an action AND a LoginListener, but since I use the latter only for a
			// log-statement I guess that doesn't do any harm
			// (and besides: without defining the action, nothing else happened...)
			this.login.setAction(CommonConstants.LoginURL);
			this.login.addLoginListener(ev -> { // void onComponentEvent(LoginEvent event)
				log.info("user '{}' logging in:", ev.getUsername());
			});
			this.login.addForgotPasswordListener(ev -> { // void onComponentEvent(ForgotPasswordEvent event)
				log.info("user clicked 'forgot password'...");
				new ConfirmDialog("Password forgotten",
				                  "Bad luck. Contact your sys-admin to reset it for you!",
				                  "OK", confirm -> {
					// nothing...
				}).open();
			});
		} catch (Throwable t) {
			log.error("Exception in c'tor:", t);// TODO: handle exception
		}
	}

	@Override
	public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
		// inform the user about an authentication error
		if (beforeEnterEvent.getLocation()
				.getQueryParameters()
				.getParameters()
				.containsKey("error")) {
			this.login.setError(true);
			log.warn("login failed.");
		}
	}
	{
		log.debug("Created {}.", this.getClass().getSimpleName());
	}

	static {
		log.debug("{} static c'tor end.", LoginView.class.getName()); //$NON-NLS-1$;
	}
}
