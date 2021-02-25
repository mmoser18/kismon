package net.mmo.utils.kism.ui.view.results;

import static net.mmo.utils.kism.ui.UIConstants.CombinedClassName;
import static net.mmo.utils.kism.ui.UIConstants.LabelPaddingTheme;
import static net.mmo.utils.kism.ui.UIConstants.ResultViewField;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.RequestInfoService;
import net.mmo.utils.kism.entities.nodes.NodeType;
import net.mmo.utils.kism.entities.results.RequestInfo;
import net.mmo.utils.kism.ui.CommonConstants;
import net.mmo.utils.kism.ui.MainLayout;
import net.mmo.utils.kism.ui.view.nodes.NodeFactory;
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
public class RequestInfoView extends VerticalLayout
{
	private static final long serialVersionUID = -352635233754729967L;

	// public to allow access by test classes
	public static final String ViewClassName = "result-view";  //$NON-NLS-1$
	public static final String ToolbarClassName     = "result-view-toolbar";  //$NON-NLS-1$
	public static final String ViewContentClassName = "result-view-content";  //$NON-NLS-1$
	public static final String GridClassName        = "result-view-grid"; //$NON-NLS-1$

	public static final String NameFilterFieldClassName = ResultViewField + "filter-name";  //$NON-NLS-1$
	public static final String TypeFilterFieldClassName = ResultViewField + "filter-type";  //$NON-NLS-1$

	public static final String DurationMinimumFieldClassName = ResultViewField + "duration-minimum"; //$NON-NLS-1$
	public static final String DurationMaximumFieldClassName = ResultViewField + "duration-maximum"; //$NON-NLS-1$
	private static final String DurationExtremesFieldClassName = ResultViewField + "duration-extremes"; //$NON-NLS-1$
	public static final String TimestampMinimumFieldClassName = ResultViewField + "timestamp-minimum"; //$NON-NLS-1$
	public static final String TimestampMaximumFieldClassName = ResultViewField + "timestamp-maximum"; //$NON-NLS-1$
	public static final String TimestampExtremesFieldClassName = ResultViewField + "timestamp-extremes"; //$NON-NLS-1$

	protected RequestInfoService resultService;

	protected TextField nameFilter = new TextField();
	protected Select<String> typeFilter = new Select<>();

	protected NumberField durationMinimum = new NumberField();
	protected NumberField durationMaximum = new NumberField();

	protected DateTimePicker timestampMinimum = new DateTimePicker();
	protected DateTimePicker timestampMaximum = new DateTimePicker();

	protected Grid<RequestInfo> grid = new Grid<>(RequestInfo.class); // package visible for UTs - is there no other way?
	protected Div content;

	protected NumberFormat numberFormat = DecimalFormat.getInstance();


	// This gets autowired but still puzzled which annotation causes this...
	public RequestInfoView(RequestInfoService resultService) {
		log.debug("Creating {}:", this.getClass().getSimpleName()); //$NON-NLS-1$
		this.resultService = resultService;

		addClassName(ViewClassName);
		setSizeFull(); // use entire browser window

//		fixJavaGroupingSeparatorBug();

		configureGrid();

		log.trace("Creating content:"); //$NON-NLS-1$
		this.content = new Div(this.grid);
		this.content.addClassName(ViewContentClassName);
		this.content.setSizeFull();

		add(getToolbar(), this.content);

		updateList();
		log.debug("{} complete.", this.getClassName()); //$NON-NLS-1$
	}

