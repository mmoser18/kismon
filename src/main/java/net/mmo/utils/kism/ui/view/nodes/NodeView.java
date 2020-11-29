package net.mmo.utils.kism.ui.view.nodes;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.IntermediateNode;
import net.mmo.utils.kism.entities.Node;
import net.mmo.utils.kism.entities.RootNode;
import net.mmo.utils.kism.ui.CommonConstants;
import net.mmo.utils.kism.ui.CommonConstants.LayoutDirection;
import net.mmo.utils.kism.ui.MainLayout;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

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
public class NodeView extends VerticalLayout
{
	private static final long serialVersionUID = -352635233754729967L;

	// public to allow access by test classes
	public static final String ViewClassName = "node-view";  //$NON-NLS-1$
	public static final String ViewToolbarClassName = "node-view-toolbar";  //$NON-NLS-1$
	public static final String ViewContentClassName = "node-view-content";  //$NON-NLS-1$
	public static final String TreeClassName        = "node-view-tree"; //$NON-NLS-1$

	private NodeService nodeService;

	TreeGrid<Node> tree; // package visible for UTs - is there no other way?
	NodeForm form; // package visible for ITs - is there no other way?
	Div content;
	LayoutDirection contentDirection = LayoutDirection.direction_row;

	// This gets autowired but I am still puzzled which annotation causes this...
	public NodeView(NodeService nodeService) {
		log.debug("creating {}:", this.getClassName()); //$NON-NLS-1$
		try {
			this.nodeService = nodeService;

			addClassName(ViewClassName);
			setSizeFull(); // use entire browser window

			configureTree();
			configureNodeForm();

			log.trace("Creating content:"); //$NON-NLS-1$
			this.content = new Div(this.tree, this.form);
			this.content.addClassName(ViewContentClassName);
			this.content.setSizeFull();
			CommonConstants.setClassName(this.content, this.contentDirection);

			add(getToolbar(), this.content);
		} catch (Throwable t) {
			log.error("Exception in c'tor:", t); //$NON-NLS-1$
		}
		log.debug("{} complete.", this.getClassName()); //$NON-NLS-1$
	}

