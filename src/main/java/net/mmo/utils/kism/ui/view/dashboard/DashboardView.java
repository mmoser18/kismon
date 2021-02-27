/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 05.10.2020
 */

package net.mmo.utils.kism.ui.view.dashboard;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.Configuration;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.RequestInfoService;
import net.mmo.utils.kism.entities.results.RequestInfo;
import net.mmo.utils.kism.ui.CommonConstants;
import net.mmo.utils.kism.ui.MainLayout;

@Route(value = "dashboard", layout = MainLayout.class) // route-name, parent // Note: this also acts as @Component annotation!
@PageTitle("Dashboard | " + CommonConstants.ApplicationFullName)
@Slf4j
public class DashboardView extends VerticalLayout
{
	private static final long serialVersionUID = -1954281705187898725L;

	final static ZoneId zoneId = ZoneId.systemDefault();

	private RequestInfoService requestInfoService;

	public DashboardView(RequestInfoService requestInfoService) {
		try {
			this.requestInfoService = requestInfoService;
			addClassName("dashboard-view"); //$NON-NLS-1$
			setDefaultHorizontalComponentAlignment(Alignment.CENTER);
			add(getTimingChart());
		} catch (Throwable t) {
			log.error("Exception in c'tor:", t); //$NON-NLS-1$
		}
	}

	private class MinMax
	{
		Double durationMin = 100000.0; // 100 seconds
		Double durationMax = 0.0;
		LocalDateTime timestampMin = LocalDateTime.now();
		LocalDateTime timestampMax = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);
	}

	private Chart getTimingChart() {
		log.info("getTimingChart:"); //$NON-NLS-1$

		log.info("new Chart:"); //$NON-NLS-1$
		Chart chart = new Chart(ChartType.LINE);
		Configuration conf = chart.getConfiguration();

		List<String> nodeNames = this.requestInfoService.getNodeNames();
		log.info("nr of nodes: {}", nodeNames.size()); //$NON-NLS-1$

		MinMax minMax = new MinMax();

		nodeNames.forEach(name ->
			{
				log.info("fetching and converting data for node '{}'", name); //$NON-NLS-1$
				List<RequestInfo> values = this.requestInfoService.findByFilter(name, null, null, null, null, null);
				DataSeries durations = new DataSeries();
				durations.setId(name);

				int nrDataPoints = values.size();
				log.info("got {} points for node '{}'", nrDataPoints, name); //$NON-NLS-1$
				Number[][] data = new Number[nrDataPoints][2];
				for (int idx = 0; idx < nrDataPoints; idx++) {
					RequestInfo value = values.get(idx);

					LocalDateTime timestamp = value.getTimestamp();
					if (timestamp.compareTo(minMax.timestampMin) < 0) minMax.timestampMin = timestamp;
					if (timestamp.compareTo(minMax.timestampMax) > 0) minMax.timestampMax = timestamp;

					Double duration = value.getDuration();
					if (duration < minMax.durationMin) minMax.durationMin = duration;
					if (duration > minMax.durationMax) minMax.durationMax = duration;

					data[idx][0] = timestamp.atZone(zoneId).toEpochSecond();
					data[idx][1] = duration;
				}
				log.info("addSeries():"); //$NON-NLS-1$
				durations.addData(data);
				conf.addSeries(durations);
			});

		log.info("timestamps: [{} - {}], durations: [{} - {}].", //$NON-NLS-1$
		         minMax.timestampMin, minMax.timestampMax,
		         minMax.durationMin, minMax.durationMax);
		// conf.setPlotOptions();
		conf.setTitle("Request Duration [msec]"); //$NON-NLS-1$
		conf.getLegend().setEnabled(true);
		conf.getxAxis().setTitle("Timestamp"); //$NON-NLS-1$
		conf.getxAxis().setMin(minMax.timestampMin.atZone(zoneId).toEpochSecond());
		conf.getxAxis().setMax(minMax.timestampMax.atZone(zoneId).toEpochSecond());
		conf.getyAxis().setTitle("Duration"); //$NON-NLS-1$
		conf.getyAxis().setMin(minMax.durationMin);
		conf.getyAxis().setMax(minMax.durationMax);

		return chart;
	}

}
