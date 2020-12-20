/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 05.10.2020
 */

package net.mmo.utils.kism.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HighlightConditions;
import com.vaadin.flow.router.RouterLink;
import net.mmo.utils.kism.ui.view.dashboard.DashboardView;
import net.mmo.utils.kism.ui.view.list.ListView;
import net.mmo.utils.kism.ui.view.nodes.NodeView;

@CssImport("./styles/shared-styles.css")
public class MainLayout extends AppLayout
{
	private static final long serialVersionUID = 8922152078442380813L;

	public static final String LogoutLinkLabel = Messages.getString("MainLayout.Link.LogOut.Label"); //$NON-NLS-1$
	public static final String TitleClassName = "main-title"; //$NON-NLS-1$
	public static final String HeaderClassName = "main-header"; //$NON-NLS-1$
	public static final String LogoutLinkClassName = "logout-link"; //$NON-NLS-1$

	public MainLayout() {
		createHeader();
		createDrawer();
	}

	private void createHeader() {
		H1 appTitle = new H1(CommonConstants.ApplicationFullName);
		appTitle.addClassName(TitleClassName);
		// add a logout link to the page:
		Anchor logout = new Anchor(CommonConstants.LogoutURLSuffix, LogoutLinkLabel);
		logout.addClassName(LogoutLinkClassName);

		HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), appTitle, logout);
		header.addClassName(HeaderClassName);
		// header.expand(appTitle);
		// header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

		addToNavbar(header);
	}

	private void createDrawer() {
		RouterLink listLink = new RouterLink(Messages.getString("MainLayout.Tab.ListView.Label"), ListView.class); //$NON-NLS-1$
		listLink.setHighlightCondition(HighlightConditions.sameLocation());
		RouterLink treeLink = new RouterLink(Messages.getString("MainLayout.Tab.NodeView.Label"), NodeView.class); //$NON-NLS-1$
		treeLink.setHighlightCondition(HighlightConditions.sameLocation());
		RouterLink dashboardLink = new RouterLink(Messages.getString("MainLayout.Tab.DashBoard.Label"), DashboardView.class); //$NON-NLS-1$
		addToDrawer(new VerticalLayout(listLink, treeLink, dashboardLink));
	}
}
