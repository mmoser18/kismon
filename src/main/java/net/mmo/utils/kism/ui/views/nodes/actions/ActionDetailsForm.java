/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.09.2022
 */

package net.mmo.utils.kism.ui.views.nodes.actions;

import com.vaadin.flow.data.binder.ValidationException;
import net.mmo.utils.kism.entities.nodes.ActionableNode;


@SuppressWarnings("javadoc")
public interface ActionDetailsForm <N extends ActionableNode> {
	static final long serialVersionUID = 1766004450436880716L;

	default void setNode(N node, Runnable dataHasChanged) {/*empty*/}
	@SuppressWarnings("unused")
	default void validate() throws ValidationException { /*empty*/}

	default void readBean(N node) {/*empty*/}

	@SuppressWarnings("unused")
	default void writeBean(N node) throws ValidationException {/*empty*/}
}
