package net.mmo.utils.kism.ui.view.history;

import static net.mmo.utils.kism.ui.UIConstants.HistoryViewField;

import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.HistoryInfoService;
import net.mmo.utils.kism.entities.nodes.NodeType;
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

	protected final boolean DO_SYNC = true;

	protected HistoryInfoService historyInfoService;
	protected UIHandlerSupport uiHandlerSupport;

	protected ComboBox<String> nameFilter = new ComboBox<>();
	protected Select<String> typeFilter = new Select<>();
	protected Button refreshData;

	protected Div content;

	// This gets autowired but still puzzled which annotation causes this...
	public AbstractHistoryInfoView(HistoryInfoService historyInfoService, UIHandlerSupport uiHandlerSupport) {
		log.debug("Creating {}:", this.getClass().getSimpleName()); //$NON-NLS-1$
		this.historyInfoService = historyInfoService;
		this.uiHandlerSupport = uiHandlerSupport;
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
		if (this.DO_SYNC) {
			List<String> allNodeNames = getAllNodeNames();
			this.nameFilter.setItems(allNodeNames);
		} else {
			UIHandlerSupport.executeLater(this::getAllNodeNames, (res) -> this.nameFilter.setItems(res));
		}

		this.typeFilter.setClassName(TypeFilterFieldClassName);
		this.typeFilter.setLabel(Messages.getString("HistoryView.TextField.TypeFilter.Label")); //$NON-NLS-1$
		this.typeFilter.setItems(NodeFactory.getLeafNodeTypes().stream()
		                        .map((type) -> NodeTypeLabelProvider.getNodeTypeLabel(type))
		                        .collect(Collectors.toList()));
		this.typeFilter.setEmptySelectionAllowed(true);
		this.typeFilter.setEmptySelectionCaption(Messages.getString("HistoryView.TextField.TypeFilter.None")); //$NON-NLS-1$

		this.refreshData = new Button(Messages.getString("HistoryView.Button.RefreshData.Label")); //$NON-NLS-1$
		this.refreshData.addClickListener(e -> refreshData());

		HorizontalLayout toolbar = new HorizontalLayout(this.nameFilter, this.typeFilter);

		toolbar.addClassName(ToolbarClassName);
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
