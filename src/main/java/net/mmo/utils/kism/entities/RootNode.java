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

	@JsonIgnore // no point in writing that value into the file itself. Restored/set after reading
	private String filePath;

	/** required for deserialization only */
	@SuppressWarnings("unused")
	private RootNode() {
		super();
	}

	public RootNode(String name, String description) {
		super(name, description);
	}

	public void setParent(Node parent) {
		if (parent != null) {
			throw new RuntimeException("This should NEVER be called!"); //$NON-NLS-1$
		}
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(super.toString());
		buf.setLength(buf.length()-1);
		return buf
			.append(", filePath:").append(this.filePath) //$NON-NLS-1$
			.append("}") //$NON-NLS-1$
			.toString();
	}
}
