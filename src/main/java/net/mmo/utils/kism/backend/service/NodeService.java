
package net.mmo.utils.kism.backend.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.repositories.IntermediateNodeRepository;
import net.mmo.utils.kism.backend.repositories.LeafNodeRepository;
import net.mmo.utils.kism.entities.IntermediateNode;
import net.mmo.utils.kism.entities.Node;
import net.mmo.utils.kism.entities.RootNode;
import org.springframework.stereotype.Service;

/**
 * Implements the service to access and store Companies
 */
@Service
@Slf4j
public class NodeService
{
	public final static String CONFIG_FILE_TYPE  = "kmc"; //$NON-NLS-1$
	public final static String DEFAULT_FILE_NAME = "default"; //$NON-NLS-1$

	@SuppressWarnings("unused")
	private IntermediateNodeRepository intermediateNodeRepository;
	@SuppressWarnings("unused")
	private LeafNodeRepository leafNodeRepository;

	private List<RootNode> roots = new ArrayList<>();
	private JavaJSONHandler jsonHandler = new JavaJSONHandler();

	public NodeService(IntermediateNodeRepository intermediateNodeRepository,
	                   LeafNodeRepository leafNodeRepository) {
		this.intermediateNodeRepository = intermediateNodeRepository;
		this.leafNodeRepository = leafNodeRepository;
	}

	/* create a new empty RootNode, if none exists */
	void init() {
		RootNode rootNode = null;
		File file = new File(DEFAULT_FILE_NAME + '.' + CONFIG_FILE_TYPE);
		try {
			rootNode = this.jsonHandler.read(file);
			log.info("Node '{}' read from file '{}'.", rootNode, file); //$NON-NLS-1$
		} catch (IOException t) {
			log.warn("File '{}' not found or not a valid configuration (reason: '{}').", file, t); //$NON-NLS-1$
		} catch (Throwable t) {
			log.error("File '{}' not found or not a valid configuration (reason: '{}').", file, t); //$NON-NLS-1$
			t.printStackTrace();
		}
		if (rootNode == null) {
			log.warn("Creating dummy root node: {}"); //$NON-NLS-1$
			rootNode = new RootNode(Messages.getString("km_root_name"), //$NON-NLS-1$
			                        Messages.getString("km_root_description"), //$NON-NLS-1$
			                        Node.State.OK);
			addSomeSubNodes(rootNode);
			log.info("Node '{}' created.", rootNode); //$NON-NLS-1$
		}
		this.roots.add(rootNode);
	}

	@SuppressWarnings({"nls"})
	private static void addSomeSubNodes(IntermediateNode parent) {
		int max = (parent.getParent() == null ? 2 : (int)(Math.random() * 3.0)); // maps [0.0 - 1.0) --> [0..2]
		if (parent.getName().length() >= Messages.getString("km_root_name").length() + 6) max = 0; // limit depth...
		log.info("adding {} children to {}:", max, parent);

		for (int i = 0; i < max; i++) {
			int idx = (int)(Math.random() * 3.0); // maps [0.0 - 1.0) --> [0..2]
			Node.State newState = Node.State.values()[idx];

			addSomeSubNodes(new IntermediateNode(parent.getName() + "." + (i+1),
			                                     parent.getDescription() + "." + (i+1),
			                                     newState,
			                                     parent));
		}
		log.info("{} children added to {}.", (parent.getChildren() != null ? parent.getChildren().size() : 0), parent);
	}

	/** enables re-initialization of root nodes for Unit-Tests - not normally used */
	public void reset() {
		this.roots.clear();
	}

	public List<RootNode> getRootNodes() {
		if (this.roots.isEmpty()) {
			init();
		}
		return this.roots;
	}

	public static RootNode getRootNode(Node startNode) {
		Node node = startNode;
		while (node.getParent() != null) node = node.getParent();
		return (RootNode)node;
	}

	public RootNode read(File file) throws IOException {
		RootNode newNode = this.jsonHandler.read(file);
		restoreParents(newNode);
		getRootNodes().add(newNode);
		return newNode;
	}

	public void saveAs(RootNode rootNode, File file) throws IOException {
		log.info("saving rootNode {} to file '{}':", rootNode.getName(), file); //$NON-NLS-1$
		this.jsonHandler.save(rootNode, file);
	}

//	public void delete(Collection<Node> nodes, boolean liftUp) {
//		nodes.forEach(node -> {
//			IntermediateNode parent = node.getParent();
//			if (parent != null) { // we only delete non-root nodes:
//				ArrayList<Node> parentSiblings = parent.getChildren();
//				int index = parentSiblings.indexOf(node); // get the node's index:
//				node.setParent(null); // unhinge the node
//				parentSiblings.remove(index);
//				if (liftUp && node instanceof IntermediateNode) { // make all children a child of the parent-node:
//					ArrayList<Node> nodeSiblings = ((IntermediateNode)node).getChildren();
//					for (Node s: nodeSiblings) {
//						parentSiblings.set(index++, s);
//					}
//				}
//			}
//		});
//	}
//
//	// delete nodes, make all children a child of the parent-node:
//	public void delete(Collection<Node> nodes) {
//		delete(nodes, true);
//	}
//
//	// make all children a child of the parent-node:
//	public void deleteAll(Collection<Node> nodes) {
//		delete(nodes, false);
//	}

//	/** utility to set all parents null (to avoid infinite recursion in some serialization library). */
//	public void setParentNull(Node node) {
//		node.setParent(null);
//		if (node instanceof IntermediateNode) {
//			IntermediateNode parent = (IntermediateNode)node;
//			if (parent.getChildren() != null) {
//				parent.getChildren().forEach(child -> setParentNull(child));
//			}
//		}
//	}

	/** utility to restore parents, e.g. after reading from a file */
	public void restoreParents(IntermediateNode parent) {
		if (parent.getChildren() != null) {
			parent.getChildren().forEach(child ->
				{
					child.setParent(parent);
					if (child instanceof IntermediateNode) restoreParents((IntermediateNode)child);
				});
		}
	}

//	public List<Node> findAll() {
//		return this.nodeRepository.findAll();
//	}
//
//	public List<Node> findAll(String stringFilter) {
//		if (stringFilter == null || stringFilter.isEmpty()) {
//			return this.nodeRepository.findAll();
//		} else {
//			return this.nodeRepository.search(stringFilter);
//		}
//	}

	static class JavaJSONHandler
	{
		ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

		public void save(RootNode rootNode, File file) throws IOException {
			synchronized(rootNode) {
				log.info("saving rootNode '{}' to file '{}':", rootNode.getName(), file); //$NON-NLS-1$
				this.mapper.writeValue(file, rootNode);
				log.info("rootNode '{}' was saved to file '{}'.", rootNode.getName(), file); //$NON-NLS-1$
			}
		}

		public RootNode read(File file) throws IOException {
			log.info("JavaJSONHandler: reading from file {}:", file); //$NON-NLS-1$
			RootNode rootNode = this.mapper.readValue(file, RootNode.class);
			rootNode.setFilePath(file.getAbsolutePath().replace('.' + CONFIG_FILE_TYPE, "")); //$NON-NLS-1$
			log.info("rootNode '{}' was read from file '{}'.", rootNode.getName(), file); //$NON-NLS-1$
			return rootNode;
		}
	}
}
