
package net.mmo.utils.kism.backend.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.IntermediateNode;
import net.mmo.utils.kism.entities.Node;
import net.mmo.utils.kism.entities.RootNode;
import net.mmo.utils.kism.utils.NodeProperties;
import net.mmo.utils.kism.utils.PrettyPrint;
import org.springframework.stereotype.Service;

/**
 * Implements the service to access and store Nodes
 */
@Service
@Slf4j
public class NodeService
{
	public final static String CONFIG_FILE_TYPE  = "kmc"; //$NON-NLS-1$
	public final static String DEFAULT_FILE_NAME = "default"; //$NON-NLS-1$

	/*
	@SuppressWarnings("unused")
	private IntermediateNodeRepository intermediateNodeRepository;
	@SuppressWarnings("unused")
	private LeafNodeRepository leafNodeRepository;
	*/

	private List<RootNode> roots = new ArrayList<>();
	private ObjectMapper mapper;

	private Properties commonRootProperties = new NodeProperties();
	{
		this.commonRootProperties.putAll(System.getenv()); // per default all Environment variables
	}

	public NodeService(// IntermediateNodeRepository intermediateNodeRepository,
	                   // LeafNodeRepository leafNodeRepository
	                  ) {
		this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
		/*
		this.intermediateNodeRepository = intermediateNodeRepository;
		this.leafNodeRepository = leafNodeRepository;
		*/
		if (getRootNodes().isEmpty()) {
			init(); // load from file
		}

	}

	private void addNewRootNode(RootNode newRootNode) {
		newRootNode.getProperties().setParentProperties(this.commonRootProperties);
		getRootNodes().add(newRootNode);
	}

	public void addSharedProperty(Object key, Object value) {
		this.commonRootProperties.put(key, value);
	}
	public void removeSharedProperty(Object key) {
		this.commonRootProperties.remove(key);
	}