	@SuppressWarnings("deprecation")
	protected HorizontalLayout getToolbar() {
		log.trace("Creating toolbar:"); //$NON-NLS-1$
		this.nameFilter.setClassName(NameFilterFieldClassName);
		this.nameFilter.setLabel(Messages.getString("RequestInfoView.TextField.NameFilter.Label")); //$NON-NLS-1$
		this.nameFilter.setPlaceholder(Messages.getString("RequestInfoView.TextField.NameFilter.PlaceHolder")); //$NON-NLS-1$
		this.nameFilter.setClearButtonVisible(true);
		this.nameFilter.setValueChangeMode(ValueChangeMode.LAZY);
		this.nameFilter.setThemeName(LabelPaddingTheme);
		this.nameFilter.addValueChangeListener(e -> { resetMinMaxValues(); updateList(); });

		this.typeFilter.setClassName(TypeFilterFieldClassName);
		this.typeFilter.setLabel(Messages.getString("RequestInfoView.TextField.TypeFilter.Label")); //$NON-NLS-1$
		this.typeFilter.setItems(NodeFactory.getLeafNodeTypes().stream()
		                        .map((type) -> NodeTypeLabelProvider.getNodeTypeLabel(type))
		                        .collect(Collectors.toList()));
		this.typeFilter.setEmptySelectionAllowed(true);
		this.typeFilter.setEmptySelectionCaption(Messages.getString("RequestInfoView.TextField.TypeFilter.None")); //$NON-NLS-1$
		this.typeFilter.addValueChangeListener(e -> { resetMinMaxValues(); updateList(); });

		this.durationMinimum.setLabel(Messages.getString("RequestInfoView.Duration.Minimum.Label")); //$NON-NLS-1$
		this.durationMinimum.setClassName(DurationMinimumFieldClassName);
		this.durationMinimum.setThemeName(LabelPaddingTheme);
		this.durationMinimum.setHasControls(true);
		this.durationMinimum.setMinLength(10);
		this.durationMinimum.setMaxLength(15);
		this.durationMinimum.addValueChangeListener(e -> updateList());

		this.durationMaximum.setLabel(Messages.getString("RequestInfoView.Duration.Maximum.Label")); //$NON-NLS-1$
		this.durationMaximum.setClassName(DurationMaximumFieldClassName);
		this.durationMaximum.setThemeName(LabelPaddingTheme);
		this.durationMaximum.setHasControls(true);
		this.durationMaximum.setMinLength(10);
		this.durationMaximum.setMaxLength(15);
		this.durationMaximum.addValueChangeListener(e -> updateList());

		VerticalLayout durationExtremes = new VerticalLayout();
		durationExtremes.setClassName(DurationExtremesFieldClassName);
		durationExtremes.addClassName(CombinedClassName);
		durationExtremes.add(this.durationMinimum, this.durationMaximum);

		this.timestampMinimum.setLabel(Messages.getString("RequestInfoView.Timestamp.Minimum.Label")); //$NON-NLS-1$
		this.timestampMinimum.setClassName(TimestampMinimumFieldClassName);
		this.timestampMinimum.setThemeName(LabelPaddingTheme);
		this.timestampMinimum.addValueChangeListener(e -> updateList());

		this.timestampMaximum.setLabel(Messages.getString("RequestInfoView.Timestamp.Maximum.Label")); //$NON-NLS-1$
		this.timestampMaximum.setClassName(TimestampMaximumFieldClassName);
		this.timestampMaximum.setThemeName(LabelPaddingTheme);
		this.timestampMaximum.addValueChangeListener(e -> updateList());

		VerticalLayout timestampExtremes = new VerticalLayout();
		timestampExtremes.setClassName(TimestampExtremesFieldClassName);
		timestampExtremes.addClassName(CombinedClassName);
		timestampExtremes.add(this.timestampMinimum, this.timestampMaximum);

		HorizontalLayout toolbar = new HorizontalLayout(this.nameFilter, this.typeFilter,
		                                                durationExtremes, timestampExtremes);

		toolbar.addClassName(ToolbarClassName);
		return toolbar;
	}

	protected void configureGrid() {
		log.trace("Configuring Grid:"); //$NON-NLS-1$
		this.grid.addClassName(GridClassName);
		this.grid.setSizeFull();
		this.grid.removeAllColumns();

		this.grid.addColumn("name") //$NON-NLS-1$
			.setHeader(Messages.getString("ResultInfoView.Grid.ColumnName.Name")) //$NON-NLS-1$
			;
		this.grid.addColumn((info) -> NodeTypeLabelProvider.getNodeTypeLabel(info.getNodeType()))
			.setHeader(Messages.getString("ResultInfoView.Grid.ColumnName." + "Type")) //$NON-NLS-1$ //$NON-NLS-2$
			;
		this.grid.addColumn((info) -> info.getDuration())
			.setHeader(Messages.getString("ResultInfoView.Grid.ColumnName.Duration")) //$NON-NLS-1$
			;
		this.grid.addColumn((info) -> info.getTimestamp())
			.setHeader(Messages.getString("ResultInfoView.Grid.ColumnName.Timestamp")) //$NON-NLS-1$
			.setTextAlign(ColumnTextAlign.END)
			;
		this.grid.addColumn("result") //$NON-NLS-1$
			.setHeader(Messages.getString("ResultInfoView.Grid.ColumnName.Result")) //$NON-NLS-1$
			.setTextAlign(ColumnTextAlign.END)
			;

		this.grid.getColumns().forEach(column ->
		{	column
				.setSortable(true)
				.setAutoWidth(true)
				.setFlexGrow(0);
		});

		this.grid.asSingleSelect().addValueChangeListener(event -> { /* tbd */ }); // getValue() returns the selected item or null if none is selected

		this.grid.addAttachListener(e -> updateList());
	}

