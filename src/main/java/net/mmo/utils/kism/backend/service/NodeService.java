/**
 * Copyright © 2020-2025 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.backend.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.vaadin.flow.component.notification.Notification;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.AbstractEntity;
import net.mmo.utils.kism.entities.nodes.IntermediateNode;
import net.mmo.utils.kism.entities.nodes.Node;
import net.mmo.utils.kism.entities.nodes.RootNode;
import net.mmo.utils.kism.utils.AppProperties;
import net.mmo.utils.kism.utils.ExceptionUtils;
import net.mmo.utils.kism.utils.NodeProperties;
import net.mmo.utils.kism.utils.PrettyPrint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Service;

/**
 * Implements the service to access and store Nodes
 */
@SuppressWarnings("javadoc")
@Service
@Slf4j
public class NodeService
{
	public final static String DEFAULT_FILE_NAME = AppProperties.getProperties().getProperty("config_dflt_name", "default"); //$NON-NLS-1$ //$NON-NLS-2$
	public final static String CONFIG_FILE_TYPE  = AppProperties.getProperties().getProperty("config_dflt_type", "kmc"); //$NON-NLS-1$ //$NON-NLS-2$

	public static ApplicationArguments applicationArguments;

	@Value("${config.files:#{null}}") // allow specification via application.properties
	protected String configFiles;
	public static String startupErrors = ""; // Since the GUI does not yet exist while starting up we //$NON-NLS-1$
	                                         // collect all error here so they can later be displayed
	                                         // to the user. We also don't want to refer to any GUI here.

	private List<RootNode> roots = new ArrayList<>();
	private JsonMapper mapper;
	private Properties commonRootProperties = new NodeProperties();
	{
		this.commonRootProperties.putAll(System.getenv()); // per default all Environment variables
	}

	public NodeService() {
		log.debug("{} c'tor", this.getClass()); //$NON-NLS-1$
		this.mapper = JsonMapper.builder()
			.enable(SerializationFeature.INDENT_OUTPUT)
			.configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
			.build();
	}

	@PostConstruct // can only be executed after the @Value above has been set.
	void readConfigFiles() {
		if (getRootNodes().isEmpty()) {
			List<String> args = (applicationArguments != null ? applicationArguments.getNonOptionArgs() : null);
			List<String> fileNames = args == null || args.isEmpty()
			                         ? this.configFiles != null // specified in application properties
			                           ? Arrays.asList(this.configFiles.split("[,; ]"))  //$NON-NLS-1$
			                           : Collections.emptyList()
			                           : args; // if defined: cmd-line beats properties
			// fileNames is never null!
			log.info("files to read initially are: '{}'", fileNames); //$NON-NLS-1$
			init(fileNames);
		}
	}

	public List<RootNode> getRootNodes() {
		return this.roots;
	}

	public boolean save(Node node) {
		//save to repo:
		if (node == null) {
			log.error("Node is null. Are you sure you have connected your form to the application?"); //$NON-NLS-1$
			return false;
		}
		// save to file:
		RootNode rootNode = getCorrespondingRootNode(node);
		String path = rootNode.getFilePath();
		path = saveToFile(rootNode, rootNode.getName(), path != null ? path : DEFAULT_FILE_NAME + '.' + CONFIG_FILE_TYPE);
		if (path != null) {
			if (!path.equals(rootNode.getFilePath())) {
				rootNode.setFilePath(path);
			}
			return true;
		}
		return false;
	}

