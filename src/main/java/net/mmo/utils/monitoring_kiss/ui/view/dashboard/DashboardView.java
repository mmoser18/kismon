/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 05.10.2020
 */

package net.mmo.utils.monitoring_kiss.ui.view.dashboard;

import java.util.Map;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.DataSeriesItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import net.mmo.utils.monitoring_kiss.backend.service.CompanyService;
import net.mmo.utils.monitoring_kiss.backend.service.ContactService;
import net.mmo.utils.monitoring_kiss.ui.CommonConstants;
import net.mmo.utils.monitoring_kiss.ui.MainLayout;

@Route(value = "dashboard", layout = MainLayout.class) // route-name, parent // Note: this also acts as @Component annotation!
@PageTitle("Dashboard | " + CommonConstants.ApplicationFullName)
public class DashboardView extends VerticalLayout
{
	private static final long serialVersionUID = -1954281705187898725L;

	private ContactService contactService;
	private CompanyService companyService;

	public DashboardView(ContactService contactService, CompanyService companyService) {
		this.contactService = contactService;
		this.companyService = companyService;
		addClassName("dashboard-view"); //$NON-NLS-1$
		setDefaultHorizontalComponentAlignment(Alignment.CENTER);
		add(getContactStats(), getCompaniesChart());
	}

	private Component getContactStats() {
		Span stats = new Span(this.contactService.count() + Messages.getString("DashboardView.Title.Label.Suffix")); //$NON-NLS-1$
		stats.addClassName("contact-stats"); //$NON-NLS-1$
		return stats;
	}

	private Chart getCompaniesChart() {
		Chart chart = new Chart(ChartType.PIE);
		DataSeries dataSeries = new DataSeries();
		Map<String, Integer> companies = this.companyService.getStats();
		companies.forEach((company, employees) -> dataSeries.add(new DataSeriesItem(company, employees)));
		chart.getConfiguration().setSeries(dataSeries);
		return chart;
	}

}
