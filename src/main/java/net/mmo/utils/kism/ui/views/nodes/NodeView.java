/**
 * Copyright © 2020-2025 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.ui.views.nodes;

import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
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
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.nodes.IntermediateNode;
import net.mmo.utils.kism.entities.nodes.LeafNode;
import net.mmo.utils.kism.entities.nodes.Node;
import net.mmo.utils.kism.entities.nodes.NodeFactory;
import net.mmo.utils.kism.entities.nodes.RootNode;
import net.mmo.utils.kism.entities.nodes.VisibleNodeType;
import net.mmo.utils.kism.security.SecurityUtils;
import net.mmo.utils.kism.ui.CommonConstants;
import net.mmo.utils.kism.ui.CommonConstants.LayoutDirection;
import net.mmo.utils.kism.ui.MainLayout;
import net.mmo.utils.kism.ui.utils.ConfirmDialog;
import net.mmo.utils.kism.ui.utils.LazyDownloadButton;
import net.mmo.utils.kism.ui.utils.PropertyChangeListenerWithPriorUIReservation;
import net.mmo.utils.kism.ui.utils.UIHandlerSupport;
import net.mmo.utils.kism.utils.AppProperties;
import net.mmo.utils.kism.utils.ExceptionUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Defines the application's main view, routed to via the base URL
 * @param <N> the node-type
 */
