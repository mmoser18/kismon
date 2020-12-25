/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.entities.connections;

import java.net.URI;

import javax.persistence.Entity;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;
import net.mmo.utils.kism.entities.LeafNode;
import net.mmo.utils.kism.utils.PropertyReplacer;

@Entity
@Setter
@Getter
public class Connection extends LeafNode
{
	private static final long serialVersionUID = -2584969925210734417L;

	private boolean enabled = true; // should connection be regularly checked?

	@Min(0) // 0 means off
	@Max(3600) // means: 1x/hr
	private int period = 10; // number of seconds every so often this connection should be checked.

	@SuppressWarnings("nls")
	@NotNull
	@NotEmpty
	@NotBlank
	private String url = "http://localhost:8080";

	/** required for deserialization only */
	protected Connection() {
		// empty
	}

	protected Connection(String name, String description) {
		super(name, description);
		setState(State.values()[(int)(Math.random()*3.0)]);
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		if (this.parent != null) this.parent.deriveNewState();
	}

	public String getResultingURL(String str) throws Exception {
		URI uri = new URI(PropertyReplacer.resolveExpression(str, getProperties()));
		return uri.toString();
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(super.toString());
		buf.setLength(buf.length()-1);
		return buf
			.append(", enabled:").append(this.enabled) //$NON-NLS-1$
			.append(", period:").append(this.period) //$NON-NLS-1$
			.append(", url:").append(this.url) //$NON-NLS-1$
			.append("]") //$NON-NLS-1$
			.toString();
	}

}
