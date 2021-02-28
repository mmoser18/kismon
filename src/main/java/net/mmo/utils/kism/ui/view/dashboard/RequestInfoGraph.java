/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 05.10.2020
 */

package net.mmo.utils.kism.ui.view.dashboard;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.AxisType;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.Configuration;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.HorizontalAlign;
import com.vaadin.flow.component.charts.model.LayoutDirection;
import com.vaadin.flow.component.charts.model.Legend;
import com.vaadin.flow.component.charts.model.Series;
import com.vaadin.flow.component.charts.model.VerticalAlign;
import com.vaadin.flow.component.charts.model.XAxis;
import com.vaadin.flow.component.charts.model.YAxis;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.RequestInfoService;
import net.mmo.utils.kism.entities.nodes.NodeType;
import net.mmo.utils.kism.entities.results.RequestInfo;
import net.mmo.utils.kism.ui.CommonConstants;
import net.mmo.utils.kism.ui.MainLayout;
import net.mmo.utils.kism.ui.view.nodes.NodeTypeLabelProvider;
import net.mmo.utils.kism.ui.view.results.AbstractRequestInfoView;

@Route(value = "dashboard", layout = MainLayout.class) // route-name, parent // Note: this also acts as @Component annotation!
@PageTitle("Dashboard | " + CommonConstants.ApplicationFullName)
@Slf4j
public class RequestInfoGraph extends AbstractRequestInfoView
{
	private static final long serialVersionUID = -1954281705187898725L;
	private static final boolean LEGENDS_AT_LEFT = false;

	protected Chart chart;
	protected List<Series> series;


	public RequestInfoGraph(RequestInfoService requestInfoService) {
		super(requestInfoService);
		setDefaultHorizontalComponentAlignment(Alignment.CENTER);
	}

	@Override
	protected HorizontalLayout getToolbar() {
		HorizontalLayout toolbar = super.getToolbar();

		this.nameFilter.addValueChangeListener(e -> { populateData(); });
		this.typeFilter.addValueChangeListener(e -> { populateData(); });

		return toolbar;
	}

	@Override
	protected void configureContent() {
		log.info("getTimingChart:"); //$NON-NLS-1$
		this.chart = new Chart(ChartType.LINE);
		this.chart.setTimeline(true);

		Configuration conf = this.chart.getConfiguration();
		conf.setTitle(Messages.getString("ResultGraph.Title")); //$NON-NLS-1$

		Legend legend = conf.getLegend();
		legend.setEnabled(true);
		if (LEGENDS_AT_LEFT) {
			legend.setLayout(LayoutDirection.VERTICAL);
			legend.setAlign(HorizontalAlign.LEFT);
			legend.setVerticalAlign(VerticalAlign.TOP);
		} else {
			legend.setLayout(LayoutDirection.HORIZONTAL);
			legend.setAlign(HorizontalAlign.LEFT);
			legend.setVerticalAlign(VerticalAlign.BOTTOM);
		}
		XAxis xAxis = conf.getxAxis();
		xAxis.setTitle(Messages.getString("ResultGraph.XAxis.Label")); //$NON-NLS-1$
		xAxis.setType(AxisType.DATETIME);

		YAxis yAxis = conf.getyAxis();
		yAxis.setTitle(Messages.getString("ResultGraph.YAxis.Label")); //$NON-NLS-1$
		yAxis.setMin(0);

		this.content.add(this.chart);

		populateData();
	}

	protected void populateData() {
		Configuration conf = this.chart.getConfiguration();
		conf.setSeries(new ArrayList<>());

		ZoneId myZoneId = ZoneId.systemDefault();

		String nameFilterValue = (this.nameFilter.getValue() != null ? this.nameFilter.getValue().replace('*', '%') : null);
		NodeType nodeTypeFilterValue = NodeTypeLabelProvider.getNodeType(this.typeFilter.getValue());

		List<String> nodeNames = this.requestInfoService.getNodeNames(nameFilterValue, nodeTypeFilterValue);
		log.info("nr of nodes: {}", nodeNames.size()); //$NON-NLS-1$

		nodeNames.forEach(name ->
			{
				log.info("fetching and converting data for node '{}'", name); //$NON-NLS-1$
				List<RequestInfo> values = this.requestInfoService.findByFilter(name, null, null, null, null, null);
				DataSeries durations = new DataSeries();
				durations.setId(name);
				durations.setName(name);

				int nrDataPoints = values.size();
				log.info("got {} points for node '{}'", nrDataPoints, name); //$NON-NLS-1$
				Number[][] data = new Number[nrDataPoints][2];
				for (int idx = 0; idx < nrDataPoints; idx++) {
					RequestInfo value = values.get(idx);
					LocalDateTime timestamp = value.getTimestamp();
					Double duration = value.getDuration();

					data[idx][0] = Date.from(timestamp.atZone(myZoneId).toInstant()).getTime();
					data[idx][1] = duration;
				}
				log.info("addSeries():"); //$NON-NLS-1$
				durations.addData(data);
				conf.addSeries(durations);
			});
	}

}
