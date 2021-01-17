/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.entities.connections;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.LeafNode;
import net.mmo.utils.kism.entities.ResultChecker;
import net.mmo.utils.kism.utils.AppProperties;

/** Name should have been just "Connection" but that class name was already used by JDBC */
@Entity
@Setter
@Getter
@Slf4j
public class IPConnection extends LeafNode
{
	private static final long serialVersionUID = -2584969925210734417L;

	public final static String PROPERTYNAME_DURATION  = "duration";  //$NON-NLS-1$
	public final static String PROPERTYNAME_RESPONSE_COMPLETE    = "responseComplete"; //$NON-NLS-1$

	protected boolean enabled = false; // should connection be regularly checked?

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

//	protected String proxyHost;
//	protected int    proxyPort;
//	protected String proxyUid;
//	protected String proxyPwd;

	protected boolean validateResponse = false; // should be boolean but when using the basic type the construct "IPConnection::getCheckResult" can not be found ||-(

	@OneToOne
	@JoinColumn(name = "result_checker_id")
	protected ResultChecker resultChecker;

	protected static boolean syncRequests =
		Boolean.getBoolean(AppProperties.getProperties().getProperty("IPConnection.SyncRequests", "false")); //$NON-NLS-1$ //$NON-NLS-2$

	// transient stuff:

	@JsonIgnore
	transient protected String resultingUrl;

	@JsonIgnore
	transient protected ExecutorService interactiveExecutor;
	@JsonIgnore
	transient protected ExecutorService periodicExecutor;

	// result stuff:
	// TODO: the below should be kept into some kind of "history"-list:
	//	transient protected history = new ArrayList();

	/** common response fields: */

	//	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
//	@JsonSerialize(using = LocalDateTimeSerializer.class)
//	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
	@JsonIgnore
	transient protected LocalDateTime timestamp;

	transient protected long duration;


	/**required for deserialization only */
	protected IPConnection() {
		initExecutors();
	}

	protected IPConnection(String name, String description) {
		super(name, description);
		// setState(State.values()[(int)(Math.random()*3.0)]); // during developing
		initExecutors();
	}

	private void initExecutors() {
		if (!syncRequests) {
			this.interactiveExecutor = Executors.newSingleThreadExecutor();
		}
		this.periodicExecutor = Executors.newSingleThreadScheduledExecutor();
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

	// convenience method to convert duration in nano-seconds to milli-sec's
	public static Double convertNanoSecsToMillis(long responseDuration) {
		return (responseDuration != 0 ? (Math.round(responseDuration/1000)*1.0e-3) : 0); // nsec's --> msec's (rounded to micro-seconds)
	}

	public void sendRequest() throws Exception {
		if (IPConnection.syncRequests) {
			log.info("sendRequest-sync:"); //$NON-NLS-1$
			sendRequestInternal();
		} else {
			log.info("sendRequest-async:"); //$NON-NLS-1$
			this.interactiveExecutor.execute(() -> {
				try {
					sendRequestInternal();
				} catch (Exception ex) {
					log.error("Exception executing sendRequest", ex); //$NON-NLS-1$
				}
			});
		}
	}

	@SuppressWarnings("unused")
	public void sendRequestInternal() throws Exception {
		// empty
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(super.toString());
		buf.setLength(buf.length()-1);
		return buf
			.append(", enabled:").append(this.enabled) //$NON-NLS-1$
			.append(", period:").append(this.period) //$NON-NLS-1$
			.append(", timeout:").append(this.timeout) //$NON-NLS-1$
			.append(", url:").append(this.url) //$NON-NLS-1$
			.append(", resultingUrl:").append(this.resultingUrl) //$NON-NLS-1$
//			.append(", proxyHost:").append(this.proxyHost) //$NON-NLS-1$
//			.append(", proxyPort:").append(this.proxyPort) //$NON-NLS-1$
//			.append(", proxyUid:").append(this.proxyUid) //$NON-NLS-1$
//			.append(", proxyPwd:").append(this.proxyPwd) //$NON-NLS-1$
			.append(", validateResponse:").append(this.validateResponse) //$NON-NLS-1$
			.append(", timestamp:").append(this.timestamp) //$NON-NLS-1$
			.append(", duration:").append(this.duration) //$NON-NLS-1$
			.append(", resultChecker:").append(this.resultChecker) //$NON-NLS-1$
			.append("}") //$NON-NLS-1$
			.toString();
	}

}
