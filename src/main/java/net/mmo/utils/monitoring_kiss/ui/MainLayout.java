/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 05.10.2020
 */

package net.mmo.utils.monitoring_kiss.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HighlightConditions;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.PWA;
import net.mmo.utils.monitoring_kiss.ui.view.dashboard.DashboardView;
import net.mmo.utils.monitoring_kiss.ui.view.list.ListView;

@CssImport("./styles/shared-styles.css")
@PWA(
	name = CommonConstants.ApplicationFullName, 
	shortName = CommonConstants.ApplicationShortName,
	offlineResources = {
		"./styles/offline.css", 
		"./images/offline.png"
	}
)
public class MainLayout extends AppLayout
{
	private static final long serialVersionUID = 8922152078442380813L;

	public static final String LogoutLinkLabel = "Log out";
	public static final String LogoutLinkClassName = "logout-link";
	
	public MainLayout() {
		createHeader();
		createDrawer();
	}

	private void createHeader() {
		H1 logo = new H1(CommonConstants.ApplicationFullName);
		logo.addClassName("logo");
		// add a logout link to the page:
		Anchor logout = new Anchor(CommonConstants.LogoutURLSuffix, LogoutLinkLabel);
		logout.addClassName(LogoutLinkClassName);

		HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), logo, logout);
		header.expand(logo);
			
		header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
		header.setWidth("100%");
		header.addClassName("header");
		addToNavbar(header);
	}

	private void createDrawer() {
		RouterLink listLink = new RouterLink("List", ListView.class);
		listLink.setHighlightCondition(HighlightConditions.sameLocation());
		RouterLink dashboardLink = new RouterLink("Dashboard", DashboardView.class);
		addToDrawer(new VerticalLayout(listLink, dashboardLink));
	}
}
