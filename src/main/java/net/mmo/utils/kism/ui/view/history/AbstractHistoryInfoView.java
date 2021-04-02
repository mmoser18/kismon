package net.mmo.utils.kism.ui.view.history;

import static net.mmo.utils.kism.ui.UIConstants.CombinedClassName;
import static net.mmo.utils.kism.ui.UIConstants.HistoryViewField;
import static net.mmo.utils.kism.ui.UIConstants.LabelPaddingTheme;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.HistoryInfoService;
import net.mmo.utils.kism.entities.nodes.NodeFactory;
import net.mmo.utils.kism.entities.nodes.NodeType;
import net.mmo.utils.kism.ui.utils.UIHandlerSupport;
import net.mmo.utils.kism.ui.view.nodes.NodeTypeLabelProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.comparator.Comparators;
import org.vaadin.miki.superfields.dates.SuperDateTimePicker;

/**
 * Defines the application's main view, routed to via the base URL
 */
@Component // to make it possible to @Autowire it
@Scope("prototype") // to ensure every test run gets a fresh instance.
@Slf4j
abstract public class AbstractHistoryInfoView <T extends Object> extends VerticalLayout
{
	private static final long serialVersionUID = 697446986998667247L;

	// public to allow access by test classes
	public static final String ViewClassName = "history-view";  //$NON-NLS-1$
	public static final String ToolbarClassName     = "history-view-toolbar";  //$NON-NLS-1$
	public static final String ViewContentClassName = "history-view-content";  //$NON-NLS-1$
	public static final String GridClassName        = "history-view-grid"; //$NON-NLS-1$

	public static final String NameFilterFieldClassName = HistoryViewField + "filter-name";  //$NON-NLS-1$
	public static final String TypeFilterFieldClassName = HistoryViewField + "filter-type";  //$NON-NLS-1$
	public static final String FiltersClassName = HistoryViewField + "filters";  //$NON-NLS-1$
	// public to allow access by test classes
	public static final String DurationMinimumFieldClassName = HistoryViewField + "duration-minimum"; //$NON-NLS-1$
	public static final String DurationMaximumFieldClassName = HistoryViewField + "duration-maximum"; //$NON-NLS-1$
	public static final String DurationExtremesFieldClassName = HistoryViewField + "duration-extremes"; //$NON-NLS-1$
	public static final String TimestampMinimumFieldClassName = HistoryViewField + "timestamp-minimum"; //$NON-NLS-1$
	public static final String TimestampMaximumFieldClassName = HistoryViewField + "timestamp-maximum"; //$NON-NLS-1$
	public static final String TimestampExtremesFieldClassName = HistoryViewField + "timestamp-extremes"; //$NON-NLS-1$

	protected static final String NameFilterValueKey = "nameFilterValue"; //$NON-NLS-1$
	protected static final String TypeFilterValueKey = "typeFilterValue"; //$NON-NLS-1$

	protected static final String MinTimestampValueKey = "minTimestampValue"; //$NON-NLS-1$
	protected static final String MaxTimestampValueKey = "maxTimestampValue"; //$NON-NLS-1$
	protected static final String MinTimestampFoundKey = "minTimestampFound"; //$NON-NLS-1$
	protected static final String MaxTimestampFoundKey = "maxTimestampFound"; //$NON-NLS-1$

	protected MyDateTimePicker timestampMinimum;
	protected MyDateTimePicker timestampMaximum;

	protected ComboBox<String> nameFilter = new ComboBox<>();
	protected String nameFilterValueStr;
	protected Select<String> typeFilter = new Select<>();
	protected Button refreshData;
	LocalDateTime lastUpdate;

	protected Div content;

	// autowired:
	protected HistoryInfoService historyInfoService;

	// This gets autowired but still puzzled which annotation causes this...
	public AbstractHistoryInfoView(HistoryInfoService historyInfoService) {
		log.debug("Creating {}:", this.getClass().getSimpleName()); //$NON-NLS-1$
		this.historyInfoService = historyInfoService;
		addClassName(ViewClassName);
		setSizeFull(); // use entire browser window

		this.content = new Div();
		this.content.addClassName(ViewContentClassName);
		setDefaultHorizontalComponentAlignment(Alignment.CENTER);

		this.content.setSizeFull();

		configureContent();
		add(getToolbar(), this.content);

		log.debug("{} complete.", this.getClassName()); //$NON-NLS-1$
	}

