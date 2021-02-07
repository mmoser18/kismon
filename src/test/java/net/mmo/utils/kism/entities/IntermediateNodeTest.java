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

	@SuppressWarnings("serial")
	class TestLeafNode extends LeafNode // LeafNode has an abstract method that we are not interested in here:
	{
		TestLeafNode(String name, String description, State state) {
			super(name, description, state);
		}
		TestLeafNode(String name, String description, State state, IntermediateNode parent) {
			super(name, description, state, parent);
		}

		@Override
		public void sendRequest() throws Exception {
			// empty
		}
	}

	@SuppressWarnings("serial")
	@Test
	void checkNodesAdded() {
		this.nodeService.reset(); // re-initialize the root to null before each test
		IntermediateNode root = this.nodeService.getRootNodes().get(0);
		Assertions.assertEquals(0, root.getChildren().size());
		LeafNode leaf1 = new TestLeafNode("leaf1", "leaf-node1", State.OK, root) {/*empty*/}; //$NON-NLS-1$ //$NON-NLS-2$
		Assertions.assertEquals(1, root.getChildren().size());
		Assertions.assertEquals(State.OK, leaf1.getState());
		LeafNode leaf2 = new TestLeafNode("leaf2", "leaf-node2", State.FAILED) {/*empty*/}; //$NON-NLS-1$ //$NON-NLS-2$
		this.nodeService.addChild(root, leaf2);
		LeafNode leaf3 = new TestLeafNode("leaf3", "leaf-node3", State.DEGRADED) {/*empty*/}; //$NON-NLS-1$ //$NON-NLS-2$
		this.nodeService.addChild(root, leaf3);
		Assertions.assertEquals(3, root.getChildren().size());
		Assertions.assertEquals(State.FAILED, leaf2.getState());
		Assertions.assertEquals(State.DEGRADED, leaf3.getState());
		Assertions.assertEquals(State.FAILED, root.getState()); // FAILED since the worst child is FAILED
		log.info("tree: {}", root.toString()); //$NON-NLS-1$
	}

	@SuppressWarnings({"unused", "serial"})
	@Test
	void checkNodesActive() {
		this.nodeService.reset(); // re-initialize the root to null before each test
		IntermediateNode root = this.nodeService.getRootNodes().get(0);
		new TestLeafNode("leaf1", "leaf-node1", State.OK, root) {/*empty*/}; //$NON-NLS-1$ //$NON-NLS-2$
		new TestLeafNode("leaf2", "leaf-node2", State.FAILED, root) {/*empty*/}.setEnabled(false); //$NON-NLS-1$ //$NON-NLS-2$
		new TestLeafNode("leaf3", "leaf-node3", State.DEGRADED, root) {/*empty*/}; //$NON-NLS-1$ //$NON-NLS-2$
		Assertions.assertEquals(State.DEGRADED, root.getState()); // FAILED since the worst child (FAILED) is ignored
	}
}
