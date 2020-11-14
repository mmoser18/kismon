
package net.mmo.utils.kism.backend.service;

import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.repositories.IntermediateNodeRepository;
import net.mmo.utils.kism.backend.repositories.LeafNodeRepository;
import net.mmo.utils.kism.entities.IntermediateNode;
import net.mmo.utils.kism.entities.Node;
import net.mmo.utils.kism.utils.PrettyPrint;
import org.springframework.stereotype.Service;

/**
 * Implements the service to access and store Companies
 */
@Service
@Slf4j
public class NodeService
{
	@SuppressWarnings("unused")
	private IntermediateNodeRepository intermediateNodeRepository;
	@SuppressWarnings("unused")
	private LeafNodeRepository leafNodeRepository;

	public NodeService(IntermediateNodeRepository intermediateNodeRepository,
	                   LeafNodeRepository leafNodeRepository) {
		this.intermediateNodeRepository = intermediateNodeRepository;
		this.leafNodeRepository = leafNodeRepository;
	}

	private static IntermediateNode root;

	static void init() {
		/** this is the one and only node that has no parent! */
		NodeService.root = new IntermediateNode(Messages.getString("km_root"), //$NON-NLS-1$
		                                        Messages.getString("km_root-node"), //$NON-NLS-1$
		                                        Node.State.OK)
			{
				private static final long serialVersionUID = -7170243160659454761L;
			};
	}

	public static void addSomeSubNodes() {
		addSomeSubNodes(getRootNode());
		log.info("created root: \n{}", PrettyPrint.prettyPrinted(getRootNode())); //$NON-NLS-1$
	}

	@SuppressWarnings({"nls", "boxing"})
	private static void addSomeSubNodes(IntermediateNode parent) {
		int max = parent == NodeService.root ? 2 : (int)(Math.random() * 3.0); // maps [0.0 - 1.0) --> [0..2]
		if (parent.getName().length() >= 8) max = 0; // limit depth...
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

	/** enables re-initialization of root for Unit-Tests - not normally used */
	public static void reset() {
		NodeService.root = null;
	}

	public static IntermediateNode getRootNode() {
		if (NodeService.root == null) init();
		return NodeService.root;
	}

	public void save(Node node) {
		// TODO Auto-generated method stub
	}

	public void delete(Node node) {
		// TODO Auto-generated method stub

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
