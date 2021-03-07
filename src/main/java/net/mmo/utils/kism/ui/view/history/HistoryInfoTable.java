package net.mmo.utils.kism.ui.view.history;

import static net.mmo.utils.kism.ui.UIConstants.CombinedClassName;
import static net.mmo.utils.kism.ui.UIConstants.HistoryViewField;
import static net.mmo.utils.kism.ui.UIConstants.LabelPaddingTheme;

import java.time.LocalDateTime;
import java.util.List;

import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.HistoryInfoService;
import net.mmo.utils.kism.entities.history.HistoryInfo;
import net.mmo.utils.kism.entities.nodes.NodeType;
import net.mmo.utils.kism.ui.CommonConstants;
import net.mmo.utils.kism.ui.MainLayout;
import net.mmo.utils.kism.ui.UIConstants;
import net.mmo.utils.kism.ui.utils.UIHandlerSupport;
import net.mmo.utils.kism.ui.view.nodes.NodeTypeLabelProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Defines the application's main view, routed to via the base URL
 */
@Route(value="history-table", layout = MainLayout.class)
@PageTitle("History Table | " + CommonConstants.ApplicationFullName)
@Component // to make it possible to @Autowire it
@Scope("prototype") // to ensure every test run gets a fresh instance.
@Slf4j
public class HistoryInfoTable extends AbstractHistoryInfoView
{
	private static final long serialVersionUID = -352635233754729967L;

	// public to allow access by test classes
	public static final String DurationMinimumFieldClassName = HistoryViewField + "duration-minimum"; //$NON-NLS-1$
	public static final String DurationMaximumFieldClassName = HistoryViewField + "duration-maximum"; //$NON-NLS-1$
	public static final String DurationExtremesFieldClassName = HistoryViewField + "duration-extremes"; //$NON-NLS-1$
	public static final String TimestampMinimumFieldClassName = HistoryViewField + "timestamp-minimum"; //$NON-NLS-1$
	public static final String TimestampMaximumFieldClassName = HistoryViewField + "timestamp-maximum"; //$NON-NLS-1$
	public static final String TimestampExtremesFieldClassName = HistoryViewField + "timestamp-extremes"; //$NON-NLS-1$

	protected NumberField durationMinimum;
	protected NumberField durationMaximum;

	protected DateTimePicker timestampMinimum;
	protected DateTimePicker timestampMaximum;

	protected Grid<HistoryInfo> grid;


	// This gets autowired but still puzzled which annotation causes this...
	public HistoryInfoTable(HistoryInfoService historyInfoService) {
		super(historyInfoService);
	}

