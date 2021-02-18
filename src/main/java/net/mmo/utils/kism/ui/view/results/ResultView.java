package net.mmo.utils.kism.ui.view.results;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.ResultService;
import net.mmo.utils.kism.entities.results.RequestResults;
import net.mmo.utils.kism.ui.CommonConstants;
import net.mmo.utils.kism.ui.MainLayout;
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
public class ResultView extends VerticalLayout
{
	private static final long serialVersionUID = -352635233754729967L;

	// public to allow access by test classes
	public static final String ViewClassName = "list-view";  //$NON-NLS-1$
	public static final String ToolbarClassName     = "list-view-toolbar";  //$NON-NLS-1$
	public static final String ViewContentClassName = "list-view-content";  //$NON-NLS-1$
	public static final String FilterFieldClassName = "list-view-filter";  //$NON-NLS-1$
	public static final String GridClassName        = "list-view-grid"; //$NON-NLS-1$

	private ResultService resultService;

	private TextField filterText = new TextField();
	Grid<RequestResults> grid = new Grid<>(RequestResults.class); // package visible for UTs - is there no other way?
	Div content;

	// This gets autowired but still puzzled which annotation causes this...
	public ResultView(ResultService resultService) {
		log.debug("creating {}:", this.getClassName()); //$NON-NLS-1$
		this.resultService = resultService;

		addClassName(ViewClassName);
		setSizeFull(); // use entire browser window

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
		this.filterText.setClassName(FilterFieldClassName);
		this.filterText.setPlaceholder(Messages.getString("ResultView.TextField.FilterText.Label")); //$NON-NLS-1$
		this.filterText.setClearButtonVisible(true);
		this.filterText.setValueChangeMode(ValueChangeMode.LAZY);
		this.filterText.addValueChangeListener(e -> updateList());

		HorizontalLayout toolbar = new HorizontalLayout(this.filterText);

		toolbar.addClassName(ToolbarClassName);
		return toolbar;
	}

	private void configureGrid() {
		log.trace("Configuring Grid:"); //$NON-NLS-1$
		this.grid.addClassName(GridClassName);
		this.grid.setSizeFull();
		this.grid.setColumns("name", // Messages.getString("ResultView.Grid.ColumnName.Name"), //$NON-NLS-1$
		                     "duration", // Messages.getString("ResultView.Grid.ColumnName.Duration"), //$NON-NLS-1$
		                     "timestamp", // Messages.getString("ResultView.Grid.ColumnName.Timestamp"), //$NON-NLS-1$
		                     "result"); // Messages.getString("ResultView.Grid.ColumnName.Result")); //$NON-NLS-1$
		this.grid.getColumns().forEach(col -> col.setAutoWidth(true));
		this.grid.asSingleSelect()
		         .addValueChangeListener(event -> { /* tbd */ }); // getValue() returns the selected item or null if none is selected
	}

	private void updateList() {
		this.grid.setItems(this.resultService.findAll(this.filterText.getValue()));
	}
}
