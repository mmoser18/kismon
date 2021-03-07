package net.mmo.utils.kism.ui.view.results;

import static net.mmo.utils.kism.ui.UIConstants.CombinedClassName;
import static net.mmo.utils.kism.ui.UIConstants.LabelPaddingTheme;
import static net.mmo.utils.kism.ui.UIConstants.ResultViewField;

import java.time.LocalDateTime;

import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.RequestInfoService;
import net.mmo.utils.kism.entities.nodes.NodeType;
import net.mmo.utils.kism.entities.results.RequestInfo;
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
@Route(value="results", layout = MainLayout.class)
@PageTitle("Results | " + CommonConstants.ApplicationFullName)
@Component // to make it possible to @Autowire it
@Scope("prototype") // to ensure every test run gets a fresh instance.
@Slf4j
public class RequestInfoTable extends AbstractRequestInfoView
{
	private static final long serialVersionUID = -352635233754729967L;

	// public to allow access by test classes
	public static final String DurationMinimumFieldClassName = ResultViewField + "duration-minimum"; //$NON-NLS-1$
	public static final String DurationMaximumFieldClassName = ResultViewField + "duration-maximum"; //$NON-NLS-1$
	public static final String DurationExtremesFieldClassName = ResultViewField + "duration-extremes"; //$NON-NLS-1$
	public static final String TimestampMinimumFieldClassName = ResultViewField + "timestamp-minimum"; //$NON-NLS-1$
	public static final String TimestampMaximumFieldClassName = ResultViewField + "timestamp-maximum"; //$NON-NLS-1$
	public static final String TimestampExtremesFieldClassName = ResultViewField + "timestamp-extremes"; //$NON-NLS-1$

	protected NumberField durationMinimum;
	protected NumberField durationMaximum;

	protected DateTimePicker timestampMinimum;
	protected DateTimePicker timestampMaximum;

	protected Grid<RequestInfo> grid;


	// This gets autowired but still puzzled which annotation causes this...
	public RequestInfoTable(RequestInfoService requestInfoService, UIHandlerSupport uiHandlerSupport) {
		super(requestInfoService, uiHandlerSupport);

		refreshData();
		log.debug("{} complete.", this.getClassName()); //$NON-NLS-1$
	}

