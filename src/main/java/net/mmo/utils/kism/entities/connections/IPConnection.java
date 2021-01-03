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
import net.mmo.utils.kism.utils.AppProperties;

@Entity
@Setter
@Getter
abstract public class IPConnection extends LeafNode
{
	private static final long serialVersionUID = -2584969925210734417L;

	public final static String PROPERTYNAME_DURATION  = "duration";  //$NON-NLS-1$
	public final static String PROPERTYNAME_RESPONSE_COMPLETE    = "responseComplete"; //$NON-NLS-1$

	protected boolean enabled = true; // should connection be regularly checked?

	@Min(0) // 0 means off
	@Max(3600) // means: 1x/hr
	protected int period = 60; // number of seconds every so often this connection should be checked.

	@Min(0) // 0 means off
	@Max(600) // 10 minutes
	protected int timeout = 45; // number of seconds after which connection setup is considered as failed

	@SuppressWarnings("nls")
	@NotNull
	@NotEmpty
	@NotBlank
	protected String url = "http://www.example.com:8080";

	protected String proxyHost;
	protected int    proxyPort;
//	protected String proxyUid;
//	protected String proxyPwd;

	/** common response fields: */
	transient protected long duration;

	protected static boolean syncRequests = Boolean.getBoolean(AppProperties.getProperties().getProperty("IPConnection.SyncRequests", "false")); //$NON-NLS-1$ //$NON-NLS-2$


	/**required for deserialization only */
	protected IPConnection() {
		// empty
	}

	protected IPConnection(String name, String description) {
		super(name, description);
		// setState(State.values()[(int)(Math.random()*3.0)]);
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		if (this.parent != null) this.parent.deriveNewState();
	}

	public void setDuration(long duration) {
		if (this.duration != duration) {
			long oldDuration = this.duration;
			this.duration = duration;
			informOnPropertyChange(PROPERTYNAME_DURATION, oldDuration, duration);
		}
	}

	public String getResultingURL(String str) throws Exception {
		URI uri = new URI(resolveProperties(str));
		return uri.toString();
	}

	// convenience method to convert duration in nano-seconds to milli-sec's
	public static Double convertNanoSecsToMillis(long responseDuration) {
		return (responseDuration != 0 ? (Math.round(responseDuration/1000)*1.0e-3) : 0); // nsec's --> msec's (rounded to micro-seconds)
	}

	abstract public void sendRequest() throws Exception;

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(super.toString());
		buf.setLength(buf.length()-1);
		return buf
			.append(", enabled:").append(this.enabled) //$NON-NLS-1$
			.append(", period:").append(this.period) //$NON-NLS-1$
			.append(", timeout:").append(this.timeout) //$NON-NLS-1$
			.append(", duration:").append(this.duration) //$NON-NLS-1$
			.append("}") //$NON-NLS-1$
			.toString();
	}

}