	private HorizontalLayout getToolbar() {
		log.trace("Creating toolbar:"); //$NON-NLS-1$
//		this.filterText.setClassName(FilterFieldClassName);
//		this.filterText.setPlaceholder(Messages.getString("ListView.TextField.FilterText.Label")); //$NON-NLS-1$
//		this.filterText.setClearButtonVisible(true);
//		this.filterText.setValueChangeMode(ValueChangeMode.LAZY);
//		this.filterText.addValueChangeListener(e -> updateList());

		Button addNodeButton = new Button(Messages.getString("NodeView.Button.AddNode.Label")); //$NON-NLS-1$
		addNodeButton.addClickListener(click ->
			{
				addNewNode();
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

		Button readFileButton = new Button(String.format(Messages.getString("NodeView.Button.ReadFile.Label"), LayoutDirection.invert(this.contentDirection).toString())); //$NON-NLS-1$
		readFileButton.addClickListener(click ->
			{
				log.info("readFileButton clicked"); //$NON-NLS-1$
				readFile();
			});

		HorizontalLayout toolbar = new HorizontalLayout(/*this.filterText,*/ addNodeButton, delNodeButton, clearListButton, directionButton, readFileButton);
		toolbar.addClassName(ViewToolbarClassName);

		return toolbar;
	}

	private void configureTree() {

		log.trace("Configuring TreeGrid:"); //$NON-NLS-1$
		// This Java restriction that one can not pass a Collection<subclass> as Collection<superclass> is soooo brain-damaged!!!  ||-(
		this.tree = new TreeGrid<>(createDataProvider());
		this.tree.addClassName(TreeClassName);
		this.tree.setSizeFull();
		this.tree.addHierarchyColumn(Node::getName).setHeader(Messages.getString("NodeView.TreeGrid.ColumnName.Name")); //$NON-NLS-1$
		this.tree.addColumn(Node::getDescription).setHeader(Messages.getString("NodeView.TreeGrid.ColumnName.Description")); //$NON-NLS-1$
		this.tree.addColumn(Node::getState      ).setHeader(Messages.getString("NodeView.TreeGrid.ColumnName.State")); //$NON-NLS-1$
		this.tree.addColumn(Node::isEnabled     ).setHeader(Messages.getString("NodeView.TreeGrid.ColumnName.Enabled")); //$NON-NLS-1$
		this.tree.addColumn(Node::isActive      ).setHeader(Messages.getString("NodeView.TreeGrid.ColumnName.Active")); //$NON-NLS-1$
		this.tree.expandRecursively(getRootNodesAsBareNodes(), 3);
		this.tree.getColumns().forEach(col -> col.setAutoWidth(true));

		// connect view and model:
		this.tree.asSingleSelect().addValueChangeListener(event -> {
			Node node = event.getValue(); // getValue() returns the selected Node or null if none is selected /
			log.info("Selected node '{}'.", node); //$NON-NLS-1$
			// action:
			editNode(node, Messages.getString("NodeView.Form.EditOrDeleteNode.Title")); //$NON-NLS-1$
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
				if (children != null) {
					log.trace("ValueProvider({}) -> {}", node.getName(), children.stream().map(n -> n.getName()).collect(Collectors.toList())); //$NON-NLS-1$
					return children;
				}
			}
			log.trace("ValueProvider({}) -> <empty-list>", node.getName()); //$NON-NLS-1$
			return Collections.emptyList();
		});
		return new TreeDataProvider<>(treeData);
	}

	// This Java restriction that one can not pass a Collection<subclass> as Collection<superclass> is soooo brain-damaged!!!
	public Collection<Node> getRootNodesAsBareNodes() {
		List<RootNode> rootNodes = this.nodeService.getRootNodes();
		return Arrays.asList(rootNodes.toArray(new Node[rootNodes.size()]));
	}

	void configureNodeForm() {
		this.form = new NodeForm();
		this.form.addListener(NodeForm.SaveEvent.class, this::saveNode);
		this.form.addListener(NodeForm.DeleteEvent.class, this::deleteNode);
		this.form.addListener(NodeForm.CloseEvent.class, e -> closeEditor());
		this.form.setVisible(false); // initially the NodeForm is hidden
	}

	void addNewNode() {
		log.info("addNewNode"); //$NON-NLS-1$
		this.tree.asSingleSelect().clear();
		editNode(new IntermediateNode(Messages.getString("NodeView.Form.NewNode.Name"), //$NON-NLS-1$
		                              Messages.getString("NodeView.Form.NewNode.Name"), //$NON-NLS-1$
		                              Node.State.OK,
		                              null), // the null also signals that this is a NEW node!
		         Messages.getString("NodeView.Form.NewNode.Title")); //$NON-NLS-1$
	}

	private void saveNode(NodeForm.SaveEvent event) {
		Node node = event.getNode();
		if (node.getParent() == null && !(node instanceof RootNode)) { // adding a new node:
			addNewNode(node);
		} else { // updating an existing node:
			updateNode(node);
		}
		// closeEditor();
	}

	private void deleteNode(NodeForm.DeleteEvent event) {
		deleteNodes(Collections.singletonList(event.getNode()));
		closeEditor();
	}

	public void editNode(Node node, String header) {
		log.info("editNode {}", node); //$NON-NLS-1$
		if (node == null) {
			closeEditor();
		} else {
			this.form.setHeader(header);
			this.form.setNode(node);
			this.form.setVisible(true);
			addClassName("editing"); //$NON-NLS-1$
		}
	}

	private void closeEditor() {
		log.info("closeEditor"); //$NON-NLS-1$
		this.form.setNode(null);
		this.form.setVisible(false);
		removeClassName("editing"); //$NON-NLS-1$
	}

	private void addNewNode(Node node) {
		log.info("addNewNode {}", node); //$NON-NLS-1$
		// TODO: implement insertion
		saveNode(node);
		this.tree.setDataProvider(createDataProvider());
		this.tree.expandRecursively(getRootNodesAsBareNodes(), 3);
		// this.tree.getDataProvider().refreshAll();
	}

	private void updateNode(Node node) {
		log.info("updateNode {}", node); //$NON-NLS-1$
		saveNode(node);
		this.tree.getDataProvider().refreshItem(node);
	}

	private void saveNode(Node node) {
		try {
			RootNode rootNode = NodeService.getRootNode(node);
			String fileName = rootNode.getFilePath();
			if (fileName == null) fileName = NodeService.DEFAULT_FILE_NAME;
			// TODO : provide some file selection box...
			File file = new File(fileName + '.' + NodeService.CONFIG_FILE_TYPE);
			if (file.canWrite()) {
				this.nodeService.saveAs(rootNode, file);
				rootNode.setFilePath(fileName);
			} else {
				String msg = String.format("File '%s' is not writable!", file); //$NON-NLS-1$
				log.info(msg);
				new Notification(msg, 0).open();
			}
		} catch (Throwable t) {
			String msg = String.format("Exception saving node '%s': %s", node, t); //$NON-NLS-1$
			log.error(msg, t);
			new Notification(msg, 0).open();
		}
	}

	@SuppressWarnings("null")
	private void readFile() {
		String fileName = null; // TODO : provide some file selection box...
		if (fileName == null) fileName = NodeService.DEFAULT_FILE_NAME;
		try {
			File file = new File(fileName + '.' + NodeService.CONFIG_FILE_TYPE);
			if (file.canRead()) {
				RootNode rootNode = this.nodeService.read(file);
				rootNode.setFilePath(fileName);
				this.tree.setDataProvider(createDataProvider());
				this.tree.expandRecursively(getRootNodesAsBareNodes(), 3);
				// this.tree.getDataProvider().refreshAll();
			} else {
				String msg = String.format("File '%s' does not exist or is not readable.", file); //$NON-NLS-1$
				log.info(msg);
				new Notification(msg, 0).open();
			}
		} catch (Throwable t) {
			String msg = String.format("Exception reading from file '%s': %s", fileName, t); //$NON-NLS-1$
			log.error(msg, t);
			new Notification(msg, 0).open();
		}
	}

	@SuppressWarnings("unused")
	private void removeNodes(Collection<Node> nodes) {
		log.info("removeNodes {}.", nodes); //$NON-NLS-1$
		String question = nodes.stream()
			.map(node -> node.getName())
			.collect(Collectors.joining(", ", "Are you sure you want to delete node(s) '", "'?")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		new ConfirmDialog("Confirm deletion", question, //$NON-NLS-1$
		                  "Delete", evt -> deleteNodes(nodes), //$NON-NLS-1$
		                  "Cancel", evt -> {/**/}); //$NON-NLS-1$
	}

	@SuppressWarnings("unused")
	private void removeList(Collection<Node> nodes) {
		log.info("removeList {}.", nodes); //$NON-NLS-1$
		String question = nodes.stream()
			.map(node -> NodeService.getRootNode(node).getName())
			.collect(Collectors.joining(", ", "Are you sure you want to delete ENTIRE list(s) '", "'?")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		new ConfirmDialog("Confirm deletion", question, //$NON-NLS-1$
		                  "Delete", evt -> nodes.forEach(node -> deleteNodes(NodeService.getRootNode(node).getChildren())), //$NON-NLS-1$
		                  "Cancel", evt -> {/**/}); //$NON-NLS-1$
	}

	private void deleteNodes(Collection<Node> nodes) {
		log.info("deleteNodes {}", nodes); //$NON-NLS-1$
		try {
			this.nodeService.delete(nodes);
		} catch (Throwable t) {
			String msg = String.format("Exception deleting node '%s': %s", nodes, t); //$NON-NLS-1$
			log.error(msg, t);
			new Notification(msg, 0).open();
		}
		this.tree.getDataProvider().refreshAll();
	}
}