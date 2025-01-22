/**
 * Copyright © 2020-2025 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.security;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import net.mmo.utils.kism.ui.views.login.LoginView;
import org.springframework.stereotype.Component;

/**
 * Listens for the initialization of the UI (the internal root component in Vaadin)
 * and then add a listener before every view transition to the UI:
 */
@Component
public class ConfigureUIServiceInitListener implements VaadinServiceInitListener
{
	private static final long serialVersionUID = -5024737646453081931L;

	@Override
	public void serviceInit(ServiceInitEvent event) {
		event.getSource().addUIInitListener(uiEvent ->
			{
				final UI ui = uiEvent.getUI();
				ui.addBeforeEnterListener(this::authenticateNavigation);
			});
	}

	// the BeforeEnterListener that's called before each transition:
	private void authenticateNavigation(BeforeEnterEvent event) {
		// as long as the user is not logged in (and we are not already
		// on the login-view) the event is rerouted to the LoginView:
		if (!LoginView.class.equals(event.getNavigationTarget()) && !SecurityUtils.isUserLoggedIn()) {
			event.rerouteTo(LoginView.class);
		}
	}
}
