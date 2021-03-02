/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 05.10.2020
 */

package net.mmo.utils.kism.ui.view.results;

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
import net.mmo.utils.kism.ui.view.dashboard.Messages;
import net.mmo.utils.kism.ui.view.nodes.NodeTypeLabelProvider;
import org.springframework.util.comparator.Comparators;

@Route(value = "graph", layout = MainLayout.class) // route-name, parent // Note: this also acts as @Component annotation!
@PageTitle("Graph | " + CommonConstants.ApplicationFullName)
@Slf4j
public class RequestInfoGraph extends AbstractRequestInfoView
{
	private static final long serialVersionUID = -1954281705187898725L;
	private static final boolean LEGENDS_AT_LEFT = false;

	protected Button selectAll;
	protected Button selectNone;
	protected Button refreshData;

	protected Chart chart;
	LocalDateTime lastUpate;

	public RequestInfoGraph(RequestInfoService requestInfoService) {
		super(requestInfoService);
		setDefaultHorizontalComponentAlignment(Alignment.CENTER);
	}

	@Override
	protected HorizontalLayout getToolbar() {
		HorizontalLayout toolbar = super.getToolbar();

		this.nameFilter.addValueChangeListener(e -> { if (e.isFromClient()) adjustVisibility(); });
		this.typeFilter.addValueChangeListener(e -> { if (e.isFromClient()) adjustVisibility(); });

		this.selectAll = new Button(Messages.getString("ResultGraph.Button.SelectAll.Label")); //$NON-NLS-1$
		this.selectAll.addClickListener(e -> selectAll());

		this.selectNone = new Button(Messages.getString("ResultGraph.Button.SelectNone.Label")); //$NON-NLS-1$
		this.selectNone.addClickListener(e -> selectNone());

		this.refreshData = new Button(Messages.getString("ResultGraph.Button.RefreshData.Label")); //$NON-NLS-1$
		this.refreshData.addClickListener(e -> populateData());

		toolbar.add(this.selectAll, this.selectNone, this.refreshData);
		return toolbar;
	}

	@Override
	protected void configureContent() {
		log.info("getTimingChart:"); //$NON-NLS-1$
		this.chart = new Chart(ChartType.LINE);
		this.chart.setTimeline(true);

		Configuration conf = this.chart.getConfiguration();
		conf.setTitle(Messages.getString("ResultGraph.Title")); //$NON-NLS-1$
		conf.setSubTitle(String.format(Messages.getString("ResultGraph.SubTitle"), this.nameFilter.getValue())); //$NON-NLS-1$

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
		xAxis.setTitle(new AxisTitle(Messages.getString("ResultGraph.XAxis.Label"))); //$NON-NLS-1$
		xAxis.setType(AxisType.DATETIME);

		YAxis yAxis = conf.getyAxis();
		yAxis.setTitle(new AxisTitle(Messages.getString("ResultGraph.YAxis.Label"))); //$NON-NLS-1$
		yAxis.setMin(0);

		this.content.add(this.chart);

		populateData();
	}

	protected void populateData() {
		ZoneId myZoneId = ZoneId.systemDefault();
		this.lastUpate = LocalDateTime.now();

		Configuration conf = this.chart.getConfiguration();
		conf.setSeries(new ArrayList<>()); // clear old series

		List<String> allNodeNames = getAllNodeNames();

		String oldSelection = this.nameFilter.getValue();
		this.nameFilter.setItems(allNodeNames); // need to update this - it could change over time
		if (oldSelection != null && !oldSelection.isBlank()) this.nameFilter.setValue(oldSelection);

		allNodeNames.forEach(name ->
			{
				log.info("fetching and converting data for node '{}'", name); //$NON-NLS-1$
				List<RequestInfo> values = this.requestInfoService.findByFilter(name, null, null, null, null, null);

				DataSeries timingData = new DataSeries();
				timingData.setId(name);
				timingData.setName(name);

				int nrDataPoints = values.size();
				log.info("got {} points for node '{}'", nrDataPoints, name); //$NON-NLS-1$
				Number[][] numberTuples = new Number[nrDataPoints][2];
				for (int idx = 0; idx < nrDataPoints; idx++) {
					RequestInfo value = values.get(idx);
					LocalDateTime timestamp = value.getTimestamp();
					Double duration = value.getDuration();
					numberTuples[idx][0] = Date.from(timestamp.atZone(myZoneId).toInstant()).getTime();
					numberTuples[idx][1] = duration;
				}
				log.info("addSeries():"); //$NON-NLS-1$
				timingData.addData(numberTuples);
				conf.addSeries(timingData);
			});

		adjustVisibility();

//		runWhileAttached(chart, () -> {
//			List<RequestInfo> values = this.requestInfoService.findByFilter(name, null, null, null, this.lastUpate, null);
//			final long x = System.currentTimeMillis();
//			final double y = random.nextDouble();
//			series.add(new DataSeriesItem(x, y), true, true);
//		}, 1000, 1000);
	}

	protected void adjustVisibility() {
		log.info("adjustVisibility"); //$NON-NLS-1$
		List<String> filteredNodeNames = getFilteredNodeNames();
		forAllSeriesDo(ds -> ds.setVisible(filteredNodeNames.contains(ds.getName()), true));
	}

	List<String> getAllNodeNames() {
		List<String> allNodeNames = this.requestInfoService.getNodeNames(null, null);
		allNodeNames.sort(Comparators.comparable());
		log.info("nr of allNodeNames: {}", allNodeNames.size()); //$NON-NLS-1$
		return allNodeNames;
	}

	List<String> getFilteredNodeNames() {
		String nameFilterValue = (this.nameFilter.getValue() != null ? this.nameFilter.getValue().replace('*', '%') : null);
		NodeType nodeTypeFilterValue = NodeTypeLabelProvider.getNodeType(this.typeFilter.getValue());
		return getFilteredNodeNames(nameFilterValue, nodeTypeFilterValue);
	}

	List<String> getFilteredNodeNames(String nameFilterValue, NodeType nodeTypeFilterValue) {
		List<String> filteredNodeNames = this.requestInfoService.getNodeNames(nameFilterValue, nodeTypeFilterValue);
		filteredNodeNames.sort(Comparators.comparable());
		log.info("nr of filteredNodeNames: {}", filteredNodeNames.size()); //$NON-NLS-1$
		return filteredNodeNames;
	}

	private void selectNone() {
		log.info("selectNone"); //$NON-NLS-1$
		forAllSeriesDo(ds -> ds.setVisible(false, true));
	}

	private void selectAll() {
		log.info("selectAll"); //$NON-NLS-1$
		forAllSeriesDo(ds -> ds.setVisible(true, true));
	}

	private void forAllSeriesDo(Consumer<DataSeries> func) {
		Configuration conf = this.chart.getConfiguration();
		conf.getSeries().forEach(series -> func.accept((DataSeries)series));
	}

}
