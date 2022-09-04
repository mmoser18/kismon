package net.mmo.utils.kism.ui.views.history;

import static net.mmo.utils.kism.ui.UIConstants.CombinedClassName;
import static net.mmo.utils.kism.ui.UIConstants.LabelPaddingTheme;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;

import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.HistoryInfoService;
import net.mmo.utils.kism.entities.history.HistoryInfo;
import net.mmo.utils.kism.entities.nodes.VisibleNodeType;
import net.mmo.utils.kism.ui.CommonConstants;
import net.mmo.utils.kism.ui.MainLayout;
import net.mmo.utils.kism.ui.utils.UIHandlerSupport;
import net.mmo.utils.kism.ui.views.nodes.NodeTypeLabelProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Defines the application's main view, routed to via the base URL
 */
@SuppressWarnings("javadoc")
@Route(value="history-table", layout = MainLayout.class)
@PageTitle("History Table | " + CommonConstants.ApplicationFullName)
@RolesAllowed({CommonConstants.Role_ADMIN, CommonConstants.Role_READ_ONLY})
@Component // to make it possible to @Autowire it
@Scope("prototype") // to ensure every test run gets a fresh instance.
@Slf4j
public class HistoryInfoTable extends AbstractHistoryInfoView<HashMap<String, Object>>
{
	private static final long serialVersionUID = -352635233754729967L;

	protected static final String MinDurationValueKey = "minDurationValue"; //$NON-NLS-1$
	protected static final String MaxDurationValueKey = "maxDurationValue"; //$NON-NLS-1$
	protected static final String MinDurationFoundKey = "minDurationFound"; //$NON-NLS-1$
	protected static final String MaxDurationFoundKey = "maxDurationFound"; //$NON-NLS-1$
	protected NumberField durationMinimum;
	protected NumberField durationMaximum;
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

		this.grid.asSingleSelect().addValueChangeListener(event -> { /* tbd */ }); // getValue() returns the selected item or null if none is selected
		this.grid.addAttachListener(e -> { if (e.isFromClient()) { refreshData(); }});

		this.content.add(this.grid);

