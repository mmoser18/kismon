/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 08.11.2020
 */

package net.mmo.utils.kism.entities;

import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.Messages;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.Node.State;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Slf4j
public class IntermediateNodeTest
{
	@BeforeEach
	void setup() {
		NodeService.reset(); // re-initialize the root to null before each test
	}
	@Test
	void rootShouldInitiallyBeEmptyTest() {
		Node root = NodeService.getRootNode();
		Assertions.assertNotNull(root, "root should be null"); //$NON-NLS-1$
		Assertions.assertEquals(Messages.getString("km_root"), root.getName()); //$NON-NLS-1$
		Assertions.assertEquals(Messages.getString("km_root-node"), root.getDescription()); //$NON-NLS-1$
	}

	@Test
	void checkNodesAdded() {
		IntermediateNode root = NodeService.getRootNode();
		Assertions.assertEquals(0, root.getChildren().size());
		LeafNode leaf1 = new LeafNode("leaf1", "leaf-node1", State.OK, root); //$NON-NLS-1$ //$NON-NLS-2$
		Assertions.assertEquals(1, root.getChildren().size());
		Assertions.assertEquals(State.OK, leaf1.getState());
		LeafNode leaf2 = new LeafNode("leaf2", "leaf-node2", State.NOT_OK); //$NON-NLS-1$ //$NON-NLS-2$
		root.addChild(leaf2);
		LeafNode leaf3 = new LeafNode("leaf3", "leaf-node3", State.DEGRADED); //$NON-NLS-1$ //$NON-NLS-2$
		root.addChild(leaf3);
		Assertions.assertEquals(3, root.getChildren().size());
		Assertions.assertEquals(State.NOT_OK, leaf2.getState());
		Assertions.assertEquals(State.DEGRADED, leaf3.getState());
		Assertions.assertEquals(State.NOT_OK, root.getState()); // NOT_OK since the worst child is NOT_OK
		log.info("tree: {}", root.toString()); //$NON-NLS-1$
	}

	@SuppressWarnings("unused")
	@Test
	void checkNodesIgnored() {
		IntermediateNode root = NodeService.getRootNode();
		new LeafNode("leaf1", "leaf-node1", State.OK, root); //$NON-NLS-1$ //$NON-NLS-2$
		new LeafNode("leaf2", "leaf-node2", State.NOT_OK, root).setIgnored(true); //$NON-NLS-1$ //$NON-NLS-2$
		new LeafNode("leaf3", "leaf-node3", State.DEGRADED, root); //$NON-NLS-1$ //$NON-NLS-2$
		Assertions.assertEquals(State.DEGRADED, root.getState()); // NOT_OK since the worst child (NOT_OK) is ignored
	}
}