	@Override
	protected HorizontalLayout getToolbar() {
		HorizontalLayout toolbar = super.getToolbar();

		this.nameFilter.addValueChangeListener(e -> { if (e.isFromClient()) { resetMinMaxValues(); refreshData(); }});
		this.typeFilter.addValueChangeListener(e -> { if (e.isFromClient()) { resetMinMaxValues(); refreshData(); }});

		this.durationMinimum = new NumberField();
		this.durationMaximum = new NumberField();

		this.durationMinimum.setLabel(Messages.getString("HistoryTable.Duration.Minimum.Label")); //$NON-NLS-1$
		this.durationMinimum.setClassName(DurationMinimumFieldClassName);
		this.durationMinimum.setThemeName(LabelPaddingTheme);
		this.durationMinimum.setHasControls(true);
		this.durationMinimum.setWidth("12em"); //$NON-NLS-1$
		this.durationMinimum.addValueChangeListener(e -> { if (e.isFromClient()) { refreshData(); }});

		this.durationMaximum.setLabel(Messages.getString("HistoryTable.Duration.Maximum.Label")); //$NON-NLS-1$
		this.durationMaximum.setClassName(DurationMaximumFieldClassName);
		this.durationMaximum.setThemeName(LabelPaddingTheme);
		this.durationMaximum.setHasControls(true);
		this.durationMaximum.setWidth("12em"); //$NON-NLS-1$
		this.durationMaximum.addValueChangeListener(e -> { if (e.isFromClient()) { refreshData(); }});

		VerticalLayout durationExtremes = new VerticalLayout();
		durationExtremes.setClassName(DurationExtremesFieldClassName);
		durationExtremes.addClassName(CombinedClassName);
		durationExtremes.add(this.durationMinimum, this.durationMaximum);

		this.timestampMinimum = new DateTimePicker();
		this.timestampMaximum = new DateTimePicker();

		this.timestampMinimum.setLabel(String.format(Messages.getString("HistoryTable.Timestamp.Minimum.Label"), UIConstants.EMPTY_STRING)); //$NON-NLS-1$
		this.timestampMinimum.setClassName(TimestampMinimumFieldClassName);
		this.timestampMinimum.setThemeName(LabelPaddingTheme);
		this.timestampMinimum.addValueChangeListener(e -> { if (e.isFromClient()) { refreshData(); }});

		this.timestampMaximum.setLabel(String.format(Messages.getString("HistoryTable.Timestamp.Maximum.Label"), UIConstants.EMPTY_STRING)); //$NON-NLS-1$
		this.timestampMaximum.setClassName(TimestampMaximumFieldClassName);
		this.timestampMaximum.setThemeName(LabelPaddingTheme);
		this.timestampMaximum.addValueChangeListener(e -> { if (e.isFromClient()) { refreshData(); }});

		VerticalLayout timestampExtremes = new VerticalLayout();
		timestampExtremes.setClassName(TimestampExtremesFieldClassName);
		timestampExtremes.addClassName(CombinedClassName);
		timestampExtremes.add(this.timestampMinimum, this.timestampMaximum);

		toolbar.add(durationExtremes, timestampExtremes, this.refreshData);
		return toolbar;
	}

	@Override
	protected void configureContent() {
		log.trace("Configuring Grid:"); //$NON-NLS-1$
		this.grid = new Grid<>(HistoryInfo.class); // package visible for UTs - is there no other way?
		this.grid.addClassName(GridClassName);
		this.grid.setSizeFull();
		this.grid.removeAllColumns();

		this.grid.addColumn("name") //$NON-NLS-1$
			.setHeader(Messages.getString("HistoryTable.Grid.ColumnName.Name")) //$NON-NLS-1$
			;
		this.grid.addColumn((info) -> NodeTypeLabelProvider.getNodeTypeLabel(info.getNodeType()))
			.setHeader(Messages.getString("HistoryTable.Grid.ColumnName." + "Type")) //$NON-NLS-1$ //$NON-NLS-2$
			;
		this.grid.addColumn((info) -> info.getDuration())
			.setHeader(Messages.getString("HistoryTable.Grid.ColumnName.Duration")) //$NON-NLS-1$
			.setTextAlign(ColumnTextAlign.END)
			;
		this.grid.addColumn((info) -> info.getTimestamp())
			.setHeader(Messages.getString("HistoryTable.Grid.ColumnName.Timestamp")) //$NON-NLS-1$
			.setTextAlign(ColumnTextAlign.END)
			;
		this.grid.addColumn("result") //$NON-NLS-1$
			.setHeader(Messages.getString("HistoryTable.Grid.ColumnName.Result")) //$NON-NLS-1$
			;

		this.grid.getColumns().forEach(column ->
		{	column
				.setSortable(true)
				.setAutoWidth(true)
				.setFlexGrow(0);
		});

		this.grid.asSingleSelect().addValueChangeListener(event -> { /* tbd */ }); // getValue() returns the selected item or null if none is selected
		this.grid.addAttachListener(e -> { if (e.isFromClient()) { refreshData(); }});

		this.content.add(this.grid);
	}

	protected void resetMinMaxValues() {
		this.durationMinimum.setValue(null);
		this.durationMaximum.setValue(null);
		this.timestampMinimum.setValue(null);
		this.timestampMaximum.setValue(null);
	}

