package net.mmo.utils.kism.ui.view.nodes;

import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.base.Objects;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.nodes.IntermediateNode;
import net.mmo.utils.kism.entities.nodes.LeafNode;
import net.mmo.utils.kism.entities.nodes.Node;
import net.mmo.utils.kism.entities.nodes.NodeType;
import net.mmo.utils.kism.entities.nodes.RootNode;
import net.mmo.utils.kism.ui.CommonConstants;
import net.mmo.utils.kism.ui.CommonConstants.LayoutDirection;
import net.mmo.utils.kism.ui.MainLayout;
import net.mmo.utils.kism.ui.utils.ConfirmDialog;
import net.mmo.utils.kism.ui.utils.PropertyChangeListenerWithPriorUIReservation;
import net.mmo.utils.kism.utils.AppProperties;
import net.mmo.utils.kism.utils.ExceptionUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.vaadin.olli.FileDownloadWrapper;

/**
 * Defines the application's main view, routed to via the base URL
 */
@Route(value="", layout = MainLayout.class)
@PageTitle("Nodes | " + CommonConstants.ApplicationFullName)
@Component // to make it possible to @Autowire it
@Scope("prototype") // to ensure every test run gets a fresh instance.
@Slf4j
// This is to prevent that a click on the hierachy column expands/collapses the subtree in a TreeGrid
// see https://github.com/vaadin/vaadin-grid/issues/1934
// With this annotation one has to click the *twisty* to collapse/expand, not the column.
@CssImport(value="./styles/grid-tree-toggle-adjust.css", themeFor="vaadin-grid-tree-toggle")
public class NodeView <N extends Node> extends VerticalLayout
{
	private static final long serialVersionUID = -352635233754729967L;

	// public to allow access by test classes
	public static final String ViewClassName = "node-view";  //$NON-NLS-1$
	public static final String ViewToolbarClassName = "node-view-toolbar";  //$NON-NLS-1$
	public static final String ViewContentClassName = "node-view-content";  //$NON-NLS-1$
	public static final String TreeClassName        = "node-view-tree"; //$NON-NLS-1$

	private NodeService nodeService;
	private NodeFactory<N> nodeFactory;
	private FormFactory<N> formFactory;

	Checkbox haltRequests;

	SplitLayout content;
	TreeGrid<Node> tree; // package visible for UTs - is there no other way?
	Div formWrapper;

	NodeForm<N> form; // package visible for ITs - is there no other way?
	LayoutDirection contentDirection = LayoutDirection.Horizontal;

	protected static boolean alwaysSave = Boolean.getBoolean(AppProperties.getProperties().getProperty("NodeView.alwaysSaveAfterEdit", "false")); //$NON-NLS-1$ //$NON-NLS-2$

	protected PropertyChangeListener nodeRefreshListener;


	public NodeView(NodeService nodeService,
	                NodeFactory<N> nodeFactory,
	                FormFactory<N> formFactory) { // defined as constructor args to force order
		log.debug("Creating {}:", this.getClass().getSimpleName()); //$NON-NLS-1$
		try {
			this.nodeService = nodeService;
			this.nodeFactory = nodeFactory;
			this.formFactory = formFactory;

			addClassName(ViewClassName);
			setSizeFull(); // use entire browser window

			configureTree();

			log.trace("Creating content:"); //$NON-NLS-1$
			this.tree.setSizeFull();

			this.formWrapper = new Div(); // the framing Div is to force a scrollbar when the inner form gets too to fit its half of the screen
			this.formWrapper.setSizeFull();

			this.content = new SplitLayout(this.tree, this.formWrapper);
			this.content.addClassName(ViewContentClassName);
			this.content.setSizeFull();
			this.content.setSplitterPosition(200); // initial position

			// this.content.setOrientation(this.contentDirection.getOrientation());
			CommonConstants.setClassName(this.content, this.contentDirection);

			add(createToolbar(), this.content);

		} catch (Throwable t) {
			log.error("Exception in c'tor:", t); //$NON-NLS-1$
		}
		this.tree.recalculateColumnWidths(); // make sure the column widths are adjusted
		this.tree.getDataProvider().refreshAll();

		log.debug("{} complete.", this.getClassName()); //$NON-NLS-1$
	}

