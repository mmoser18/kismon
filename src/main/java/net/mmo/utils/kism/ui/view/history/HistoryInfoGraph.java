/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 05.10.2020
 */

package net.mmo.utils.kism.ui.view.history;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.AxisTitle;
import com.vaadin.flow.component.charts.model.AxisType;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.Configuration;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.HorizontalAlign;
import com.vaadin.flow.component.charts.model.LayoutDirection;
import com.vaadin.flow.component.charts.model.Legend;
import com.vaadin.flow.component.charts.model.Series;
import com.vaadin.flow.component.charts.model.Tooltip;
import com.vaadin.flow.component.charts.model.VerticalAlign;
import com.vaadin.flow.component.charts.model.XAxis;
import com.vaadin.flow.component.charts.model.YAxis;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.HistoryInfoService;
import net.mmo.utils.kism.entities.history.HistoryInfo;
import net.mmo.utils.kism.ui.CommonConstants;
import net.mmo.utils.kism.ui.MainLayout;
import net.mmo.utils.kism.ui.utils.UIHandlerSupport;


@Route(value = "history-graph", layout = MainLayout.class) // route-name, parent // Note: this also acts as @Component annotation!
@PageTitle("History Graph | " + CommonConstants.ApplicationFullName)
@Slf4j
public class HistoryInfoGraph extends AbstractHistoryInfoView
{
	private static final long serialVersionUID = -1954281705187898725L;
	private static final boolean LEGENDS_AT_LEFT = false;

	protected Button selectAll;
	protected Button selectNone;

	protected Chart chart;
	LocalDateTime lastUpdate;


	public HistoryInfoGraph(HistoryInfoService historyInfoService) {
		super(historyInfoService);
	}

	@Override
	protected HorizontalLayout getToolbar() {
		HorizontalLayout toolbar = super.getToolbar();

		this.nameFilter.addValueChangeListener(e -> { if (e.isFromClient()) adjustVisibility(); });
		this.typeFilter.addValueChangeListener(e -> { if (e.isFromClient()) adjustVisibility(); });

		this.selectAll = new Button(Messages.getString("HistoryGraph.Button.SelectAll.Label")); //$NON-NLS-1$
		this.selectAll.addClickListener(e -> selectAll());

		this.selectNone = new Button(Messages.getString("HistoryGraph.Button.SelectNone.Label")); //$NON-NLS-1$
		this.selectNone.addClickListener(e -> selectNone());

		toolbar.add(this.selectAll, this.selectNone, this.refreshData);
		return toolbar;
	}

	@Override
	protected void configureContent() {
		log.info("configureContent:"); //$NON-NLS-1$
		setDefaultHorizontalComponentAlignment(Alignment.CENTER);

		this.chart = new Chart(ChartType.LINE);
		this.chart.setTimeline(true);

		Configuration conf = this.chart.getConfiguration();
		conf.setTitle(Messages.getString("HistoryGraph.Title")); //$NON-NLS-1$

		Tooltip tooltip = new Tooltip();
		tooltip.setShared(true);
		tooltip.setValueSuffix("[msec]"); //$NON-NLS-1$
		conf.setTooltip(tooltip);

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
		xAxis.setTitle(new AxisTitle(Messages.getString("HistoryGraph.XAxis.Label"))); //$NON-NLS-1$
		xAxis.setType(AxisType.DATETIME);

		YAxis yAxis = conf.getyAxis();
		yAxis.setTitle(new AxisTitle(Messages.getString("HistoryGraph.YAxis.Label"))); //$NON-NLS-1$
		yAxis.setMin(0); // duration 0 signals an error

		this.content.add(this.chart);
	}

	@Override
	protected void refreshData() {
		log.info("refreshData"); //$NON-NLS-1$
		UIHandlerSupport.executeLater(this::fetchData, this::updateGraph);
	}

	/** This method fetches all data to be displayed from the DB and converts it to the DataSeries
	 * format expected by the chart */
	protected List<Series> fetchData() {
		log.debug("fetchData"); //$NON-NLS-1$
		ZoneId myZoneId = ZoneId.systemDefault();
		this.lastUpdate = LocalDateTime.now();

		List<String> nodeNames = getFilteredNodeNames();
		List<Series> timingData = new ArrayList<>(nodeNames.size());

		nodeNames.forEach(name ->
			{
				log.debug("fetching and converting data for node '{}'", name); //$NON-NLS-1$
				List<HistoryInfo> values = this.historyInfoService.findByFilter(name, null, null, null, null, null);

				DataSeries timingSeries = new DataSeries();
				timingSeries.setId(name);
				timingSeries.setName(name);

				int nrDataPoints = values.size();
				log.info("got {} data points for '{}'", nrDataPoints, name); //$NON-NLS-1$
				Number[][] numberTuples = new Number[nrDataPoints][2];
				for (int idx = 0; idx < nrDataPoints; idx++) {
					HistoryInfo value = values.get(idx);
					LocalDateTime timestamp = value.getTimestamp();
					Double duration = value.getDuration();
					numberTuples[idx][0] = Date.from(timestamp.atZone(myZoneId).toInstant()).getTime();
					numberTuples[idx][1] = duration;
				}
				log.trace("addSeries():"); //$NON-NLS-1$
				timingSeries.addData(numberTuples);
				timingData.add(timingSeries);
			});
		log.info("got timingData for {} series", timingData.size()); //$NON-NLS-1$
		return timingData;
	}

	/**
	 * This method takes the DataSeies data and assigns it to the chart
	 * @param timingData
	 */
	protected void updateGraph(List<Series> timingData) {
		log.debug("updateGraph"); //$NON-NLS-1$
		Configuration conf = this.chart.getConfiguration();
		conf.setSeries(timingData);
		this.chart.drawChart();
	}

	protected void adjustVisibility() {
		log.info("adjustVisibility"); //$NON-NLS-1$
		UIHandlerSupport.executeLater(this::getFilteredNodeNames, this::updateVisibility);
	}

	protected void updateVisibility(List<String> filteredNodeNames) {
		forAllSeriesDo(ds -> ds.setVisible(filteredNodeNames.contains(ds.getName()), true));
	}

	private void forAllSeriesDo(Consumer<DataSeries> func) {
		log.debug("forAllSeriesDo"); //$NON-NLS-1$
		Configuration conf = this.chart.getConfiguration();
		conf.getSeries().forEach(series -> func.accept((DataSeries)series));
		this.chart.drawChart();
	}

	private void selectNone() {
		log.info("selectNone"); //$NON-NLS-1$
		UIHandlerSupport.executeLater(null, (x) -> forAllSeriesDo(ds -> ds.setVisible(false, true)));
	}

	private void selectAll() {
		log.info("selectAll"); //$NON-NLS-1$
		UIHandlerSupport.executeLater(null, (x) -> forAllSeriesDo(ds -> ds.setVisible(true, true)));
	}
}
