/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.entities.nodes.connections;

import java.util.Objects;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import net.mmo.utils.kism.entities.nodes.CheckableNode;

/** Name should have been just "Connection" but that class name was already used by JDBC */
@SuppressWarnings("javadoc")
@Setter
@Getter
// @Slf4j
abstract public class IPConnection extends CheckableNode
{
	private static final long serialVersionUID = 120134114096068701L;

	@SuppressWarnings("nls")
	@NotNull
	@NotEmpty
	@NotBlank
	protected String hostAddress = "www.example.com";

	@JsonIgnore
	protected transient String resultingHostAddress;


	protected IPConnection() { // required for deserialization
		super();
	}

	protected IPConnection(String name, String description) {
		super(name, description);
	}

	@Override
	public void updateResolvableValues() {
		super.updateResolvableValues();
		updateResultingHostAddress();
	}

	public void setHostAddress(String hostAddress) {
		if (!Objects.equals(this.hostAddress, hostAddress)) {
			this.hostAddress = hostAddress;
			updateResultingHostAddress();
		}
	}

	protected void updateResultingHostAddress() {
		updateResultingValue(getHostAddress(),
		                     (str) -> setResultingHostAddress(str));
	}

	public String resultingHostAddressResolved() throws Exception {
		return resultingFieldResolved(getHostAddress(),
		                              () -> getResultingHostAddress(),
		                              (str) -> setResultingHostAddress(str));
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(super.toString());
		buf.setLength(buf.length()-1);
		return buf
			.append(", hostAddress:").append(this.hostAddress) //$NON-NLS-1$
			.append(", resultingHostAddress:").append(this.resultingHostAddress) //$NON-NLS-1$
			.append("}") //$NON-NLS-1$
			.toString();
	}
}