	protected void resetMinMaxValues() {
		this.durationMinimum.setValue(null);
		this.durationMaximum.setValue(null);
		this.timestampMinimum.setValue(null);
		this.timestampMaximum.setValue(null);
	}

//	protected static String timestampFormatted(LocalDateTime timestamp) {
//		return (timestamp != null ?timestamp.format(UIConstants.dateTimeFormatter) : UIConstants.EMPTY_STRING);
//	}

	protected void updateList() {
		String nameFilterValue = (this.nameFilter.getValue() != null ? this.nameFilter.getValue().replace('*', '%') : null);
		NodeType nodeTypeFilterValue = NodeTypeLabelProvider.getNodeType(this.typeFilter.getValue());

		// adjust min/maxDuration:
		Double minDurationValue = this.durationMinimum.getValue();
		Double maxDurationValue = this.durationMaximum.getValue();

		Double minDuration = this.resultService.findLowestDurationWithFilter(nameFilterValue, nodeTypeFilterValue);
		Double maxDuration = this.resultService.findHighestDurationWithFilter(nameFilterValue, nodeTypeFilterValue);

		if (minDuration != null) {
			double minDurationConverted = minDuration;
			this.durationMinimum.setLabel(Messages.getString("RequestInfoView.Duration.Minimum.Label") + " (>" + minDurationConverted + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			this.durationMinimum.setMin(minDurationConverted);
		}
		if (maxDuration != null) {
			double maxDurationConverted = maxDuration;
			this.durationMaximum.setLabel(Messages.getString("RequestInfoView.Duration.Maximum.Label") + " (<" + maxDurationConverted + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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

		LocalDateTime minTimestamp = this.resultService.findLowestTimestampWithFilter(nameFilterValue, nodeTypeFilterValue);
		LocalDateTime maxTimestamp = this.resultService.findHighestTimestampWithFilter(nameFilterValue, nodeTypeFilterValue);

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
		this.grid.setItems(this.resultService.findByFilter(nameFilterValue, nodeTypeFilterValue,
		                                                   minDurationValue, maxDurationValue,
		                                                   null, null));
	}

//	// for some stupid reason Java does not take over the OS' number digit grouping and separator
//	// settings (at least not on Windows 10) - so I tried to work around that:
//	protected void fixJavaGroupingSeparatorBug() {
//		DecimalFormatSymbols customSymbol = DecimalFormatSymbols.getInstance();
//		boolean adjustNumberFormat = false;
//		String groupingSeparator = AppProperties.getProperties().getProperty("DigitGroupingSeparator"); //$NON-NLS-1$
//		if (nonBlank(groupingSeparator)) {
//			customSymbol.setGroupingSeparator(groupingSeparator.charAt(0));
//			adjustNumberFormat = true;
//		}
//		String decimalSeparator = AppProperties.getProperties().getProperty("DecimalSeparator"); //$NON-NLS-1$
//		if (nonBlank(decimalSeparator)) {
//			customSymbol.setDecimalSeparator(decimalSeparator.charAt(0));
//			adjustNumberFormat = true;
//		}
//		if (adjustNumberFormat) {
//			((DecimalFormat)this.numberFormat).setDecimalFormatSymbols(customSymbol);
//			this.numberFormat.setGroupingUsed(true);
//		}
//	}
//	protected static boolean nonBlank(String str) {
//		return str != null && !str.isBlank();
//	}
}
