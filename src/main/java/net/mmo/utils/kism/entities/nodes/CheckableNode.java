/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.entities.nodes;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

/**
 * a connection that has a result checker
 */
@Setter
@Getter
// @Slf4j
abstract public class CheckableNode extends LeafNode
{
	private static final long serialVersionUID = -7861536138222066083L;

	// result stuff:`
	protected boolean checkResults = false;

	@OneToOne
	@JoinColumn(name = "result_checker_id")
	protected ResultChecker resultChecker;

	protected CheckableNode() { // required for deserialization
		super();
	}

	protected CheckableNode(String name, String description) {
		super(name, description);
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(super.toString());
		buf.setLength(buf.length()-1);
		return buf
			.append(", checkResults:").append(this.checkResults) //$NON-NLS-1$
			.append(", resultChecker:").append(this.resultChecker) //$NON-NLS-1$
			.append('}')
			.toString();
	}
}
