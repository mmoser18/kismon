/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.ui.views.history;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.HistoryInfoService;
import net.mmo.utils.kism.entities.history.HistoryInfo;
import net.mmo.utils.kism.entities.nodes.VisibleNodeType;
import net.mmo.utils.kism.ui.CommonConstants;
import net.mmo.utils.kism.ui.MainLayout;
import net.mmo.utils.kism.ui.utils.UIHandlerSupport;
import net.mmo.utils.kism.ui.views.nodes.NodeTypeLabelProvider;


@Route(value = "history-graph", layout = MainLayout.class) // route-name, parent // Note: this also acts as @Component annotation!
@PageTitle("History Graph | " + CommonConstants.ApplicationFullName)
@RolesAllowed({CommonConstants.Role_ADMIN, CommonConstants.Role_READ_ONLY})
@Slf4j
@SuppressWarnings("javadoc")
public class HistoryInfoGraph extends AbstractHistoryInfoView<HashMap<String, Object>>
{
	static {
		log.debug("{} static c'tor begin:", HistoryInfoGraph.class.getName()); //$NON-NLS-1$;
	}
	{ // instance c'tor:
		log.debug("Creating {}:", this.getClass().getSimpleName()); //$NON-NLS-1$
	}
	private static final long serialVersionUID = -1954281705187898725L;
	private static final boolean LEGENDS_AT_LEFT = false;

	protected final static String TimingDataKey = "timingData"; //$NON-NLS-1$

	protected Button selectAll;
	protected Button selectNone;

	protected Chart chart;

	public HistoryInfoGraph(HistoryInfoService historyInfoService) {
		super(historyInfoService);
		log.debug(this.getClass().getSimpleName() + " created."); //$NON-NLS-1$
	}

	@Override
	protected HorizontalLayout getToolbar() {
		HorizontalLayout toolbar = super.getToolbar();

		this.nameFilter.addValueChangeListener(e -> { if (e.isFromClient()) adjustVisibility(); });
		this.typeFilter.addValueChangeListener(e -> { if (e.isFromClient()) adjustVisibility(); });

		this.selectAll = new Button(Messages.getString("HistoryGraph.Button.SelectAll.Label")); //$NON-NLS-1$
		this.selectAll.addClickListener(_ -> selectAll());

		this.selectNone = new Button(Messages.getString("HistoryGraph.Button.SelectNone.Label")); //$NON-NLS-1$
		this.selectNone.addClickListener(_ -> selectNone());

		toolbar.addComponentAtIndex(toolbar.getComponentCount()-1, this.selectAll);
		toolbar.addComponentAtIndex(toolbar.getComponentCount()-1, this.selectNone);

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

	/* NOTE: this is executed by a different thread - it MUST NOT access any GUI elements! */
	@Override
	protected void fetchData(HashMap <String, Object> res) {
		log.info("HistoryInfoGraph.fetchData"); //$NON-NLS-1$
		ZoneId myZoneId = ZoneId.systemDefault();

		VisibleNodeType nodeTypeFilterValue = NodeTypeLabelProvider.getNodeType(this.typeFilter.getValue());
		LocalDateTime minTimestampValue = (LocalDateTime)res.get(MinTimestampValueKey);
		LocalDateTime maxTimestampValue = (LocalDateTime)res.get(MaxTimestampValueKey);

		List<String> nodeNames = getFilteredNodeNames();
		List<Series> timingData = new ArrayList<>(nodeNames.size());

		for (String name: nodeNames) { // no stream.forEach here because we want to be able to quit preemptively
			if (refreshIsObsolete(res)) {
				log.info("another refresh pending - aborting this one..."); //$NON-NLS-1$
				return; // if meanwhile another one has been triggered then abort this refresh
			}

			log.debug("fetching and converting data for node '{}'", name); //$NON-NLS-1$
			List<HistoryInfo> values = this.historyInfoService.findByFilter(name, nodeTypeFilterValue
			                                                               ,null, null // min-/max-duration doesn't make sense for graphs
			                                                               ,minTimestampValue, maxTimestampValue
			                                                               ,0, 0
			                                                               );

			if (refreshIsObsolete(res)) {
				log.info("another refresh pending - aborting this one..."); //$NON-NLS-1$
				return; // if meanwhile another one has been triggered then abort this refresh
			}

			DataSeries timingSeries = new DataSeries();
			timingSeries.setId(name);
			timingSeries.setName(name);

			int nrDataPoints = values.size();
			log.info("got {} data points for '{}'", nrDataPoints, name); //$NON-NLS-1$
			if (nrDataPoints > 0) {
				Number[][] numberTuples = new Number[nrDataPoints][2];
				for (int idx = 0; idx < nrDataPoints; idx++) {
					HistoryInfo value = values.get(idx);
					LocalDateTime timestamp = value.getTimestamp();
					numberTuples[idx][0] = Date.from(timestamp.atZone(myZoneId).toInstant()).getTime();
					Double duration = value.getDuration();
					numberTuples[idx][1] = duration;
				}
				log.trace("addSeries():"); //$NON-NLS-1$
				timingSeries.addData(numberTuples);
				timingData.add(timingSeries);
			}
		}
		log.info("got timingData for {} series", timingData.size()); //$NON-NLS-1$

		res.put(TimingDataKey, timingData);
	}

	/**
	 * This method takes the DataSeries data and assigns it to the chart
	 * @param timingData
	 */
	@Override
	protected void updateView(HashMap <String, Object> res) {
		log.info("HistoryInfoGraph.updateTable"); //$NON-NLS-1$
		updateMinMaxFields(res);

		if (refreshIsObsolete(res)) {
			log.info("another refresh pending - aborting this one..."); //$NON-NLS-1$
			return; // if meanwhile another one has been triggered then abort this refresh
		}

		// update graph data:
		@SuppressWarnings("unchecked")
		List<Series> timingData = (List<Series>)res.get("timingData"); //$NON-NLS-1$

		log.info("setting chart data for {} series", timingData.size()); //$NON-NLS-1$
		Configuration conf = this.chart.getConfiguration();
		conf.setSeries(timingData);
		this.chart.drawChart(true);
	}

	protected void adjustVisibility() {
		log.info("adjustVisibility"); //$NON-NLS-1$
		UIHandlerSupport.executeLater(this, (Supplier<List<String>>)this::getFilteredNodeNames, this::updateVisibility);
	}

	protected void updateVisibility(List<String> filteredNodeNames) {
		forAllSeriesDo(ds -> ds.setVisible(filteredNodeNames.contains(ds.getName()), true));
	}

	private void forAllSeriesDo(Consumer<DataSeries> func) {
		log.debug("forAllSeriesDo"); //$NON-NLS-1$
		Configuration conf = this.chart.getConfiguration();
		conf.getSeries().forEach(series -> func.accept((DataSeries)series));
		this.chart.drawChart(true);
	}

	private void selectNone() {
		log.info("selectNone"); //$NON-NLS-1$
		UIHandlerSupport.executeLater(this, 0, () -> forAllSeriesDo(ds -> ds.setVisible(false, true)));
	}

	private void selectAll() {
		log.info("selectAll"); //$NON-NLS-1$
		UIHandlerSupport.executeLater(this, 0, () -> forAllSeriesDo(ds -> ds.setVisible(true, true)));
	}

	static {
		log.debug("{} static c'tor end.", HistoryInfoTable.class.getName()); //$NON-NLS-1$;
	}
}