	/* create a new empty RootNode, if none exists */
	public void init() {
		RootNode rootNode = null;
		File file = new File(DEFAULT_FILE_NAME + '.' + CONFIG_FILE_TYPE);
		try {
			rootNode = read(file);
			log.info("Node '{}' read from file '{}'.", rootNode, file); //$NON-NLS-1$
		} catch (IOException t) {
			log.warn("File '{}' not found or not a valid configuration (reason: '{}').", file, t); //$NON-NLS-1$
		} catch (Throwable t) {
			log.error("Exception processing file '" + file + ":", t); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (getRootNodes().isEmpty()) { // make sure we always have at least one root node:
			createDummyRootNode();
		}
		log.info("Root nodes: {}", getRootNodes()); //$NON-NLS-1$
	}

	public void createDummyRootNode() {
		log.info("Creating dummy root node: {}"); //$NON-NLS-1$
		RootNode rootNode = new RootNode(Messages.getString("km_root_name"), //$NON-NLS-1$
		                                 Messages.getString("km_root_description")); //$NON-NLS-1$
		// addSomeSubNodes(rootNode);
		log.info("Node '{}' created.", rootNode); //$NON-NLS-1$
		addNewRootNode(rootNode);
	}

//	@SuppressWarnings({})
//	private static void addSomeSubNodes(IntermediateNode parent) {
//		int max = (parent.getParent() == null ? 2 : (int)(Math.random() * 3.0)); // maps [0.0 - 1.0) --> [0..2]
//		if (parent.getName().length() >= Messages.getString("km_root_name").length() + 6) max = 0; // limit depth... //$NON-NLS-1$
//		log.info("adding {} children to {}:", max, parent); //$NON-NLS-1$
//
//		for (int i = 0; i < max; i++) {
//			int idx = (int)(Math.random() * 3.0); // maps [0.0 - 1.0) --> [0..2]
//			Node.State newState = Node.State.values()[idx];
//
//			addSomeSubNodes(new IntermediateNode(parent.getName() + "." + (i+1), //$NON-NLS-1$
//			                                     parent.getDescription() + "." + (i+1), //$NON-NLS-1$
//			                                     newState,
//			                                     parent));
//		}
//		log.info("{} children added to {}.", (parent.getChildren() != null ? parent.getChildren().size() : 0), parent); //$NON-NLS-1$
//	}

	/** enables re-initialization of root nodes for Unit-Tests - not normally used */
	public void reset() {
		getRootNodes().clear();
		createDummyRootNode();
	}

	public List<RootNode> getRootNodes() {
		return this.roots;
	}

	public static RootNode getCorrespondingRootNode(Node startNode) {
		Node node = startNode;
		while (node.getParent() != null) node = node.getParent();
		return (RootNode)node;
	}

	public RootNode read(File file) throws IOException {
		log.info("reading from file {}:", file); //$NON-NLS-1$
		if (file.isFile() && file.canRead()) {
			try (FileInputStream fis = new FileInputStream(file);
			     BufferedInputStream bis = new BufferedInputStream(fis)) {
				RootNode newRoot = read(bis, file.getAbsolutePath());
				log.info("rootNode '{}' was read from file '{}'.", newRoot.getName(), file); //$NON-NLS-1$
				return newRoot;
			} catch (IOException t) { // try must have a catch to make the Frontend-Compiler happy... ||-(
				throw t;
			} finally {
				// to automatically close the above streams
			}
		} else {
			return null;
		}
	}

	public RootNode read(InputStream inp, String fileName) throws IOException {
		Node.initializing = true; // since our "beans" also contain logic we need to prevent certain functions while the are being deserialized
		RootNode newRoot = this.mapper.readValue(inp, RootNode.class);
		restoreParents(newRoot);
		Node.initializing = false;
		newRoot.setFilePath(fileName);
		newRoot.deriveNewState();
		newRoot.updateAllResolvableValues(); // this triggers a trickle-down updating all resolvable properties in the entire tree
		addNewRootNode(newRoot);
		if (log.isTraceEnabled()) log.trace("created root: \n{}", PrettyPrint.prettyPrinted(newRoot)); //$NON-NLS-1$
		return newRoot;
	}

	public void write(RootNode rootNode, File file) throws IOException {
		log.info("saving rootNode '{}' to file '{}':", rootNode.getName(), file); //$NON-NLS-1$
		try (OutputStream fos = new FileOutputStream(file);
		     BufferedOutputStream bos = new BufferedOutputStream(fos)) {
			write(rootNode, bos);
			log.info("rootNode '{}' was written to file '{}'.", rootNode.getName(), file); //$NON-NLS-1$
		} catch (IOException t) { // try must have a catch to make the Frontend-Compiler happy... ||-(
			throw t;
		} finally {
			// to automatically close the above streams
		}
	}

	public void write(RootNode rootNode, OutputStream out) throws IOException {
		// this.mapper.writeValue(out, rootNode);
		// we want the DefaultPrettyPrinter to use \t instead of 2 spaces for indentation:
		this.mapper.writer(new DefaultPrettyPrinter().withObjectIndenter(new DefaultIndenter("\t", DefaultIndenter.SYS_LF))).writeValue(out, rootNode); //$NON-NLS-1$

	}

	public Node addChild(IntermediateNode parent, Node child) {
		if (parent == null) throw new IllegalArgumentException("parent must not be null for addChild"); //$NON-NLS-1$
		parent.addChild(child);
		if (log.isTraceEnabled()) log.trace("added child '{}': \n{}", child, PrettyPrint.prettyPrinted(parent)); //$NON-NLS-1$
		return child;
	}

	public Node removeChild(IntermediateNode parent, Node child) {
		if (parent == null) throw new IllegalArgumentException("parent must not be null for removeChild"); //$NON-NLS-1$
		parent.removeChild(child);
		if (log.isTraceEnabled()) log.trace("removed child '{}': \n{}", child.getName(), PrettyPrint.prettyPrinted(parent)); //$NON-NLS-1$
		return child;
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
//			parent.getChildren().forEach(child -> setParentNull(child));
//		}
//	}

	/** utility to restore parents, e.g. after reading from a file */
	public void restoreParents(IntermediateNode node) {
		List<Node> children = node.getChildren();
		if (children == null) {
			node.setChildren(new ArrayList<>());
		} else {
			node.getChildren().forEach(child ->
				{
					child.setParent(node);
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
}