@Route(value="", layout = MainLayout.class)
@PageTitle("Nodes | " + CommonConstants.ApplicationFullName)
@RolesAllowed({CommonConstants.Role_ADMIN, CommonConstants.Role_READ_ONLY})
@Component // to make it possible to @Autowire it
@Scope("prototype") // to ensure every test run gets a fresh instance.
@Slf4j
@SuppressWarnings("javadoc")
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

	public static final String ViewClassEditing = "editing"; //$NON-NLS-1$
	public static final String ViewClassViewing = "viewing"; //$NON-NLS-1$
	public static final String ViewClassHighlight = "highlight"; //$NON-NLS-1$

	private static final int MAX_DESCRIPTION_LENGTH =
		Integer.parseInt(AppProperties.getProperties().getProperty("NodeForm.MaxDescriptionLength", //$NON-NLS-1$
		                                                           "100")); //$NON-NLS-1$

	private NodeService nodeService;
	private NodeFactory<N> nodeFactory;
	private FormFactory<N> formFactory;

	private Checkbox haltRequestsCheckbox;
	private Button saveAllButton;
	// this is so that NodeForm can access the saveButton
	static NodeView<?> nodeView;
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
			this.content.setSplitterPosition(100); // initial position

			// this.content.setOrientation(this.contentDirection.getOrientation());
			CommonConstants.setClassName(this.content, this.contentDirection);

			if (SecurityUtils.isAdminUser()) {
				log.info("admin user"); //$NON-NLS-1$
				add(createToolbar(), this.content);
			} else {
				log.info("non-admin user"); //$NON-NLS-1$
				add(this.content);
			}

		} catch (Throwable t) {
			log.error("Exception in c'tor:", t); //$NON-NLS-1$
		}
		this.tree.getDataProvider().refreshAll();
		recalculateColumnWidths(1000); // make sure the column widths are adjusted

		nodeView = this;
		log.debug("{} complete.", this.getClassName()); //$NON-NLS-1$
	}

	private HorizontalLayout createToolbar() {
		log.trace("Creating toolbar:"); //$NON-NLS-1$

		ComboBox<VisibleNodeType> nodeTypeSelector = new ComboBox<>(Messages.getString("NodeView.ComboBox.NodeTypes.Label")); //$NON-NLS-1$
		nodeTypeSelector.setItems(VisibleNodeType.values());
		nodeTypeSelector.setValue(VisibleNodeType.IntermediateNode);
		nodeTypeSelector.addValueChangeListener(change ->
			{
				log.info("nodeTypeSelector changed to {}", change.getValue()); //$NON-NLS-1$
			});

		Button addNodeButton = new Button(Messages.getString("NodeView.Button.AddNode.Label")); //$NON-NLS-1$
		addNodeButton.addClickListener(click -> addNode(nodeTypeSelector.getValue()));

		Button duplicateNodeButton = new Button(Messages.getString("NodeView.Button.DuplicateNode.Label")); //$NON-NLS-1$
		duplicateNodeButton.addClickListener(click -> duplicateNode());

		Button delNodeButton = new Button(Messages.getString("NodeView.Button.DeleteNode.Label")); //$NON-NLS-1$
		delNodeButton.addClickListener(click -> deleteNode());

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

		Button moveUpButton = new Button(Messages.getString("NodeView.Button.MoveUp.Label")); //$NON-NLS-1$
		moveUpButton.addClickListener(click -> ifSingleNodeSelectedDo(node -> moveUp(node)));

		Button moveDownButton = new Button(Messages.getString("NodeView.Button.MoveDown.Label")); //$NON-NLS-1$
		moveDownButton.addClickListener(click -> ifSingleNodeSelectedDo(node -> moveDown(node)));

		Button promoteButton = new Button(Messages.getString("NodeView.Button.Promote.Label")); //$NON-NLS-1$
		promoteButton.addClickListener(click -> ifSingleNodeSelectedDo(node -> promote(node)));

		Button demoteButton = new Button(Messages.getString("NodeView.Button.Demote.Label")); //$NON-NLS-1$
		demoteButton.addClickListener(click -> ifSingleNodeSelectedDo(node -> demote(node)));

		this.saveAllButton = new Button(Messages.getString("NodeView.Button.SaveAll.Label")); //$NON-NLS-1$
		this.saveAllButton.addClickListener(click ->
			{
				log.info("saveAllButton clicked"); //$NON-NLS-1$
				if (saveAllRootNodes()) { // saving was successful:
					NodeService.createNotification(Messages.getString("NodeView.Button.SaveAll.Success"), 3000); //$NON-NLS-1$
					if (!NodeView.alwaysSave) {
						highlightSaveButton(false);
					}
				} else {
					NodeService.createNotification(Messages.getString("NodeView.Button.SaveAll.Error")); //$NON-NLS-1$
				}
			});

		LazyDownloadButton downloadFileButton =
			// Create a new instance - it needs at least a caption (or icon) and a callback to create the
			// content on click time. Note that the callback is called OUTSIDE of the current UI.
			// You need to use UI.access() if you want to access the UI.
			new LazyDownloadButton(Messages.getString("NodeView.Button.Download.Label"), //$NON-NLS-1$,
			                       () -> { // get filename:
			                        	RootNode root = getSelectedNodeRootNode();
			                        	return (root != null ? new File(root.getFilePath()).getName() : null);
			                       	},
			                       () -> { // get file data
			                        	RootNode root = getSelectedNodeRootNode();
			                        	if (root != null) { // error message already give in getSelectedNodeRootNodeFilename()
			                        		File file = new File(root.getFilePath());
			                        		String fullPath = file.getAbsolutePath();
			                        		if (file.exists() && file.isFile() && file.canRead()) {
			                        			try {
			                        				log.info("Opening file '{}':", fullPath); //$NON-NLS-1$
			                        				return new FileInputStream(file);
			                        			} catch (Exception ex) {
			                        				log.error("Exception opening file ''" + fullPath, ex); //$NON-NLS-1$
			                        			}
			                        		} else {
			                        			log.error("File '{}' not found or not readable", fullPath); //$NON-NLS-1$
			                        		}
			                        	}
			                        	return null;
			                       })
			{
				private static final long serialVersionUID = -948298009586080827L;
			};
		add(downloadFileButton);

		downloadFileButton.setDisableOnClick(true);
		downloadFileButton.addClickListener(event -> {
			// show some feedback to the user, that the download is being prepared in the background
			event.getSource().setText(Messages.getString("NodeView.Button.Download.Preparing")); //$NON-NLS-1$
		});
		// LDB also provides a "download start" listener - this listener is fired, when the client
		// side starts the download. Please be aware to NOT remove the button here or its content,
		// otherwise the download can fail.
		downloadFileButton.addDownloadStartsListener(event -> {
			log.info("Download started..."); //$NON-NLS-1$
			// reset the download button text:
			event.getSource().setText(Messages.getString("NodeView.Button.Download.Label")); //$NON-NLS-1$
			// restore normal state, so that the user can click to download the content again:
			downloadFileButton.setEnabled(true);
		});
		downloadFileButton.addDownloadAbortedListener(event -> {
			String msg = "Download was aborted - reason: " + event.getReason(); //$NON-NLS-1$
			log.info(msg);
			ExecutionError(msg, null);
			// reset the download button text:
			event.getSource().setText(Messages.getString("NodeView.Button.Download.Label")); //$NON-NLS-1$
			// restore normal state, so that the user can click to download the content again:
			downloadFileButton.setEnabled(true);
		});

		MemoryBuffer buffer = new MemoryBuffer(); // alternative would be: new FileBuffer();
		Upload uploadFile = new Upload(buffer);
		// uploadFile.setI18n(new UploadI18N());
		uploadFile.addStartedListener(event -> {
			String fileName = event.getFileName();
			log.info("Upload of file '{}' started.", fileName); //$NON-NLS-1$
		});
		uploadFile.addSucceededListener(event ->
			{
				String fileName = event.getFileName();
				log.info("Upload of file '{}' succeeded.", fileName); //$NON-NLS-1$
				try (InputStream is = buffer.getInputStream();
				     BufferedInputStream bis = new BufferedInputStream(is)) {
					RootNode rootNode = this.nodeService.readStream(is, fileName);
					reportUploadResult(rootNode, fileName);
				} catch (Throwable t) {
					reportFileReadException(fileName, t);
				}
			});

		this.haltRequestsCheckbox = new Checkbox(Messages.getString("NodeView.HaltAllRequests.Label")); //$NON-NLS-1$
		this.haltRequestsCheckbox.addValueChangeListener(event ->
			{
				LeafNode.setHaltAllRequests(event.getValue());
				adjustHaltRequest();
			});
		adjustHaltRequest();

		HorizontalLayout toolbar = new HorizontalLayout(nodeTypeSelector, addNodeButton, duplicateNodeButton, delNodeButton,
		                                                moveUpButton, moveDownButton, promoteButton, demoteButton,
		                                                directionButton, this.saveAllButton,
		                                                downloadFileButton, uploadFile,
		                                                this.haltRequestsCheckbox);
		toolbar.addClassName(ViewToolbarClassName);
		return toolbar;
	}

	protected void adjustHaltRequest() {
		boolean halted = LeafNode.isHaltAllRequests();
		// make sure the checkbox (and its coloring) and the actual value are in sync:
		if (this.haltRequestsCheckbox.getValue() != halted) this.haltRequestsCheckbox.setValue(halted);
		this.haltRequestsCheckbox.setLabel(Messages.getString(halted ? "NodeView.HaltAllRequests.Halted" : "NodeView.HaltAllRequests.Enabled")); //$NON-NLS-1$ //$NON-NLS-2$
		this.haltRequestsCheckbox.setClassName(halted ? "halted" : "enabled"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private RootNode getSelectedNodeRootNode() {
		RootNode root = null;
		Set<Node> nodes = this.tree.getSelectedItems();
		if (nodes.isEmpty()) {
			List<Node> roots = this.tree.getTreeData().getRootItems();
			if (roots.isEmpty()) {
				log.error("No root node found."); //$NON-NLS-1$
				return null;
			}
			root = (RootNode)roots.get(0);
			log.info("Selected first root node for download: '{}'", root.getName()); //$NON-NLS-1$
		} else { // node selected - find corresponding root node:
			Node node = nodes.iterator().next();
			root = node.getRootNode();
			if (root == null) {
				log.error("No root node found for node '{}'", node.getName()); //$NON-NLS-1$
				return null;
			}
			log.info("Selected root node of node '{}' for download: '{}'", node.getName(), root.getName()); //$NON-NLS-1$
		}
		return root;
	}

	private void configureTree() {
		log.trace("Configuring TreeGrid:"); //$NON-NLS-1$
		this.tree = new TreeGrid<>();
		this.tree.addClassName(TreeClassName);
		this.tree.setSizeFull();

		this.tree.addComponentHierarchyColumn(node -> {
				Node.State state = node.getState();
				if (state != null) {
					String stateText = StateLabelProvider.getStateLabel(state);
					String classSuffix = state.name(); // will yield class names icon-<classSuffix> and label-<classSuffix> - used to style these red/orange/green
					switch (state) {
					case FAILED:   return new LabelWithIcon(VaadinIcon.CLOSE_CIRCLE, stateText, classSuffix); // alternatives: BAN, BOLT, FROWN_O, THUMBS_DOWN?
					case DEGRADED: return new LabelWithIcon(VaadinIcon.WARNING,      stateText, classSuffix); // alternatives: EXCLAMATION_CIRCLE, QUESTION_CIRCLE
					case OK:       return new LabelWithIcon(VaadinIcon.CHECK_CIRCLE, stateText, classSuffix); // alternatives: SMILEY_O, THUMBS_UP
					default: throw new IllegalArgumentException("illegal state: " + state); //$NON-NLS-1$
					}
				} else {
					return new LabelWithIcon(VaadinIcon.MINUS, "<undefined>", ""); //$NON-NLS-1$ //$NON-NLS-2$
				}
			})
			.setHeader(Messages.getString("NodeView.TreeGrid.ColumnName.State")) //$NON-NLS-1$
			.setKey("state") // set a column key so you can access this column later on using grid.getColumnByKey("myIcon"); //$NON-NLS-1$
			;
		this.tree.addColumn(node -> NodeTypeLabelProvider.getNodeTypeLabel(node))
			.setHeader(Messages.getString("NodeView.TreeGrid.ColumnName.Type")) //$NON-NLS-1$
			.setKey("nodeType") //$NON-NLS-1$
			;
		this.tree.addColumn(Node::getName)
			.setHeader(Messages.getString("NodeView.TreeGrid.ColumnName.Name")) //$NON-NLS-1$
			.setKey("name") //$NON-NLS-1$
			;
		this.tree.addColumn(node -> getNodeDescription(node))
			.setHeader(Messages.getString("NodeView.TreeGrid.ColumnName.Description")) //$NON-NLS-1$
			.setKey("description") //$NON-NLS-1$
			;
		this.tree.addColumn((node) -> node.isApplicable() ? "\u2714" : "\u2716") // ✔ ✖ //$NON-NLS-1$ //$NON-NLS-2$
			.setHeader(Messages.getString("NodeView.TreeGrid.ColumnName.Applicable")) //$NON-NLS-1$
			.setKey("applicable") //$NON-NLS-1$`
			;

		this.tree.getColumns().forEach(column ->
		{	column
				.setAutoWidth(true)
				.setFlexGrow(0);
		});

		// connect view and model:
		this.tree.asSingleSelect().addValueChangeListener(event ->
			{
				if (event.isFromClient()) { // can also be triggered programmatically!
					@SuppressWarnings("unchecked")
					N node = (N)event.getValue(); // getValue() returns the selected Node or null if none is selected /
					log.info("Selection changed: node='{}'.", (node != null ? node.getName() : "null")); //$NON-NLS-1$ //$NON-NLS-2$
					if (this.form != null) {
						if (this.form.isVisible()) {
							if (node != null) {
								editNode(node, null);
							} else {
								closeEditor();
							}
						} else {
							log.info("form not visible"); //$NON-NLS-1$
						}
						if (this.form != null) { // editNode() may change that, so need to check again!
							this.form.setUiChanges(false);
						}
					} else {
						log.info("form is null"); //$NON-NLS-1$
					}
				}
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
			if (event.isFromClient()) recalculateColumnWidths(); // make sure the column widths are adjusted
		});
		this.tree.addCollapseListener(event -> {
			Collection<Node> nodes = event.getItems(); // getValue() returns the selected Node or null if none is selected /
			log.trace("Collapsed {} node(s): '{}'.", nodes.size(), nodes); //$NON-NLS-1$
			nodes.forEach(node -> ((IntermediateNode)node).expanded = false);
			if (event.isFromClient()) recalculateColumnWidths(); // make sure the column widths are adjusted
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

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		setNewNodeRefreshListener();
	}

	@Override
	protected void onDetach(DetachEvent detachEvent) {
		// after detaching we don't need any updates anymore:
		removeOldNodeRefreshListener();
	}

	protected PropertyChangeListener createNodeRefreshListener() {
		return new PropertyChangeListenerWithPriorUIReservation<N>(this)
		{
			@Override
			protected void handlePropertyChangeEvent(N sourceNode, String propertyName, Object oldValue, Object newValue) {
				NodeView.this.handleChangeEvent(sourceNode, propertyName, oldValue, newValue);
			}

			// as long as there are no derivatives of NodeView we can short-cut the call to handleChangeEvent:
			@Override
			protected boolean predicate(N source, String propertyName, Object oldValue, Object newValue) {
				return Objects.equals(propertyName, Node.PROPERTYNAME_STATE);
			}
			// for better log-messages:
			@Override
			public String toString() {
				return "ChangeListener_on_NodeView"; //$NON-NLS-1$
			}
		};
	}

	private static String getNodeDescription(Node node) {
		String description = node.getDescription();
		return node instanceof RootNode
			? description + " [" + ((RootNode)node).getFilePath() + "]" //$NON-NLS-1$ //$NON-NLS-2$
			: description.length() < MAX_DESCRIPTION_LENGTH ? description : description.substring(0, MAX_DESCRIPTION_LENGTH) + "..."; //$NON-NLS-1$
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
		log.info("select: {}", node); //$NON-NLS-1$
		this.tree.select(node);
		scrollTo(node);
	}

	protected static void expandPath(Node node) {
		for (IntermediateNode n = node.getParent(); n != null; n = n.getParent()) { n.setExpanded(true); }
	}

	/** scroll view such that the current node is (or becomes) visible */
	protected void scrollTo(Node node) {
		int[] indexes = calculateIndexes(node);
		if (indexes != null) this.tree.scrollToIndex(indexes);
	}

	/** counts level of current node. Root-node is one level */
	protected static int countLevels(Node node) {
		int level = 0;
		for (Node n = node; n != null; level++) { n = n.getParent(); }
		return level;
	}

	/**
	 * Calculate the indexes for a given node. See @see com.vaadin.flow.component.treegrid.TreeGrid.scrollToIndex
	 * for an explanation of indexes used for positioning.
	 */
	protected int[] calculateIndexes(Node node) {
		int nrLevels = countLevels(node);
		if (nrLevels > 1) {
			int indexes[] = new int[nrLevels];
			Node n = node;
			IntermediateNode p;
			for (int level = nrLevels-1; (p = n.getParent()) != null && level > 0; level--) {
				log.trace("{}: parent: {} ({} children) - child: {}", level, p, p.getChildren().size(), n); //$NON-NLS-1$
				indexes[level] = p.getChildren().indexOf(n);
				n = p;
			}
			indexes[0] = this.nodeService.getRootNodes().indexOf(n);
			log.debug("indexes: {}", indexes); //$NON-NLS-1$
			return indexes;
		}
		return null;
	}

	private void addNode(VisibleNodeType type) {
		log.info("add node (nodeType={})", type); //$NON-NLS-1$
		if (type == VisibleNodeType.RootNode) {
			createNewRootNode();
		} else {
			ifIntermediateNodeSelectedDo((selectedNode) ->
				{
					N newNode = createNewChildNode(type, selectedNode); // in the first step we only create a new node
					if (newNode != null) {
						// We don't hook the newly defined node into the tree, yet, but before we allow a user to
						// edit it we need to provide it with an upstream properties-set or else the user will get
						// "property not found"-errors during validation when entering a placeholder into a field:
						newNode.getProperties().setParentProperties(selectedNode.getProperties());
						editNode(newNode, selectedNode);
					}
				});
		}
	}

	private void duplicateNode() {
		ifSingleNodeSelectedDo((selectedNode) -> duplicateNode(selectedNode));
	}

	private void deleteNode() {
		ifSingleNodeSelectedDo((selectedNode) -> removeNodes(Collections.singletonList(selectedNode)));
	}

	private void duplicateNode(Node selectedNode) {
		if (selectedNode instanceof LeafNode) {
			VisibleNodeType nodeType = VisibleNodeType.valueOf(selectedNode.getClass().getSimpleName());
			log.info("duplicating node of type '{}':", nodeType); //$NON-NLS-1$
			IntermediateNode parent = selectedNode.getParent();
//			N newNode = createNewChildNode(nodeType, parent); // in the first step we only create a new node
			@SuppressWarnings("unchecked")
			N newNode = (N)NodeService.deepCloneNode(selectedNode);
			if (newNode != null) { // cloning worked ok:
				// hook the newly created node into the tree
				List<Node> siblings = parent.getChildren();
				int pos = siblings.indexOf(selectedNode) + 1;
				addNewNode(newNode, parent, pos);
				// and directly allow to edit it:
				editNode(newNode, parent);
			}
		} else {
			UsageError("Can only duplicate leaf nodes, yet."); //$NON-NLS-1$
		}
	}


	private void saveNodeAfterEditing(Node node, IntermediateNode parentNode) {
		log.debug("saveNode[SaveEvent] '{}' as child of '{}'", node.getName(), (parentNode != null ? parentNode.getName() : "-no parent-")); //$NON-NLS-1$ //$NON-NLS-2$
		if (node.getParent() == null && !(node instanceof RootNode)) { // adding a new node:
			addNewNode(node, parentNode, -1); // -1: append at end
		} else { // updating an existing node:
			updateNode(node);
		}
		// closeEditor();
	}

	private void deleteNode(NodeForm.DeleteEvent event) {
		Node node = event.getNode();
		log.debug("deleteNode[DeleteEvent] '{}'", node.getName()); //$NON-NLS-1$
		if (this.form != null && !this.form.setNode(null, true)) { // signal that we want to leave that node:
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

	private void addNewNode(Node newNode, IntermediateNode parentNode, int pos) {
		if (newNode == null) {
			log.error("newNode can not be null in addNode() - when adding child of {}", parentNode); return; //$NON-NLS-1$
		}
		if (parentNode == null) {
			log.error("parentNode can not be null in addNode() - when adding node {}", newNode.getName()); return; //$NON-NLS-1$
		}
		log.info("addNewNode '{}' to parent '{}'", newNode.getName(), parentNode.getName()); //$NON-NLS-1$
		this.nodeService.addChild(parentNode, newNode, pos);
		parentNode.expanded = true; // make sure the newly added child gets visible
		saveNode(newNode);
		updateTree();
		select(newNode); // and select it
		if (this.form != null) {
			this.form.setUiChanges(false);
		}
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

	private void ifIntermediateNodeSelectedDo(Consumer<IntermediateNode> action) {
		ifSingleNodeSelectedDo((selectedNode) ->
		{
			if (selectedNode instanceof IntermediateNode) {
				action.accept((IntermediateNode)selectedNode);
			} else {
				UsageError(Messages.getString("NodeView.ErrorMsg.OnlyForIntermediateNodes")); //$NON-NLS-1$
			}
		});
	}

	private N createNewChildNode(VisibleNodeType nodeType, IntermediateNode parentNode) {
		log.info("createNewChildNode(type={}, parentNode='{}')", nodeType, parentNode.getName()); //$NON-NLS-1$
		// this.tree.asSingleSelect().clear();
		N newNode;
		try {
			newNode = this.nodeFactory.createNode(nodeType);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException ex) {
			ExecutionError(String.format("Exception creating node of type '%s':", nodeType), ex); //$NON-NLS-1$
			return null;
		}
		return newNode;
	}

	private void updateNode(Node node) {
		log.info("updateNode '{}'", node.getName()); //$NON-NLS-1$
//		HierarchicalDataProvider<Node, SerializablePredicate<Node>> dataProvider = this.tree.getDataProvider();
// not necessary
//		node.bubbleUp(n ->
//			{
//				dataProvider.refreshItem(n);
//				saveNode(n);
//			});
		saveNode(node);
		refreshNode(node, false);
	}

	protected void refreshNode(Node node, boolean refreshChildren) {
		NodeView.this.tree.getDataProvider().refreshItem(node, refreshChildren);
		// workaround since refreshChildren==true in the above call has no effect:
		if (refreshChildren && node instanceof IntermediateNode) {
			// does not work :-(
			((IntermediateNode)node).getChildren().forEach(n -> refreshNode(n, refreshChildren));
			// NodeView.this.tree.getDataProvider().refreshAll();
		}
	}

	@SuppressWarnings("unchecked")
	public void editNode(N node, IntermediateNode parentNode) {
		log.info("editNode '{}' (parent='{}')", node.getName(), (parentNode != null ? parentNode.getName() : "null")); //$NON-NLS-1$ //$NON-NLS-2$
		if (this.form != null) { // form displayed?
			log.info("form for '{}' has changes: {}", node.getName(), this.form.hasUiChanges()); //$NON-NLS-1$
			N currentNode = this.form.getNode();
			if (!this.form.setNode(null, false)) { // signal that we want to leave that node:
				select(currentNode); // if there are unsaved changes remain on form's current node
				return;
			}
			this.form.clearListeners();
			removeClassNames(ViewClassEditing, ViewClassViewing);
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

			if (newForm != this.form) { // new or different form assigned:
				log.info("editNode: new form {}", newForm); //$NON-NLS-1$
				this.form = newForm;
				this.formWrapper.getChildren().forEach((child) -> child.getElement().removeFromTree());
				this.formWrapper.removeAll();
				this.formWrapper.add(newForm);
				this.formWrapper.setSizeFull();
				log.info("editNode: setting splitter to 50:"); //$NON-NLS-1$
				this.content.setSplitterPosition(50);
			}
			if (SecurityUtils.isAdminUser()) {
				this.form.addListener(NodeForm.SaveEvent.class, (event) -> saveNodeAfterEditing(event.getNode(), parentNode));
				this.form.addListener(NodeForm.DeleteEvent.class, this::deleteNode);
				this.form.addListener(NodeForm.CloseEvent.class, (event) -> closeEditor());
				this.form.addListener(NodeForm.UpdateEvent.class, (event) -> updateNode(event.getNode()));
				addClassName(ViewClassEditing);
			} else {
				addClassName(ViewClassViewing);
			}
			this.form.setNode(node, true);
			this.form.setVisible(true);
		} catch (java.lang.IllegalStateException ex) { // I keep getting these - still no idea why...
			log.error("WTF?", ex); //$NON-NLS-1$
			this.formFactory = new FormFactory<N>();
			this.formWrapper.removeAll(); // remove old form
			this.form = null;
		}
	}

	private void closeEditor() {
		log.info("NodeView.closeEditor"); //$NON-NLS-1$
		if (this.form.setNode(null, false)) {
			removeClassName("editing"); //$NON-NLS-1$
			this.formWrapper.setSizeUndefined();
			log.info("closeEditor: setting splitter to 100:"); //$NON-NLS-1$
			this.content.setSplitterPosition(100);
			this.form.setVisible(false);
			this.form = null;
		}
	}

	/**
	 * This removes nodes or subtrees
	 * @param nodes
	 */
	private void removeNodes(Collection<Node> nodes) {
		List<String> nodeNames = nodes.stream().map(n -> n.getName()).collect(Collectors.toList());
		log.info("removeNodes {}.", nodeNames); //$NON-NLS-1$
		String question = nodeNames.stream()
			.collect(Collectors.joining(Messages.getString("NodeView.Deletion.Question.ListSeparator"), //$NON-NLS-1$
			                            Messages.getString("NodeView.Deletion.Question.Prefix"), //$NON-NLS-1$
			                            Messages.getString("NodeView.Deletion.Question.Suffix"))); //$NON-NLS-1$
		new ConfirmDialog(Messages.getString("NodeView.Deletion.Header"), question, //$NON-NLS-1$
		                  Messages.getString("NodeView.Deletion.Confirm"), evt -> { log.info("delete '{}':", nodeNames); deleteNodes(nodes); }, //$NON-NLS-1$ //$NON-NLS-2$
		                  Messages.getString("NodeView.Deletion.Cancel"), evt -> { log.info("cancel '{}':", nodeNames); } //$NON-NLS-1$ //$NON-NLS-2$
		                 ).open();
	}

//	/**
//	 * This removes entire (root)lists - which is not yet really supported...
//	 * @param nodes
//	 */
//	private void removeList(Collection<N> nodes) {
//		List<String> nodeNames = nodes.stream().map(n -> NodeService.getCorrespondingRootNode(node).getName()).collect(Collectors.toList());
//		log.info("removeList {}.", nodeNames); //$NON-NLS-1$
//		String question = nodeNames.stream()
//			.collect(Collectors.joining(Messages.getString("NodeView.Deletion.Question.ListSeparator"), //$NON-NLS-1$
//			                            Messages.getString("NodeView.Deletion.Question.Prefix"), //$NON-NLS-1$
//			                            Messages.getString("NodeView.Deletion.Question.Suffix"))); //$NON-NLS-1$
//		new ConfirmDialog(Messages.getString("NodeView.Deletion.Header"), question, //$NON-NLS-1$
//		                  Messages.getString("NodeView.Deletion.Confirm"), evt -> nodes.forEach(node -> deleteNodes(NodeService.getCorrespondingRootNode(node).getChildren())), //$NON-NLS-1$
//		                  Messages.getString("NodeView.Deletion.Cancel"), evt -> {/**/}); //$NON-NLS-1$
//	}

	private void saveNode(Node node) {
		if (NodeView.alwaysSave) {
			this.nodeService.save(node);
		} else {
			NodeView.nodeView.highlightSaveButton(true);
		}
	}

	private boolean saveAllRootNodes() {
		return this.nodeService.saveAllRootNodes();
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
				saveNode(node);
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
				saveNode(node);
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
				saveNode(node);
				updateTree(); // redraw the modified tree
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
					saveNode(node);
					expandPath(node); // the target parent may be collapsed.
					updateTree(); // redraw the modified tree
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
//			reportReadException(fileName, t);
//		}
//	}
//
	private void reportUploadResult(RootNode rootNode, String sourceName) {
		log.info("Reading of file '{}' yielded tree below: '{}'", sourceName, rootNode != null ? rootNode.getName() : null); //$NON-NLS-1$
		if (rootNode != null) { // new root was read: update UI
			updateTree();
		} else {
			String msg = String.format(Messages.getString("NodeView.ErrorMsg.IllegalFileName"), sourceName); //$NON-NLS-1$
			log.info(msg);
			NodeService.createNotification(msg);
		}
	}

	private void updateTree() {
		log.info("updateTree"); //$NON-NLS-1$
		removeOldNodeRefreshListener();
		log.trace("data-provider: {}", this.tree.getDataProvider()); //$NON-NLS-1$
//		if (this.tree.getDataProvider() == null) {
			this.tree.setDataProvider(createDataProvider());
			setNewNodeRefreshListener();
//		}
		// expand/collapse tree to previously saved state:
		getRootNodesAsBareNodes().forEach(node -> adjustCollapseExpanded(node));
		recalculateColumnWidths(); // make sure the column widths are adjusted
		// scroll to currently selected item...
	}

	protected void setNewNodeRefreshListener() {
		if (this.nodeRefreshListener == null) {
			this.nodeRefreshListener = createNodeRefreshListener(); // make sure we have one...
		}
		// register our refresh-listener with each (new)root-node:
		this.nodeService.getRootNodes().forEach((node) -> node.addChangeListener(this.nodeRefreshListener));
	}

	protected void removeOldNodeRefreshListener() {
		if (this.nodeRefreshListener != null) {
			this.nodeService.getRootNodes().forEach((node) -> node.removeChangeListener(this.nodeRefreshListener));
			this.nodeRefreshListener = null;
		}
	}

	private void recalculateColumnWidths() {
		recalculateColumnWidths(250);
	}
	private void recalculateColumnWidths(long delay) {
		// original
		// this.tree.recalculateColumnWidths(); // make sure the column widths are adjusted
		// with 1/4 sec delay we finally saw some improvement:
		UIHandlerSupport.executeLater(this, delay, () -> this.tree.recalculateColumnWidths());
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
	private void reportFileReadException(String sourceName, Throwable t) {
		String hdr = "Exception:"; //$NON-NLS-1$
		String msg = String.format(Messages.getString("NodeView.ErrorMsg.ExceptionReading"), sourceName, t); //$NON-NLS-1$
		log.info(hdr + ": " + msg, t); //$NON-NLS-1$
		openDialog(hdr, msg, t);
	}

	void highlightSaveButton(boolean value) {
		if (value) {
			this.saveAllButton.addClassName(ViewClassHighlight);
		} else {
			this.saveAllButton.removeClassName(ViewClassHighlight);
		}
	}

	/**
	 * The method could handle multiple nodes, but we currently allow selection of one node only
	 * @param nodes
	 */
	private void deleteNodes(Collection<Node> nodes) {
		List<String> nodeNames = nodes.stream().map(n -> n.getName()).collect(Collectors.toList());
		log.info("deleteNodes {}", nodeNames); //$NON-NLS-1$
		List<IntermediateNode> parents = new ArrayList<>();
		try {
			nodes.forEach((node) -> {
				log.info("deleting nodes '{}'", node.getName()); //$NON-NLS-1$
				if (node instanceof RootNode) {
					this.nodeService.removeRootNode((RootNode)node);
				} else {
					IntermediateNode parent = node.getParent();
					Node deletedNode = this.nodeService.removeChild(parent, node);
					log.info("deleted node '{}'", deletedNode.getName()); //$NON-NLS-1$
					saveNode(parent);
					parents.add(parent);
				}
			});
		} catch (Throwable t) {
			String msg = String.format("Exception deleting node(s) '%s': %s", nodeNames, t); //$NON-NLS-1$
			log.error(msg, t);
			NodeService.createNotification(msg);
		}
		updateTree();
		// do we want to select (all) parent node(s) after a deletion:
		// parents.forEach((node) -> this.tree.select(node));
		// or rather:
		this.tree.select(null);
		if (this.form != null) {
			this.form.setNode(null, true);
		}
	}

	/* User operation error - this CAN happen but does no harm */
	public static void UsageError(String msg) {
		String hdr =  Messages.getString("NodeView.Notification.UsageError"); //$NON-NLS-1$
		log.info(hdr + ": " + msg); //$NON-NLS-1$
		openDialog(hdr, msg, null);
	}

	public static void ConfigError(String msg, Throwable t) {
		String hdr = Messages.getString("NodeView.Notification.ConfigError"); //$NON-NLS-1$
		log.warn(hdr + ": " + msg, t); //$NON-NLS-1$
		openDialog(hdr, msg, t);
	}

	public static void ExecutionError(String msg, Throwable t) {
		String hdr = Messages.getString("NodeView.Notification.ExecutionError"); //$NON-NLS-1$
		log.warn(hdr + ": " + msg, t); //$NON-NLS-1$
		openDialog(hdr, msg, t);
	}

	/* This should of course never happen... ;-) */
	public static void ProgrammingError(String msg, Throwable t) {
		String hdr = Messages.getString("NodeView.Notification.ProgrammingError"); //$NON-NLS-1$
		log.error(hdr + ": " + msg, t); //$NON-NLS-1$
		openDialog(hdr, msg, t);
	}

	private static void openDialog(String header, String message, Throwable t) {
		UI ui = UI.getCurrent();
		ui.access(() ->
		{
			new ConfirmDialog(header, message + "\n" + ExceptionUtils.exceptionCauseSummary(t),  //$NON-NLS-1$
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
				NativeLabel label = new NativeLabel(text);
				label.setClassName("label-" + classSuffix); //$NON-NLS-1$
				add(label);
			}
		}
	}
}