/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 05.10.2020
 */

package net.mmo.utils.kism.ui.views.login;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.ui.CommonConstants;

@Route(CommonConstants.LoginURL) // Note: this also acts as @Component annotation!
@PageTitle("Login | " + CommonConstants.ApplicationFullName)
@Slf4j
@SuppressWarnings({"nls", "javadoc"})
public class LoginView extends VerticalLayout implements BeforeEnterObserver
{
	private static final long serialVersionUID = 2530016773527276009L;

	// public to allow access by tests classes
	public static final String ViewClassName = "login-view";

	private LoginForm login = new LoginForm();

	public LoginView() {
		log.debug("Creating {}:", this.getClass().getSimpleName());
		try {
			addClassName(ViewClassName);
			setSizeFull();
			setAlignItems(Alignment.CENTER);
			setJustifyContentMode(JustifyContentMode.CENTER);
			this.login.setAction(CommonConstants.LoginURL);
			add(new H1(CommonConstants.ApplicationFullName), this.login);

			this.login.addLoginListener(ev -> { log.info("user '{}' logging in:", ev.getUsername());});
		} catch (Throwable t) {
			log.error("Exception in c'tor:", t);// TODO: handle exception
		}
	}

	@Override
	public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
		// inform the user about an authentication error
		if (beforeEnterEvent.getLocation().getQueryParameters().getParameters().containsKey("error")) {
			this.login.setError(true);
			log.warn("login failed.");
		}
	}
}
