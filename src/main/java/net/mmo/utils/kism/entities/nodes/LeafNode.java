/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 08.11.2020
 */

package net.mmo.utils.kism.entities.nodes;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.HistoryInfoService;
import net.mmo.utils.kism.entities.history.HistoryInfo;
import net.mmo.utils.kism.utils.AppProperties;
import net.mmo.utils.kism.utils.ExceptionUtils;

/**
 * Common superclass for leaf nodes.
 */
@SuppressWarnings("javadoc")
@Setter
@Getter
@Slf4j
abstract public class LeafNode extends ActionableNode
{
	private static final long serialVersionUID = 265720619758262963L;

	public final static String PROPERTYNAME_DURATION  = "duration";  //$NON-NLS-1$
	public final static String PROPERTYNAME_TIMESTAMP  = "timestamp";  //$NON-NLS-1$
	public final static String PROPERTYNAME_RESPONSE_COMPLETE    = "responseComplete"; //$NON-NLS-1$
	public final static int NO_RESPONSE_DURATION = 0; // used as duration if there was no response (timeout or other error)

	/**
	 * Should this node be included in the parent condition evaluation?
	 */
	protected boolean active = false;

	/**
	 * Number of seconds every so often this connection should be checked.
	 */
	@Min(0) // 0 means off
	@Max(3600) // means: 1x/hr
	protected int period = 60;

	/**
	 * Number of seconds after which connection setup is considered as failed
	 */
	@Min(0) // 0 means off
	@Max(120) // 2 minutes
	protected int timeout = 15;

	/** common response fields: */

	@JsonIgnore
	protected transient long duration; // in nano-seconds

	//	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	//	@JsonSerialize(using = LocalDateTimeSerializer.class)
	//	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
	@JsonIgnore
	protected transient LocalDateTime timestamp;

	// TODO: the below should be kept into some kind of "history"-list:
	//	transient protected history = new ArrayList();

	// transient stuff:
	@JsonIgnore
	transient protected String requestResult;  // used for history entry

	/**
	 * This allows to halt ALL automatic requests. This can be useful e.g. while doing changes to avoid
	 * being locked out of an account by too many failed attempts.
	 */
	@JsonIgnore
	private static boolean haltAllRequests =
		Boolean.parseBoolean(AppProperties.getProperties().getProperty("StartupWithHaltedRequests", //$NON-NLS-1$
		                                                               "false")); //$NON-NLS-1$;

	@JsonIgnore
	protected transient Runnable requestSender; // runs the automatic requests
	@JsonIgnore
	protected transient ScheduledExecutorService periodicExecutor; // for periodic/automatic execution
	@JsonIgnore
	protected transient ScheduledFuture<?> future;
	@JsonIgnore
	protected transient ExecutorService interactiveExecutor; // for interactive execution (via GUI)


	@JsonIgnore
	public transient static boolean asyncRequests =
		Boolean.parseBoolean(AppProperties.getProperties().getProperty("LeafNode.AsyncRequests", //$NON-NLS-1$
		                                                               "true")); //$NON-NLS-1$

	@JsonIgnore
	public transient static boolean shortRequestLogEntries =
		Boolean.parseBoolean(AppProperties.getProperties().getProperty("LeafNode.ShortRequestLogEntries", //$NON-NLS-1$
		                                                               "true")); //$NON-NLS-1$

	public transient static HistoryInfoService requestInfoService;