	@Override
	protected void refreshData() {
		log.info("refreshData"); //$NON-NLS-1$
		UIHandlerSupport.executeLater(this::fetchData, this::updateTable);
	}

	protected List<HistoryInfo> fetchData() {
		log.info("fetchData"); //$NON-NLS-1$
		String nameFilterValue = (this.nameFilter.getValue() != null ? this.nameFilter.getValue().replace('*', '%') : null);
		NodeType nodeTypeFilterValue = NodeTypeLabelProvider.getNodeType(this.typeFilter.getValue());

		// adjust min/maxDuration:
		Double minDurationValue = this.durationMinimum.getValue();
		Double maxDurationValue = this.durationMaximum.getValue();

		Double minDuration = this.historyInfoService.findLowestDurationWithFilter(nameFilterValue, nodeTypeFilterValue);
		Double maxDuration = this.historyInfoService.findHighestDurationWithFilter(nameFilterValue, nodeTypeFilterValue);

		if (minDuration != null) {
			double minDurationConverted = minDuration;
			this.durationMinimum.setLabel(String.format(Messages.getString("HistoryTable.Duration.Minimum.LblWithMin"), minDurationConverted)); //$NON-NLS-1$
			this.durationMinimum.setMin(minDurationConverted);
		}
		if (maxDuration != null) {
			double maxDurationConverted = maxDuration;
			this.durationMaximum.setLabel(String.format(Messages.getString("HistoryTable.Duration.Maximum.LblWithMax"), maxDurationConverted)); //$NON-NLS-1$
			this.durationMaximum.setMax(maxDurationConverted);
		}

		// limit the values to [min..max] (if available):
		if (minDuration != null && (minDurationValue == null || minDurationValue < minDuration)) {
			minDurationValue = minDuration;
		}
		if (maxDuration != null && (maxDurationValue == null || maxDurationValue > maxDuration)) {
			maxDurationValue = maxDuration;
		}

		if (minDurationValue != null) {
			this.durationMinimum.setValue(minDurationValue);
			// set the max/min to the respective other value: (is this a clever idea?)
			this.durationMaximum.setMin(minDurationValue);
		}
		if (maxDurationValue != null) {
			this.durationMaximum.setValue(maxDurationValue);
			// set the max/min to the respective other value: (is this a clever idea?)
			this.durationMinimum.setMax(maxDurationValue);
		}

		// adjust min/maxTimestamp:
		LocalDateTime minTimestampValue = this.timestampMinimum.getValue();
		LocalDateTime maxTimestampValue = this.timestampMaximum.getValue();

		LocalDateTime minTimestamp = this.historyInfoService.findLowestTimestampWithFilter(nameFilterValue, nodeTypeFilterValue);
		LocalDateTime maxTimestamp = this.historyInfoService.findHighestTimestampWithFilter(nameFilterValue, nodeTypeFilterValue);

		// limit the values to [min..max] (if available):
		if (minTimestamp != null && (minTimestampValue == null || minTimestampValue.compareTo(minTimestamp) < 0)) {
			minTimestampValue = minTimestamp;
		}
		if (maxTimestamp != null && (maxTimestampValue == null || maxTimestampValue.compareTo(maxTimestamp) > 0)) {
			maxTimestampValue = maxTimestamp;
		}

		if (minTimestampValue != null) {
			this.timestampMinimum.setValue(minTimestampValue);
			// set the max/min to the respective other value: (is this a clever idea?)
			// this.timestampMaximum.setMin(minTimestampValue);
		}
		if (maxTimestampValue != null) {
			this.timestampMaximum.setValue(maxTimestampValue);
			// set the max/min to the respective other value: (is this a clever idea?)
			// this.timestampMinimum.setMax(maxTimestampValue);
		}

		return this.historyInfoService.findByFilter(nameFilterValue, nodeTypeFilterValue,
		                                            minDurationValue, maxDurationValue,
		                                            null, null);
	}

	protected void updateTable(List<HistoryInfo> data) {
		log.info("updateTable"); //$NON-NLS-1$
		this.grid.setItems(data);
	}
}