	private HorizontalLayout createToolbar() {
		log.trace("Creating toolbar:"); //$NON-NLS-1$

		ComboBox<NodeType> nodeTypeSelector = new ComboBox<>(Messages.getString("NodeView.ComboBox.NodeTypes.Label")); //$NON-NLS-1$
		nodeTypeSelector.setItems(NodeType.values());
		nodeTypeSelector.setValue(NodeType.IntermediateNode);
		nodeTypeSelector.addValueChangeListener(change ->
			{
				log.info("nodeTypeSelector changed to {}", change.getValue()); //$NON-NLS-1$
			});

		Button addNodeButton = new Button(Messages.getString("NodeView.Button.AddNode.Label")); //$NON-NLS-1$
		addNodeButton.addClickListener(click ->
			{
				NodeType type = nodeTypeSelector.getValue();
				log.info("add node (nodeType={})", type); //$NON-NLS-1$
				if (type == NodeType.RootNode) {
					createNewRootNode();
				} else {
					IfSuitableNodeSelectedForAddition((selectedNode) -> createNewChildNode(type, selectedNode)); // in the first step we only create a new node
				}
			});

		Button delNodeButton = new Button(Messages.getString("NodeView.Button.DeleteNode.Label")); //$NON-NLS-1$
		delNodeButton.addClickListener(click ->
			{
				Set<Node> nodes = this.tree.getSelectedItems();
				if (nodes.isEmpty()) {
					/// TODO: AlertUser("Select nodes to be deleted first!");
				} else {
					removeNodes(nodes);
				}
			});

		Button clearListButton = new Button(Messages.getString("NodeView.Button.ClearList.Label")); //$NON-NLS-1$
		clearListButton.addClickListener(click ->
			{
				Set<Node> nodes = this.tree.getSelectedItems();
				if (nodes.isEmpty()) {
					/// TODO: AlertUser("Select nodes to be deleted first!");
				} else {
					removeList(nodes);
				}
			});

		Button directionButton = new Button(String.format(Messages.getString("NodeView.Button.Direction.Label"), LayoutDirection.invert(this.contentDirection).toString())); //$NON-NLS-1$
		directionButton.addClickListener(click ->
			{
				// We re-label the button with the current direction...
				directionButton.setText(String.format(Messages.getString("NodeView.Button.Direction.Label"), this.contentDirection.toString())); //$NON-NLS-1$
				// ... before changing the direction:
				this.contentDirection = LayoutDirection.invert(this.contentDirection);
				// this.content.setOrientation(this.contentDirection.getOrientation());
				CommonConstants.setClassName(this.content, this.contentDirection);
			});

		Button moveUp = new Button(Messages.getString("NodeView.Button.MoveUp.Label")); //$NON-NLS-1$
		moveUp.addClickListener(click -> ifSingleNodeSelectedDo(node -> moveUp(node)));

		Button moveDown = new Button(Messages.getString("NodeView.Button.MoveDown.Label")); //$NON-NLS-1$
		moveDown.addClickListener(click -> ifSingleNodeSelectedDo(node -> moveDown(node)));

		Button promote = new Button(Messages.getString("NodeView.Button.Promote.Label")); //$NON-NLS-1$
		promote.addClickListener(click -> ifSingleNodeSelectedDo(node -> promote(node)));

		Button demote = new Button(Messages.getString("NodeView.Button.Demote.Label")); //$NON-NLS-1$
		demote.addClickListener(click -> ifSingleNodeSelectedDo(node -> demote(node)));

		Button saveAllButton = new Button(Messages.getString("NodeView.Button.SaveAll.Label")); //$NON-NLS-1$
		saveAllButton.addClickListener(click ->
			{
				log.info("saveAllButton clicked"); //$NON-NLS-1$
				saveAllRootNodes();
			});

		Button downloadFileButton = new Button(Messages.getString("NodeView.Button.Download.Label")); //$NON-NLS-1$
		FileDownloadWrapper buttonWrapper =
			new FileDownloadWrapper(new StreamResource("content", //$NON-NLS-1$
			                                           () -> {
//			                                              Set<Node> nodes = this.tree.getSelectedItems();
//			                                              if (nodes.isEmpty()) {
//			                                              	return null;
//			                                              }
//			                                              Node node = nodes.iterator().next();
//			                                              return new ByteArrayInputStream(node.getName().getBytes());
				                                          return new ByteArrayInputStream("some bytes".getBytes()); //$NON-NLS-1$
			                                           }));
		buttonWrapper.wrapComponent(downloadFileButton);

		MemoryBuffer buffer = new MemoryBuffer(); // alternative: new FileBuffer();
		Upload uploadFile = new Upload(buffer);
		// uploadFile.setI18n(new UploadI18N());
		uploadFile.addSucceededListener(event ->
			{
				String fileName = event.getFileName();
				try (InputStream is = buffer.getInputStream();
				     BufferedInputStream bis = new BufferedInputStream(is)) {
					RootNode rootNode = this.nodeService.readStream(is, fileName);
					reportResult(rootNode, fileName);
				} catch (Throwable t) {
					reportException(fileName, t);
				}
			});

		this.haltRequests = new Checkbox(Messages.getString("NodeView.HaltAllRequests.Label")); //$NON-NLS-1$
		this.haltRequests.addValueChangeListener(event ->
			{
				LeafNode.setHaltAllRequests(event.getValue());
				adjustHaltRequest();
			});
		adjustHaltRequest();

		HorizontalLayout toolbar = new HorizontalLayout(nodeTypeSelector, addNodeButton, delNodeButton, clearListButton,
		                                                moveUp, moveDown, promote, demote,
		                                                directionButton, saveAllButton,
		                                                downloadFileButton, uploadFile,
		                                                this.haltRequests);
		toolbar.addClassName(ViewToolbarClassName);
		return toolbar;
	}

