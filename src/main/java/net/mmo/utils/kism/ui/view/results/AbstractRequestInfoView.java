package net.mmo.utils.kism.ui.view.results;

import static net.mmo.utils.kism.ui.UIConstants.ResultViewField;

import java.util.stream.Collectors;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.RequestInfoService;
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
abstract public class AbstractRequestInfoView extends VerticalLayout
{
	private static final long serialVersionUID = 697446986998667247L;

	// public to allow access by test classes
	public static final String ViewClassName = "result-view";  //$NON-NLS-1$
	public static final String ToolbarClassName     = "result-view-toolbar";  //$NON-NLS-1$
	public static final String ViewContentClassName = "result-view-content";  //$NON-NLS-1$
	public static final String GridClassName        = "result-view-grid"; //$NON-NLS-1$

	public static final String NameFilterFieldClassName = ResultViewField + "filter-name";  //$NON-NLS-1$
	public static final String TypeFilterFieldClassName = ResultViewField + "filter-type";  //$NON-NLS-1$

	protected RequestInfoService requestInfoService;

	protected ComboBox<String> nameFilter = new ComboBox<>();
	protected Select<String> typeFilter = new Select<>();

	protected Div content;

	// This gets autowired but still puzzled which annotation causes this...
	public AbstractRequestInfoView(RequestInfoService requestInfoService) {
		log.debug("Creating {}:", this.getClass().getSimpleName()); //$NON-NLS-1$
		this.requestInfoService = requestInfoService;
		addClassName(ViewClassName);
		setSizeFull(); // use entire browser window

		this.content = new Div();
		this.content.addClassName(ViewContentClassName);
		setDefaultHorizontalComponentAlignment(Alignment.CENTER);

		this.content.setSizeFull();

		configureContent();

		add(getToolbar(), this.content);
	}

	abstract protected void configureContent();

	protected HorizontalLayout getToolbar() {
		log.trace("Creating toolbar:"); //$NON-NLS-1$
		this.nameFilter.setClassName(NameFilterFieldClassName);
		this.nameFilter.setLabel(Messages.getString("RequestInfoTable.TextField.NameFilter.Label")); //$NON-NLS-1$
		this.nameFilter.setPlaceholder(Messages.getString("RequestInfoTable.TextField.NameFilter.PlaceHolder")); //$NON-NLS-1$
		this.nameFilter.setClearButtonVisible(true);

		this.typeFilter.setClassName(TypeFilterFieldClassName);
		this.typeFilter.setLabel(Messages.getString("RequestInfoTable.TextField.TypeFilter.Label")); //$NON-NLS-1$
		this.typeFilter.setItems(NodeFactory.getLeafNodeTypes().stream()
		                        .map((type) -> NodeTypeLabelProvider.getNodeTypeLabel(type))
		                        .collect(Collectors.toList()));
		this.typeFilter.setEmptySelectionAllowed(true);
		this.typeFilter.setEmptySelectionCaption(Messages.getString("RequestInfoTable.TextField.TypeFilter.None")); //$NON-NLS-1$

		HorizontalLayout toolbar = new HorizontalLayout(this.nameFilter, this.typeFilter);

		toolbar.addClassName(ToolbarClassName);
		return toolbar;
	}
}
