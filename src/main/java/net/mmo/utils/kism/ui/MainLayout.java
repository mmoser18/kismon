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
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HighlightConditions;
import com.vaadin.flow.router.RouterLink;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.security.SecurityUtils;
import net.mmo.utils.kism.ui.views.history.HistoryInfoGraph;
import net.mmo.utils.kism.ui.views.history.HistoryInfoTable;
import net.mmo.utils.kism.ui.views.nodes.NodeView;

@SuppressWarnings("javadoc")
@CssImport("./styles/shared-styles.css")
@CssImport(value = "./styles/vaadin-button-styles.css", themeFor = "vaadin-button")
@CssImport(value = "./styles/vaadin-checkbox-styles.css", themeFor = "vaadin-checkbox")
@CssImport(value = "./styles/vaadin-combo-box-styles.css", themeFor = "vaadin-combo-box")
@CssImport(value = "./styles/vaadin-form-layout-styles.css", themeFor = "vaadin-form-layout")
@CssImport(value = "./styles/vaadin-date-time-picker-styles.css", themeFor = "vaadin-date-time-picker")
@CssImport(value = "./styles/vaadin-date-time-picker-custom-field-styles.css", themeFor = "vaadin-date-time-picker-custom-field")
@CssImport(value = "./styles/vaadin-form-layout-styles.css", themeFor = "vaadin-form-layout")
@CssImport(value = "./styles/vaadin-integer-field-styles.css", themeFor = "vaadin-integer-field")
@CssImport(value = "./styles/vaadin-number-field-styles.css", themeFor = "vaadin-number-field")
@CssImport(value = "./styles/vaadin-select-styles.css", themeFor = "vaadin-select")
@CssImport(value = "./styles/vaadin-text-area-styles.css", themeFor = "vaadin-text-area")
@CssImport(value = "./styles/vaadin-text-field-styles.css", themeFor = "vaadin-text-field")
@CssImport(value = "./styles/vaadin-upload-styles.css", themeFor = "vaadin-upload")
@Slf4j
public class MainLayout extends AppLayout
{
	private static final long serialVersionUID = 8922152078442380813L;

	public static final String LogoutLinkLabel = Messages.getString("MainLayout.Link.LogOut.Label"); //$NON-NLS-1$
	public static final String TitleClassName = "main-title"; //$NON-NLS-1$
	public static final String HeaderClassName = "main-header"; //$NON-NLS-1$
	public static final String LogoutLinkClassName = "logout-link"; //$NON-NLS-1$
	public static final String StartupErrorMessageClassName = "startup-error"; //$NON-NLS-1$

	public MainLayout() {
		log.debug("{} c'tor", this.getClass()); //$NON-NLS-1$
		createHeader();
		createDrawer();
		setDrawerOpened(false);
	}

	private void createHeader() {
		log.debug("createHeader"); //$NON-NLS-1$
		H1 appTitle = new H1(CommonConstants.ApplicationFullName);
		appTitle.addClassName(TitleClassName);
		// add a logout link to the page:
		Anchor logout = new Anchor(CommonConstants.LogoutURL, LogoutLinkLabel);
		logout.addClassName(LogoutLinkClassName);

		HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), appTitle, logout);
		header.addClassName(HeaderClassName);
//		header.expand(appTitle);
//		header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

		log.info(NodeService.startupErrors.length() > 0
		         ? String.format("startup errors: '{}'", NodeService.startupErrors) //$NON-NLS-1$
		         : "no startup errors"); //$NON-NLS-1$
		if (SecurityUtils.isAdminUser() && !NodeService.startupErrors.isEmpty()) {
			VerticalLayout panel = new VerticalLayout(header);
			for (String msg: NodeService.startupErrors.split("\n")) { //$NON-NLS-1$
				Label label = new Label(msg);
				label.setClassName(StartupErrorMessageClassName);
				panel.add(label);
			}
			addToNavbar(panel);
		} else {
			addToNavbar(header);
		}
	}

	private void createDrawer() {
		log.debug("createDrawer"); //$NON-NLS-1$
		RouterLink nodesLink = new RouterLink(Messages.getString("MainLayout.Tab.NodeView.Label"), NodeView.class); //$NON-NLS-1$
		nodesLink.setHighlightCondition(HighlightConditions.sameLocation());
		RouterLink resultsLink = new RouterLink(Messages.getString("MainLayout.Tab.RequestInfoTable.Label"), HistoryInfoTable.class); //$NON-NLS-1$
		resultsLink.setHighlightCondition(HighlightConditions.sameLocation());
		RouterLink dashboardLink = new RouterLink(Messages.getString("MainLayout.Tab.RequestInfoGraph.Label"), HistoryInfoGraph.class); //$NON-NLS-1$
		dashboardLink.setHighlightCondition(HighlightConditions.sameLocation());

		addToDrawer(new VerticalLayout(nodesLink, resultsLink, dashboardLink));
	}
}