	public boolean saveAllRootNodes() {
		return getRootNodes().stream().map(node -> save(node)).allMatch((res -> res));
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
	public void init(List<String> filenames) {
		filenames.stream().forEach((filename) ->
		{
			File file = new File(filename);
			try {
				readFile(file);
			} catch (IOException t) {
				String msg = String.format("File '%s' is not a valid configuration (reason: '%s').", //$NON-NLS-1$
				                           file, ExceptionUtils.exceptionCauseSummary(t));
				log.error(msg);
				startupErrors += msg + '\n';
			} catch (Throwable t) {
				String msg = "Exception processing file '" + file + ":"; //$NON-NLS-1$ //$NON-NLS-2$
				log.error(msg, t);
				startupErrors += msg + t.getMessage() + '\n';
			}
		});
		if (getRootNodes().isEmpty()) { // make sure we always have at least one root node:
			createDummyRootNode();
		}
		log.trace("Root nodes: {}", getRootNodes()); //$NON-NLS-1$
	}

	public void createDummyRootNode() {
		log.info("Creating dummy root node:"); //$NON-NLS-1$
		RootNode rootNode = new RootNode(Messages.getString("km_root_name"), //$NON-NLS-1$
		                                 Messages.getString("km_root_description")); //$NON-NLS-1$
		// addSomeSubNodes(rootNode);
		log.info("Node '{}' created.", rootNode); //$NON-NLS-1$
		addNewRootNode(rootNode);
	}

	/** enables re-initialization of root nodes for Unit-Tests - not normally used */
	public void reset() {
		getRootNodes().clear();
		createDummyRootNode();
	}

	public static RootNode getCorrespondingRootNode(Node startNode) {
		Node node = startNode;
		while (node.getParent() != null) node = node.getParent();
		return (RootNode)node;
	}

	public String saveToFile(Object obj, String rootNodeName, String fileName) {
		try {
			// TODO : provide some file selection box?
			File finalFile = new File(fileName);
			if ((!finalFile.exists() && finalFile.createNewFile()) || finalFile.canWrite()) {
				File tmpFile = new File(fileName + "_$temp$"); //$NON-NLS-1$
				if ((!tmpFile.exists() && tmpFile.createNewFile()) || tmpFile.canWrite()) {
					this.writeFile(obj, rootNodeName, tmpFile);
					// still here: the writing succeeded
					File backupFile = new File(fileName + ".bak"); //$NON-NLS-1$
					if (backupFile.exists()) backupFile.delete(); // delete old backup file - if exists
					if (finalFile.exists()) finalFile.renameTo(backupFile); // old file renamed to backup filename
					tmpFile.renameTo(finalFile); // newly written file gets original filename
					log.info("'{}' successfully renamed to '{}'.", tmpFile, finalFile); //$NON-NLS-1$
					return finalFile.getCanonicalPath();
				} else {
					String msg = String.format("File '%s' is not writable!", tmpFile); //$NON-NLS-1$
					log.info(msg);
					createNotification(msg);
				}
			} else {
				String msg = String.format("File '%s' is not writable!", finalFile); //$NON-NLS-1$
				log.info(msg);
				createNotification(msg);
			}
		} catch (Throwable t) {
			String msg = String.format("Exception saving '%s': %s", obj, t); //$NON-NLS-1$
			log.error(msg, t);
			createNotification(msg);
		}
		return null;
	}

	/* moved this here to avoid references of views/components in entities (although this internally
	 * references Vaadin UI components...) */

	public static void createNotification(String msg) {
		createNotification(msg, 0); // 0: means: no auto-closing, i.e. stay until clicked
	}

	public static void createNotification(String msg, int durationInMillis) {
		Notification notif = new Notification(msg, durationInMillis)
		{
			private static final long serialVersionUID = 8046905230067171276L;

			@Override
			public String toString() {
				return super.toString() + "[msg='" + msg + "']"; //$NON-NLS-1$ //$NON-NLS-2$
			}
		};
		notif.addAttachListener(evt ->
		{
			log.trace("Notification Attach-Event: {}", evt); //$NON-NLS-1$
			// notif.close();
		});
		notif.addOpenedChangeListener(evt ->
		{
			log.trace("Notification OpenedChange-Event: {}", evt); //$NON-NLS-1$
			// notif.close();
		});
		notif.open();
	}

	public void writeFile(Object obj, String rootNodeName, File file) throws IOException {
		log.info("saving configuration '{}' to file '{}':'",rootNodeName, file, obj); //$NON-NLS-1$
		log.debug("object: '{}'", obj); //$NON-NLS-1$
		try (OutputStream fos = new FileOutputStream(file);
		     BufferedOutputStream bos = new BufferedOutputStream(fos)) {
			writeStream(obj, bos);
			bos.flush(); // this one proofed essential!
			log.info("'{}' successfully written to file '{}'.", rootNodeName, file.getCanonicalPath()); //$NON-NLS-1$
		} catch (IOException t) { // try must have a catch to make the Frontend-Compiler happy... ||-(
			throw t;
		} finally {
			// to automatically close the above streams
		}
	}

	public void writeStream(Object obj, OutputStream out) throws IOException {
		// we want the DefaultPrettyPrinter to use \t instead of 2 spaces for indentation:
		this.mapper.writer(new DefaultPrettyPrinter()
		                   .withObjectIndenter(new DefaultIndenter("\t", DefaultIndenter.SYS_LF))) //$NON-NLS-1$
			.writeValue(out, obj);

	}

	public RootNode readFile(File file) throws IOException {
		String fullPath = file.getAbsolutePath();
		log.info("Reading configuration from file '{}':", fullPath); //$NON-NLS-1$
		if (file.isFile() && file.canRead()) {
			try (FileInputStream fis = new FileInputStream(file);
			     BufferedInputStream bis = new BufferedInputStream(fis)) {
				RootNode newRoot = readStream(bis, fullPath);
				log.info("Tree for root-node '{}' successfully read from file '{}'.", newRoot.getName(), fullPath); //$NON-NLS-1$
				if (log.isTraceEnabled()) { // it's on purpose that the following uses log.debug(...)!
					log.debug("Created root: \n{}", PrettyPrint.prettyPrinted(newRoot)); //$NON-NLS-1$
				}
				return newRoot;
			} catch (IOException t) { // try must have a catch to make the Frontend-Compiler happy... ||-(
				throw t;
			// } finally {
				// only to automatically close the above streams - done automatically...
			}
		} else {
			throw new IOException("File '" + fullPath + "' not found or not readable"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public RootNode readStream(InputStream inp, String fileName) throws IOException {
		AbstractEntity.initializing = true; // since our "beans" also contain logic we need to prevent certain functions from being called while the are being deserialized
		RootNode newRoot = this.mapper.readValue(inp, RootNode.class);
		// restoreParents(newRoot); // not necessary anymore thanks to @JsonManagedReference/@JsonBackReference, instead of @JsonIgnore
		AbstractEntity.initializing = false;
		newRoot.setFilePath(fileName);
		newRoot.deriveNewState();
		newRoot.updateAllResolvableValues(); // this triggers a trickle-down updating all resolvable properties in the entire tree
		addNewRootNode(newRoot);
		return newRoot;
	}

	/**
	 * Add a child at the desired position of the parent's list.
	 * @param parent the to-be parent of the child
	 * @param child to be added
	 * @param pos desired position within list. -1 to signal to append.
	 */
	public Node addChild(IntermediateNode parent, Node child, int pos) {
		if (parent == null) throw new IllegalArgumentException("parent must not be null for addChild"); //$NON-NLS-1$
		parent.addChildAtPos(pos, child);
		if (log.isTraceEnabled()) log.trace("added child '{}': \n{}", child, PrettyPrint.prettyPrinted(parent)); //$NON-NLS-1$
		return child;
	}

	public Node removeChild(IntermediateNode parent, Node child) {
		if (parent == null) throw new IllegalArgumentException("parent must not be null for removeChild"); //$NON-NLS-1$
		parent.removeChild(child);
		if (log.isTraceEnabled()) log.trace("removed child '{}': \n{}", child.getName(), PrettyPrint.prettyPrinted(parent)); //$NON-NLS-1$
		return child;
	}

	public boolean removeRootNode(RootNode node) {
		return getRootNodes().remove(node);
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

	/**
	 * Clones an object 1:1 but assigns it a new name
	 * @param node
	 * @return cloned object
	 */
	public static Node deepCloneNode(Node node) {
		Node clone = deepClone(node);
		clone.setName(node.getName() + "-Copy"); //$NON-NLS-1$
		clone.postClone(node);
		return clone;
	}

	/** generic deep clone method using serialization to guarantee that no references are shared
	 * @param <T>
	 * @param t
	 * @return cloned object
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Serializable> T deepClone(T t) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
		     ObjectOutputStream oos = new ObjectOutputStream(baos);) {
			oos.writeObject(t);
			byte[] bytes = baos.toByteArray();
			try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
				return (T)ois.readObject();
			}
		} catch (IOException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

//	// make all children a child of the parent-node:
//	public void deleteAll(Collection<Node> nodes) {
//		delete(nodes, false);
//	}

//	/** utility to restore parents, e.g. after reading from a file */
//	public void restoreParents(IntermediateNode node) {
//		List<Node> children = node.getChildren();
//		if (children == null) {
//			node.setChildren(new ArrayList<>());
//		} else {
//			node.getChildren().forEach(child ->
//				{
//					child.setParent(node);
//					if (child instanceof IntermediateNode) restoreParents((IntermediateNode)child);
//				});
//		}
//	}
}
