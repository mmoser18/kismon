/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.entities.connections;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.LeafNode;
import net.mmo.utils.kism.entities.ResultChecker;
import net.mmo.utils.kism.utils.AppProperties;
import net.mmo.utils.kism.utils.ExceptionUtils;
import net.mmo.utils.kism.utils.NodeProperties;

/** Name should have been just "Connection" but that class name was already used by JDBC */
@Entity
@Setter
@Getter
@Slf4j
abstract public class IPConnection extends LeafNode
{
	private static final long serialVersionUID = -2584969925210734417L;

	public final static String PROPERTYNAME_DURATION  = "duration";  //$NON-NLS-1$
	public final static String PROPERTYNAME_TIMESTAMP  = "timestamp";  //$NON-NLS-1$
	public final static String PROPERTYNAME_RESPONSE_COMPLETE    = "responseComplete"; //$NON-NLS-1$

	protected boolean enabled = false; // should connection be regularly checked?

	@Min(0) // 0 means off
	@Max(3600) // means: 1x/hr
	protected int period = 60; // number of seconds every so often this connection should be checked.

	@Min(0) // 0 means off
	@Max(600) // 10 minutes
	protected int timeout = 45; // number of seconds after which connection setup is considered as failed

//	protected String proxyHost;
//	protected int    proxyPort;
//	protected String proxyUid;
//	protected String proxyPwd;

	@SuppressWarnings("nls")
	@NotNull
	@NotEmpty
	@NotBlank
	protected String url = "http://www.example.com:8080";

	@JsonIgnore
	transient protected String resultingUrl;

	protected boolean checkResults = false; // should be boolean but when using the basic type the construct "IPConnection::getCheckResult" can not be found ||-(

	@OneToOne
	@JoinColumn(name = "result_checker_id")
	protected ResultChecker resultChecker;

	// transient stuff:

	@JsonIgnore
	transient protected Runnable request; // runs the request
	@JsonIgnore
	transient protected ScheduledExecutorService periodicExecutor; // for periodic/automatic execution
	@JsonIgnore
	transient protected ScheduledFuture<?> future;
	@JsonIgnore
	transient protected ExecutorService interactiveExecutor; // for interactive execution (via GUI)

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

	protected static boolean asyncRequests =
					Boolean.parseBoolean(AppProperties.getProperties().getProperty("IPConnection.asyncRequests", "`true")); //$NON-NLS-1$ //$NON-NLS-2$
	protected static boolean shortLogEntries =
					Boolean.parseBoolean(AppProperties.getProperties().getProperty("IPConnection.ShortLogEntries", "true")); //$NON-NLS-1$ //$NON-NLS-2$

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
		this.request = new Runnable() {
			@Override
			public void run() {
				try {
					sendRequest();
				} catch (Exception ex) { // Exceptions are normal here - so we try to minimize the log load:
					if (shortLogEntries) {
						log.info("Error executing request '{}':{}", getName(), ExceptionUtils.exceptionCauseSummary(ex)); //$NON-NLS-1$
					} else {
						log.error("Error executing request '" + getName() + "':", ex); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}
		};
		if (asyncRequests) {
			this.interactiveExecutor = Executors.newSingleThreadExecutor();
		}
	}

	@Override
	public void setProperties(NodeProperties properties) {
		super.setProperties(properties);
		updateResultingUrl();
	}

	public void setUrl(String url) {
		this.url = url;
		updateResultingUrl();
	}

	private void updateResultingUrl() {
		updateResultingValue(getUrl(), (str) -> setResultingUrl(str));
	}

	public String resultingUrlResolved() throws Exception {
		return resultingFieldResolved(getUrl(), () -> getResultingUrl(), (str) -> setResultingUrl(str));
	}

	public void setEnabled(boolean enabled) {
		if (this.enabled != enabled) {
			this.enabled = enabled;
			if (this.parent != null) this.parent.deriveNewState();
			adjustExecutor(enabled);
		}
	}

	public void setPeriod(int period) {
		if (this.period != period) { // we are changing the period
			this.period = period;
			adjustExecutor(this.enabled); // restart (if applicable)
		}
	}

	private void adjustExecutor(boolean start) {
		if (this.periodicExecutor != null && !this.periodicExecutor.isShutdown()) {
			if (this.future != null) this.future.cancel(false);
			log.info("stopping periodicExecutor for {}:", getName()); //$NON-NLS-1$
			this.periodicExecutor.shutdown();
		}
		if (start) { // we (re)start - possibly with a different period:
			log.info("starting periodicExecutor for '{}' (period: {} secs):", getName(), getPeriod()); //$NON-NLS-1$
			this.periodicExecutor = Executors.newScheduledThreadPool(1);
			this.future = this.periodicExecutor.scheduleAtFixedRate(this.request, getPeriod(), getPeriod(), TimeUnit.SECONDS);
		}
	}

	public void setDuration(long duration) {
		if (this.duration != duration) {
			long oldDuration = this.duration;
			this.duration = duration;
			informOnPropertyChange(PROPERTYNAME_DURATION, oldDuration, duration);
		}
	}

	public void setTimestamp(LocalDateTime timestamp) {
		if (!Objects.equal(this.timestamp, timestamp)) {
			LocalDateTime oldTimestamp = this.timestamp;
			this.timestamp = timestamp;
			informOnPropertyChange(PROPERTYNAME_TIMESTAMP, oldTimestamp, timestamp);
		}
	}

	// convenience method to convert duration in nano-seconds to milli-sec's
	public static Double convertNanoSecsToMillis(long responseDuration) {
		return (responseDuration != 0 ? (Math.round(responseDuration/1000.0)*1.0e-3) : 0); // nsec's --> usec's (rounded to micro-seconds)
	}

	public void executeRequest() throws Exception {
		if (IPConnection.asyncRequests) {
			log.info("sendRequest-async:"); //$NON-NLS-1$
			this.interactiveExecutor.execute(() -> {
				try {
					sendRequest();
				} catch (Exception ex) {
					log.error("Exception executing sendRequest", ex); //$NON-NLS-1$
				}
			});
		} else {
			log.info("sendRequest-sync:"); //$NON-NLS-1$
			sendRequest();
		}
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
//			.append(", proxyHost:").append(this.proxyHost) //$NON-NLS-1$
//			.append(", proxyPort:").append(this.proxyPort) //$NON-NLS-1$
//			.append(", proxyUid:").append(this.proxyUid) //$NON-NLS-1$
//			.append(", proxyPwd:").append(this.proxyPwd) //$NON-NLS-1$
			.append(", url:").append(this.url) //$NON-NLS-1$
			.append(", resultingUrl:").append(this.resultingUrl) //$NON-NLS-1$
			.append(", checkResults:").append(this.checkResults) //$NON-NLS-1$
			.append(", resultChecker:").append(this.resultChecker) //$NON-NLS-1$
			.append(", timestamp:").append(this.timestamp) //$NON-NLS-1$
			.append(", duration:").append(this.duration) //$NON-NLS-1$
			.append("}") //$NON-NLS-1$
			.toString();
	}

}
