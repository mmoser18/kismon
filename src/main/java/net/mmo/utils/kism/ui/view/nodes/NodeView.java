package net.mmo.utils.kism.ui.view.nodes;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
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

	// This gets autowired but still puzzled which annotation causes this...
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
		addNodeButton.addClickListener(click -> addNewNode());

		Button delNodeButton = new Button(Messages.getString("NodeView.Button.DeleteNode.Label")); //$NON-NLS-1$
		delNodeButton.addClickListener(click ->
			{
				Set<Node> nodes = this.tree.getSelectedItems();
				if (nodes.isEmpty()) {
					/// TODO: AlertUser("Select nodes to be deleted first!");
				} else {
					deleteNodes(nodes);
				}
			});

		Button clearListButton = new Button(Messages.getString("NodeView.Button.ClearList.Label")); //$NON-NLS-1$
		clearListButton.addClickListener(click ->
			{
				Set<Node> nodes = this.tree.getSelectedItems();
				if (nodes.isEmpty()) {
					/// TODO: AlertUser("Select nodes to be deleted first!");
				} else {
					clearNodesList(nodes);
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

		HorizontalLayout toolbar = new HorizontalLayout(/*this.filterText,*/ addNodeButton, delNodeButton, clearListButton, directionButton);
		toolbar.addClassName(ViewToolbarClassName);

		return toolbar;
	}

	private void configureTree() {
		NodeService.addSomeSubNodes(); // test only

		log.trace("Configuring TreeGrid:"); //$NON-NLS-1$
		TreeData<Node> treeData = new TreeData<>();
		// This Java restriction that one can not pass a Collection<subclass> as Collection<superclass> is soooo brain-damaged!!!  ||-(
		treeData.addItems(NodeService.getRootNodesAsBareNodes(), node -> {
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
		this.tree = new TreeGrid<>(new TreeDataProvider<>(treeData)); // package visible for UTs - is there no other way?
		this.tree.addClassName(TreeClassName);
		this.tree.setSizeFull();
		this.tree.addHierarchyColumn(Node::getName).setHeader(Messages.getString("NodeView.TreeGrid.ColumnName.Name")); //$NON-NLS-1$
		this.tree.addColumn(Node::getDescription).setHeader(Messages.getString("NodeView.TreeGrid.ColumnName.Description")); //$NON-NLS-1$
		this.tree.addColumn(Node::getState      ).setHeader(Messages.getString("NodeView.TreeGrid.ColumnName.State")); //$NON-NLS-1$
		this.tree.addColumn(Node::isEnabled     ).setHeader(Messages.getString("NodeView.TreeGrid.ColumnName.Enabled")); //$NON-NLS-1$
		this.tree.addColumn(Node::isIgnored     ).setHeader(Messages.getString("NodeView.TreeGrid.ColumnName.Ignored")); //$NON-NLS-1$
		this.tree.expandRecursively(NodeService.getRootNodesAsBareNodes(), 3);
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
		if (node.getParent() == null) { // adding a new node:
			addNewNode(node);
		} else { // updating an existing node:
			updateNode(node);
		}
		// closeEditor();
	}

	private void deleteNode(NodeForm.DeleteEvent event) {
		deleteNode(event.getNode());
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
		this.nodeService.save(node);

		this.tree.getDataProvider().refreshAll();
	}

	private void updateNode(Node node) {
		log.info("updateNode {}", node); //$NON-NLS-1$
		this.nodeService.save(node);
		this.tree.getDataProvider().refreshItem(node);
	}

	private void deleteNode(Node node) {
		log.info("deleteNode {}", node); //$NON-NLS-1$
		this.nodeService.delete(Collections.singletonList(node));
		this.tree.getDataProvider().refreshAll();
	}

	private void deleteNodes(Collection<Node> nodes) {
		log.info("deleteNodes {}", nodes); //$NON-NLS-1$
		this.nodeService.delete(nodes);
		this.tree.getDataProvider().refreshAll();
	}

	private void clearNodesList(Collection<Node> nodes) {
		log.info("clearNodesList"); //$NON-NLS-1$
		this.nodeService.deleteAll(nodes);
	}
}
