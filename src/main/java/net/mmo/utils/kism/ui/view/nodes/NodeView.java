package net.mmo.utils.kism.ui.view.nodes;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
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
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.IntermediateNode;
import net.mmo.utils.kism.entities.Node;
import net.mmo.utils.kism.entities.RootNode;
import net.mmo.utils.kism.ui.CommonConstants;
import net.mmo.utils.kism.ui.CommonConstants.LayoutDirection;
import net.mmo.utils.kism.ui.MainLayout;
import net.mmo.utils.kism.ui.utils.PropertyChangeListenerWithPriorUIReservation;
import net.mmo.utils.kism.ui.view.nodes.NodeFactory.NodeType;
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

	TreeGrid<Node> tree; // package visible for UTs - is there no other way?
	Div content;

	NodeForm<N> form; // package visible for ITs - is there no other way?
	LayoutDirection contentDirection = LayoutDirection.DirectionRow;

	protected static boolean alwaysSave = Boolean.getBoolean(AppProperties.getProperties().getProperty("NodeView.alwaysSaveAfterEdit", "false")); //$NON-NLS-1$ //$NON-NLS-2$

	protected PropertyChangeListenerWithPriorUIReservation<N> nodeRefreshListener;

	public NodeView(NodeService nodeService,
	                NodeFactory<N> nodeFactory,
	                FormFactory<N> formFactory) { // defined as constructor args to force order
		log.debug("creating {}:", this.getClassName()); //$NON-NLS-1$
		try {
			this.nodeService = nodeService;
			this.nodeFactory = nodeFactory;
			this.formFactory = formFactory;

			addClassName(ViewClassName);
			setSizeFull(); // use entire browser window

			configureTree();

			log.trace("Creating content:"); //$NON-NLS-1$
			this.content = new Div(this.tree);
			this.content.addClassName(ViewContentClassName);
			this.content.setSizeFull();
			CommonConstants.setClassName(this.content, this.contentDirection);

			add(createToolbar(), this.content);

		} catch (Throwable t) {
			log.error("Exception in c'tor:", t); //$NON-NLS-1$
		}
		log.debug("{} complete.", this.getClassName()); //$NON-NLS-1$
	}

	private HorizontalLayout createToolbar() {
		log.trace("Creating toolbar:"); //$NON-NLS-1$
//		this.filterText.setClassName(FilterFieldClassName);
//		this.filterText.setPlaceholder(Messages.getString("ListView.TextField.FilterText.Label")); //$NON-NLS-1$
//		this.filterText.setClearButtonVisible(true);
//		this.filterText.setValueChangeMode(ValueChangeMode.LAZY);
//		this.filterText.addValueChangeListener(e -> updateList());

		ComboBox<NodeType> nodeTypeSelector = new ComboBox<>(Messages.getString("NodeView.ComboBox.NodeTypes.Label")); //$NON-NLS-1$
		nodeTypeSelector.setItems(NodeType.values());
		nodeTypeSelector.setValue(NodeType.IntermediateNode);
		nodeTypeSelector.addValueChangeListener(change ->
			{
				log.info("nodeTypeSelector changed to {}", change.getValue()); //$NON-NLS-1$
			});

		Button addChildNodeButton = new Button(Messages.getString("NodeView.Button.AddNode.Label")); //$NON-NLS-1$
		addChildNodeButton.addClickListener(click ->
			{
				NodeType type = nodeTypeSelector.getValue();
				log.info("add node (type={})", type); //$NON-NLS-1$
				if (type.ordinal() <= 0) {
					UsageError("NodeView.ErrorMsg.NodeTypeMustBeSelected"); //$NON-NLS-1$
				} else {
					IfSuitableNodeSelectedForAddition((selectedNode) -> createNewChildNode(type)); // in the first step we only create a new node
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
				CommonConstants.setClassName(this.content, this.contentDirection);
			});

		Button saveAllButton = new Button(Messages.getString("NodeView.Button.SaveAll.Label")); //$NON-NLS-1$
		saveAllButton.addClickListener(click ->
			{
				log.info("saveAllButton clicked"); //$NON-NLS-1$
				saveAllRoots();
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
		uploadFile.addSucceededListener(event -> {
			String fileName = event.getFileName();
			try (InputStream is = buffer.getInputStream();
			     BufferedInputStream bis = new BufferedInputStream(is)) {
				RootNode rootNode = this.nodeService.read(is, fileName);
				reportResult(rootNode, fileName);
			} catch (Throwable t) {
				reportException(fileName, t);
			}
		});

		HorizontalLayout toolbar = new HorizontalLayout(/*this.filterText,*/ nodeTypeSelector, addChildNodeButton, delNodeButton, clearListButton, directionButton, saveAllButton, downloadFileButton, uploadFile);
		toolbar.addClassName(ViewToolbarClassName);
		return toolbar;
	}

	private void configureTree() {
		log.trace("Configuring TreeGrid:"); //$NON-NLS-1$
		this.tree = new TreeGrid<>();
		this.tree.addClassName(TreeClassName);
		// this.tree.setSizeFull();

		// this.tree.addHierachyColumn(Node::getState)
		this.tree.addComponentHierarchyColumn(node -> {
				Node.State state = node.getState();
				switch (state) {
				case FAILED: return new LabelWithIcon(VaadinIcon.CLOSE_CIRCLE, state);  // alternatives: BAN, BOLT, FROWN_O, THUMBS_DOWN?
				case DEGRADED: return new LabelWithIcon(VaadinIcon.WARNING, state); // alternatives: EXCLAMATION_CIRCLE, QUESTION_CIRCLE
				case OK: return new LabelWithIcon(VaadinIcon.CHECK_CIRCLE, state); // alternatives: SMILEY_O, THUMBS_UP
				default: throw new IllegalArgumentException("illegal state: " + state); //$NON-NLS-1$
				}
			})
			.setHeader(Messages.getString("NodeView.TreeGrid.ColumnName.State")) //$NON-NLS-1$
			.setKey("state"); // set a column key so you can access this column later on using grid.getColumnByKey("myIcon"); //$NON-NLS-1$
		// this.tree.addHierarchyColumn(Node::getName)
		this.tree.addColumn(Node::getName)
			.setHeader(Messages.getString("NodeView.TreeGrid.ColumnName.Name")) //$NON-NLS-1$
			.setKey("name"); //$NON-NLS-1$
		this.tree.addColumn(Node::getDescription)
			.setHeader(Messages.getString("NodeView.TreeGrid.ColumnName.Description")) //$NON-NLS-1$
			.setKey("description"); //$NON-NLS-1$
		this.tree.addColumn(Node::isEnabled)
			.setHeader(Messages.getString("NodeView.TreeGrid.ColumnName.Enabled")) //$NON-NLS-1$
			.setKey("enabled"); //$NON-NLS-1$`

		this.tree.getColumns().forEach(col -> col.setAutoWidth(true));
		updateTree();

		// connect view and model:
		this.tree.asSingleSelect().addValueChangeListener(event ->
			{
				if (event.isFromClient()) {
					@SuppressWarnings("unchecked")
					N node = (N)event.getValue(); // getValue() returns the selected Node or null if none is selected /
					log.info("Selection changed: node='{}'.", node); //$NON-NLS-1$
					if (this.form != null && this.form.isVisible()) {
						if (node != null) {
							editNode(node, Messages.getString("NodeView.Form.EditOrDeleteNode.Title")); //$NON-NLS-1$)
						} else {
							closeEditor();
						}
					}
				}
			});

		this.tree.addItemClickListener(event ->
			{
				Node node = event.getItem();
				log.info("Clicked node '{}'.", node); //$NON-NLS-1$
				// any action? / already done in asSingleSelect
			});

		this.tree.addItemDoubleClickListener(event -> {
			@SuppressWarnings("unchecked")
			N node = (N)event.getItem(); // returns the selected Node or null if none is selected /
			log.info("DoubleClicked node '{}'.", node); //$NON-NLS-1$
			// action:
			editNode(node, Messages.getString("NodeView.Form.EditOrDeleteNode.Title")); //$NON-NLS-1$)
		});

		this.tree.addExpandListener(event -> {
			Collection<Node> nodes = event.getItems(); // getValue() returns the selected Node or null if none is selected /
			log.info("Expanded {} node(s): '{}'.", nodes.size(), nodes); //$NON-NLS-1$
			// any action?
		});
		this.tree.addCollapseListener(event -> {
			Collection<Node> nodes = event.getItems(); // getValue() returns the selected Node or null if none is selected /
			log.info("Collapsed {} node(s): '{}'.", nodes.size(), nodes); //$NON-NLS-1$
			// any action?
		});

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

	protected void createNodeRefreshListener() {
		this.nodeRefreshListener = new PropertyChangeListenerWithPriorUIReservation<N>(this)
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
			NodeView.this.tree.getDataProvider().refreshItem(sourceNode, true);
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

	private void saveNode(NodeForm.SaveEvent event) {
		Node node = event.getNode();
		log.debug("NodeView.saveNode[SaveEvent] {}", node); //$NON-NLS-1$
		if (node.getParent() == null && !(node instanceof RootNode)) { // adding a new node:
			addNewNode(node);
		} else { // updating an existing node:
			updateNode(node);
		}
		// closeEditor();
	}

	private void deleteNode(NodeForm.DeleteEvent event) {
		Node node = event.getNode();
		log.debug("NodeView.deleteNode[DeleteEvent] {}", node); //$NON-NLS-1$
		deleteNodes(Collections.singletonList(node));
		if (NodeView.alwaysSave) saveNode(node);
		closeEditor();
	}

	private void addNewNode(Node newNode) {
		log.info("addNewNode '{}'", newNode); //$NON-NLS-1$
		IfSuitableNodeSelectedForAddition((selectedNode) -> // in the second step we add node to the selected node
			{
				this.nodeService.addChild(selectedNode, newNode);
				// this.tree.getDataProvider().refreshAll();
			});
		if (NodeView.alwaysSave) saveNode(newNode);
		updateTree();
	}

	private void IfSuitableNodeSelected(Consumer<Node> action) {
		Set<Node> nodes = this.tree.getSelectedItems();
		if (nodes.isEmpty()) {
			UsageError("NodeView.ErrorMsg.AddChildNodeSelectNode"); //$NON-NLS-1$
		} else if (nodes.size() > 1) {
			UsageError("NodeView.ErrorMsg.AddChildNodeSelectOneNodeOnly"); //$NON-NLS-1$
		} else {
			Node selectedNode = nodes.iterator().next(); // get first selected node
			action.accept(selectedNode);
		}
	}

	private void IfSuitableNodeSelectedForAddition(Consumer<IntermediateNode> action) {
		IfSuitableNodeSelected((selectedNode) ->
		{
			if (selectedNode instanceof IntermediateNode) {
				action.accept((IntermediateNode)selectedNode);
			} else {
				UsageError("NodeView.ErrorMsg.AddChildNodeOnlyForIntermediateNodes"); //$NON-NLS-1$
			}
		});
	}

	void createNewChildNode(NodeType nodeType) {
		log.info("createNewChildNode"); //$NON-NLS-1$
		// this.tree.asSingleSelect().clear();
		editNode(this.nodeFactory.createNode(nodeType),
		         Messages.getString("NodeView.Form.NewNode.Title")); //$NON-NLS-1$
	}

	private void updateNode(Node node) {
		log.info("updateNode {}", node); //$NON-NLS-1$
		HierarchicalDataProvider<Node, SerializablePredicate<Node>> dataProvider = this.tree.getDataProvider();
// not necessary
//		node.bubbleUp(n ->
//			{
//				dataProvider.refreshItem(n);
//				if (NodeView.alwaysSave) saveNode(n);
//			});
		dataProvider.refreshItem(node);
		if (NodeView.alwaysSave) saveNode(node);
	}

	@SuppressWarnings("unchecked")
	public void editNode(N node, String header) {
		log.info("editNode {}", node); //$NON-NLS-1$
		if (this.form != null) {
			N currentNode = this.form.getNode();
			if (!this.form.setNode(null)) { // signal that we want to leave that node:
				this.tree.select(currentNode); // if there are unsaved changes remain on current node
				return;
			}
		}
		try {
			NodeForm<N> newForm;
			try {
				newForm = this.formFactory.createForm((Class<N>)node.getClass());
			} catch (Exception ex) {
				log.error("Error creating node:", ex); //$NON-NLS-1$
				ProgrammingError("Error creating node", ex); //$NON-NLS-1$
				return;
			}
			if (newForm != this.form) { // different form assigned:
				log.info("editNode: new form {}", newForm); //$NON-NLS-1$
				this.form = newForm;
				this.content.removeAll(); // remove old form
				this.content.add(this.tree, this.form); // add new form
				// add listeners (if not already present):
				this.form.addListener(NodeForm.SaveEvent.class, this::saveNode);
				this.form.addListener(NodeForm.DeleteEvent.class, this::deleteNode);
				this.form.addListener(NodeForm.CloseEvent.class, e -> closeEditor());
				this.form.addListener(NodeForm.UpdateEvent.class, e -> updateNode(e.getNode()));
			}
			this.form.setHeader(header);
			this.form.setNode(node);
			this.form.setVisible(true);
			addClassName("editing"); //$NON-NLS-1$
		} catch (java.lang.IllegalStateException ex) {
			log.error("WTF?", ex); //$NON-NLS-1$
			this.content.removeAll(); // remove old form
			this.form = null;
		}
	}

	private void closeEditor() {
		log.debug("NodeView.closeEditor"); //$NON-NLS-1$
		this.form.setNode(null);
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
			.collect(Collectors.joining(", ", "Are you sure you want to delete ENTIRE list(s) [", "]?")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		new ConfirmDialog("Confirm deletion", question, //$NON-NLS-1$
		                  "Delete", evt -> nodes.forEach(node -> deleteNodes(NodeService.getCorrespondingRootNode(node).getChildren())), //$NON-NLS-1$
		                  "Cancel", evt -> {/**/}); //$NON-NLS-1$
	}

	private void saveNode(Node node) {
		RootNode rootNode = NodeService.getCorrespondingRootNode(node);
		saveRootNode(rootNode);
	}

	private void saveAllRoots() {
		this.nodeService.getRootNodes().forEach(node -> saveRootNode(node));
	}

	private void saveRootNode(RootNode rootNode) {
		try {
			String fileName = rootNode.getFilePath();
			if (fileName == null) fileName = NodeService.DEFAULT_FILE_NAME + '.' + NodeService.CONFIG_FILE_TYPE;
			// TODO : provide some file selection box...
			File file = new File(fileName);
			if (file.canWrite()) {
				this.nodeService.write(rootNode, file);
				rootNode.setFilePath(fileName);
			} else {
				String msg = String.format("File '%s' is not writable!", file); //$NON-NLS-1$
				log.info(msg);
				new Notification(msg, 0).open();
			}
		} catch (Throwable t) {
			String msg = String.format("Exception saving node '%s': %s", rootNode, t); //$NON-NLS-1$
			log.error(msg, t);
			new Notification(msg, 0).open();
		}
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
		// remove old refreshListener:
		if (this.nodeRefreshListener == null) this.nodeService.getRootNodes().forEach((node) -> node.removeChangeListener(this.nodeRefreshListener));
		this.tree.setDataProvider(createDataProvider());
		// register our refresh-listener with each (new)root-node:
		if (this.nodeRefreshListener == null) createNodeRefreshListener(); // make sure we have one...
		this.nodeService.getRootNodes().forEach((node) -> node.addChangeListener(this.nodeRefreshListener));
		this.tree.expandRecursively(getRootNodesAsBareNodes(), 3);
		// this.tree.getDataProvider().refreshAll();
	}

	@SuppressWarnings("static-method")
	private void reportException(String sourceName, Throwable t) {
		String msg = String.format(Messages.getString("NodeView.ErrorMsg.ExceptionReading"), sourceName, t); //$NON-NLS-1$
		log.info(msg, t);
		openDialog(msg, t);
	}

	private void deleteNodes(Collection<Node> nodes) {
		log.info("deleteNodes {}", nodes); //$NON-NLS-1$
		try {
			nodes.forEach((node) -> {
				log.info("deleting nodes '{}'", node); //$NON-NLS-1$
				if (node instanceof RootNode) {
					this.nodeService.getRootNodes().remove(NodeService.getCorrespondingRootNode(node));
				} else {
					Node deletedNode = this.nodeService.removeChild(node.getParent(), node);
					log.info("deleted node '{}'", deletedNode); //$NON-NLS-1$
				}
			});
		} catch (Throwable t) {
			String msg = String.format("Exception deleting node '%s': %s", nodes, t); //$NON-NLS-1$
			log.error(msg, t);
			new Notification(msg, 10000).open();
		} finally {
			updateTree();
		}
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
				Dialog dialog = new Dialog();
				VerticalLayout msgLane = new VerticalLayout();
				for (String str: msg.split("\n")) { //$NON-NLS-1$
					msgLane.add(new Label(str));
				}
				for (String str: ExceptionUtils.exceptionCauseSummary(t).split("\n")) { //$NON-NLS-1$
					msgLane.add(new Label(str));
				}
				Button button = new Button(Messages.getString("NodeView.Notification.Close.Label")); //$NON-NLS-1$
				button.addClickListener(event -> dialog.close());
				dialog.add(msgLane, button);
				dialog.open();
			});
	}

	private static class LabelWithIcon extends HorizontalLayout
	{
		private static final long serialVersionUID = 5149939812863789723L;

		public LabelWithIcon(VaadinIcon icon, Node.State state) {
			add(new Icon(icon));
			add(new Label(StateLabelProvider.getStateLabel(state)));
		}
	}
}