	@Override
	protected HorizontalLayout getToolbar() {
		HorizontalLayout toolbar = super.getToolbar();

		this.nameFilter.addValueChangeListener(e -> { resetMinMaxValues(); refreshData(); });
		this.typeFilter.addValueChangeListener(e -> { resetMinMaxValues(); refreshData(); });

		this.durationMinimum = new NumberField();
		this.durationMaximum = new NumberField();

		this.durationMinimum.setLabel(Messages.getString("ResultTable.Duration.Minimum.Label")); //$NON-NLS-1$
		this.durationMinimum.setClassName(DurationMinimumFieldClassName);
		this.durationMinimum.setThemeName(LabelPaddingTheme);
		this.durationMinimum.setHasControls(true);
		this.durationMinimum.setWidth("15em"); //$NON-NLS-1$
		this.durationMinimum.addValueChangeListener(e -> refreshData());

		this.durationMaximum.setLabel(Messages.getString("ResultTable.Duration.Maximum.Label")); //$NON-NLS-1$
		this.durationMaximum.setClassName(DurationMaximumFieldClassName);
		this.durationMaximum.setThemeName(LabelPaddingTheme);
		this.durationMaximum.setHasControls(true);
		this.durationMaximum.setWidth("15em"); //$NON-NLS-1$
		this.durationMaximum.addValueChangeListener(e -> refreshData());

		VerticalLayout durationExtremes = new VerticalLayout();
		durationExtremes.setClassName(DurationExtremesFieldClassName);
		durationExtremes.addClassName(CombinedClassName);
		durationExtremes.add(this.durationMinimum, this.durationMaximum);

		this.timestampMinimum = new DateTimePicker();
		this.timestampMaximum = new DateTimePicker();

		this.timestampMinimum.setLabel(String.format(Messages.getString("ResultTable.Timestamp.Minimum.Label"), UIConstants.EMPTY_STRING)); //$NON-NLS-1$
		this.timestampMinimum.setClassName(TimestampMinimumFieldClassName);
		this.timestampMinimum.setThemeName(LabelPaddingTheme);
		this.timestampMinimum.addValueChangeListener(e -> refreshData());

		this.timestampMaximum.setLabel(String.format(Messages.getString("ResultTable.Timestamp.Maximum.Label"), UIConstants.EMPTY_STRING)); //$NON-NLS-1$
		this.timestampMaximum.setClassName(TimestampMaximumFieldClassName);
		this.timestampMaximum.setThemeName(LabelPaddingTheme);
		this.timestampMaximum.addValueChangeListener(e -> refreshData());

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
		this.grid = new Grid<>(RequestInfo.class); // package visible for UTs - is there no other way?
		this.grid.addClassName(GridClassName);
		this.grid.setSizeFull();
		this.grid.removeAllColumns();

		this.grid.addColumn("name") //$NON-NLS-1$
			.setHeader(Messages.getString("ResultTable.Grid.ColumnName.Name")) //$NON-NLS-1$
			;
		this.grid.addColumn((info) -> NodeTypeLabelProvider.getNodeTypeLabel(info.getNodeType()))
			.setHeader(Messages.getString("ResultTable.Grid.ColumnName." + "Type")) //$NON-NLS-1$ //$NON-NLS-2$
			;
		this.grid.addColumn((info) -> info.getDuration())
			.setHeader(Messages.getString("ResultTable.Grid.ColumnName.Duration")) //$NON-NLS-1$
			.setTextAlign(ColumnTextAlign.END)
			;
		this.grid.addColumn((info) -> info.getTimestamp())
			.setHeader(Messages.getString("ResultTable.Grid.ColumnName.Timestamp")) //$NON-NLS-1$
			.setTextAlign(ColumnTextAlign.END)
			;
		this.grid.addColumn("result") //$NON-NLS-1$
			.setHeader(Messages.getString("ResultTable.Grid.ColumnName.Result")) //$NON-NLS-1$
			;

		this.grid.getColumns().forEach(column ->
		{	column
				.setSortable(true)
				.setAutoWidth(true)
				.setFlexGrow(0);
		});

		this.grid.asSingleSelect().addValueChangeListener(event -> { /* tbd */ }); // getValue() returns the selected item or null if none is selected
		this.grid.addAttachListener(e -> refreshData());

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
		String nameFilterValue = (this.nameFilter.getValue() != null ? this.nameFilter.getValue().replace('*', '%') : null);
		NodeType nodeTypeFilterValue = NodeTypeLabelProvider.getNodeType(this.typeFilter.getValue());

		// adjust min/maxDuration:
		Double minDurationValue = this.durationMinimum.getValue();
		Double maxDurationValue = this.durationMaximum.getValue();

		Double minDuration = this.requestInfoService.findLowestDurationWithFilter(nameFilterValue, nodeTypeFilterValue);
		Double maxDuration = this.requestInfoService.findHighestDurationWithFilter(nameFilterValue, nodeTypeFilterValue);

		if (minDuration != null) {
			double minDurationConverted = minDuration;
			this.durationMinimum.setLabel(String.format(Messages.getString("ResultTable.Duration.Minimum.LblWithMin"), minDurationConverted)); //$NON-NLS-1$
			this.durationMinimum.setMin(minDurationConverted);
		}
		if (maxDuration != null) {
			double maxDurationConverted = maxDuration;
			this.durationMaximum.setLabel(String.format(Messages.getString("ResultTable.Duration.Maximum.LblWithMax"), maxDurationConverted)); //$NON-NLS-1$
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

		LocalDateTime minTimestamp = this.requestInfoService.findLowestTimestampWithFilter(nameFilterValue, nodeTypeFilterValue);
		LocalDateTime maxTimestamp = this.requestInfoService.findHighestTimestampWithFilter(nameFilterValue, nodeTypeFilterValue);

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

		// TODO: replace by call-back data provider to minimize GUI loading when there are LOTs of data:
		this.grid.setItems(this.requestInfoService.findByFilter(nameFilterValue, nodeTypeFilterValue,
		                                                        minDurationValue, maxDurationValue,
		                                                        null, null));
	}

}
