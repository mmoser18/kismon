package net.mmo.utils.kism.ui.view.history;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.HistoryInfoService;
import net.mmo.utils.kism.entities.history.HistoryInfo;
import net.mmo.utils.kism.entities.nodes.NodeType;
import net.mmo.utils.kism.ui.CommonConstants;
import net.mmo.utils.kism.ui.MainLayout;
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
public class HistoryInfoTable extends AbstractHistoryInfoView<HashMap<String, Object>>
{
	private static final long serialVersionUID = -352635233754729967L;

	protected Grid<HistoryInfo> grid;


	// This gets autowired but still puzzled which annotation causes this...
	public HistoryInfoTable(HistoryInfoService historyInfoService) {
		super(historyInfoService);
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
		this.grid.addColumn((info) -> info.getTimestamp())
			.setHeader(Messages.getString("HistoryTable.Grid.ColumnName.Timestamp")) //$NON-NLS-1$
			.setTextAlign(ColumnTextAlign.END)
			;
		this.grid.addColumn((info) -> info.getDuration())
			.setHeader(Messages.getString("HistoryTable.Grid.ColumnName.Duration")) //$NON-NLS-1$
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

	/* NOTE: this is executed by a different thread - it MUST NOT access any GUI elements! */
	@Override
	protected HashMap<String, Object> fetchData() {
		log.info("HistoryInfoTable.fetchData"); //$NON-NLS-1$

		HashMap <String, Object> res = super.getMinMaxInfo();

		List<HistoryInfo> data = this.historyInfoService.findByFilter((String)res.get("nameFilterValue"), (NodeType)res.get("nodeTypeFilterValue"), //$NON-NLS-1$ //$NON-NLS-2$
		                                                              (Double)res.get("minDurationValue"), (Double)res.get("maxDurationValue"), //$NON-NLS-1$ //$NON-NLS-2$
		                                                              (LocalDateTime)res.get("minTimestampValue"), (LocalDateTime)res.get("maxTimestampValue")); //$NON-NLS-1$ //$NON-NLS-2$

		log.info("fetchData-result: ", data != null ? "found "+ data.size() + " entries": "null"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		res.put("data", data); //$NON-NLS-1$
		return res;
	}

	@Override
	protected void updateView(HashMap <String, Object> res) {
		log.info("HistoryInfoTable.updateTable"); //$NON-NLS-1$
		super.updateMinMaxFields(res);

		// update grid data:
		@SuppressWarnings("unchecked")
		List<HistoryInfo> data = (List<HistoryInfo>)res.get("data"); //$NON-NLS-1$

		log.info("setting grid data:"); //$NON-NLS-1$
		this.grid.setItems(data);
	}
}
