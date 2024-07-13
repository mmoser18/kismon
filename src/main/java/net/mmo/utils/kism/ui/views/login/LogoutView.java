/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 05.10.2020
 */

package net.mmo.utils.kism.ui.views.login;

import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.ui.CommonConstants;

@Route(CommonConstants.LogoutURL) // Note: this also acts as @Component annotation!
@PageTitle("Logout | " + CommonConstants.ApplicationFullName)
@RolesAllowed({CommonConstants.Role_ADMIN, CommonConstants.Role_READ_ONLY})
@Slf4j
@SuppressWarnings("javadoc")
public class LogoutView extends VerticalLayout
{
	private static final long serialVersionUID = 2530016773527276009L;

	public static final String LoginLinkClassName = "login-link"; //$NON-NLS-1$

	// public to allow access by tests classes
	public static final String ViewClassName = "logout-view"; //$NON-NLS-1$

	public LogoutView() {
		log.debug("Creating {}:", this.getClass().getSimpleName()); //$NON-NLS-1$
		try {
			addClassName(ViewClassName);
			setSizeFull();
			setAlignItems(Alignment.CENTER);
			setJustifyContentMode(JustifyContentMode.CENTER);

			add(new NativeLabel(Messages.getString("LogoutView.Label.LogOut.Text"))); //$NON-NLS-1$

			VaadinSession current = VaadinSession.getCurrent();
			current.getSession().invalidate();
			current.close();

		} catch (Throwable t) {
			log.error("Exception in c'tor:", t);// TODO: handle exception //$NON-NLS-1$
		}
	}
}