	public static boolean isHaltAllRequests() {
		return LeafNode.haltAllRequests;
	}
	public static void setHaltAllRequests(boolean haltAllRequests) {
		LeafNode.haltAllRequests = haltAllRequests;
		log.info("All requests are now " + (LeafNode.haltAllRequests ? "halted" : "enabled")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/** required for deserialization only */
	protected LeafNode() {
		initExecutors();
	}

	public LeafNode(String name, String description) {
		super(name, description);
		initExecutors();
	}
	// required for Unit-Tests:
	public LeafNode(String name, String description, State state) {
		super(name, description, state);
		initExecutors();
	}
	// required for Unit-Tests:
	public LeafNode(String name, String description, State state, IntermediateNode parent) {
		super(name, description, state, parent);
		initExecutors();
	}

	@Override
	public void postClone(Node original) {
		super.postClone(original);
		initExecutors();
	}

	public void setActive(boolean active) {
		if (this.active != active) {
			this.active = active;
			adjustExecutor();
		}
	}

	public void setPeriod(int period) {
		if (this.period != period) { // we are changing the period
			this.period = period;
			adjustExecutor(); // restart (if applicable)
		}
	}

	public void setTimeout(int timeout) {
		if (this.timeout != timeout) { // we are changing the timeout
			this.timeout = timeout;
			adjustExecutor(); // restart (if applicable)
		}
	}

	private void initExecutors() {
		setRequestSender(new Runnable()
			{ // this is the code being executed by the automatic executors
				@Override
				public void run() {
					if (isHaltAllRequests()) {
						log.trace("Requests halted for '{}':", getName()); //$NON-NLS-1$
					} else {
						log.debug("Calling sendRequest() for '{}':", getName()); //$NON-NLS-1$
						executeRequestInternal("calling"); //$NON-NLS-1$
					}
				}
			});
		if (asyncRequests) {
			setInteractiveExecutor(Executors.newSingleThreadExecutor());
		}
	}

	abstract public void sendRequest() throws Exception;

	/**
	 * Note that this allows to execute individual requests even when they are globally halted!
	 */
	@Override
	public void executeRequest() throws Exception {
		if (asyncRequests) {
			this.interactiveExecutor.execute(() -> {
				log.info("executing sendRequest(async): '{}'", getName()); //$NON-NLS-1$
				executeRequestInternal("executing(async)"); //$NON-NLS-1$
			});
		} else {
			log.info("executing sendRequest(sync):  '{}'", getName()); //$NON-NLS-1$
			executeRequestInternal("executing(sync)"); //$NON-NLS-1$
		}
	}

	public void executeRequestInternal(String logFragment) {
		String resultMsg;
		try {
			sendRequest();
			resultMsg = getRequestResult();
		} catch (Exception ex) {
			if (shortRequestLogEntries) {
				if (log.isInfoEnabled()) { // debug since info was still too verbose / note the info() below is on purpose!
					log.info("Error {} '{}': {}", logFragment, getName(), ExceptionUtils.exceptionCauseSummary(ex)); //$NON-NLS-1$
				}
			} else {
				log.info("Error " + logFragment + " '" + getName() + "':", ex); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			setDuration(NO_RESPONSE_DURATION); // signals an exception
			resultMsg = getRequestResult();
			if (resultMsg == null) resultMsg = ExceptionUtils.exceptionRootCauseMsg(ex);
		}
		requestInfoService.save(new HistoryInfo(getName(), getClass(), getDuration(), getTimestamp(), resultMsg));
	}

	protected void adjustExecutor() {
//		// Because this method gets called from the UI and it may take a while we need to offload
//		// that to another thread to complete it in the background: / didn't work... :-(
//		UIHandlerSupport.executeLater(this::adjustExecutorInternal);
//	}
//
//	protected void adjustExecutorInternal() {
		if (getPeriodicExecutor() != null) {
			log.debug("stopping periodicExecutor for {}:", getName()); //$NON-NLS-1$
			if (!getPeriodicExecutor().isTerminated()) {
				getPeriodicExecutor().shutdown();
				try {
					getPeriodicExecutor().awaitTermination(3, TimeUnit.SECONDS);
				} catch (InterruptedException ex) {
					log.error("Error shutting down '" + getName() + '"' , ex); //$NON-NLS-1$
				}
			}
			setPeriodicExecutor(null);
		}
		if (getFuture() != null) {
			getFuture().cancel(true);
			setFuture(null);
		}

		if (isActive()) { // we (re)start - possibly with a different period and/or timeout:
			log.debug("starting periodicExecutor for '{}' (period: {} secs, timeout: {} secs):", getName(), getPeriod(), getTimeout()); //$NON-NLS-1$
			setPeriodicExecutor(Executors.newScheduledThreadPool(1));
			setFuture(getPeriodicExecutor().scheduleAtFixedRate(getRequestSender(), getPeriod(), getPeriod(), TimeUnit.SECONDS));
		}
	}

	/**
	 * Set duration in nano-seconds
	 * @param duration
	 */
	public void setDuration(long duration) {
		if (this.duration != duration) {
			long oldDuration = this.duration;
			this.duration = duration;
			informOnPropertyChange(PROPERTYNAME_DURATION, oldDuration, duration);
		}
	}

	public void setDurationMillis(long duration) {
		setDuration(duration * 1000000); // convert msecs --> nano-secs);
	}

	public void setTimestamp(LocalDateTime timestamp) {
		if (!Objects.equals(this.timestamp, timestamp)) {
			LocalDateTime oldTimestamp = this.timestamp;
			this.timestamp = timestamp;
			informOnPropertyChange(PROPERTYNAME_TIMESTAMP, oldTimestamp, timestamp);
		}
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(super.toString());
		buf.setLength(buf.length()-1);
		return buf
			.append(", active:").append(this.active) //$NON-NLS-1$
			.append(", period:").append(this.period) //$NON-NLS-1$
			.append(", timeout:").append(this.timeout) //$NON-NLS-1$
			.append(", timestamp:").append(this.timestamp) //$NON-NLS-1$
			.append(", duration:").append(this.duration) //$NON-NLS-1$
			.append("}") //$NON-NLS-1$
			.toString();
	}
}
