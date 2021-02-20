package net.mmo.utils.kism.ui.view.results;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.stream.Collectors;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.RequestInfoService;
import net.mmo.utils.kism.entities.results.RequestInfo;
import net.mmo.utils.kism.ui.CommonConstants;
import net.mmo.utils.kism.ui.MainLayout;
import net.mmo.utils.kism.ui.view.nodes.NodeFactory;
import net.mmo.utils.kism.ui.view.nodes.NodeTypeLabelProvider;
import net.mmo.utils.kism.utils.AppProperties;
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
	public static final String ViewClassName = "list-view";  //$NON-NLS-1$
	public static final String ToolbarClassName     = "list-view-toolbar";  //$NON-NLS-1$
	public static final String ViewContentClassName = "list-view-content";  //$NON-NLS-1$
	public static final String NameFilterFieldClassName = "list-view-filter-name";  //$NON-NLS-1$
	public static final String TypeFilterFieldClassName = "list-view-filter-type";  //$NON-NLS-1$
	public static final String GridClassName        = "list-view-grid"; //$NON-NLS-1$

	private RequestInfoService resultService;

	private TextField nameFilter = new TextField();
	private Select<String> typeFilter = new Select<>();

	Grid<RequestInfo> grid = new Grid<>(RequestInfo.class); // package visible for UTs - is there no other way?
	Div content;

	NumberFormat numberFormat = DecimalFormat.getInstance();


	// This gets autowired but still puzzled which annotation causes this...
	public RequestInfoView(RequestInfoService resultService) {
		log.debug("Creating {}:", this.getClass().getSimpleName()); //$NON-NLS-1$
		this.resultService = resultService;

		addClassName(ViewClassName);
		setSizeFull(); // use entire browser window

		fixJavaGroupingSeparatorBug();

		configureGrid();

		log.trace("Creating content:"); //$NON-NLS-1$
		this.content = new Div(this.grid);
		this.content.addClassName(ViewContentClassName);
		this.content.setSizeFull();

		add(getToolbar(), this.content);

		updateList();
		log.debug("{} complete.", this.getClassName()); //$NON-NLS-1$
	}

	private HorizontalLayout getToolbar() {
		log.trace("Creating toolbar:"); //$NON-NLS-1$
		this.nameFilter.setClassName(NameFilterFieldClassName);
		this.nameFilter.setPlaceholder(Messages.getString("RequestInfoView.TextField.NameFilter.Label")); //$NON-NLS-1$
		this.nameFilter.setClearButtonVisible(true);
		this.nameFilter.setValueChangeMode(ValueChangeMode.LAZY);
		this.nameFilter.addValueChangeListener(e -> updateList());

		this.typeFilter.setClassName(TypeFilterFieldClassName);
//		this.typeFilter.setPlaceholder(Messages.getString("RequestInfoView.TextField.TypeFilter.Label")); //$NON-NLS-1$

		this.typeFilter.setItems(NodeFactory.getLeafNodeTypes().stream()
		                        .map((type) -> NodeTypeLabelProvider.getNodeTypeLabel(type))
		                        .collect(Collectors.toList()));
		this.typeFilter.setEmptySelectionAllowed(true);
		this.typeFilter.setEmptySelectionCaption(Messages.getString("RequestInfoView.TextField.TypeFilter.None")); //$NON-NLS-1$
		this.typeFilter.addValueChangeListener(e -> updateList());

		HorizontalLayout toolbar = new HorizontalLayout(this.nameFilter, this.typeFilter);

		toolbar.addClassName(ToolbarClassName);
		return toolbar;
	}

	private void configureGrid() {
		log.trace("Configuring Grid:"); //$NON-NLS-1$
		this.grid.addClassName(GridClassName);
		this.grid.setSizeFull();
		this.grid.removeAllColumns();

		this.grid.addColumn("name") //$NON-NLS-1$
			.setHeader(Messages.getString("ResultInfoView.Grid.ColumnName.Name")) //$NON-NLS-1$
			.setAutoWidth(true)
			.setFlexGrow(0);
		this.grid.addColumn((info) -> NodeTypeLabelProvider.getNodeTypeLabel(info.getNodeType()))
			.setHeader(Messages.getString("ResultInfoView.Grid.ColumnName." + "Type")) //$NON-NLS-1$ //$NON-NLS-2$
			.setAutoWidth(true)
			.setFlexGrow(0);
		this.grid.addColumn((info) -> this.numberFormat.format(info.getDuration() / 1000000.0))
			.setHeader(Messages.getString("ResultInfoView.Grid.ColumnName.Duration")) //$NON-NLS-1$
			.setAutoWidth(true)
			.setFlexGrow(0);
		this.grid.addColumn("timestamp") //$NON-NLS-1$
			.setHeader(Messages.getString("ResultInfoView.Grid.ColumnName.Timestamp")) //$NON-NLS-1$
			.setAutoWidth(true)
			.setFlexGrow(0);
		this.grid.addColumn("result") //$NON-NLS-1$
			.setHeader(Messages.getString("ResultInfoView.Grid.ColumnName.Result")) //$NON-NLS-1$
			.setAutoWidth(true)
			.setFlexGrow(0);

		this.grid.asSingleSelect()
		         .addValueChangeListener(event -> { /* tbd */ }); // getValue() returns the selected item or null if none is selected
	}

	private void updateList() {
		this.grid.setItems(this.resultService.findAll((this.nameFilter.getValue() != null ? this.nameFilter.getValue().replace('*', '%') : null),
		                                              NodeTypeLabelProvider.getNodeType(this.typeFilter.getValue()),
		                                              0, 0,
		                                              null, null));
	}

	// for some stupid reason Java does not take over the OS' number digit grouping and separator
	// settings (at least not on Windows 10) - so I tried to work around that:
	private void fixJavaGroupingSeparatorBug() {
		DecimalFormatSymbols customSymbol = DecimalFormatSymbols.getInstance();
		boolean adjustNumberFormat = false;
		String groupingSeparator = AppProperties.getProperties().getProperty("DigitGroupingSeparator"); //$NON-NLS-1$
		if (nonBlank(groupingSeparator)) {
			customSymbol.setGroupingSeparator(groupingSeparator.charAt(0));
			adjustNumberFormat = true;
		}
		String decimalSeparator = AppProperties.getProperties().getProperty("DecimalSeparator"); //$NON-NLS-1$
		if (nonBlank(decimalSeparator)) {
			customSymbol.setDecimalSeparator(decimalSeparator.charAt(0));
			adjustNumberFormat = true;
		}
		if (adjustNumberFormat) {
			((DecimalFormat)this.numberFormat).setDecimalFormatSymbols(customSymbol);
			this.numberFormat.setGroupingUsed(true);
		}
	}
	private static boolean nonBlank(String str) {
		return str != null && !str.isBlank();
	}
}
