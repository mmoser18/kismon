/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.entities.connections;

import javax.persistence.Entity;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;
import net.mmo.utils.kism.entities.LeafNode;

@Entity
@Setter
@Getter
public class Connection extends LeafNode
{
	private static final long serialVersionUID = -2584969925210734417L;

	@SuppressWarnings("nls")
	@NotNull
	@NotEmpty
	@NotBlank
	private String protocol = "http";

	@SuppressWarnings("nls")
	@NotNull
	@NotEmpty
	@NotBlank
	private String host = "localhost";

	@SuppressWarnings("nls")
	@NotNull
	@NotEmpty
	@NotBlank
	private String port = "8080";

	private String path;

	private String args;

	private boolean enabled = true; // should connection be regularly checked?

	protected Connection(String name, String description) {
		super(name, description);
		setState(State.values()[(int)(Math.random()*3.0)]);
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		if (this.parent != null) this.parent.deriveNewState();
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
			.append(", enabled:").append(this.enabled) //$NON-NLS-1$
			.append("]") //$NON-NLS-1$
			.toString();
	}

}