	abstract protected void configureContent();

	protected HorizontalLayout getToolbar() {
		log.trace("Creating toolbar:"); //$NON-NLS-1$

		this.nameFilter.setClassName(NameFilterFieldClassName);
		this.nameFilter.setLabel(Messages.getString("HistoryView.TextField.NameFilter.Label")); //$NON-NLS-1$
		this.nameFilter.setPlaceholder(Messages.getString("HistoryView.TextField.NameFilter.PlaceHolder")); //$NON-NLS-1$
		this.nameFilter.setClearButtonVisible(true);
		this.nameFilter.setAllowCustomValue(true);
		this.nameFilter.addCustomValueSetListener((e) -> {
			if (e.isFromClient()) {
				log.info("custom value: '{}'", e.getDetail()); //$NON-NLS-1$
				this.nameFilterValueStr = e.getDetail();
				try {
					this.nameFilter.setValue(this.nameFilterValueStr);
				} catch (Exception ex) {
					// ignore - even if setAllowCustomValue is set to true the ComboBox only accepts values from its predefined list ||-(
				}
				resetMinMaxValues();
				refreshData();
			}
		});
//		this.nameFilter.addValueChangeListener(e -> {
//			log.info("nameFilter changed: fromClient={}", e.isFromClient()); //$NON-NLS-1$
//			if (e.isFromClient()) {
//				resetMinMaxValues();
//				refreshData();
//			}
//		});
		UIHandlerSupport.executeLater(this, this::getAllNodeNames, (res) -> this.nameFilter.setItems(res));

		this.typeFilter.setClassName(TypeFilterFieldClassName);
		this.typeFilter.setLabel(Messages.getString("HistoryView.TextField.TypeFilter.Label")); //$NON-NLS-1$
		this.typeFilter.setItems(NodeFactory.getLeafNodeTypes().stream()
		                        .map((type) -> NodeTypeLabelProvider.getNodeTypeLabel(type))
		                        .collect(Collectors.toList()));
		this.typeFilter.setEmptySelectionAllowed(true);
		this.typeFilter.setEmptySelectionCaption(Messages.getString("HistoryView.TextField.TypeFilter.None")); //$NON-NLS-1$
		this.typeFilter.addValueChangeListener(e -> {
			log.info("typeFilter changed: fromClient={}", e.isFromClient()); //$NON-NLS-1$
			if (e.isFromClient()) {
				resetMinMaxValues();
				refreshData();
			}
		});

		VerticalLayout filters = new VerticalLayout(this.nameFilter, this.typeFilter);
		filters.addClassName(FiltersClassName);
		filters.setWidth("13em"); // seems impossible to assign that via styling ||-(  //$NON-NLS-1$

		this.refreshData = new Button(Messages.getString("HistoryView.Button.RefreshData.Label")); //$NON-NLS-1$
		this.refreshData.addClickListener(e -> {
			log.info("refreshData clicked"); //$NON-NLS-1$
			refreshData();
		});

		this.timestampMinimum = new MyDateTimePicker();
		this.timestampMaximum = new MyDateTimePicker();

		this.timestampMinimum.setLabel(Messages.getString("HistoryView.Timestamp.Minimum.Label")); //$NON-NLS-1$
		this.timestampMinimum.setClassName(TimestampMinimumFieldClassName);
		this.timestampMinimum.setThemeName(LabelPaddingTheme);
		this.timestampMinimum.addValueChangeListener(e -> {
			log.info("timestampMinimum changed: fromClient={}", e.isFromClient()); //$NON-NLS-1$
			if (e.isFromClient() && e.getValue() != null) {
				refreshData();
			}
		});

		this.timestampMaximum.setLabel(Messages.getString("HistoryView.Timestamp.Maximum.Label")); //$NON-NLS-1$
		this.timestampMaximum.setClassName(TimestampMaximumFieldClassName);
		this.timestampMaximum.setThemeName(LabelPaddingTheme);
		this.timestampMaximum.addValueChangeListener(e -> {
			log.info("timestampMaximum changed: fromClient={}", e.isFromClient()); //$NON-NLS-1$
			if (e.isFromClient() && e.getValue() != null) {
				refreshData();
			}
		});


		VerticalLayout timestampExtremes = new VerticalLayout();
		timestampExtremes.setClassName(TimestampExtremesFieldClassName);
		timestampExtremes.addClassName(CombinedClassName);
		timestampExtremes.add(this.timestampMinimum, this.timestampMaximum);
		timestampExtremes.setWidth("25em");  // seems impossible to assign that via styling ||-(  //$NON-NLS-1$


		HorizontalLayout toolbar = new HorizontalLayout(filters, timestampExtremes, this.refreshData);
		toolbar.addClassName(ToolbarClassName);
		toolbar.setWidthFull();
		return toolbar;
	}

