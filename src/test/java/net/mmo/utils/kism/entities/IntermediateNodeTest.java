/**
 * @author Michael Moser (mmo@zurich.ibm.com)
 * @since 08.11.2020
 */

package net.mmo.utils.kism.entities;

import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.Node.State;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
public class IntermediateNodeTest
{
	@Test
	void rootShouldInitiallyBeEmptyTest() {
		Node root = IntermediateNode.getRoot();
		Assertions.assertNotNull(root, "root should be null"); //$NON-NLS-1$
		Assertions.assertEquals(Messages.getString("mk_root"), root.getName()); //$NON-NLS-1$
		Assertions.assertEquals(Messages.getString("mk_root-node"), root.getDescription()); //$NON-NLS-1$
	}

	@Test
	void checkNodesAdded() {
		IntermediateNode root = IntermediateNode.getRoot();
		Assertions.assertEquals(0, root.getChildren().size());
		LeafNode leaf1 = new LeafNode("leaf1", "leaf-node1", State.OK, root); //$NON-NLS-1$ //$NON-NLS-2$
		Assertions.assertEquals(1, root.getChildren().size());
		Assertions.assertEquals(State.OK, leaf1.getState());
		LeafNode leaf2 = new LeafNode("leaf2", "leaf-node2", State.NOT_OK); //$NON-NLS-1$ //$NON-NLS-2$
		root.addChild(leaf2);
		Assertions.assertEquals(2, root.getChildren().size());
		Assertions.assertEquals(State.NOT_OK, leaf2.getState());
		Assertions.assertEquals(State.NOT_OK, root.getState());
		log.info("tree: {}", root.toString()); //$NON-NLS-1$
	}

	@Test
	void checkNodesIgnored() {
		IntermediateNode root = IntermediateNode.getRoot();
		@SuppressWarnings("unused")
		LeafNode leaf1 = new LeafNode("leaf1", "leaf-node1", State.OK, root); //$NON-NLS-1$ //$NON-NLS-2$
		LeafNode leaf2 = new LeafNode("leaf2", "leaf-node2", State.NOT_OK); //$NON-NLS-1$ //$NON-NLS-2$
		leaf2.setIgnored(true);
		Assertions.assertEquals(State.OK, root.getState());
	}
}