	protected void adjustHaltRequest() {
		boolean halted = LeafNode.isHaltAllRequests();
		log.info("Requests are now " + (halted ? "halted" : "enabled")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		// make sure the checkbox (and its coloring) and the actual value are in sync:
		if (this.haltRequests.getValue() != halted) this.haltRequests.setValue(halted);
		this.haltRequests.setLabel(Messages.getString(halted ? "NodeView.HaltAllRequests.Halted" : "NodeView.HaltAllRequests.Enabled")); //$NON-NLS-1$ //$NON-NLS-2$
		this.haltRequests.setClassName(halted ? "halted" : "enabled"); //$NON-NLS-1$ //$NON-NLS-2$
	}


	private void configureTree() {
		log.trace("Configuring TreeGrid:"); //$NON-NLS-1$
		this.tree = new TreeGrid<>();
		this.tree.addClassName(TreeClassName);
		this.tree.setSizeFull();

		this.tree.addComponentHierarchyColumn(node -> {
				Node.State state = node.getState();
				String stateText = StateLabelProvider.getStateLabel(state);
				String classSuffix = state.name(); // will yield class names icon-<classSuffix> and label-<classSuffix> - used to style these red/orange/green
				switch (state) {
				case FAILED:   return new LabelWithIcon(VaadinIcon.CLOSE_CIRCLE, stateText, classSuffix); // alternatives: BAN, BOLT, FROWN_O, THUMBS_DOWN?
				case DEGRADED: return new LabelWithIcon(VaadinIcon.WARNING,      stateText, classSuffix); // alternatives: EXCLAMATION_CIRCLE, QUESTION_CIRCLE
				case OK:       return new LabelWithIcon(VaadinIcon.CHECK_CIRCLE, stateText, classSuffix); // alternatives: SMILEY_O, THUMBS_UP
				default: throw new IllegalArgumentException("illegal state: " + state); //$NON-NLS-1$
				}
			})
			.setHeader(Messages.getString("NodeView.TreeGrid.ColumnName.State")) //$NON-NLS-1$
			.setKey("state") // set a column key so you can access this column later on using grid.getColumnByKey("myIcon"); //$NON-NLS-1$
			.setAutoWidth(true)
			.setFlexGrow(0)
			;
		this.tree.addColumn(node -> NodeTypeLabelProvider.getNodeTypeLabel(node))
			.setHeader(Messages.getString("NodeView.TreeGrid.ColumnName.Type")) //$NON-NLS-1$
			.setKey("nodeType") //$NON-NLS-1$
			.setAutoWidth(true)
			.setFlexGrow(0)
			;
		this.tree.addColumn(Node::getName)
			.setHeader(Messages.getString("NodeView.TreeGrid.ColumnName.Name")) //$NON-NLS-1$
			.setKey("name") //$NON-NLS-1$
			.setAutoWidth(true)
			.setFlexGrow(0)
			;
		this.tree.addColumn(node -> getNodeDescription(node))
			.setHeader(Messages.getString("NodeView.TreeGrid.ColumnName.Description")) //$NON-NLS-1$
			.setKey("description") //$NON-NLS-1$
			.setAutoWidth(false)
			.setFlexGrow(1)
			;
		this.tree.addColumn((node) -> node.isApplicable() ? "\u2714" : "\u2716") // ✔ ✖ //$NON-NLS-1$ //$NON-NLS-2$
			.setHeader(Messages.getString("NodeView.TreeGrid.ColumnName.Applicable")) //$NON-NLS-1$
			.setKey("applicable") //$NON-NLS-1$`
			.setAutoWidth(true)
			.setFlexGrow(0)
			;

		// connect view and model:
		this.tree.asSingleSelect().addValueChangeListener(event ->
			{
//				if (event.isFromClient()) { // can also be triggered programmatically!
					@SuppressWarnings("unchecked")
					N node = (N)event.getValue(); // getValue() returns the selected Node or null if none is selected /
					log.info("Selection changed: node='{}'.", (node != null ? node.getName() : "null")); //$NON-NLS-1$ //$NON-NLS-2$
					if (this.form != null && this.form.isVisible()) {
						if (node != null) {
							editNode(node, null);
						} else {
							closeEditor();
						}
					}
//				}
			});

		this.tree.addItemClickListener(event ->
			{
				Node node = event.getItem();
				log.info("Clicked node '{}'.", (node != null ? node.getName() : "null")); //$NON-NLS-1$ //$NON-NLS-2$
				// any action? / already done in asSingleSelect
			});

		this.tree.addItemDoubleClickListener(event -> {
			@SuppressWarnings("unchecked")
			N node = (N)event.getItem(); // returns the selected Node or null if none is selected /
			log.info("DoubleClicked node '{}'.", (node != null ? node.getName() : "null")); //$NON-NLS-1$ //$NON-NLS-2$
			// action:
			editNode(node, null);
		});

		this.tree.addExpandListener(event -> {
			Collection<Node> nodes = event.getItems(); // getValue() returns the selected Node or null if none is selected /
			log.trace("Expanded {} node(s): '{}'.", nodes.size(), nodes); //$NON-NLS-1$
			nodes.forEach(node -> ((IntermediateNode)node).expanded = true);
			this.tree.recalculateColumnWidths(); // make sure the column widths are adjusted
		});
		this.tree.addCollapseListener(event -> {
			Collection<Node> nodes = event.getItems(); // getValue() returns the selected Node or null if none is selected /
			log.trace("Collapsed {} node(s): '{}'.", nodes.size(), nodes); //$NON-NLS-1$
			nodes.forEach(node -> ((IntermediateNode)node).expanded = false);
			this.tree.recalculateColumnWidths(); // make sure the column widths are adjusted
		});

		// this.tree.getColumns().forEach(col -> col.setAutoWidth(true));
		updateTree();
	}

	private TreeDataProvider<Node> createDataProvider() {
		TreeData<Node> treeData = new TreeData<>();
		treeData.addItems(getRootNodesAsBareNodes(), node -> {
			if (node instanceof IntermediateNode) {
				IntermediateNode iNode = (IntermediateNode)node;
				List<Node> children = iNode.getChildren();
				if (!children.isEmpty()) {
					log.trace("ValueProvider({}) -> {}", node.getName(), children.stream().map(n -> n.getName()).collect(Collectors.toList())); //$NON-NLS-1$
					return children;
				}
			}
			log.trace("ValueProvider({}) -> <empty-list>", node.getName()); //$NON-NLS-1$
			return Collections.emptyList();
		});
		TreeDataProvider<Node> dataProvider = new TreeDataProvider<>(treeData);

		// dataProvider.
		return dataProvider;
	}

	protected PropertyChangeListener createNodeRefreshListener() {
		return new PropertyChangeListenerWithPriorUIReservation<N>(this)
		{
			@Override
			protected void handlePropertyChangeEvent(N sourceNode, String propertyName, Object oldValue, Object newValue) {
				NodeView.this.handleChangeEvent(sourceNode, propertyName, oldValue, newValue);
			}

			// as long as ther are no derivatived of NodeView we can short-cut the call to handleChangeEvent:
			@Override
			protected boolean predicate(N source, String propertyName, Object oldValue, Object newValue) {
				return Objects.equal(propertyName, Node.PROPERTYNAME_STATE);
			}
			// for better log-messages:
			@Override
			public String toString() {
				return "ChangeListener_on_NodeView"; //$NON-NLS-1$
			}
		};
	}

	private static String getNodeDescription(Node node) {
		return node instanceof RootNode
			? node.getDescription() + " [" + ((RootNode)node).getFilePath() + "]" //$NON-NLS-1$ //$NON-NLS-2$
			: node.getDescription();
	}

	/**
	 * Handles updates to the data that is NOT triggered via the UI but which may require UI updates (like
	 * the automatic node state updates).
	 * This method can be extended by subclasses that need to react to further field changes.
	 * @param sourceNode
	 * @param propertyName
	 * @param oldValue
	 * @param newValue
	 */
	protected void handleChangeEvent(N sourceNode, String propertyName, Object oldValue, Object newValue) {
		log.trace("handleChangeEvent called for '{}': '{}' -> '{}' for '{}'", propertyName, oldValue, newValue, sourceNode); //$NON-NLS-1$
		switch (propertyName) {
		case Node.PROPERTYNAME_STATE:
			// we only need to refresh the tree view, if the state changed. Everything else is handled via NodeUpdateEvent
			log.trace("refreshItem on node '{}': state={}", sourceNode.getName(), newValue); //$NON-NLS-1$
			refreshNode(sourceNode, false);
			break;
		default:
			// ignore others
		}
	}

	// This Java restriction that one can not pass a Collection<subclass> as Collection<superclass> is soooo brain-damaged!!!
	public Collection<Node> getRootNodesAsBareNodes() {
		List<RootNode> rootNodes = this.nodeService.getRootNodes();
		return Arrays.asList(rootNodes.toArray(new Node[rootNodes.size()]));
	}

	private void select(Node node) {
		this.tree.select(node);
	}

	private void saveNodeAfterEditing(NodeForm.SaveEvent event, IntermediateNode parentNode) {
		Node node = event.getNode();
		log.debug("NodeView.saveNode[SaveEvent] '{}'", node.getName(), (parentNode != null ? parentNode.getName() : "null")); //$NON-NLS-1$ //$NON-NLS-2$
		if (node.getParent() == null && !(node instanceof RootNode)) { // adding a new node:
			addNewNode(node, parentNode);
		} else { // updating an existing node:
			updateNode(node);
		}
		// closeEditor();
	}

	private void deleteNode(NodeForm.DeleteEvent event) {
		Node node = event.getNode();
		log.debug("NodeView.deleteNode[DeleteEvent] '{}'", node.getName()); //$NON-NLS-1$
		if (!this.form.setNode(null, true)) { // signal that we want to leave that node:
			return; // if there are unsaved changes remain on form's current node
		}
		deleteNodes(Collections.singletonList(node));
		closeEditor();
	}

	@SuppressWarnings("static-method")
	private void createNewRootNode() {
//		RootNode newNode = new RootNode("new node", "new description"); //$NON-NLS-1$ //$NON-NLS-2$
//		this.nodeService.getRootNodes().add(newNode);
//		if (NodeView.alwaysSave) saveNode(newNode);
//		updateTree();
		// disabled until I have decided how to name this and where to store etc.
		UsageError("Sorry, additional RootNode(s) not supported, yet."); //$NON-NLS-1$
	}

	private void addNewNode(Node newNode, IntermediateNode parentNode) {
		log.info("addNewNode '{}' to parent '{}'", newNode.getName(), parentNode.getName()); //$NON-NLS-1$
		this.nodeService.addChild(parentNode, newNode);
		parentNode.expanded = true; // make sure the newly added child gets visible
		if (NodeView.alwaysSave) saveNode(newNode);
		updateTree();
		select(newNode); // and select it
	}

	private void ifSingleNodeSelectedDo(Consumer<Node> action) {
		Set<Node> nodes = this.tree.getSelectedItems();
		if (nodes.isEmpty()) {
			UsageError(Messages.getString("NodeView.ErrorMsg.ChildNodeSelectNode")); //$NON-NLS-1$
		} else if (nodes.size() > 1) {
			UsageError(Messages.getString("NodeView.ErrorMsg.ChildNodeSelectOneNodeOnly")); //$NON-NLS-1$
		} else {
			Node selectedNode = nodes.iterator().next(); // get first selected node
			action.accept(selectedNode);
		}
	}

	private void IfSuitableNodeSelectedForAddition(Consumer<IntermediateNode> action) {
		ifSingleNodeSelectedDo((selectedNode) ->
		{
			if (selectedNode instanceof IntermediateNode) {
				action.accept((IntermediateNode)selectedNode);
			} else {
				UsageError(Messages.getString("NodeView.ErrorMsg.AddChildNodeOnlyForIntermediateNodes")); //$NON-NLS-1$
			}
		});
	}

	void createNewChildNode(NodeType nodeType, IntermediateNode parentNode) {
		log.info("createNewChildNode(type={}, parentNode='{}')", nodeType, parentNode.getName()); //$NON-NLS-1$
		// this.tree.asSingleSelect().clear();
		N newNode;
		try {
			newNode = this.nodeFactory.createNode(nodeType);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException ex) {
			ExecutionError(String.format("Exception creating node of type '%s':", nodeType), ex); //$NON-NLS-1$
			return;
		}
		// We don't hook the newly defined node into the tree, yet, but before we allow a user to
		// edit it we need to provide it with an upstream properties-set or else the user will get
		// "property not found"-errors during validation when entering a placeholder into a field:
		newNode.getProperties().setParentProperties(parentNode.getProperties());
		editNode(newNode, parentNode);
	}

	private void updateNode(Node node) {
		log.info("updateNode '{}'", node.getName()); //$NON-NLS-1$
//		HierarchicalDataProvider<Node, SerializablePredicate<Node>> dataProvider = this.tree.getDataProvider();
// not necessary
//		node.bubbleUp(n ->
//			{
//				dataProvider.refreshItem(n);
//				if (NodeView.alwaysSave) saveNode(n);
//			});
		refreshNode(node, false);
		if (NodeView.alwaysSave) saveNode(node);
	}

	protected void refreshNode(Node node, boolean refreshChildren) {
		NodeView.this.tree.getDataProvider().refreshItem(node, refreshChildren);
		// workaround since refreshChildren==true in the above call has no effect:
		if (refreshChildren && node instanceof IntermediateNode) {
			// does not work :-(
			((IntermediateNode)node).getChildren().forEach(n -> refreshNode(n, refreshChildren));
			NodeView.this.tree.getDataProvider().refreshAll();
		}
	}

	@SuppressWarnings("unchecked")
	public void editNode(N node, IntermediateNode parentNode) {
		log.info("editNode {} (parent='{}')", node.getName(), (parentNode != null ? parentNode.getName() : "null")); //$NON-NLS-1$ //$NON-NLS-2$
		if (this.form != null) {
			N currentNode = this.form.getNode();
			if (!this.form.setNode(null, false)) { // signal that we want to leave that node:
				select(currentNode); // if there are unsaved changes remain on form's current node
				return;
			}
			this.form.clearListeners();
		}
		try {
			NodeForm<N> newForm;
			Class<N> clazz = (Class<N>)node.getClass();
			try {
				newForm = this.formFactory.createForm(clazz, this.nodeService);
			} catch (Exception ex) {
				log.error("Error creating form for " + clazz , ex); //$NON-NLS-1$
				ProgrammingError("Error creating node for " + clazz, ex); //$NON-NLS-1$
				return;
			}

			if (newForm != this.form) { // different form assigned:
				log.info("editNode: new form {}", newForm); //$NON-NLS-1$
				this.form = newForm;
				this.formWrapper.removeAll();
				this.formWrapper.add(this.form);
			}
			this.form.addListener(NodeForm.SaveEvent.class, (event) -> { saveNodeAfterEditing(event, parentNode); } );
			this.form.addListener(NodeForm.DeleteEvent.class, this::deleteNode);
			this.form.addListener(NodeForm.CloseEvent.class, e -> closeEditor());
			this.form.addListener(NodeForm.UpdateEvent.class, e -> updateNode(e.getNode()));
			this.form.setNode(node, false);

			this.form.setVisible(true);
			addClassName("editing"); //$NON-NLS-1$
		} catch (java.lang.IllegalStateException ex) {
			log.error("WTF?", ex); //$NON-NLS-1$
			this.formWrapper.removeAll(); // remove old form
			this.form = null;
		}
	}

	private void closeEditor() {
		log.debug("NodeView.closeEditor"); //$NON-NLS-1$
		this.form.setNode(null, false);
		this.form.setVisible(false);
		removeClassName("editing"); //$NON-NLS-1$
	}

	private void removeNodes(Collection<Node> nodes) {
		log.info("removeNodes {}.", nodes); //$NON-NLS-1$
//		String question = nodes.stream()
//			.map(node -> node.getName())
//			.collect(Collectors.joining(", ", "Are you sure you want to delete node(s) [", "]?")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//		log.info("removeNodes: question: '{}'.", question); //$NON-NLS-1$
//		new ConfirmDialog("Confirm deletion", question, //$NON-NLS-1$
//		                  "Delete", evt -> { log.info("delete '{}':", nodes); //$NON-NLS-1$ //$NON-NLS-2$
//		                                     deleteNodes(nodes); },
//		                  "Cancel", evt -> { log.info("cancel '{}':", nodes); } //$NON-NLS-1$ //$NON-NLS-2$
//		                 );
		deleteNodes(nodes);
	}

	@SuppressWarnings("unused")
	private void removeList(Collection<Node> nodes) {
		log.info("removeList {}.", nodes); //$NON-NLS-1$
		String question = nodes.stream()
			.map(node -> NodeService.getCorrespondingRootNode(node).getName())
			.collect(Collectors.joining(Messages.getString("NodeView.Deletion.Question.ListSeparator"), //$NON-NLS-1$
			                            Messages.getString("NodeView.Deletion.Question.Prefix"), //$NON-NLS-1$
			                            Messages.getString("NodeView.Deletion.Question.Suffix"))); //$NON-NLS-1$
		new ConfirmDialog(Messages.getString("NodeView.Deletion.Header"), question, //$NON-NLS-1$
		                  Messages.getString("NodeView.Deletion.Confirm"), evt -> nodes.forEach(node -> deleteNodes(NodeService.getCorrespondingRootNode(node).getChildren())), //$NON-NLS-1$
		                  Messages.getString("NodeView.Deletion.Cancel"), evt -> {/**/}); //$NON-NLS-1$
	}

	private void saveNode(Node node) {
		this.nodeService.save(node);
	}

	private void saveAllRootNodes() {
		this.nodeService.saveAllRootNodes();
	}

	private void moveUp(Node node) {
		log.info("moveUp '{}'", node.getName()); //$NON-NLS-1$
		if (node instanceof RootNode) {
			UsageError(Messages.getString("NodeView.ErrorMsg.NoMoveUpForRootNodes")); //$NON-NLS-1$
		} else {
			IntermediateNode parent = node.getParent();
			List<Node> siblings = parent.getChildren();
			int currentIdx = siblings.indexOf(node);
			if (currentIdx <= 0) {
				UsageError(Messages.getString("NodeView.ErrorMsg.NoMoveUpBeyondFirstPosition")); //$NON-NLS-1$
			} else {
				synchronized(siblings) {
					siblings.remove(node);
					siblings.add(currentIdx-1, node);
				}
				if (NodeView.alwaysSave) saveNode(node);
				updateSubTree(parent);
			}
		}
		select(node);
	}

	private void moveDown(Node node) {
		log.info("moveDown '{}'", node.getName()); //$NON-NLS-1$
		if (node instanceof RootNode) {
			UsageError(Messages.getString("NodeView.ErrorMsg.NoMoveDownForRootNodes")); //$NON-NLS-1$
		} else {
			IntermediateNode parent = node.getParent();
			List<Node> siblings = parent.getChildren();
			int currentIdx = siblings.indexOf(node);
			if (currentIdx+1 >= siblings.size()) {
				UsageError(Messages.getString("NodeView.ErrorMsg.NoMoveDownBeyondLastPosition")); //$NON-NLS-1$
			} else {
				synchronized(siblings) {
					siblings.remove(node);
					siblings.add(currentIdx+1, node);
				}
				if (NodeView.alwaysSave) saveNode(node);
				updateSubTree(parent);
			}
		}
		select(node);
	}

	private void promote(Node node) {
		log.info("promote '{}'", node.getName()); //$NON-NLS-1$
		if (node instanceof RootNode) {
			UsageError(Messages.getString("NodeView.ErrorMsg.NoPromoteForRootNodes")); //$NON-NLS-1$
		} else {
			IntermediateNode parent = node.getParent();
			IntermediateNode grandParent = parent.getParent();
			if (grandParent == null) {
				UsageError(Messages.getString("NodeView.ErrorMsg.NoPromoteForFirstLevelNodes")); //$NON-NLS-1$
			} else {
				List<Node> newSiblings = grandParent.getChildren();
				int parentPos = newSiblings.indexOf(parent);
				log.info("promote '{}' old parent '{}' -> new parent: '{}'", //$NON-NLS-1$
				         node.getName(), parent.getName(), grandParent.getName());
				grandParent.addChildAtPos(parentPos+1, node);
				if (NodeView.alwaysSave) saveNode(node);
				updateTree();
			}
		}
		select(node);
	}

	private void demote(Node node) {
		log.info("demote '{}'", node.getName()); //$NON-NLS-1$
		if (node instanceof RootNode) {
			UsageError(Messages.getString("NodeView.ErrorMsg.NoDemoteForRootNodes")); //$NON-NLS-1$
		} else {
			IntermediateNode parent = node.getParent();
			List<Node> siblings = parent.getChildren();
			int myPos = siblings.indexOf(node);
			int pos = myPos-1;
			for (; pos >= 0; pos--) {
				Node sibling = siblings.get(pos);
				if (sibling instanceof IntermediateNode) {
					IntermediateNode newParent = (IntermediateNode)sibling;
					newParent.addChild(node);
					// node.setParent(newParent);
					if (NodeView.alwaysSave) saveNode(node);
					updateTree();
					break;
				}
			}
			if (pos < 0) {
				UsageError(Messages.getString("NodeView.ErrorMsg.NoParentForDemotion")); //$NON-NLS-1$
			}
		}
		select(node);
	}


//	@SuppressWarnings("null")
//	private void readLocalFile() {
//		String fileName = null; // TODO : provide some file selection box...
//		if (fileName == null) fileName = NodeService.DEFAULT_FILE_NAME;
//		try {
//			File file = new File(fileName + '.' + NodeService.CONFIG_FILE_TYPE);
//			RootNode rootNode = this.nodeService.read(file);
//			reportResult(rootNode, file.getAbsolutePath());
//		} catch (Throwable t) {
//			reportException(fileName, t);
//		}
//	}
//
	private void reportResult(RootNode rootNode, String sourceName) {
		if (rootNode != null) { // new root was read: update UI
			updateTree();
		} else { // reading failed:
			String msg = String.format(Messages.getString("NodeView.ErrorMsg.IllegalFileName"), sourceName); //$NON-NLS-1$
			log.info(msg);
			new Notification(msg, 0).open();
		}
	}

	private void updateTree() {
		log.info("updateTree"); //$NON-NLS-1$
//		// remove old refreshListener:
		if (this.nodeRefreshListener == null) this.nodeService.getRootNodes().forEach((node) -> node.removeChangeListener(this.nodeRefreshListener));
		log.trace("data-provider: {}", this.tree.getDataProvider()); //$NON-NLS-1$
//		if (this.tree.getDataProvider() == null) {
			this.tree.setDataProvider(createDataProvider());
			if (this.nodeRefreshListener == null) {
				this.nodeRefreshListener = createNodeRefreshListener(); // make sure we have one...
			}
			// register our refresh-listener with each (new)root-node:
			this.nodeService.getRootNodes().forEach((node) -> node.addChangeListener(this.nodeRefreshListener));
//		}
		// expand/collapse tree to previously saved state:
		getRootNodesAsBareNodes().forEach(node -> adjustCollapseExpanded(node));
		this.tree.recalculateColumnWidths(); // make sure the column widths are adjusted
	}

	private void updateSubTree(IntermediateNode parent) {
		TreeData<Node> treeData = this.tree.getTreeData();
		// remove all children:
		parent.getChildren().forEach((child) -> treeData.removeItem(child));
		// re-add all children:
		addChildren(treeData, parent);
		adjustCollapseExpanded(parent);
	}

	private void addChildren(TreeData<Node> treeData, Node parent) {
		if (parent instanceof IntermediateNode) {
			List<Node> children = ((IntermediateNode)parent).getChildren();
			treeData.addItems(parent, children);
			children.forEach(child -> addChildren(treeData, child));
		}
	}

	private void adjustCollapseExpanded(Node startNode) {
		// expand/collapse tree to previously saved state:
		startNode.trickleDown(n ->
			{
				if (n instanceof IntermediateNode) {
					IntermediateNode rn = (IntermediateNode)n;
					if (rn.expanded) {
						this.tree.expand(rn);
					} else {
						this.tree.collapse(rn);
					}
				}
			});
		refreshNode(startNode, true);
	}

	@SuppressWarnings("static-method")
	private void reportException(String sourceName, Throwable t) {
		String msg = String.format(Messages.getString("NodeView.ErrorMsg.ExceptionReading"), sourceName, t); //$NON-NLS-1$
		log.info(msg, t);
		openDialog(msg, t);
	}

	/**
	 * The method could handle multiple nodes, but we currently allow selection of one node only
	 * @param nodes
	 */
	private void deleteNodes(Collection<Node> nodes) {
		log.info("deleteNodes {}", nodes); //$NON-NLS-1$
		List<IntermediateNode> parents = new ArrayList<>();
		try {
			nodes.forEach((node) -> {
				log.info("deleting nodes '{}'", node); //$NON-NLS-1$
				if (node instanceof RootNode) {
					this.nodeService.removeRootNode((RootNode)node);
				} else {
					IntermediateNode parent = node.getParent();
					Node deletedNode = this.nodeService.removeChild(parent, node);
					log.info("deleted node '{}'", deletedNode); //$NON-NLS-1$
					if (NodeView.alwaysSave) saveNode(parent);
					parents.add(parent);
				}
			});
		} catch (Throwable t) {
			String msg = String.format("Exception deleting node '%s': %s", nodes, t); //$NON-NLS-1$
			log.error(msg, t);
			new Notification(msg, 5000).open();
		}
		updateTree();
		parents.forEach((node) -> this.tree.select(node));
	}

	/* User operation error - this CAN happen but does no harm */
	public static void UsageError(String msg) {
		String text =  String.format(Messages.getString("NodeView.Notification.UsageError"), msg); //$NON-NLS-1$
		log.info(text);
		openDialog(text, null);
	}

	public static void ConfigError(String msg, Throwable t) {
		String text = String.format(Messages.getString("NodeView.Notification.ConfigError"), msg); //$NON-NLS-1$
		log.warn(text, t);
		openDialog(text, t);
	}

	public static void ExecutionError(String msg, Throwable t) {
		String text = String.format(Messages.getString("NodeView.Notification.ExecutionError"), msg); //$NON-NLS-1$
		log.warn(msg, t);
		openDialog(text, t);
	}

	/* This should of course never happen... ;-) */
	public static void ProgrammingError(String msg, Throwable t) {
		String text = String.format(Messages.getString("NodeView.Notification.ProgrammingError"), msg); //$NON-NLS-1$
		log.error(text, t);
		openDialog(text, t);
	}

	private static void openDialog(String msg, Throwable t) {
		UI ui = UI.getCurrent();
		ui.access(() ->
			{
				new ConfirmDialog(null, msg + "\n" + ExceptionUtils.exceptionCauseSummary(t),  //$NON-NLS-1$
				                  Messages.getString("NodeView.Notification.Close.Label"), null) //$NON-NLS-1$
					.open();
			});
	}

	private static class LabelWithIcon extends HorizontalLayout
	{
		private static final long serialVersionUID = 5149939812863789723L;

		public LabelWithIcon(VaadinIcon iconName, String text, String classSuffix) {
			if (iconName != null) {
				Icon icon = new Icon(iconName);
				icon.setClassName("icon-" + classSuffix); //$NON-NLS-1$
				add(icon);
			}
			if (text != null) {
				Label label = new Label(text);
				label.setClassName("label-" + classSuffix); //$NON-NLS-1$
				add(label);
			}
		}
	}
}