	// setting the values to null will cause them to be refreshed with the minimum/maximum
	protected void resetMinMaxValues() {
		this.timestampMinimum.setValue(null);
		this.timestampMaximum.setValue(null);
	}

	protected void refreshData() {
		log.info("AbstractHistoryInfoView.refreshData"); //$NON-NLS-1$
		this.lastUpdate = LocalDateTime.now();
		UIHandlerSupport.executeLater(this, this::fetchData, this::updateView);
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		log.info("AbstractHistoryInfoView.onAttach"); //$NON-NLS-1$
		super.onAttach(attachEvent);
		refreshData();
	}

	/* NOTE: this is executed by a different thread - it MUST NOT access any GUI elements! */
	abstract protected T fetchData();

	/* NOTE: this is supposed to update the view */
	abstract protected void updateView(T res);

	// this fetches the minimum/maximum timestamp
	protected HashMap <String, Object> getMinMaxInfo() {

		HashMap <String, Object> res = new HashMap<>();

		String nameFilterValue = (this.nameFilterValueStr != null ? this.nameFilterValueStr.replace('*', '%') : null);
		NodeType nodeTypeFilterValue = NodeTypeLabelProvider.getNodeType(this.typeFilter.getValue());

		res.put(NameFilterValueKey, nameFilterValue);
		res.put(TypeFilterValueKey, nodeTypeFilterValue);

		log.info("getMinMaxInfo-filter: name {} / type: {}", this.nameFilter.getValue(), this.typeFilter.getValue()); //$NON-NLS-1$

		// adjust min/maxTimestamp:
		LocalDateTime minTimestampValue = this.timestampMinimum.getValue();
		LocalDateTime maxTimestampValue = this.timestampMaximum.getValue();
		if (minTimestampValue == null) {
			log.info("getMinMaxInfo: minTimestampValue is null"); //$NON-NLS-1$
			minTimestampValue = this.lastUpdate.minus(1, ChronoUnit.HOURS); // set minimum to 1 hr. before now
		}
		if (maxTimestampValue == null) {
			log.info("getMinMaxInfo: maxTimestampValue is null"); //$NON-NLS-1$
			maxTimestampValue = this.lastUpdate; // set maximum to now
		}

		LocalDateTime minTimestampFound = this.historyInfoService.findLowestTimestampWithFilter(nameFilterValue, nodeTypeFilterValue);
		LocalDateTime maxTimestampFound = this.historyInfoService.findHighestTimestampWithFilter(nameFilterValue, nodeTypeFilterValue);
		log.info("getMinMaxInfo-timestamp: min: {}, max: {}", minTimestampFound, maxTimestampFound); //$NON-NLS-1$

		// limit the values to [min..max] (if available):
		if (minTimestampFound != null) {
			if ((minTimestampValue == null || minTimestampValue.compareTo(minTimestampFound) < 0)) {
				minTimestampValue = minTimestampFound;
			}
		}
		if (maxTimestampFound != null) {
			if ((maxTimestampValue == null || maxTimestampValue.compareTo(maxTimestampFound) > 0)) {
				maxTimestampValue = maxTimestampFound;
			}
		}
		log.info("getMinMaxInfo-timestamp: range: [{} - {}]", minTimestampValue, maxTimestampValue); //$NON-NLS-1$

		res.put(MinTimestampValueKey, minTimestampValue);
		res.put(MaxTimestampValueKey, maxTimestampValue);
		res.put(MinTimestampFoundKey, minTimestampFound);
		res.put(MaxTimestampFoundKey, maxTimestampFound);

		return res;
	}

