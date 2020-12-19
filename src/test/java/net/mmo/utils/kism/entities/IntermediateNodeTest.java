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
import org.junit.jupiter.api.Test;

@Slf4j
public class IntermediateNodeTest
{
	private NodeService nodeService = new NodeService(); // autowiring this didn't work, but what the heck!

	public IntermediateNodeTest() {
		// empty
	}

	@Test
	void rootShouldInitiallyBeEmptyTest() {
		this.nodeService.reset(); // re-initialize the root to null before each test
		IntermediateNode root = this.nodeService.getRootNodes().get(0);
		Assertions.assertNotNull(root, "root should not be null"); //$NON-NLS-1$
		Assertions.assertEquals(Messages.getString("km_root_name"), root.getName()); //$NON-NLS-1$
		Assertions.assertEquals(Messages.getString("km_root_description"), root.getDescription()); //$NON-NLS-1$
	}

	@Test
	void checkNodesAdded() {
		this.nodeService.reset(); // re-initialize the root to null before each test
		IntermediateNode root = this.nodeService.getRootNodes().get(0);
		Assertions.assertEquals(0, root.getChildren().size());
		LeafNode leaf1 = new LeafNode("leaf1", "leaf-node1", State.OK, root); //$NON-NLS-1$ //$NON-NLS-2$
		Assertions.assertEquals(1, root.getChildren().size());
		Assertions.assertEquals(State.OK, leaf1.getState());
		LeafNode leaf2 = new LeafNode("leaf2", "leaf-node2", State.NOT_OK); //$NON-NLS-1$ //$NON-NLS-2$
		this.nodeService.addChild(root, leaf2);
		LeafNode leaf3 = new LeafNode("leaf3", "leaf-node3", State.DEGRADED); //$NON-NLS-1$ //$NON-NLS-2$
		this.nodeService.addChild(root, leaf3);
		Assertions.assertEquals(3, root.getChildren().size());
		Assertions.assertEquals(State.NOT_OK, leaf2.getState());
		Assertions.assertEquals(State.DEGRADED, leaf3.getState());
		Assertions.assertEquals(State.NOT_OK, root.getState()); // NOT_OK since the worst child is NOT_OK
		log.info("tree: {}", root.toString()); //$NON-NLS-1$
	}

	@SuppressWarnings("unused")
	@Test
	void checkNodesActive() {
		this.nodeService.reset(); // re-initialize the root to null before each test
		IntermediateNode root = this.nodeService.getRootNodes().get(0);
		new LeafNode("leaf1", "leaf-node1", State.OK, root); //$NON-NLS-1$ //$NON-NLS-2$
		new LeafNode("leaf2", "leaf-node2", State.NOT_OK, root).setActive(false); //$NON-NLS-1$ //$NON-NLS-2$
		new LeafNode("leaf3", "leaf-node3", State.DEGRADED, root); //$NON-NLS-1$ //$NON-NLS-2$
		Assertions.assertEquals(State.DEGRADED, root.getState()); // NOT_OK since the worst child (NOT_OK) is ignored
	}
}
