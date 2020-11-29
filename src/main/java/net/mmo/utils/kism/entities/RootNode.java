/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 27.11.2020
 */

package net.mmo.utils.kism.entities;

import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
// @Slf4j
public class RootNode extends IntermediateNode
{
	private static final long serialVersionUID = -1717387035835870973L;

	@JsonIgnore // no need to write that value into the file itself.
	private String filePath;

	/** required for deserialization only */
	@SuppressWarnings("unused")
	private RootNode() {
		super();
	}

	public RootNode(String name, String description, State state) {
		super(name, description, state);
	}

	public void setParent(Node parent) {
		if (parent != null) {
			throw new RuntimeException("This should NEVER be called!"); //$NON-NLS-1$
		}
	}
}
