package net.mmo.utils.kism.ui.view.history;

import static net.mmo.utils.kism.ui.UIConstants.CombinedClassName;
import static net.mmo.utils.kism.ui.UIConstants.HistoryViewField;
import static net.mmo.utils.kism.ui.UIConstants.LabelPaddingTheme;

import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.HistoryInfoService;
import net.mmo.utils.kism.entities.nodes.NodeType;
import net.mmo.utils.kism.ui.UIConstants;
import net.mmo.utils.kism.ui.utils.UIHandlerSupport;
import net.mmo.utils.kism.ui.view.nodes.NodeFactory;
import net.mmo.utils.kism.ui.view.nodes.NodeTypeLabelProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.comparator.Comparators;

/**
 * Defines the application's main view, routed to via the base URL
 */
@Component // to make it possible to @Autowire it
@Scope("prototype") // to ensure every test run gets a fresh instance.
@Slf4j
abstract public class AbstractHistoryInfoView extends VerticalLayout
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

	protected NumberField durationMinimum;
	protected NumberField durationMaximum;

	protected DateTimePicker timestampMinimum;
	protected DateTimePicker timestampMaximum;

	protected ComboBox<String> nameFilter = new ComboBox<>();
	protected Select<String> typeFilter = new Select<>();
	protected Button refreshData;

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

		refreshData();
		log.debug("{} complete.", this.getClassName()); //$NON-NLS-1$
	}

	abstract protected void configureContent();

	abstract protected void refreshData();

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
				this.nameFilter.setValue(e.getDetail());
				refreshData();
			}
		});
		UIHandlerSupport.executeLater(this, this::getAllNodeNames, (res) -> this.nameFilter.setItems(res));

		this.typeFilter.setClassName(TypeFilterFieldClassName);
		this.typeFilter.setLabel(Messages.getString("HistoryView.TextField.TypeFilter.Label")); //$NON-NLS-1$
		this.typeFilter.setItems(NodeFactory.getLeafNodeTypes().stream()
		                        .map((type) -> NodeTypeLabelProvider.getNodeTypeLabel(type))
		                        .collect(Collectors.toList()));
		this.typeFilter.setEmptySelectionAllowed(true);
		this.typeFilter.setEmptySelectionCaption(Messages.getString("HistoryView.TextField.TypeFilter.None")); //$NON-NLS-1$

		VerticalLayout filters = new VerticalLayout(this.nameFilter, this.typeFilter);
		filters.addClassName(FiltersClassName);
		filters.setWidth("13em"); // seems impossible to assign that via styling ||-(  //$NON-NLS-1$

		this.refreshData = new Button(Messages.getString("HistoryView.Button.RefreshData.Label")); //$NON-NLS-1$
		this.refreshData.addClickListener(e -> {
			log.info("refreshData clicked"); //$NON-NLS-1$
			refreshData();
		});

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
		durationExtremes.setWidth("13em"); // seems impossible to assign that via styling ||-(  //$NON-NLS-1$

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
		timestampExtremes.setWidth("25em");  // seems impossible to assign that via styling ||-(  //$NON-NLS-1$

		HorizontalLayout toolbar = new HorizontalLayout(filters, durationExtremes, timestampExtremes, this.refreshData);
		toolbar.addClassName(ToolbarClassName);
		toolbar.setWidthFull();
		return toolbar;
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
}
