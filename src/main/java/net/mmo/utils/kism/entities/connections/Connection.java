/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.entities.connections;

import net.mmo.utils.kism.entities.LeafNode;

public class Connection extends LeafNode
{
	private static final long serialVersionUID = -2584969925210734417L;

	@SuppressWarnings("nls")
//	@NotNull
//	@NotEmpty
//	@NotBlank
	String protocol = "http";

	@SuppressWarnings("nls")
//	@NotNull
//	@NotEmpty
//	@NotBlank
	String host = "localhost";

	@SuppressWarnings("nls")
//	@NotNull
//	@NotEmpty
//	@NotBlank
	String port = "8080";

	String path;

	String args;

	protected Connection(String name, String description) {
		super(name, description);
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(super.toString());
		buf.setLength(buf.length()-1);
		return buf
			.append(", protocol:").append(this.protocol) //$NON-NLS-1$
			.append(", host:").append(this.host) //$NON-NLS-1$
			.append(", port:").append(this.port) //$NON-NLS-1$
			.append(", path:").append(this.path) //$NON-NLS-1$
			.append(", args:").append(this.args) //$NON-NLS-1$
			.append("]") //$NON-NLS-1$
			.toString();
	}

}
