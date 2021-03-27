/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.entities.nodes.connections;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import javax.persistence.Entity;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Entity
@Setter
@Getter
@Slf4j
public class PingConnection extends IPConnection
{
	private static final long serialVersionUID = 643208328101928988L;

	boolean isWindows;
	{
		this.isWindows = System.getProperty("os.name").toLowerCase().contains("win"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	transient ProcessBuilder processBuilder;


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

	/**
	 * This implementation is neither very elegant nor performant, but the issue is that Java does
	 * not provide access to ICMP. Its "<url>.isReachable() method uses ICMP only if running as admin
	 * (or su on *nix) but falls back to the port 7 echo service when not. Since the echo service is
	 * often not supported this is not a reliable solution.
	 * The only solution seems to be to run the ping-command in a separate process, but this comes with
	 * considerable overhead and measured times are much, much higher than the actual ping-times.
	 * Solution for the latter would be to parse the output of the ping command but I was too lazy for
	 * that. The point here is anyway only to check whether the system is responding, not how fast.
	 */
	@Override
	public void sendRequest() throws Exception {
		if (this.processBuilder == null) {
			this.processBuilder =
				new ProcessBuilder("ping", //$NON-NLS-1$
				                   (this.isWindows ? "-n" : "-c"), "1", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				                   "-w", Integer.toString(getTimeout() * 1000), // timeout in milliseconds //$NON-NLS-1$
				                   resultingHostAddressResolved());
		}
		// take timestamp:
		setTimestamp(LocalDateTime.now());
		// ... right before executing the actual request:
		long startTime = System.nanoTime();
		Process proc = this.processBuilder.start();
		if (proc.waitFor(getTimeout()+3, TimeUnit.SECONDS)) {
			long callDuration = System.nanoTime() - startTime;
			log.info("ping to '{}' responded in {} msecs", getResultingHostAddress(), callDuration / 1e6); //$NON-NLS-1$
			setState(State.OK);
			setDuration(callDuration);
			setRequestResult(getState().name() + '/' + (callDuration / 1e6)/*=nano-secs to milli-secs*/);
		} else {
			log.info("ping to '{}' timed out.", getResultingHostAddress()); //$NON-NLS-1$
			setState(State.FAILED);
			setDuration(-1);
			setRequestResult(getState().name() + "/timed out"); //$NON-NLS-1$
		}
	}
}
