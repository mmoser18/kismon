/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
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