		this.grid.getColumns().forEach(column ->
		{	column
				.setSortable(true)
				.setAutoWidth(true)
				.setFlexGrow(0);
		});
	}

	@Override
	protected HorizontalLayout getToolbar() {
		HorizontalLayout toolbar = super.getToolbar();

		this.durationMinimum = new NumberField();
		this.durationMaximum = new NumberField();

		this.durationMinimum.setLabel(Messages.getString("HistoryView.Duration.Minimum.Label")); //$NON-NLS-1$
		this.durationMinimum.setClassName(DurationMinimumFieldClassName);
		this.durationMinimum.setThemeName(LabelPaddingTheme);
		this.durationMinimum.setHasControls(true);
		this.durationMinimum.setWidth("12em"); //$NON-NLS-1$
		this.durationMinimum.addValueChangeListener(e -> {
			log.info("durationMinimum changed: fromClient={}", e.isFromClient()); //$NON-NLS-1$
			if (e.isFromClient() && e.getValue() != null) {
				refreshData();
			}
		});

		this.durationMaximum.setLabel(Messages.getString("HistoryView.Duration.Maximum.Label")); //$NON-NLS-1$
		this.durationMaximum.setClassName(DurationMaximumFieldClassName);
		this.durationMaximum.setThemeName(LabelPaddingTheme);
		this.durationMaximum.setHasControls(true);
		this.durationMaximum.setWidth("12em"); //$NON-NLS-1$
		this.durationMaximum.addValueChangeListener(e -> {
			log.info("durationMaximum changed: fromClient={}", e.isFromClient()); //$NON-NLS-1$
			if (e.isFromClient() && e.getValue() != null) {
				refreshData();
			}
		});

		VerticalLayout durationExtremes = new VerticalLayout();
		durationExtremes.setClassName(DurationExtremesFieldClassName);
		durationExtremes.addClassName(CombinedClassName);
		durationExtremes.add(this.durationMinimum, this.durationMaximum);
		durationExtremes.setWidth("13em"); // seems impossible to assign that via styling ||-(  //$NON-NLS-1$

		toolbar.addComponentAtIndex(toolbar.getComponentCount()-1, durationExtremes);

		return toolbar;
	}

	// setting the values to null will cause them to be refreshed with the minimum/maximum
	@Override
	protected void resetMinMaxValues() {
		super.resetMinMaxValues();
		this.durationMinimum.setValue(null);
		this.durationMaximum.setValue(null);
	}

	@Override
	protected void getMinMaxInfo(HashMap <String, Object> res) {

		super.getMinMaxInfo(res);

		String nameFilterValue = (String)res.get(NameFilterValueKey);
		VisibleNodeType nodeTypeFilterValue = (VisibleNodeType)res.get(TypeFilterValueKey);

		// adjust min/maxDuration:
		Double minDurationValue = this.durationMinimum.getValue();
		Double maxDurationValue = this.durationMaximum.getValue();

		Double minDurationFound = this.historyInfoService.findLowestDurationWithFilter(nameFilterValue, nodeTypeFilterValue);
		Double maxDurationFound = this.historyInfoService.findHighestDurationWithFilter(nameFilterValue, nodeTypeFilterValue);
		log.info("fetchData-duration: min: {}, max: {}", minDurationFound, maxDurationValue); //$NON-NLS-1$

		if (minDurationFound != null) {
			if ((minDurationValue == null || minDurationValue < minDurationFound)) {
				minDurationValue = minDurationFound;
			}
		}
		if (maxDurationFound != null) {
			if ((maxDurationValue == null || maxDurationValue > maxDurationFound)) {
				maxDurationValue = maxDurationFound;
			}
		}
		log.info("fetchData-duration: range: [{} - {}]", minDurationValue, maxDurationValue); //$NON-NLS-1$

		res.put(MinDurationValueKey, minDurationValue);
		res.put(MaxDurationValueKey, maxDurationValue);
		res.put(MinDurationFoundKey, minDurationFound);
		res.put(MaxDurationFoundKey, maxDurationFound);
	}

	@Override
	protected void updateMinMaxFields(HashMap <String, Object> res) {
		log.info("HistoryInfoTable.updateMinMaxFields"); //$NON-NLS-1$
		super.updateMinMaxFields(res);

		Double minDurationFound = (Double)res.get(MinDurationValueKey);
		Double maxDurationFound = (Double)res.get(MaxDurationValueKey);
		Double minDurationValue = (Double)res.get(MinDurationFoundKey);
		Double maxDurationValue = (Double)res.get(MaxDurationFoundKey);

		// update Duration-fields:
		if (minDurationFound != null) {
			this.durationMinimum.setMin(minDurationFound);
			this.durationMinimum.setLabel(String.format(Messages.getString("HistoryView.Duration.Minimum.LblWithMin"), minDurationFound)); //$NON-NLS-1$
		}
		if (maxDurationFound != null) {
			this.durationMaximum.setMax(maxDurationFound);
			this.durationMaximum.setLabel(String.format(Messages.getString("HistoryView.Duration.Maximum.LblWithMax"), maxDurationFound)); //$NON-NLS-1$
		}
		if (minDurationValue != null) {
			this.durationMinimum.setValue(minDurationValue);
			// set the max/min to the respective other value: (is this a clever idea?)
			// this.durationMaximum.setMin(minDurationValue);
		}
		if (maxDurationValue != null) {
			this.durationMaximum.setValue(maxDurationValue);
			// set the max/min to the respective other value: (is this a clever idea?)
			// this.durationMinimum.setMax(maxDurationValue);
		}
	}

	/* NOTE: this method is executed by a different thread - it MUST NOT access any GUI elements! */
	@Override
	protected void fetchData(HashMap <String, Object> res) {
		log.info("HistoryInfoTable.fetchData"); //$NON-NLS-1$
		// nothing to put into the map - we fetch the data in chunks later
	}

	/* NOTE: this method IS executed by a GUI thread - it CAN access GUI elements! */
	@Override
	@SuppressWarnings("serial")
	protected void updateView(HashMap <String, Object> res) {
		log.info("HistoryInfoTable.updateView"); //$NON-NLS-1$
		// we are doing the DB query inline in the GUI thread - to be seen how well this works:
		this.grid.setItems(
			new CallbackDataProvider<HistoryInfo, Void>(
				new CallbackDataProvider.FetchCallback<HistoryInfo, Void>() {
					private static final long serialVersionUID = 1L;
					{
						log.debug("creating data provider:"); //$NON-NLS-1$
					}
					@Override
					public Stream<HistoryInfo> fetch(Query<HistoryInfo, Void> query) {
						// Since it may have taken a while until this method was called we check again:
						if (refreshIsObsolete(res)) {
							log.debug("another refresh pending - aborting this one..."); //$NON-NLS-1$
							return Stream.empty();
						}
						// The index of the first item to load
						final int offset = query.getOffset();
						// The number of items to load
						final int limit = query.getLimit();
						log.debug("CallbackDataProvider-fetch: offset:{} / limit:{}", offset, limit); //$NON-NLS-1$

						List<HistoryInfo> data =
							HistoryInfoTable.this.historyInfoService.findByFilter((String)res.get(NameFilterValueKey), (VisibleNodeType)res.get(TypeFilterValueKey)
							                                                     ,(Double)res.get(MinDurationValueKey), (Double)res.get(MaxDurationValueKey)
							                                                     ,(LocalDateTime)res.get(MinTimestampValueKey), (LocalDateTime)res.get(MaxTimestampValueKey)
							                                                     ,offset, limit
							                                                     );
						// Since the above DB call may have taken a while we check again:
						if (refreshIsObsolete(res)) {
							log.debug("another refresh pending - aborting this one..."); //$NON-NLS-1$
							return Stream.empty();
						}
						log.debug("CallbackDataProvider-fetch: {} entries found.", data.size()); //$NON-NLS-1$

						recalculateColumnWidths(res); // recalculate the columns width

						return data.stream();
					}
				},
				new CallbackDataProvider.CountCallback<HistoryInfo, Void>() {
					@Override
					public int count(Query<HistoryInfo, Void> query) {
						Long count = HistoryInfoTable.this.historyInfoService.countWithFilter((String)res.get(NameFilterValueKey)
						                                                                     ,(VisibleNodeType)res.get(TypeFilterValueKey)
						                                                                     ,(Double)res.get(MinDurationValueKey), (Double)res.get(MaxDurationValueKey)
						                                                                     ,(LocalDateTime)res.get(MinTimestampValueKey), (LocalDateTime)res.get(MaxTimestampValueKey)
						                                                                     );
						log.debug("CallbackDataProvider-count: {}", count); //$NON-NLS-1$
						return count.intValue();
					}
				}
			)
		);
	}

	private void recalculateColumnWidths(HashMap <String, Object> res) {
		recalculateColumnWidths(250, res);
	}
	private void recalculateColumnWidths(long delay, HashMap <String, Object> res) {
		if (!refreshIsObsolete(res)) {
			// original
			// this.tree.recalculateColumnWidths(); // make sure the column widths are adjusted
			// The above always came too early - with 1/4 sec delay we finally saw some improvement:
			UIHandlerSupport.executeLater(this, delay, () -> {
				if (!refreshIsObsolete(res)) {
					this.grid.recalculateColumnWidths();
				}
			});
		}
	}
}