	protected void updateMinMaxFields(HashMap <String, Object> res) {

//		try {
//			this.nameFilter.setValue((String)res.get(NameFilterValueKey));
//		} catch (Exception ex) {
//			// ignore - even if setAllowCustomValue is set to true the ComboBox only accepts values from its predefined list ||-(
//		}

		LocalDateTime minTimestampValue = (LocalDateTime)res.get(MinTimestampValueKey);
		LocalDateTime maxTimestampValue = (LocalDateTime)res.get(MaxTimestampValueKey);
		LocalDateTime minTimestampFound = (LocalDateTime)res.get(MinTimestampFoundKey);
		LocalDateTime maxTimestampFound = (LocalDateTime)res.get(MaxTimestampFoundKey);

		// update Timestamp-fields:
		if (minTimestampFound != null) {
			LocalDateTime minTimestampFoundTruncated = truncatedToMinutes(minTimestampFound);
			this.timestampMinimum.setMin(minTimestampFoundTruncated);
			this.timestampMinimum.setLabel(String.format(Messages.getString("HistoryView.Timestamp.Minimum.LblWithMin"), //$NON-NLS-1$
			                                             minTimestampFoundTruncated, minTimestampFoundTruncated));
		}
		if (maxTimestampFound != null) {
			LocalDateTime maxTimestampFoundTruncated = truncatedToMinutes(maxTimestampFound.plusSeconds(59)); // rounding up
			this.timestampMaximum.setMax(truncatedToMinutes(maxTimestampFoundTruncated));
			this.timestampMaximum.setLabel(String.format(Messages.getString("HistoryView.Timestamp.Maximum.LblWithMax"), //$NON-NLS-1$
			                                             maxTimestampFoundTruncated, maxTimestampFoundTruncated));

		}
		if (minTimestampValue != null) {
			this.timestampMinimum.setValue(truncatedToMinutes(minTimestampValue));
			this.timestampMinimum.setMax(truncatedToMinutes(maxTimestampFound)); // the maximum minimal level is the maximum value found
		}
		if (maxTimestampValue != null) {
			this.timestampMaximum.setValue(truncatedToMinutes(maxTimestampValue.plusSeconds(59))); // rounding up
			this.timestampMaximum.setMin(truncatedToMinutes(minTimestampFound)); // the minimal maximum value is the minimum value found
		}

	}

	protected List<String> getAllNodeNames() {
		List<String> allNodeNames = this.historyInfoService.getNodeNames(null, null);
		allNodeNames.sort(Comparators.comparable());
		log.info("nr of allNodeNames: {}", allNodeNames.size()); //$NON-NLS-1$
		return allNodeNames;
	}

	protected List<String> getFilteredNodeNames() {
		String nameFilterValue = (this.nameFilter.getValue() != null ? this.nameFilter.getValue().replace('*', '%') : null);
		NodeType nodeTypeFilterValue = NodeTypeLabelProvider.getNodeType(this.typeFilter.getValue());
		return getFilteredNodeNames(nameFilterValue, nodeTypeFilterValue);
	}

	protected List<String> getFilteredNodeNames(String nameFilterValue, NodeType nodeTypeFilterValue) {
		List<String> filteredNodeNames = this.historyInfoService.getNodeNames(nameFilterValue, nodeTypeFilterValue);
		filteredNodeNames.sort(Comparators.comparable());
		log.info("nr of filteredNodeNames({},{}): {}", nameFilterValue, nodeTypeFilterValue, filteredNodeNames.size()); //$NON-NLS-1$
		return filteredNodeNames;
	}

	protected LocalDateTime truncatedToMinutes(LocalDateTime timestamp) {
		return (timestamp != null ? timestamp.minusSeconds(timestamp.getSecond()).minusNanos(timestamp.getNano()) : null);
	}

	protected class MyDateTimePicker extends SuperDateTimePicker
	{
		private static final long serialVersionUID = 7607754515556993999L;

		MyDateTimePicker() {
			super();
			setStep(Duration.ofMinutes(1));
			setLocale(Locale.getDefault());
		}
	}

}
