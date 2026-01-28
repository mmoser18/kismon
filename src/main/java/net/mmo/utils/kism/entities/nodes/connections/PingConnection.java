/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.entities.nodes.connections;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("javadoc")
@Setter
@Getter
public class PingConnection extends IPConnection
{
	private static final long serialVersionUID = 643208328101928988L;

	public final static String PROPERTYNAME_RESPONSE  = "ping_response";  //$NON-NLS-1$

	/**
	 * Responses are assumed to look like:
	 * Request timed out.
	 * Reply from 142.250.203.100: bytes=32 time=19ms TTL=118
	 * Reply from ::1: time<1ms
	 *
	 */
	@JsonIgnore
	private final static String PingHostNotFound = "Ping request could not find host"; //$NON-NLS-1$
	@JsonIgnore
	private final static String PingTimedOut = "Request timed out."; //$NON-NLS-1$
	@JsonIgnore
	private final static String PingSuccessIPv4  = "Reply from \\d{1,3}(\\.\\d{1,3}){3}: (bytes=\\d{1,4} )?time(=\\d{1,5}|<1)ms( TTL=\\d{1,3})?"; //$NON-NLS-1$\
	private final static int MatchGroupIPv4 = 3;	//                 1           1     2               2     3            3  4             4
	@JsonIgnore
	private final static String PingSuccessIPv6  = "Reply from ([\\dA-Fa-f]){0,4}(:([\\dA-Fa-f]){0,4}){0,7}: (bytes=\\d{1,4} )?time(=\\d{1,5}|<1)ms( TTL=\\d{1,3})?"; //$NON-NLS-1$
	private final static int MatchGroupIPv6 = 5;	//         1           1     2 3           3     2       4               4     5            5  6             6

	private static Pattern patternHostNotFound = Pattern.compile(PingHostNotFound);
	private static Pattern patternTimedOut = Pattern.compile(PingTimedOut);
	private static Pattern patternSuccessIPv4 = Pattern.compile(PingSuccessIPv4);
	private static Pattern patternSuccessIPv6 = Pattern.compile(PingSuccessIPv6);

	@JsonIgnore
	protected transient String response;

	@JsonIgnore
	private final static boolean isWindows;
	static {
		isWindows = System.getProperty("os.name").toLowerCase().contains("win"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	@JsonIgnore
	private transient ProcessBuilder processBuilder;


	public PingConnection() { // required for deserialization and NodeFactory
		super();
	}
	public PingConnection(String name, String description) {
		super(name, description);
	}

	@Override
	public void setResultingHostAddress(String resultingHostAddress) {
		super.setResultingHostAddress(resultingHostAddress);
		setProcessBuilder(null); // trigger a recreation of the processBuilder
	}

	@Override
	public void setTimeout(int timeout) {
		super.setTimeout(timeout);
		setProcessBuilder(null); // trigger a recreation of the processBuilder
	}

	public void setResponse(String response) {
		if (!Objects.equals(this.response, response)) {
			String oldResponse = this.response;
			this.response = response;
			informOnPropertyChange(PROPERTYNAME_RESPONSE, oldResponse, response);
		}
	}

	/**
	 * This implementation is neither very elegant nor performant, but the issue is that Java does
	 * not provide access to ICMP. Its "<url>.isReachable() method uses ICMP only if running as admin
	 * (or su on *nix) but falls back to the port 7 echo service when not. Since the echo service is
	 * typically not supported this is not a reliable solution.
	 * The only solution seems to be to run the ping-command in a separate process, but this comes with
	 * considerable overhead and measured times are much, much higher than the actual ping-times.
	 * Work-around for the latter was be to parse the output of the ping command...
	 */
	@Override
	public void sendRequest() throws Exception {
		if (this.processBuilder == null) {
			this.processBuilder =
				new ProcessBuilder("ping", //$NON-NLS-1$
				                   (isWindows ? "-n" : "-c"), "1", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				                   "-w", Integer.toString(getTimeout() * 1000), // timeout in milliseconds //$NON-NLS-1$
				                   resultingHostAddressResolved());
			this.processBuilder.redirectErrorStream(true); // redirect error to input stream

		}
		// take timestamp:
		setTimestamp(LocalDateTime.now());
		// ... right before executing the actual request:
		long startTime = System.nanoTime();
		Process proc = this.processBuilder.start();
		if (proc.waitFor(getTimeout(), TimeUnit.SECONDS)) {
			String time = null;
			String details = null;
			try (InputStream is = new BufferedInputStream(proc.getInputStream())) {
				String res = new String(is.readAllBytes(), StandardCharsets.UTF_8);
				setResponse(res);
				this.log.trace("ping response is '{}'", res); //$NON-NLS-1$
				if (res.contains(PingTimedOut)) {
					setDuration(-1); // signal: "no response"
					setState(State.FAILED);
					details = PingTimedOut;
				} else {
					try {
						Matcher matcherIPv4 = patternSuccessIPv4.matcher(res);
						if (matcherIPv4.find()) {
							this.log.trace("matcherIPv4 matched."); //$NON-NLS-1$
							time = matcherIPv4.group(MatchGroupIPv4);
						} else {
							Matcher matcherIPv6 = patternSuccessIPv6.matcher(res);
							if (matcherIPv6.find()) {
								this.log.trace("matcherIPv6 matched."); //$NON-NLS-1$
								time = matcherIPv6.group(MatchGroupIPv6);
							} else {
								this.log.trace("no matcherIPx matched."); //$NON-NLS-1$
							}
						}
						if (time != null) {
							long timeValue = time.equals("<1") ? 1 : Long.parseLong(time.substring(1)); //$NON-NLS-1$
							setDurationMillis(timeValue);
							details = time + " msecs"; //$NON-NLS-1$
							setState(getDuration() > getTimeout() * 10e9  ? State.DEGRADED : State.OK);
						} else if (patternHostNotFound.matcher(res).find()) {
							details = "Host not found"; //$NON-NLS-1$
							this.log.debug("{}: '{}'", details, res); //$NON-NLS-1$
							setDuration(0); // signal: "unknown response"
							setState(State.FAILED);
						} else if (patternTimedOut.matcher(res).find()) {
							details = "Ping timed out"; //$NON-NLS-1$
							this.log.debug("{}: '{}'", details, res); //$NON-NLS-1$
							setDuration(0); // signal: "unknown response"
							setState(State.FAILED);
						} else { // time not found in response - we take the entire process' duration:
							details = "No time found in response"; //$NON-NLS-1$
							this.log.debug("{}: '{}'", details, res); //$NON-NLS-1$
							setDuration(System.nanoTime() - startTime);
							setState(State.DEGRADED); // degraded since we didn't find the time in the response...
						}
					} catch (Exception ex) {
						this.log.error("Exception analyzing/converting response '" + res + "'", ex); //$NON-NLS-1$ //$NON-NLS-2$
						setDuration(0); // signal: "unknown response"
						setState(State.FAILED);
					}
					if (details == null) {
						details = "unexpected response: '" + (res.length() > 200 ? res.substring(0,200) + "..." : res) + "'"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
				}
			}
			this.log.debug("ping to '{}': {}", getResultingHostAddress(), details); //$NON-NLS-1$
			setRequestResult(getState().name() + '/' + details);
		} else {
			this.log.debug("ping to '{}' timed out.", getResultingHostAddress()); //$NON-NLS-1$
			setDuration(-1);
			setState(State.FAILED);
			setRequestResult(getState().name() + "/timed out"); //$NON-NLS-1$
		}
		informOnPropertyChange(PROPERTYNAME_RESPONSE_COMPLETE, null, System.nanoTime());

	}
}
